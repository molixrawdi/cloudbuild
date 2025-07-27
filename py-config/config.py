# config.py
import os
from pathlib import Path

# Base paths
BASE_DIR = Path(__file__).parent.absolute()
JENKINS_HOME = os.environ.get('JENKINS_HOME', '/var/lib/jenkins')

# Pipeline configuration paths
PIPELINE_CONFIG_DIR = BASE_DIR / 'pipeline_configs'
PIPELINE_TEMPLATES_DIR = BASE_DIR / 'templates'
PIPELINE_SCRIPTS_DIR = BASE_DIR / 'scripts'

# Specific config files
PIPELINE_CONFIG_FILE = PIPELINE_CONFIG_DIR / 'pipeline.yaml'
SHARED_LIBRARY_CONFIG = PIPELINE_CONFIG_DIR / 'shared_libraries.json'
ENVIRONMENT_CONFIG = PIPELINE_CONFIG_DIR / 'environments.yaml'

# Jenkins-specific paths
JOBS_DIR = Path(JENKINS_HOME) / 'jobs'
WORKSPACE_DIR = Path(JENKINS_HOME) / 'workspace'
PLUGINS_DIR = Path(JENKINS_HOME) / 'plugins'