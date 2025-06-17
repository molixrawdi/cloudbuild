# test_app.py
import unittest
import json
import os
from unittest.mock import patch, MagicMock
import sys
sys.path.append('.')

# Assuming you have an app.py file with these functions
from app import Calculator, DataProcessor, APIClient

class TestCalculator(unittest.TestCase):
    """Test cases for Calculator class"""
    
    def setUp(self):
        """Set up test fixtures before each test method."""
        self.calc = Calculator()
    
    def test_add_positive_numbers(self):
        """Test addition of positive numbers"""
        result = self.calc.add(5, 3)
        self.assertEqual(result, 8)
    
    def test_add_negative_numbers(self):
        """Test addition with negative numbers"""
        result = self.calc.add(-5, 3)
        self.assertEqual(result, -2)
    
    def test_divide_by_zero(self):
        """Test division by zero raises exception"""
        with self.assertRaises(ValueError):
            self.calc.divide(10, 0)
    
    def test_divide_normal_case(self):
        """Test normal division"""
        result = self.calc.divide(10, 2)
        self.assertEqual(result, 5.0)

class TestDataProcessor(unittest.TestCase):
    """Test cases for DataProcessor class"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.processor = DataProcessor()
        self.sample_data = [
            {"name": "Alice", "age": 30, "city": "New York"},
            {"name": "Bob", "age": 25, "city": "San Francisco"},
            {"name": "Charlie", "age": 35, "city": "New York"}
        ]
    
    def test_filter_by_city(self):
        """Test filtering data by city"""
        result = self.processor.filter_by_city(self.sample_data, "New York")
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0]["name"], "Alice")
        self.assertEqual(result[1]["name"], "Charlie")
    
    def test_calculate_average_age(self):
        """Test average age calculation"""
        result = self.processor.calculate_average_age(self.sample_data)
        self.assertEqual(result, 30.0)
    
    def test_empty_data_handling(self):
        """Test handling of empty data"""
        result = self.processor.filter_by_city([], "New York")
        self.assertEqual(result, [])
        
        with self.assertRaises(ValueError):
            self.processor.calculate_average_age([])

class TestAPIClient(unittest.TestCase):
    """Test cases for API client with mocking"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.api_client = APIClient("https://api.example.com")
    
    @patch('requests.get')
    def test_get_user_success(self, mock_get):
        """Test successful API call"""
        # Mock the response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "id": 1,
            "name": "John Doe",
            "email": "john@example.com"
        }
        mock_get.return_value = mock_response
        
        result = self.api_client.get_user(1)
        
        # Assertions
        self.assertEqual(result["name"], "John Doe")
        self.assertEqual(result["email"], "john@example.com")
        mock_get.assert_called_once_with("https://api.example.com/users/1")
    
    @patch('requests.get')
    def test_get_user_not_found(self, mock_get):
        """Test API call when user not found"""
        mock_response = MagicMock()
        mock_response.status_code = 404
        mock_get.return_value = mock_response
        
        with self.assertRaises(Exception) as context:
            self.api_client.get_user(999)
        
        self.assertIn("User not found", str(context.exception))
    
    @patch('requests.post')
    def test_create_user(self, mock_post):
        """Test user creation via API"""
        mock_response = MagicMock()
        mock_response.status_code = 201
        mock_response.json.return_value = {
            "id": 2,
            "name": "Jane Doe",
            "email": "jane@example.com"
        }
        mock_post.return_value = mock_response
        
        user_data = {"name": "Jane Doe", "email": "jane@example.com"}
        result = self.api_client.create_user(user_data)
        
        self.assertEqual(result["id"], 2)
        mock_post.assert_called_once()

class TestEnvironmentVariables(unittest.TestCase):
    """Test cases that depend on environment variables"""
    
    @patch.dict(os.environ, {'DATABASE_URL': 'postgresql://test:test@localhost/testdb'})
    def test_database_connection_string(self):
        """Test database connection with environment variables"""
        from app import get_database_url
        result = get_database_url()
        self.assertEqual(result, 'postgresql://test:test@localhost/testdb')
    
    def test_missing_environment_variable(self):
        """Test behavior when environment variable is missing"""
        with patch.dict(os.environ, {}, clear=True):
            from app import get_database_url
            with self.assertRaises(ValueError):
                get_database_url()

class TestFileOperations(unittest.TestCase):
    """Test cases for file operations"""
    
    def setUp(self):
        """Set up test files"""
        self.test_file = 'test_data.json'
        self.test_data = {"test": "data", "numbers": [1, 2, 3]}
        
        # Create test file
        with open(self.test_file, 'w') as f:
            json.dump(self.test_data, f)
    
    def tearDown(self):
        """Clean up test files"""
        if os.path.exists(self.test_file):
            os.remove(self.test_file)
    
    def test_read_json_file(self):
        """Test reading JSON file"""
        from app import read_json_file
        result = read_json_file(self.test_file)
        self.assertEqual(result, self.test_data)
    
    def test_read_nonexistent_file(self):
        """Test reading non-existent file"""
        from app import read_json_file
        with self.assertRaises(FileNotFoundError):
            read_json_file('nonexistent.json')

if __name__ == '__main__':
    # Configure test runner for CI/CD
    unittest.main(
        verbosity=2,
        buffer=True,  # Capture stdout/stderr during test runs
        catchbreak=True,  # Allow Ctrl+C to interrupt test run
        testRunner=unittest.TextTestRunner(
            stream=sys.stdout,
            verbosity=2
        )
    )