import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import java.util.concurrent.TimeUnit
import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

/**
 * Class to interact with Google Container Registry and list Docker images
 */
@CompileStatic
class GcrImageLister {
    private String projectId
    private String registryUrl
    private JsonSlurper jsonSlurper
    
    GcrImageLister(String projectId, String registryUrl = "gcr.io") {
        this.projectId = projectId
        this.registryUrl = registryUrl
        this.jsonSlurper = new JsonSlurper()
    }
    
    /**
     * Lists all repositories in the GCR project
     * @return List of repository names
     */
    List<String> listRepositories() {
        try {
            String cmd = "gcloud container images list --project=${projectId} --format=json"
            Process process = executeCommand(cmd)
            
            if (process.waitFor() == 0) {
                String output = process.inputStream.text
                def repos = jsonSlurper.parseText(output) as List<Map>
                return repos.collect { it.name as String }
            } else {
                throw new RuntimeException("Failed to list repositories: ${process.errorStream.text}")
            }
        } catch (Exception e) {
            println "Error listing repositories: ${e.message}"
            return []
        }
    }
    
    /**
     * Lists all tags for a specific repository
     * @param repository The repository name (e.g., 'gcr.io/project-id/app-name')
     * @return List of image tags with metadata
     */
    List<Map<String, Object>> listImageTags(String repository) {
        try {
            String cmd = "gcloud container images list-tags ${repository} --format=json --limit=50"
            Process process = executeCommand(cmd)
            
            if (process.waitFor() == 0) {
                String output = process.inputStream.text
                def images = jsonSlurper.parseText(output) as List<Map>
                
                return images.collect { image ->
                    [
                        repository: repository,
                        tags: image.tags ?: ['<untagged>'],
                        digest: image.digest,
                        timestamp: image.timestamp,
                        fullName: buildFullImageName(repository, image.tags)
                    ]
                }
            } else {
                throw new RuntimeException("Failed to list image tags: ${process.errorStream.text}")
            }
        } catch (Exception e) {
            println "Error listing image tags for ${repository}: ${e.message}"
            return []
        }
    }
    
    /**
     * Gets all available Docker images across all repositories
     * @return List of all images with their details
     */
    List<Map<String, Object>> getAllImages() {
        List<Map<String, Object>> allImages = []
        List<String> repositories = listRepositories()
        
        repositories.each { repo ->
            allImages.addAll(listImageTags(repo))
        }
        
        return allImages
    }
    
    /**
     * Gets a simplified list of image names for dropdown display
     * @return List of formatted image names
     */
    List<String> getImageNamesForDropdown() {
        return getAllImages().collect { image ->
            String repo = image.repository as String
            List<String> tags = image.tags as List<String>
            String primaryTag = tags.find { it != '<untagged>' } ?: tags[0]
            return "${repo}:${primaryTag}"
        }.unique().sort()
    }
    
    private Process executeCommand(String command) {
        return Runtime.runtime.exec(command.split(' ') as String[])
    }
    
    private String buildFullImageName(String repository, List<String> tags) {
        if (!tags || tags.contains('<untagged>')) {
            return repository
        }
        return "${repository}:${tags[0]}"
    }
}

/**
 * Class to create and manage a dropdown menu for Docker image selection
 */
@CompileStatic
class DockerImageDropdown {
    private JFrame frame
    private JComboBox<String> imageComboBox
    private JLabel selectedLabel
    private List<String> imageList
    private Closure onSelectionCallback
    
    DockerImageDropdown(List<String> images) {
        this.imageList = images ?: []
        initializeUI()
    }
    
    /**
     * Sets a callback function to be called when an image is selected
     * @param callback Closure that receives the selected image name
     */
    void setOnSelectionCallback(Closure callback) {
        this.onSelectionCallback = callback
    }
    
    /**
     * Updates the dropdown with a new list of images
     * @param newImages List of new image names
     */
    void updateImageList(List<String> newImages) {
        this.imageList = newImages ?: []
        SwingUtilities.invokeLater {
            imageComboBox.removeAllItems()
            imageList.each { image ->
                imageComboBox.addItem(image)
            }
            if (imageList.isEmpty()) {
                imageComboBox.addItem("No images available")
            }
        }
    }
    
    /**
     * Shows the dropdown window
     */
    void show() {
        SwingUtilities.invokeLater {
            frame.setVisible(true)
        }
    }
    
    /**
     * Hides the dropdown window
     */
    void hide() {
        SwingUtilities.invokeLater {
            frame.setVisible(false)
        }
    }
    
    /**
     * Gets the currently selected image
     * @return Selected image name or null if none selected
     */
    String getSelectedImage() {
        return imageComboBox.selectedItem as String
    }
    
    private void initializeUI() {
        SwingUtilities.invokeLater {
            // Create main frame
            frame = new JFrame("Docker Image Selector")
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            frame.setLayout(new BorderLayout())
            frame.setSize(600, 200)
            frame.setLocationRelativeTo(null)
            
            // Create main panel
            JPanel mainPanel = new JPanel(new GridBagLayout())
            GridBagConstraints gbc = new GridBagConstraints()
            
            // Title label
            JLabel titleLabel = new JLabel("Select Docker Image:")
            titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14))
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.gridwidth = 2
            gbc.insets = new Insets(10, 10, 10, 10)
            gbc.anchor = GridBagConstraints.WEST
            mainPanel.add(titleLabel, gbc)
            
            // Dropdown combo box
            imageComboBox = new JComboBox<>()
            imageComboBox.setPreferredSize(new Dimension(500, 30))
            imageComboBox.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12))
            
            // Add images to combo box
            if (imageList.isEmpty()) {
                imageComboBox.addItem("No images available")
            } else {
                imageList.each { image ->
                    imageComboBox.addItem(image)
                }
            }
            
            gbc.gridx = 0
            gbc.gridy = 1
            gbc.gridwidth = 2
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.insets = new Insets(5, 10, 10, 10)
            mainPanel.add(imageComboBox, gbc)
            
            // Selected image display
            selectedLabel = new JLabel("Selected: None")
            selectedLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12))
            selectedLabel.setForeground(Color.BLUE)
            gbc.gridx = 0
            gbc.gridy = 2
            gbc.gridwidth = 1
            gbc.anchor = GridBagConstraints.WEST
            gbc.insets = new Insets(5, 10, 5, 10)
            mainPanel.add(selectedLabel, gbc)
            
            // Refresh button
            JButton refreshButton = new JButton("Refresh")
            refreshButton.addActionListener(new ActionListener() {
                @Override
                void actionPerformed(ActionEvent e) {
                    refreshImages()
                }
            })
            gbc.gridx = 1
            gbc.gridy = 2
            gbc.anchor = GridBagConstraints.EAST
            gbc.insets = new Insets(5, 10, 5, 10)
            mainPanel.add(refreshButton, gbc)
            
            // Add selection listener
            imageComboBox.addActionListener(new ActionListener() {
                @Override
                void actionPerformed(ActionEvent e) {
                    String selected = imageComboBox.getSelectedItem() as String
                    selectedLabel.setText("Selected: ${selected}")
                    
                    if (onSelectionCallback) {
                        onSelectionCallback.call(selected)
                    }
                }
            })
            
            frame.add(mainPanel, BorderLayout.CENTER)
        }
    }
    
    private void refreshImages() {
        // This method can be overridden or connected to refresh logic
        println "Refresh button clicked - implement refresh logic here"
    }
}

// Example usage and main method
class DockerImageManager {
    static void main(String[] args) {
        // Example usage
        println "Docker Image Manager - Example Usage"
        println "======================================"
        
        if (args.length == 0) {
            println "Usage: groovy DockerImageManager.groovy <project-id>"
            println "Example: groovy DockerImageManager.groovy my-gcp-project"
            return
        }
        
        String projectId = args[0]
        
        try {
            // Create GCR image lister
            GcrImageLister lister = new GcrImageLister(projectId)
            
            println "Fetching images from GCR project: ${projectId}"
            List<String> images = lister.getImageNamesForDropdown()
            
            if (images.isEmpty()) {
                println "No images found in the registry"
                images = ["No images available"]
            } else {
                println "Found ${images.size()} images:"
                images.each { println "  - ${it}" }
            }
            
            // Create dropdown
            DockerImageDropdown dropdown = new DockerImageDropdown(images)
            
            // Set callback for selection
            dropdown.setOnSelectionCallback { selectedImage ->
                println "User selected: ${selectedImage}"
            }
            
            // Show the dropdown
            dropdown.show()
            
        } catch (Exception e) {
            println "Error: ${e.message}"
            e.printStackTrace()
            
            // Show dropdown with error message for demo
            DockerImageDropdown dropdown = new DockerImageDropdown(["Error loading images: ${e.message}"])
            dropdown.show()
        }
    }
}