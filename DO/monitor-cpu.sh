#!/bin/bash

# CPU Usage Monitor Script
# Monitors CPU usage and logs alerts when it reaches 80% threshold

# Configuration
THRESHOLD=80
LOG_FILE="/var/log/cpu_monitor.log"
CHECK_INTERVAL=30  # seconds
SCRIPT_NAME="cpu_monitor"

# Colors for console output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Function to log messages with timestamp
log_message() {
    local level="$1"
    local message="$2"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Log to file
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
    
    # Also display on console with colors
    case "$level" in
        "ERROR")
            echo -e "${RED}[$timestamp] [$level] $message${NC}"
            ;;
        "WARNING")
            echo -e "${YELLOW}[$timestamp] [$level] $message${NC}"
            ;;
        "INFO")
            echo -e "${GREEN}[$timestamp] [$level] $message${NC}"
            ;;
        *)
            echo "[$timestamp] [$level] $message"
            ;;
    esac
}

# Function to get CPU usage percentage
get_cpu_usage() {
    # Method 1: Using top command (more reliable)
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
    
    # If top method fails, try alternative method with /proc/stat
    if [[ -z "$cpu_usage" ]] || [[ ! "$cpu_usage" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        # Method 2: Calculate from /proc/stat
        local cpu_line=($(head -n1 /proc/stat))
        local idle=${cpu_line[4]}
        local total=0
        
        for value in "${cpu_line[@]:1}"; do
            total=$((total + value))
        done
        
        sleep 1
        
        local cpu_line_new=($(head -n1 /proc/stat))
        local idle_new=${cpu_line_new[4]}
        local total_new=0
        
        for value in "${cpu_line_new[@]:1}"; do
            total_new=$((total_new + value))
        done
        
        local idle_diff=$((idle_new - idle))
        local total_diff=$((total_new - total))
        
        if [[ $total_diff -ne 0 ]]; then
            cpu_usage=$(awk "BEGIN {printf \"%.1f\", (($total_diff - $idle_diff) * 100 / $total_diff)}")
        else
            cpu_usage="0.0"
        fi
    fi
    
    echo "$cpu_usage"
}

# Function to get top CPU consuming processes
get_top_processes() {
    ps aux --sort=-%cpu | head -6 | tail -5
}

# Function to send alert (can be extended to email/slack/etc)
send_alert() {
    local cpu_usage="$1"
    local message="HIGH CPU USAGE ALERT: CPU usage is at ${cpu_usage}% (threshold: ${THRESHOLD}%)"
    
    log_message "WARNING" "$message"
    
    # Get top processes consuming CPU
    log_message "INFO" "Top CPU consuming processes:"
    get_top_processes | while read line; do
        log_message "INFO" "  $line"
    done
    
    # Optional: Send email alert (uncomment and configure)
    # echo "$message" | mail -s "CPU Alert on $(hostname)" admin@example.com
    
    # Optional: Send to syslog
    logger -t "$SCRIPT_NAME" "$message"
}

# Function to create log file if it doesn't exist
setup_logging() {
    # Create log directory if it doesn't exist
    local log_dir=$(dirname "$LOG_FILE")
    if [[ ! -d "$log_dir" ]]; then
        sudo mkdir -p "$log_dir" 2>/dev/null || {
            LOG_FILE="./cpu_monitor.log"
            log_message "WARNING" "Cannot create $log_dir, using current directory for logging"
        }
    fi
    
    # Create log file if it doesn't exist
    touch "$LOG_FILE" 2>/dev/null || {
        LOG_FILE="./cpu_monitor.log"
        touch "$LOG_FILE"
        log_message "WARNING" "Cannot write to /var/log/, using current directory for logging"
    }
}

# Function to handle script termination gracefully
cleanup() {
    log_message "INFO" "CPU monitor stopping..."
    exit 0
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -t, --threshold NUM    Set CPU threshold percentage (default: 80)"
    echo "  -i, --interval NUM     Set check interval in seconds (default: 30)"
    echo "  -l, --logfile PATH     Set log file path (default: /var/log/cpu_monitor.log)"
    echo "  -d, --daemon           Run as daemon (background process)"
    echo "  -h, --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                          # Run with default settings"
    echo "  $0 -t 90 -i 60             # 90% threshold, check every 60 seconds"
    echo "  $0 -d                      # Run as daemon"
}

# Function to run as daemon
run_as_daemon() {
    if [[ -f /var/run/${SCRIPT_NAME}.pid ]]; then
        local pid=$(cat /var/run/${SCRIPT_NAME}.pid)
        if kill -0 "$pid" 2>/dev/null; then
            echo "CPU monitor is already running with PID $pid"
            exit 1
        else
            rm -f /var/run/${SCRIPT_NAME}.pid
        fi
    fi
    
    # Fork to background
    nohup "$0" --no-daemon > /dev/null 2>&1 &
    local daemon_pid=$!
    
    echo "$daemon_pid" > /var/run/${SCRIPT_NAME}.pid
    echo "CPU monitor started as daemon with PID $daemon_pid"
    log_message "INFO" "CPU monitor daemon started with PID $daemon_pid"
    exit 0
}

# Parse command line arguments
DAEMON_MODE=false
NO_DAEMON=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--threshold)
            THRESHOLD="$2"
            shift 2
            ;;
        -i|--interval)
            CHECK_INTERVAL="$2"
            shift 2
            ;;
        -l|--logfile)
            LOG_FILE="$2"
            shift 2
            ;;
        -d|--daemon)
            DAEMON_MODE=true
            shift
            ;;
        --no-daemon)
            NO_DAEMON=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate threshold
if [[ ! "$THRESHOLD" =~ ^[0-9]+$ ]] || [[ "$THRESHOLD" -lt 1 ]] || [[ "$THRESHOLD" -gt 100 ]]; then
    echo "Error: Threshold must be a number between 1 and 100"
    exit 1
fi

# Validate interval
if [[ ! "$CHECK_INTERVAL" =~ ^[0-9]+$ ]] || [[ "$CHECK_INTERVAL" -lt 1 ]]; then
    echo "Error: Check interval must be a positive number"
    exit 1
fi

# Handle daemon mode
if [[ "$DAEMON_MODE" == true ]] && [[ "$NO_DAEMON" == false ]]; then
    run_as_daemon
fi

# Set up logging
setup_logging

# Set up signal handlers
trap cleanup SIGTERM SIGINT

# Main monitoring loop
main() {
    log_message "INFO" "Starting CPU monitor (threshold: ${THRESHOLD}%, interval: ${CHECK_INTERVAL}s)"
    
    local consecutive_alerts=0
    local last_alert_time=0
    local alert_cooldown=300  # 5 minutes cooldown between alerts
    
    while true; do
        cpu_usage=$(get_cpu_usage)
        current_time=$(date +%s)
        
        # Remove any % symbol and convert to integer for comparison
        cpu_int=$(echo "$cpu_usage" | sed 's/%//' | cut -d'.' -f1)
        
        if [[ -z "$cpu_int" ]] || [[ ! "$cpu_int" =~ ^[0-9]+$ ]]; then
            log_message "ERROR" "Failed to get CPU usage"
            sleep "$CHECK_INTERVAL"
            continue
        fi
        
        log_message "INFO" "Current CPU usage: ${cpu_usage}%"
        
        if [[ "$cpu_int" -ge "$THRESHOLD" ]]; then
            consecutive_alerts=$((consecutive_alerts + 1))
            
            # Send alert if it's the first time or cooldown period has passed
            if [[ $consecutive_alerts -eq 1 ]] || [[ $((current_time - last_alert_time)) -ge $alert_cooldown ]]; then
                send_alert "$cpu_usage"
                last_alert_time=$current_time
            fi
        else
            if [[ $consecutive_alerts -gt 0 ]]; then
                log_message "INFO" "CPU usage back to normal: ${cpu_usage}%"
                consecutive_alerts=0
            fi
        fi
        
        sleep "$CHECK_INTERVAL"
    done
}

# Run the main function
main