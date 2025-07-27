# config.py
import os
from pathlib import Path
from typing import Optional

class JenkinsPipelineConfig:
    def __init__(self, base_path: Optional[str] = None):
        self.base_path = Path(base_path or os.getenv('PIPELINE_BASE_PATH', './'))
        self.jenkins_home = Path(os.getenv('JENKINS_HOME', '/var/lib/jenkins'))
    
    @property
    def pipeline_configs_dir(self) -> Path:
        return self.base_path / 'configs'
    
    @property
    def pipeline_templates_dir(self) -> Path:
        return self.base_path / 'templates'
    
    @property
    def shared_libraries_dir(self) -> Path:
        return self.base_path / 'shared_libs'
    
    @property
    def pipeline_scripts_dir(self) -> Path:
        return self.base_path / 'scripts'
    
    def get_config_file(self, config_name: str) -> Path:
        return self.pipeline_configs_dir / f"{config_name}.yaml"
    
    def get_template_file(self, template_name: str) -> Path:
        return self.pipeline_templates_dir / f"{template_name}.j2"

# Usage
config = JenkinsPipelineConfig('/opt/jenkins/pipeline_workspace')
pipeline_config = config.get_config_file('my_pipeline')