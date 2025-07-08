# Basic Jenkins container
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts



# Wait for Jenkins to start

# Production ready

# Create a dedicated network
docker network create jenkins

# Run Jenkins with proper volume mounting and security
docker run -d \
  --name jenkins \
  --network jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(which docker):/usr/bin/docker \
  --group-add $(getent group docker | cut -d: -f3) \
  jenkins/jenkins:lts


  # Docker swarm example

  version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
      - /usr/bin/docker:/usr/bin/docker
    environment:
      - JENKINS_OPTS=--httpPort=8080
    networks:
      - jenkins
    user: root  # Only if you need Docker access

  # Optional: Add Docker-in-Docker for isolated builds
  docker-dind:
    image: docker:dind
    container_name: jenkins-docker
    privileged: true
    restart: unless-stopped
    networks:
      - jenkins
    environment:
      - DOCKER_TLS_CERTDIR=/certs
    volumes:
      - jenkins-docker-certs:/certs/client
      - jenkins_home:/var/jenkins_home

volumes:
  jenkins_home:
  jenkins-docker-certs:

networks:
  jenkins:
    driver: bridge

    # Custom mode

    FROM jenkins/jenkins:lts

# Switch to root to install additional packages
USER root

# Install Docker CLI
RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
RUN echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN apt-get update && apt-get install -y docker-ce-cli

# Install additional tools
RUN apt-get install -y \
    python3 \
    python3-pip \
    nodejs \
    npm \
    git

# Switch back to jenkins user
USER jenkins

# Install Jenkins plugins
RUN jenkins-plugin-cli --plugins \
    blueocean \
    docker-workflow \
    docker-pipeline \
    github \
    git \
    pipeline-stage-view \
    workflow-aggregator



    # other possibility

    docker build -t my-jenkins .
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  my-jenkins



  # Get password

  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword



  # Declare environment variables

  -e JENKINS_OPTS="--httpPort=8080 --prefix=/jenkins"
  -e JAVA_OPTS="-Xmx2048m -Xms1024m"

# Volume mounts

-v /host/path/jenkins_home:/var/jenkins_home  # Jenkins data
-v /var/run/docker.sock:/var/run/docker.sock  # Docker access
-v /host/path/workspace:/var/jenkins_home/workspace  # Workspace

# nework and ports:
--network jenkins  # Custom network
-p 8080:8080      # Web UI
-p 50000:50000    # Agent communication


# process management
docker start jenkins
docker stop jenkins
docker restart jenkins

# Logs

docker logs -f jenkins

# Commands

docker exec -it jenkins bash

# Backup jenkins data

docker run --rm \
  -v jenkins_home:/var/jenkins_home \
  -v $(pwd):/backup \
  alpine tar czf /backup/jenkins_backup.tar.gz -C /var/jenkins_home .

# Restore jenkins data

docker run --rm \
  -v jenkins_home:/var/jenkins_home \
  -v $(pwd):/backup \
  alpine sh -c "cd /var/jenkins_home && tar xzf /backup/jenkins_backup.tar.gz"