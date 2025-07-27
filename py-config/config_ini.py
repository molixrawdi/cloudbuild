# config.py
import configparser
import os
from pathlib import Path

def load_pipeline_config():
    config = configparser.ConfigParser()
    config.read('pipeline_config.ini')
    
    return {
        'pipeline_dir': Path(config.get('paths', 'pipeline_dir', fallback='./pipelines')),
        'config_dir': Path(config.get('paths', 'config_dir', fallback='./configs')),
        'templates_dir': Path(config.get('paths', 'templates_dir', fallback='./templates')),
        'logs_dir': Path(config.get('paths', 'logs_dir', fallback='./logs'))
    }

# pipeline_config.ini
"""
[paths]
pipeline_dir = /opt/jenkins/pipelines
config_dir = /opt/jenkins/configs
templates_dir = /opt/jenkins/templates
logs_dir = /var/log/jenkins/pipelines
"""