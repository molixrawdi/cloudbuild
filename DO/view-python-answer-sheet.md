🐍 Python for DevOps – Interview Cheat Sheet
1. Log Processing (Large Files)

Read line by line → avoid memory issues.

Use regex for IP extraction.

For S3 logs → use boto3.get_object() stream.

with open("auth.log") as f:
    for line in f:
        if "Failed password" in line:
            match = re.search(r"\d+\.\d+\.\d+\.\d+", line)

2. SSH Automation (Parallelized)

Use asyncssh with asyncio for hundreds of servers.

Avoid sequential SSH calls.

async def check(host): 
    async with asyncssh.connect(host) as c: 
        return await c.run("echo ok")

3. API Calls with Resilience

Retry with exponential backoff + jitter.

Raise on final failure.

for attempt in range(retries):
    try: requests.get(url).raise_for_status()
    except: time.sleep(delay); delay *= 2

4. Config Management (JSON/YAML)

Parse with yaml.safe_load() or json.load().

Validate with Pydantic.

class Config(BaseModel): app_name: str; replicas: int

5. CI/CD – Docker Build & Push

Use docker Python SDK.

Stream build logs for visibility.

client.images.build(path=".", tag="myapp:latest")
client.images.push("myapp:latest")

6. System Monitoring → Prometheus

Use psutil to collect CPU/memory.

Expose metrics with prometheus_client.

cpu.set(psutil.cpu_percent())
start_http_server(8000)

⚡ Key Principles

Efficiency: Always stream logs, don’t load into memory.

Parallelism: Use asyncio / multiprocessing for scale.

Resilience: Retry with backoff, validate configs, catch failures early.

Integration: Python as glue → CI/CD, Docker, Prometheus, APIs.

Maintainability: Modular code, error handling, logging.

