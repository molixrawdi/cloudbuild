# app.py - Sample application code to be tested
import os
import json
import requests
from typing import List, Dict, Any

class Calculator:
    """Simple calculator class for demonstration"""
    
    def add(self, a: float, b: float) -> float:
        """Add two numbers"""
        return a + b
    
    def subtract(self, a: float, b: float) -> float:
        """Subtract two numbers"""
        return a - b
    
    def multiply(self, a: float, b: float) -> float:
        """Multiply two numbers"""
        return a * b
    
    def divide(self, a: float, b: float) -> float:
        """Divide two numbers"""
        if b == 0:
            raise ValueError("Cannot divide by zero")
        return a / b

class DataProcessor:
    """Data processing utilities"""
    
    def filter_by_city(self, data: List[Dict[str, Any]], city: str) -> List[Dict[str, Any]]:
        """Filter data by city"""
        return [item for item in data if item.get('city') == city]
    
    def calculate_average_age(self, data: List[Dict[str, Any]]) -> float:
        """Calculate average age from data"""
        if not data:
            raise ValueError("Cannot calculate average of empty data")
        
        ages = [item.get('age', 0) for item in data]
        return sum(ages) / len(ages)

class APIClient:
    """API client for external services"""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
    
    def get_user(self, user_id: int) -> Dict[str, Any]:
        """Get user by ID"""
        response = requests.get(f"{self.base_url}/users/{user_id}")
        
        if response.status_code == 404:
            raise Exception("User not found")
        elif response.status_code != 200:
            raise Exception(f"API error: {response.status_code}")
        
        return response.json()
    
    def create_user(self, user_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create a new user"""
        response = requests.post(f"{self.base_url}/users", json=user_data)
        
        if response.status_code != 201:
            raise Exception(f"Failed to create user: {response.status_code}")
        
        return response.json()

def get_database_url() -> str:
    """Get database URL from environment variables"""
    db_url = os.getenv('DATABASE_URL')
    if not db_url:
        raise ValueError("DATABASE_URL environment variable is required")
    return db_url

def read_json_file(filename: str) -> Dict[str, Any]:
    """Read and parse JSON file"""
    try:
        with open(filename, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        raise FileNotFoundError(f"File {filename} not found")
    except json.JSONDecodeError:
        raise ValueError(f"Invalid JSON in file {filename}")

if __name__ == "__main__":
    # Example usage
    calc = Calculator()
    print(f"2 + 3 = {calc.add(2, 3)}")
    
    processor = DataProcessor()
    sample_data = [
        {"name": "Alice", "age": 30, "city": "New York"},
        {"name": "Bob", "age": 25, "city": "San Francisco"}
    ]
    
    ny_users = processor.filter_by_city(sample_data, "New York")
    print(f"Users in New York: {ny_users}")