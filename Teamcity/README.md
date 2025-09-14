How to install teamcity

Download the package and unzip

This is the systemd configuration

```

# Create service file
sudo nano /etc/systemd/system/teamcity.service

# Add content:
[Unit]
Description=TeamCity Server
After=network.target

[Service]
Type=forking
ExecStart=/opt/TeamCity/bin/teamcity-server.sh start
ExecStop=/opt/TeamCity/bin/teamcity-server.sh stop
User=teamcity
Group=teamcity

[Install]
WantedBy=multi-user.target

# Enable and start
sudo systemctl enable teamcity
sudo systemctl start teamcity

```