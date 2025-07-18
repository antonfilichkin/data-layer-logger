#!/bin/bash

# Chromium DataLayer Logger - Run Script
# This script provides convenient ways to run the DataLayer logger

echo "Chromium DataLayer Logger"
echo "========================="

# Function to run with default settings
run_default() {
    echo "Running with default settings (GTM homepage, 10 seconds)..."
    mvn exec:java
}

# Function to run with custom URL
run_custom() {
    if [ $# -eq 0 ]; then
        echo "Usage: $0 custom <URL> [wait_time_seconds]"
        echo "Example: $0 custom https://www.google.com 15"
        exit 1
    fi
    
    URL=$1
    WAIT_TIME=${2:-10}
    
    echo "Running with custom settings (JavaScript injection method):"
    echo "  URL: $URL"
    echo "  Wait time: $WAIT_TIME seconds"
    
    mvn exec:java -Dexec.args="$URL $WAIT_TIME"
}

# Function to run with LoggingPreferences method
run_logging() {
    if [ $# -eq 0 ]; then
        echo "Usage: $0 logging <URL> [wait_time_seconds]"
        echo "Example: $0 logging https://developers.google.com 8"
        exit 1
    fi
    
    URL=$1
    WAIT_TIME=${2:-10}
    
    echo "Running with LoggingPreferences method:"
    echo "  URL: $URL"
    echo "  Wait time: $WAIT_TIME seconds"
    echo "  Method: Non-intrusive network monitoring"
    
    mvn exec:java@run-with-logging -Dexec.args="$URL $WAIT_TIME"
}

# Function to compile the project
compile() {
    echo "Compiling project..."
    mvn clean compile
}

# Function to install dependencies
install() {
    echo "Installing dependencies..."
    mvn clean install
}

# Main script logic
case "${1:-default}" in
    "default")
        run_default
        ;;
    "custom")
        shift
        run_custom "$@"
        ;;
    "logging")
        shift
        run_logging "$@"
        ;;
    "compile")
        compile
        ;;
    "install")
        install
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  default              Run with default settings (JavaScript injection, GTM homepage, 10 seconds)"
        echo "  custom <URL> [time]  Run with custom URL using JavaScript injection method"
        echo "  logging <URL> [time] Run with LoggingPreferences method (recommended for GTM monitoring)"
        echo "  compile              Compile the project"
        echo "  install              Install dependencies"
        echo "  help                 Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                                           # Run with defaults (JavaScript injection)"
        echo "  $0 default                                  # Same as above"
        echo "  $0 custom https://example.com               # Monitor example.com with injection method"
        echo "  $0 custom https://example.com 30            # Monitor example.com for 30 seconds"
        echo "  $0 logging https://developers.google.com    # Monitor with LoggingPreferences (recommended)"
        echo "  $0 logging https://developers.google.com 15 # Monitor for 15 seconds with LoggingPreferences"
        echo "  $0 install                                  # Install dependencies first"
        echo ""
        echo "Method Comparison:"
        echo "  JavaScript Injection: Direct dataLayer access, modifies page JavaScript"
        echo "  LoggingPreferences:   Network monitoring, non-intrusive, captures more events"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac 