# config.py
import os
from pathlib import Path

class PipelineConfig:
    def __init__(self):
        # Environment-based paths with defaults
        self.JENKINS_HOME = Path(os.getenv('JENKINS_HOME', '/var/lib/jenkins'))
        self.PIPELINE_CONFIG_ROOT = Path(os.getenv('PIPELINE_CONFIG_ROOT', './pipeline_configs'))
        
        # Derived paths
        self.PIPELINE_DEFINITIONS = self.PIPELINE_CONFIG_ROOT / 'definitions'
        self.PIPELINE_TEMPLATES = self.PIPELINE_CONFIG_ROOT / 'templates'
        self.SHARED_LIBRARIES = self.PIPELINE_CONFIG_ROOT / 'shared_libs'
        
        # Jenkins workspace paths
        self.WORKSPACE_ROOT = self.JENKINS_HOME / 'workspace'
        self.JOBS_CONFIG = self.JENKINS_HOME / 'jobs'
        
        # Create directories if they don't exist
        self._ensure_directories()
    
    def _ensure_directories(self):
        for path in [self.PIPELINE_DEFINITIONS, self.PIPELINE_TEMPLATES, self.SHARED_LIBRARIES]:
            path.mkdir(parents=True, exist_ok=True)
    
    def get_pipeline_config_path(self, pipeline_name):
        return self.PIPELINE_DEFINITIONS / f"{pipeline_name}.yaml"
    
    def get_template_path(self, template_name):
        return self.PIPELINE_TEMPLATES / f"{template_name}.j2"

# Usage
config = PipelineConfig()