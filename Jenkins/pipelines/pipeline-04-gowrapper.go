package main

import (
	"fmt"
	"os"
	"strings"
)

type Parameter struct {
	Type        string   `json:"type"`
	Name        string   `json:"name"`
	Options     []string `json:"options,omitempty"`
	Default     string   `json:"default,omitempty"`
	Description string   `json:"description,omitempty"`
}

type Stage struct {
	Name     string   `json:"name"`
	Steps    []string `json:"steps"`
	When     string   `json:"when,omitempty"`
	Parallel bool     `json:"parallel,omitempty"`
}

type PipelineConfig struct {
	Agent       string            `json:"agent"`
	Parameters  []Parameter       `json:"parameters,omitempty"`
	Environment map[string]string `json:"environment,omitempty"`
	Stages      []Stage           `json:"stages,omitempty"`
	PostActions map[string][]string `json:"post_actions,omitempty"`
}

type JenkinsPipelineBuilder struct {
	config PipelineConfig
}

func NewJenkinsPipelineBuilder(config PipelineConfig) *JenkinsPipelineBuilder {
	return &JenkinsPipelineBuilder{config: config}
}

func (j *JenkinsPipelineBuilder) generateParameters() string {
	if len(j.config.Parameters) == 0 {
		return ""
	}

	var params []string
	for _, param := range j.config.Parameters {
		switch param.Type {
		case "choice":
			optionsStr := fmt.Sprintf("['%s']", strings.Join(param.Options, "', '"))
			params = append(params, fmt.Sprintf(`        choice(
            name: '%s',
            choices: %s,
            description: '%s'
        )`, param.Name, optionsStr, param.Description))
		case "string":
			params = append(params, fmt.Sprintf(`        string(
            name: '%s',
            defaultValue: '%s',
            description: '%s'
        )`, param.Name, param.Default, param.Description))
		}
	}

	return fmt.Sprintf("    parameters {\n%s\n    }", strings.Join(params, "\n"))
}

func (j *JenkinsPipelineBuilder) generateEnvironment() string {
	if len(j.config.Environment) == 0 {
		return ""
	}

	var envVars []string
	for k, v := range j.config.Environment {
		envVars = append(envVars, fmt.Sprintf("        %s = \"%s\"", k, v))
	}

	return fmt.Sprintf("    environment {\n%s\n    }", strings.Join(envVars, "\n"))
}

func (j *JenkinsPipelineBuilder) generateStage(stage Stage) string {
	stepsCode := make([]string, len(stage.Steps))
	for i, step := range stage.Steps {
		stepsCode[i] = fmt.Sprintf("                %s", step)
	}

	whenClause := ""
	if stage.When != "" {
		whenClause = fmt.Sprintf(`            when {
                %s
            }`, stage.When)
	}

	stageCode := fmt.Sprintf(`        stage('%s') {
%s
            steps {
%s
            }
        }`, stage.Name, whenClause, strings.Join(stepsCode, "\n"))

	return stageCode
}

func (j *JenkinsPipelineBuilder) generateStages() string {
	if len(j.config.Stages) == 0 {
		return ""
	}

	var stagesCode []string
	for _, stage := range j.config.Stages {
		stagesCode = append(stagesCode, j.generateStage(stage))
	}

	return fmt.Sprintf("    stages {\n%s\n    }", strings.Join(stagesCode, "\n"))
}

func (j *JenkinsPipelineBuilder) generatePost() string {
	if len(j.config.PostActions) == 0 {
		return ""
	}

	var postBlocks []string
	for condition, actions := range j.config.PostActions {
		actionCode := make([]string, len(actions))
		for i, action := range actions {
			actionCode[i] = fmt.Sprintf("            %s", action)
		}
		postBlocks = append(postBlocks, fmt.Sprintf(`        %s {
%s
        }`, condition, strings.Join(actionCode, "\n")))
	}

	return fmt.Sprintf("    post {\n%s\n    }", strings.Join(postBlocks, "\n"))
}

func (j *JenkinsPipelineBuilder) Build() string {
	var pipelineParts []string

	pipelineParts = append(pipelineParts, "pipeline {")
	pipelineParts = append(pipelineParts, fmt.Sprintf("    agent %s", j.config.Agent))

	if params := j.generateParameters(); params != "" {
		pipelineParts = append(pipelineParts, params)
	}

	if env := j.generateEnvironment(); env != "" {
		pipelineParts = append(pipelineParts, env)
	}

	if stages := j.generateStages(); stages != "" {
		pipelineParts = append(pipelineParts, stages)
	}

	if post := j.generatePost(); post != "" {
		pipelineParts = append(pipelineParts, post)
	}

	pipelineParts = append(pipelineParts, "}")

	return strings.Join(pipelineParts, "\n\n")
}

func createFlaskPipeline() PipelineConfig {
	return PipelineConfig{
		Agent: "any",
		Parameters: []Parameter{
			{
				Type:        "choice",
				Name:        "PYTHON_VERSION",
				Options:     []string{"3.9", "3.10", "3.11", "3.12"},
				Description: "Select Python version",
			},
			{
				Type:        "string",
				Name:        "DOCKER_TAG",
				Default:     "latest",
				Description: "Docker image tag",
			},
		},
		Environment: map[string]string{
			"DOCKER_IMAGE": "flask-app",
			"REGISTRY":     "your-registry.com",
		},
		Stages: []Stage{
			{
				Name:  "Checkout",
				Steps: []string{"checkout scm"},
			},
			{
				Name: "Build",
				Steps: []string{
					`script {
                    def imageTag = "${env.DOCKER_IMAGE}:${params.PYTHON_VERSION}-${params.DOCKER_TAG}"
                    sh "docker build --build-arg PYTHON_VERSION=${params.PYTHON_VERSION} -t ${imageTag} ."
                    env.BUILT_IMAGE = imageTag
                }`,
				},
			},
			{
				Name: "Test",
				Steps: []string{
					`sh "docker run --rm ${env.BUILT_IMAGE} python -m pytest tests/ -v"`,
				},
			},
			{
				Name: "Deploy",
				Steps: []string{
					`sh "docker push ${env.BUILT_IMAGE}"`,
				},
				When: "branch 'main'",
			},
		},
		PostActions: map[string][]string{
			"always": {
				`sh "docker image prune -f"`,
			},
			"success": {
				`echo "Build successful!"`,
			},
			"failure": {
				`echo "Build failed!"`,
			},
		},
	}
}

func main() {
	config := createFlaskPipeline()
	builder := NewJenkinsPipelineBuilder(config)
	pipelineCode := builder.Build()

	fmt.Println(pipelineCode)

	// Save to Jenkinsfile
	err := os.WriteFile("Jenkinsfile", []byte(pipelineCode), 0644)
	if err != nil {
		fmt.Printf("Error writing Jenkinsfile: %v\n", err)
	} else {
		fmt.Println("\nJenkinsfile generated successfully!")
	}
}