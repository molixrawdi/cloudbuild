def REPO = 'https://github.com/lvthillo/python-flask-docker.git'
def proc0 = "git clone ${REPO}".execute()

proc0.waitForProcessOutput(System.out, System.err)

if (proc0.exitValue() != 0) {
    println "❌ Git clone failed"
    System.exit(1)
} else {
    println "✅ Git clone successful"
}
