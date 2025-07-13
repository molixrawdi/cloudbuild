from flask import Flask, jsonify
import os

app = Flask(__name__)

@app.route('/')
def hello():
    return jsonify({
        'message': 'Hello from Flask Docker app!',
        'status': 'success'
    })

@app.route('/health')
def health_check():
    return jsonify({
        'status': 'healthy',
        'service': 'flask-app'
    })

@app.route('/api/data')
def get_data():
    return jsonify({
        'data': [1, 2, 3, 4, 5],
        'environment': os.environ.get('FLASK_ENV', 'production')
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)