# Chromium DataLayer Logger

A minimal Java-Selenium project that captures `dataLayer.push()` event logs from web pages. This tool is useful for debugging Google Tag Manager (GTM) implementations and tracking analytics events.

## Features

- **Dual Capture Approaches**: Includes both JavaScript injection and LoggingPreferences methods
- **Multiple Capture Methods**: Uses three different approaches to capture dataLayer events:
  1. Chrome DevTools Runtime API for console events
  2. JavaScript injection to monitor dataLayer.push() calls
  3. LoggingPreferences for network-level tracking
  4. Direct dataLayer content extraction
- **Automatic Driver Management**: Uses WebDriverManager for automatic ChromeDriver setup
- **JSON Output**: Formats captured events as pretty-printed JSON
- **Configurable**: Supports custom URLs and wait times via command line arguments

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Chrome browser installed

## Installation

1. Clone or download this project
2. Navigate to the project directory
3. Install dependencies:
   ```bash
   mvn clean install
   ```

## Usage

### Approach 1: JavaScript Injection Method (Default)

Run with default settings (monitors Google Tag Manager homepage for 10 seconds):
```bash
mvn exec:java
```

Monitor a specific website:
```bash
mvn exec:java -Dexec.args="https://example.com 30"
```

### Approach 2: LoggingPreferences Method (Recommended)

This method uses Chrome's logging capabilities to capture network-level GTM/Analytics activity:

```bash
mvn exec:java@run-with-logging -Dexec.args="https://developers.google.com 8"
```

Or using the convenience script:
```bash
./run.sh logging https://developers.google.com 8
```

### Convenience Script Usage

The `run.sh` script provides easy access to both methods:

```bash
# JavaScript injection method (default)
./run.sh default

# LoggingPreferences method  
./run.sh logging <URL> [time]

# Custom URL with injection method
./run.sh custom https://example.com 30

# Help
./run.sh help
```

### Command Line Arguments

- **Argument 1**: URL to monitor (default varies by method)
- **Argument 2**: Wait time in seconds (default: `10`)

## Method Comparison

### JavaScript Injection vs LoggingPreferences

| Aspect | JavaScript Injection | LoggingPreferences |
|--------|---------------------|-------------------|
| **Intrusiveness** | Modifies page JavaScript | Non-intrusive |
| **Data Captured** | Direct dataLayer events | Network requests + performance |
| **Reliability** | Depends on console logging | Captures all network activity |
| **Event Count** | 5-10 typical events | 40+ comprehensive events |
| **Best For** | Direct dataLayer debugging | GTM/Analytics monitoring |

### When to Use Each Method

**Use JavaScript Injection when:**
- You need direct access to dataLayer events
- The website logs dataLayer events to console
- You want to see the exact dataLayer.push() calls
- Debugging specific GTM implementations

**Use LoggingPreferences when:**
- You want comprehensive GTM/Analytics monitoring
- The website doesn't log to console
- You need network-level activity tracking
- Monitoring overall analytics implementation

## How It Works

### 1. JavaScript Injection Method
- Preserves the original `dataLayer.push` method
- Overrides it to capture all pushed events
- Stores captured events in `window.capturedDataLayerEvents`
- Logs events to the console

### 2. LoggingPreferences Method
- Uses Chrome's built-in logging capabilities
- Captures browser console logs and performance logs
- Filters for GTM/Analytics related network requests
- Monitors `googletagmanager.com` and `google-analytics.com` traffic

### 3. DevTools Console Monitoring
The application uses Chrome DevTools Protocol to listen for console API calls that mention "dataLayer" or "gtag".

### 4. Content Extraction
After the monitoring period, extracts:
- Current `window.dataLayer` content
- All events captured by the injected monitor
- Network performance data (LoggingPreferences method)

## Output Format

The application outputs captured events in JSON format, showing:
- **source**: Where the event was captured from (`console_log`, `performance_log`, `monitor_captured`, `final_dataLayer`)
- **data/content**: The actual event data
- **timestamp**: When the event was captured
- **type**: Event type (for console events)
- **level**: Log level (for LoggingPreferences)

### JavaScript Injection Output Example:
```json
{
  "source": "monitor_captured",
  "events": [
    {
      "data": {
        "event": "page_view",
        "page_title": "Homepage",
        "page_location": "https://example.com"
      },
      "timestamp": 1703123456789,
      "source": "dataLayer.push"
    }
  ],
  "timestamp": 1703123456790
}
```

### LoggingPreferences Output Example:
```json
{
  "source": "performance_log",
  "level": "INFO",
  "message": "{\"method\":\"Network.requestWillBeSent\",\"params\":{\"url\":\"https://www.googletagmanager.com/gtag/js?id=G-272J68FCRF\"}}",
  "timestamp": 1703123456789,
  "capturedAt": 1703123456790
}
```

## Configuration Options

### Chrome Options
The application runs Chrome with these options by default:
- `--disable-blink-features=AutomationControlled`
- `--disable-extensions`
- `--no-sandbox`
- `--disable-dev-shm-usage`
- `--enable-logging` (LoggingPreferences method)

To run in headless mode, uncomment the headless option in the respective Java class:
```java
options.addArguments("--headless");
```

### Logging
Logging is configured via SLF4J Simple Logger. Log levels can be adjusted by creating a `simplelogger.properties` file in `src/main/resources/`.

## Building

### Compile Only
```bash
mvn compile
```

### Create JAR
```bash
mvn package
```

### Run Tests (if any)
```bash
mvn test
```

## Troubleshooting

### ChromeDriver Issues
- Ensure Chrome browser is installed
- WebDriverManager should automatically download the appropriate ChromeDriver version
- If issues persist, try updating Chrome browser

### No Events Captured

**JavaScript Injection Method:**
- Check if the website actually uses Google Tag Manager or dataLayer
- Increase wait time to allow for page loading and user interactions
- Check browser console for JavaScript errors

**LoggingPreferences Method:**
- Verify the website uses GTM or Google Analytics
- Check that network requests to `googletagmanager.com` or `google-analytics.com` are being made
- Increase monitoring time for more comprehensive capture

### DevTools Not Working
- DevTools features require Chrome browser (not other Chromium-based browsers)
- Ensure you're using compatible Selenium and Chrome versions

## Dependencies

- **Selenium WebDriver**: Browser automation
- **WebDriverManager**: Automatic driver management
- **Jackson**: JSON processing
- **SLF4J**: Logging framework

## License

This project is provided as-is for educational and debugging purposes.

## Contributing

Feel free to submit issues and enhancement requests! 