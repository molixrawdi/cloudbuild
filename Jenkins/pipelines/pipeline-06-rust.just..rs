// Cargo.toml
// [package]
// name = "pipeline-runner"
// version = "0.1.0"
// edition = "2021"
// 
// [dependencies]
// tokio = { version = "1.0", features = ["full"] }
// serde = { version = "1.0", features = ["derive"] }
// serde_json = "1.0"
// clap = { version = "4.0", features = ["derive"] }

use clap::{Parser, Subcommand};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::process::Command;
use tokio::process::Command as TokioCommand;

#[derive(Parser)]
#[command(author, version, about, long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    Build {
        #[arg(short, long, default_value = "3.11")]
        python_version: String,
        #[arg(short, long, default_value = "python")]
        base_image: String,
        #[arg(short, long, default_value = "latest")]
        tag: String,
    },
    Test {
        #[arg(short, long)]
        image: String,
    },
    Deploy {
        #[arg(short, long)]
        image: String,
        #[arg(short, long, default_value = "staging")]
        environment: String,
    },
    Pipeline {
        #[arg(short, long)]
        config: String,
    },
}

#[derive(Serialize, Deserialize, Debug)]
struct PipelineConfig {
    name: String,
    python_versions: Vec<String>,
    base_images: Vec<String>,
    environments: HashMap<String, Environment>,
    stages: Vec<Stage>,
}

#[derive(Serialize, Deserialize, Debug)]
struct Environment {
    name: String,
    registry: String,
    deployment_target: String,
}

#[derive(Serialize, Deserialize, Debug)]
struct Stage {
    name: String,
    commands: Vec<String>,
    parallel: Option<bool>,
    when: Option<String>,
}

struct PipelineRunner {
    config: PipelineConfig,
}

impl PipelineRunner {
    fn new(config: PipelineConfig) -> Self {
        Self { config }
    }

    async fn run_command(&self, cmd: &str) -> Result<String, Box<dyn std::error::Error>> {
        println!("🚀 Running: {}", cmd);
        
        let output = if cfg!(target_os = "windows") {
            TokioCommand::new("cmd")
                .args(["/C", cmd])
                .output()
                .await?
        } else {
            TokioCommand::new("sh")
                .args(["-c", cmd])
                .output()
                .await?
        };

        if output.status.success() {
            let stdout = String::from_utf8_lossy(&output.stdout);
            println!("✅ Success: {}", stdout);
            Ok(stdout.to_string())
        } else {
            let stderr = String::from_utf8_lossy(&output.stderr);
            println!("❌ Error: {}", stderr);
            Err(format!("Command failed: {}", stderr).into())
        }
    }

    async fn build_image(&self, python_version: &str, base_image: &str, tag: &str) -> Result<String, Box<dyn std::error::Error>> {
        let image_name = format!("flask-app:{}_{}-{}", python_version, base_image, tag);
        
        // Generate Dockerfile
        let dockerfile_content = format!(
            r#"FROM {}:{}

WORKDIR /app

# Install system dependencies based on base image
{}

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["flask", "run", "--host=0.0.0.0"]
"#,
            base_image,
            python_version,
            if base_image.contains("alpine") {
                "RUN apk add --no-cache gcc musl-dev linux-headers"
            } else {
                "RUN apt-get update && apt-get install -y --no-install-recommends gcc && rm -rf /var/lib/apt/lists/*"
            }
        );

        tokio::fs::write("Dockerfile.generated", dockerfile_content).await?;

        let build_cmd = format!("docker build -f Dockerfile.generated -t {} .", image_name);
        self.run_command(&build_cmd).await?;

        Ok(image_name)
    }

    async fn test_image(&self, image: &str) -> Result<(), Box<dyn std::error::Error>> {
        let test_cmd = format!("docker run --rm {} python -m pytest tests/ -v", image);
        self.run_command(&test_cmd).await?;

        // Security scan
        let security_cmd = format!(
            "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:latest image {}",
            image
        );
        self.run_command(&security_cmd).await?;

        Ok(())
    }

    async fn deploy_image(&self, image: &str, environment: &str) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(env_config) = self.config.environments.get(environment) {
            println!("🚀 Deploying {} to {}", image, environment);

            // Push to registry
            let registry_image = format!("{}/{}", env_config.registry, image);
            let tag_cmd = format!("docker tag {} {}", image, registry_image);
            self.run_command(&tag_cmd).await?;

            let push_cmd = format!("docker push {}", registry_image);
            self.run_command(&push_cmd).await?;

            // Deploy to target
            let deploy_cmd = format!(
                "kubectl set image deployment/flask-app flask-app={} --namespace={}",
                registry_image, environment
            );
            self.run_command(&deploy_cmd).await?;

            println!("✅ Deployment to {} completed", environment);
        } else {
            return Err(format!("Environment {} not found in config", environment).into());
        }

        Ok(())
    }

    async fn run_pipeline(&self) -> Result<(), Box<dyn std::error::Error>> {
        println!("🎯 Starting pipeline: {}", self.config.name);

        // Build matrix
        for python_version in &self.config.python_versions {
            for base_image in &self.config.base_images {
                println!("🔨 Building with Python {} on {}", python_version, base_image);

                let image = self.build_image(python_version, base_image, "pipeline").await?;
                self.test_image(&image).await?;

                // Deploy based on version (example logic)
                let environment = if python_version == "3.11" { "production" } else { "staging" };
                if self.config.environments.contains_key(environment) {
                    self.deploy_image(&image, environment).await?;
                }
            }
        }

        println!("🎉 Pipeline completed successfully!");
        Ok(())
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    match &cli.command {
        Commands::Build { python_version, base_image, tag } => {
            let config = PipelineConfig {
                name: "manual-build".to_string(),
                python_versions: vec![],
                base_images: vec![],
                environments: HashMap::new(),
                stages: vec![],
            };
            let runner = PipelineRunner::new(config);
            let image = runner.build_image(python_version, base_image, tag).await?;
            println!("✅ Built image: {}", image);
        }
        Commands::Test { image } => {
            let config = PipelineConfig {
                name: "manual-test".to_string(),
                python_versions: vec![],
                base_images: vec![],
                environments: HashMap::new(),
                stages: vec![],
            };
            let runner = PipelineRunner::new(config);
            runner.test_image(image).await?;
        }
        Commands::Deploy { image, environment } => {
            let config = PipelineConfig {
                name: "manual-deploy".to_string(),
                python_versions: vec![],
                base_images: vec![],
                environments: HashMap::new(),
                stages: vec![],
            };
            let runner = PipelineRunner::new(config);
            runner.deploy_image(image, environment).await?;
        }
        Commands::Pipeline { config } => {