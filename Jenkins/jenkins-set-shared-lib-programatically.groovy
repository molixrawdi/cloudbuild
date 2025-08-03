import jenkins.plugins.git.GitSCMSource
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever

def globalLibraries = GlobalLibraries.get()

def libraryName = "my-shared-library"
def gitUrl = "https://github.com/your-org/jenkins-shared-library.git"
def defaultVersion = "main"

// Create SCM source
def scmSource = new GitSCMSource(gitUrl)
scmSource.setCredentialsId("github-credentials") // Optional

// Create library configuration
def libraryConfig = new LibraryConfiguration(libraryName, new SCMSourceRetriever(scmSource))
libraryConfig.setDefaultVersion(defaultVersion)
libraryConfig.setImplicit(true)
libraryConfig.setAllowVersionOverride(true)

// Add to global libraries
def libraries = globalLibraries.getLibraries()
libraries.add(libraryConfig)
globalLibraries.setLibraries(libraries)

// Save configuration
globalLibraries.save()

println "Shared library '${libraryName}' configured successfully"