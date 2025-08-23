#!/bin/bash
set -e

# Semantic Version Management Script
# Usage: ./version.sh [major|minor|patch|current|next]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION_FILE="$SCRIPT_DIR/VERSION"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to validate semantic version
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_color $RED "Error: Invalid semantic version format: $version"
        print_color $YELLOW "Expected format: MAJOR.MINOR.PATCH (e.g., 1.2.3)"
        exit 1
    fi
}

# Function to get current version
get_current_version() {
    if [[ -f "$VERSION_FILE" ]]; then
        cat "$VERSION_FILE"
    else
        # Try to get from git tags
        local git_version=$(git describe --tags --abbrev=0 2>/dev/null | sed 's/^v//' || echo "")
        if [[ -n "$git_version" ]]; then
            echo "$git_version"
        else
            echo "0.0.0"
        fi
    fi
}

# Function to bump version
bump_version() {
    local current_version=$1
    local bump_type=$2
    
    IFS='.' read -ra VERSION_PARTS <<< "$current_version"
    local major=${VERSION_PARTS[0]}
    local minor=${VERSION_PARTS[1]}
    local patch=${VERSION_PARTS[2]}
    
    case $bump_type in
        "major")
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        "minor")
            minor=$((minor + 1))
            patch=0
            ;;
        "patch")
            patch=$((patch + 1))
            ;;
        *)
            print_color $RED "Error: Invalid bump type: $bump_type"
            print_color $YELLOW "Valid types: major, minor, patch"
            exit 1
            ;;
    esac
    
    echo "$major.$minor.$patch"
}

# Function to determine next version based on git commits
get_next_version() {
    local current_version=$1
    local last_tag="v$current_version"
    
    # Get commits since last tag
    local commits=$(git log "$last_tag"..HEAD --oneline 2>/dev/null || git log --oneline)
    
    if echo "$commits" | grep -qE "(BREAKING CHANGE|feat!:|fix!:)"; then
        bump_version "$current_version" "major"
    elif echo "$commits" | grep -qE "^[a-f0-9]+ feat:"; then
        bump_version "$current_version" "minor"
    else
        bump_version "$current_version" "patch"
    fi
}

# Function to update version files
update_version_files() {
    local new_version=$1
    
    # Update VERSION file
    echo "$new_version" > "$VERSION_FILE"
    print_color $GREEN "Updated VERSION file: $new_version"
    
    # Update package.json if it exists
    if [[ -f "package.json" ]]; then
        if command -v jq &> /dev/null; then
            jq ".version = \"$new_version\"" package.json > package.json.tmp && mv package.json.tmp package.json
            print_color $GREEN "Updated package.json: $new_version"
        else
            print_color $YELLOW "Warning: jq not found. Please update package.json manually."
        fi
    fi
    
    # Update Dockerfile if it exists and has version label
    if [[ -f "Dockerfile" ]]; then
        if grep -q "LABEL.*version=" Dockerfile; then
            sed -i.bak "s/LABEL version=\".*\"/LABEL version=\"$new_version\"/" Dockerfile
            rm -f Dockerfile.bak
            print_color $GREEN "Updated Dockerfile: $new_version"
        fi
    fi
    
    # Update docker-compose.yml if it exists
    if [[ -f "docker-compose.yml" ]]; then
        if grep -q "image:.*:" docker-compose.yml; then
            sed -i.bak "s/image: \(.*\):.*/image: \1:$new_version/" docker-compose.yml
            rm -f docker-compose.yml.bak
            print_color $GREEN "Updated docker-compose.yml: $new_version"
        fi
    fi
}

# Function to create git tag
create_git_tag() {
    local version=$1
    local tag="v$version"
    
    if git rev-parse "$tag" >/dev/null 2>&1; then
        print_color $YELLOW "Warning: Tag $tag already exists"
        return
    fi
    
    git add .
    git commit -m "Bump version to $version" || true
    git tag -a "$tag" -m "Release version $version"
    print_color $GREEN "Created git tag: $tag"
    
    print_color $BLUE "To push the tag, run: git push origin $tag"
}

# Function to show version info
show_version_info() {
    local current_version=$(get_current_version)
    local next_version=$(get_next_version "$current_version")
    
    print_color $BLUE "Current version: $current_version"
    print_color $BLUE "Next version (auto): $next_version"
    
    echo ""
    print_color $YELLOW "Available bump types:"
    echo "  patch: $(bump_version "$current_version" "patch") (bug fixes)"
    echo "  minor: $(bump_version "$current_version" "minor") (new features)"
    echo "  major: $(bump_version "$current_version" "major") (breaking changes)"
}

# Main script logic
main() {
    local action=${1:-"current"}
    
    case $action in
        "current")
            local current_version=$(get_current_version)
            print_color $GREEN "Current version: $current_version"
            ;;
        "next")
            show_version_info
            ;;
        "major"|"minor"|"patch")
            local current_version=$(get_current_version)
            validate_version "$current_version"
            local new_version=$(bump_version "$current_version" "$action")
            
            print_color $BLUE "Bumping version: $current_version → $new_version"
            update_version_files "$new_version"
            create_git_tag "$new_version"
            ;;
        "auto")
            local current_version=$(get_current_version)
            validate_version "$current_version"
            local new_version=$(get_next_version "$current_version")
            
            print_color $BLUE "Auto-bumping version: $current_version → $new_version"
            update_version_files "$new_version"
            create_git_tag "$new_version"
            ;;
        "help"|"-h"|"--help")
            echo "Semantic Version Management Script"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  current    Show current version"
            echo "  next       Show next version info"
            echo "  patch      Bump patch version (bug fixes)"
            echo "  minor      Bump minor version (new features)"
            echo "  major      Bump major version (breaking changes)"
            echo "  auto       Auto-determine version bump from git commits"
            echo "  help       Show this help message"
            ;;
        *)
            print_color $RED "Error: Unknown command: $action"
            echo "Run '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Check if git is available
if ! command -v git &> /dev/null; then
    print_color $RED "Error: git is required but not installed"
    exit 1
fi

# Run main function
main "$@"



