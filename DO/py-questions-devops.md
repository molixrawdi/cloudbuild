Python Questions for a Principal DevOps Engineer

File & Log Processing

Question: Write a Python function that reads a large log file line by line and extracts all unique IP addresses that failed authentication. How would you ensure this works efficiently for multi-GB log files?

Follow-up: How would you adapt this to stream logs from AWS S3 or Kafka instead of a local file?

Automation & Scripting

Question: Given a list of servers in a text file, write a Python script that checks SSH connectivity (using paramiko or asyncssh) and reports unreachable hosts.

Follow-up: How would you parallelize this check to handle hundreds of servers efficiently?

Error Handling & Resilience

Question: Suppose you’re calling a REST API from Python to deploy resources. How would you implement retry logic with exponential backoff when the API fails intermittently?

Follow-up: How would you structure this so that it can be reused across different API calls in your automation scripts?

Data Serialization & Config Management

Question: Write Python code that reads a YAML or JSON configuration file, validates required fields, and outputs it in a standardized format.

Follow-up: How would you integrate schema validation (e.g., using pydantic or jsonschema)?

CI/CD Integration

Question: You need a Python script in a CI/CD pipeline that triggers a Docker build and pushes the image to a container registry. How would you implement this using the Docker SDK or subprocess?

Follow-up: How would you add error handling and logging so that pipeline failures are clear and traceable?

System Monitoring & Metrics

Question: Write a Python function that collects CPU and memory usage of a system every 5 seconds and exposes the data as a Prometheus metric (using prometheus_client).

Follow-up: How would you extend this to monitor multiple servers and aggregate results?

👉 These questions mix practical coding challenges with DevOps-specific applications of Python, testing for:

Efficiency with large-scale data/logs.

Automation of infrastructure tasks.

Robust error handling.

Integration with CI/CD, Docker, and monitoring tools.

Ability to design reusable, maintainable code.

Do you want me to also provide example solutions (with clean, production-ready Python code), or just keep them as interview questions?

You said:
yes please

Python said:

Perfect 🚀
I’ll provide example solutions for each of the 6 Python interview questions. Since these are for a Principal DevOps Engineer, I’ll write the code in a production-ready style:

clean structure

error handling

scalability considerations

using appropriate libraries

1. File & Log Processing
import re

def extract_failed_ips(filepath: str) -> set[str]:
    """Extract unique IPs from failed authentication logs."""
    ip_pattern = re.compile(r'(\d+\.\d+\.\d+\.\d+)')
    failed_ips = set()
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            if "Failed password" in line:  # example SSH failure pattern
                match = ip_pattern.search(line)
                if match:
                    failed_ips.add(match.group(1))
    return failed_ips


# Example usage
if __name__ == "__main__":
    ips = extract_failed_ips("auth.log")
    print(f"Found {len(ips)} unique failed IPs.")


✅ Efficient because it streams line by line.
For S3 streaming, you’d replace open with boto3.client("s3").get_object() and iterate over the stream.

2. Automation & Scripting (SSH Check)
import asyncio, asyncssh

async def check_ssh(host: str, username: str = "ubuntu") -> bool:
    """Check if SSH is available on the given host."""
    try:
        async with asyncssh.connect(host, username=username) as conn:
            result = await conn.run("echo ok", check=True)
            return result.stdout.strip() == "ok"
    except Exception:
        return False

async def main():
    with open("servers.txt") as f:
        servers = [line.strip() for line in f if line.strip()]

    results = await asyncio.gather(*(check_ssh(host) for host in servers))
    for host, ok in zip(servers, results):
        print(f"{host}: {'reachable' if ok else 'unreachable'}")

if __name__ == "__main__":
    asyncio.run(main())


✅ Parallelized SSH checks with asyncio.

3. Error Handling & Resilience (Exponential Backoff)
import time, requests, random

def api_call_with_backoff(url: str, retries: int = 5):
    """Call API with exponential backoff on failure."""
    delay = 1
    for attempt in range(1, retries + 1):
        try:
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            if attempt == retries:
                raise
            sleep_time = delay + random.uniform(0, 0.5)  # jitter
            print(f"Attempt {attempt} failed: {e}, retrying in {sleep_time:.2f}s")
            time.sleep(sleep_time)
            delay *= 2  # exponential increase

# Example
if __name__ == "__main__":
    data = api_call_with_backoff("https://httpbin.org/status/503")
    print(data)


✅ Implements retries, exponential backoff, and jitter (to avoid thundering herd problem).

4. Config Management with YAML/JSON
import json, yaml
from pydantic import BaseModel, ValidationError

class Config(BaseModel):
    app_name: str
    replicas: int
    image: str

def load_config(filepath: str) -> Config:
    with open(filepath, "r") as f:
        if filepath.endswith(".yaml") or filepath.endswith(".yml"):
            data = yaml.safe_load(f)
        else:
            data = json.load(f)
    return Config(**data)

if __name__ == "__main__":
    try:
        cfg = load_config("config.yaml")
        print(cfg.json(indent=2))
    except ValidationError as e:
        print("❌ Invalid configuration:", e.json())


✅ Uses Pydantic for schema validation and strong typing.

5. CI/CD Integration (Docker Build & Push)
import docker, sys

def build_and_push(image_name: str, dockerfile: str = "."):
    client = docker.from_env()
    print("🚀 Building image...")
    image, logs = client.images.build(path=dockerfile, tag=image_name)
    for chunk in logs:
        if "stream" in chunk:
            sys.stdout.write(chunk["stream"])

    print("📦 Pushing image...")
    for line in client.images.push(image_name, stream=True, decode=True):
        print(line)

if __name__ == "__main__":
    build_and_push("myregistry/myapp:latest")


✅ Uses the Docker SDK, logs build output, and pushes to registry.
Would be wrapped with CI/CD secrets (credentials for Docker Hub, ECR, etc.).

6. System Monitoring & Prometheus Metrics
import time, psutil
from prometheus_client import start_http_server, Gauge

cpu_gauge = Gauge("system_cpu_usage", "CPU usage percentage")
mem_gauge = Gauge("system_memory_usage", "Memory usage percentage")

def collect_metrics():
    """Collect system CPU and memory usage."""
    cpu_gauge.set(psutil.cpu_percent())
    mem_gauge.set(psutil.virtual_memory().percent)

if __name__ == "__main__":
    start_http_server(8000)  # Expose /metrics
    print("✅ Metrics server running at http://localhost:8000/metrics")
    while True:
        collect_metrics()
        time.sleep(5)


✅ Exposes system metrics on /metrics, Prometheus-compatible.
For multi-server monitoring, you’d deploy this agent per host and aggregate with Prometheus.

🔥 These solutions show:

Real-world Python usage in DevOps automation

Best practices (error handling, retries, logging, async, validation)

Integrations with cloud/infra tools (Docker, Prometheus, YAML, APIs, SSH)