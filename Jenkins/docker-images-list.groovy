def repo = "my-repo"           // Replace with your repo
def location = "us-central1"   // Replace with your location
def project = "my-gcp-project" // Replace with your project

def output = "gcloud artifacts docker images list ${location}-docker.pkg.dev/${project}/${repo} --format='value(NAME)'"
def proc = output.execute()
proc.waitFor()

if (proc.exitValue() == 0) {
    return proc.in.text.readLines()
} else {
    return ["Error: " + proc.err.text]
}
