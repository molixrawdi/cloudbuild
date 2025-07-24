#!/usr/bin/env python3
"""
Python-based Jenkins Pipeline Generator
Generates Groovy pipeline code from Python configuration
"""

from dataclasses import dataclass
from typing import List, Dict, Optional
import json

@dataclass
class Stage:
    name: str
    steps: List[str]
    when: Optional[str] = None
    parallel: bool = False

@dataclass
class PipelineConfig:
    agent: str = "any"
    parameters: List[Dict] = None
    environment: Dict[str, str] = None
    stages: List[Stage] = None
    post_actions: Dict[str, List[str]] = None

class JenkinsPipelineBuilder:
    def __init__(self, config: PipelineConfig):
        self.config = config
    
    def generate_parameters(self) -> str:
        if not self.config.parameters:
            return ""
        
        params = []
        for param in self.config.parameters:
            if param['type'] == 'choice':
                params.append(f"""choice(
            name: '{param['name']}',
            choices: {param['options']},
            description: '{param.get('description', '')}'
        )""")
            elif param['type'] == 'string':
                params.append(f"""string(
            name: '{param['name']}',
            defaultValue: '{param.get('default', '')}',
            description: '{param.get('description', '')}'
        )""")
        
        return f"""    parameters {{
{chr(10).join(params)}
    }}"""
    
    def generate_environment(self) -> str:
        if not self.config.environment:
            return ""
        
        env_vars = [f'        {k} = "{v}"' for k, v in self.config.environment.items()]
        return f"""    environment {{
{chr(10).join(env_vars)}
    }}"""
    
    def generate_stage(self, stage: Stage) -> str:
        steps_code = "\n".join([f"                {step}" for step in stage.steps])
        
        when_clause = ""
        if stage.when:
            when_clause = f"""            when {{
                {stage.when}
            }}"""
        
        return f"""        stage('{stage.name}') {{
{when_clause}
            steps {{
{steps_code}
            }}
        }}"""
    
    def generate_stages(self) -> str:
        if not self.config.stages:
            return ""
        
        stages_code = []
        for stage in self.config.stages:
            stages_code.append(self.generate_stage(stage))
        
        return f"""    stages {{
{chr(10).join(stages_code)}
    }}"""
    
    def generate_post(self) -> str:
        if not self.config.post_actions:
            return ""
        
        post_blocks = []
        for condition, actions in self.config.post_actions.items():
            action_code = "\n".join([f"            {action}" for action in actions])
            post_blocks.append(f"""        {condition} {{
{action_code}
        }}""")
        
        return f"""    post {{
{chr(10).join(post_blocks)}
    }}"""
    
    def build(self) -> str:
        pipeline_parts = [
            f"pipeline {{",
            f"    agent {self.config.agent}",
            self.generate_parameters(),
            self.generate_environment(),
            self.generate_stages(),
            self.generate_post(),
            "}"
        ]
        
        # Filter out empty parts
        pipeline_parts = [part for part in pipeline_parts if part.strip()]
        return "\n\n".join(pipeline_parts)

# Example usage
def create_flask_pipeline():
    config = PipelineConfig(
        agent="any",
        parameters=[
            {
                'type': 'choice',
                'name': 'PYTHON_VERSION',
                'options': ['3.9', '3.10', '3.11', '3.12'],
                'description': 'Select Python version'
            },
            {
                'type': 'string',
                'name': 'DOCKER_TAG',
                'default': 'latest',
                'description': 'Docker image tag'
            }
        ],
        environment={
            'DOCKER_IMAGE': 'flask-app',
            'REGISTRY': 'your-registry.com'
        },
        stages=[
            Stage(
                name="Checkout",
                steps=["checkout scm"]
            ),
            Stage(
                name="Build",
                steps=[
                    '''script {
                    def imageTag = "${env.DOCKER_IMAGE}:${params.PYTHON_VERSION}-${params.DOCKER_TAG}"
                    sh "docker build --build-arg PYTHON_VERSION=${params.PYTHON_VERSION} -t ${imageTag} ."
                    env.BUILT_IMAGE = imageTag
                }'''
                ]
            ),
            Stage(
                name="Test",
                steps=[
                    'sh "docker run --rm ${env.BUILT_IMAGE} python -m pytest tests/ -v"'
                ]
            ),
            Stage(
                name="Deploy",
                steps=[
                    'sh "docker push ${env.BUILT_IMAGE}"'
                ],
                when="branch 'main'"
            )
        ],
        post_actions={
            'always': [
                'sh "docker image prune -f"'
            ],
            'success': [
                'echo "Build successful!"'
            ],
            'failure': [
                'echo "Build failed!"'
            ]
        }
    )
    
    builder = JenkinsPipelineBuilder(config)
    return builder.build()

if __name__ == "__main__":
    pipeline_code = create_flask_pipeline()
    print(pipeline_code)
    
    # Save to Jenkinsfile
    with open("Jenkinsfile", "w") as f:
        f.write(pipeline_code)