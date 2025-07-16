def repoUrl = 'https://github.com/lvthillo/python-flask-docker.git'
def repoName = 'python-flask-docker'
def dockerUser = 'molixrawdi@gmail.com'
def imageName = 'flasker'
// Clone the repository
println "Cloning repository..."
// def cloneCmd = "git clone ${repoUrl}"
// def proc = cloneCmd.execute()
// proc.waitForProcessOutput(System.out, System.err)

// if (proc.exitValue() != 0) {
//     println "Git clone failed!"
//     System.exit(1)
// }
def REPO = 'https://github.com/lvthillo/python-flask-docker.git'
def proc0 = "git clone ${REPO}".execute()

proc0.waitForProcessOutput(System.out, System.err)

if (proc0.exitValue() != 0) {
    println "❌ Git clone failed"
    System.exit(1)
} else {
    println "✅ Git clone successful"
}

// Get the short SHA of the latest commit
def gitDir = new File(repoName)
def shortSha = 'unknown'

def shaProc = "git rev-parse --short HEAD".execute(null, gitDir)
shaProc.waitForProcessOutput(new StringBuffer(), System.err)
if (shaProc.exitValue() == 0) {
    shortSha = shaProc.text.trim()
} else {
    println "Failed to get short SHA!"
    System.exit(1)
}

def imageTag = "${dockerUser}/${imageName}:${shortSha}"

// Build Docker image
println "Building Docker image: ${imageTag}"
def buildCmd = "docker build -t ${imageTag} .".execute(null, gitDir)
buildCmd.waitForProcessOutput(System.out, System.err)
if (buildCmd.exitValue() != 0) {
    println "Docker build failed!"
    System.exit(1)
}

def loginCmd = "docker login -u ${dockerUser} -p Qw3rty2020Rd!"
def loginProc = loginCmd.execute()
loginProc.waitForProcessOutput(System.out, System.err)
if (loginProc.exitValue() != 0) {
    println "Docker login failed!"
    System.exit(1)
}


// Push Docker image
println "Pushing Docker image: ${imageTag}"
def pushCmd = "docker push ${imageTag}".execute()
pushCmd.waitForProcessOutput(System.out, System.err)
if (pushCmd.exitValue() != 0) {
    println "Docker push failed!"
    System.exit(1)
}

println "✅ Docker image pushed: ${imageTag}"
