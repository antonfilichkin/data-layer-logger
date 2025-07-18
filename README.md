# Chromium DataLayer Logger

A minimal Java-Selenium project that captures `dataLayer.push()` event logs from web pages. This tool is useful for debugging Google Tag Manager (GTM) implementations and tracking analytics events.

## Features

- **Multiple Capture Methods**: Uses three different approaches to capture dataLayer events:
  1. Chrome DevTools Runtime API for console events
  2. JavaScript injection to monitor dataLayer.push() calls
  3. Direct dataLayer content extraction
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

### Basic Usage

Run with default settings (monitors Google Tag Manager homepage for 10 seconds):
```bash
mvn exec:java
```

### Custom URL and Wait Time

Monitor a specific website for a custom duration:
```bash
mvn exec:java -Dexec.args="https://example.com 30"
```

### Command Line Arguments

- **Argument 1**: URL to monitor (default: `https://tagmanager.google.com/`)
- **Argument 2**: Wait time in seconds (default: `10`)

## How It Works

### 1. DevTools Console Monitoring
The application uses Chrome DevTools Protocol to listen for console API calls that mention "dataLayer" or "gtag".

### 2. JavaScript Injection
Injects a monitoring script that:
- Preserves the original `dataLayer.push` method
- Overrides it to capture all pushed events
- Stores captured events in `window.capturedDataLayerEvents`
- Logs events to the console

### 3. Content Extraction
After the monitoring period, extracts:
- Current `window.dataLayer` content
- All events captured by the injected monitor

## Output Format

The application outputs captured events in JSON format, showing:
- **source**: Where the event was captured from (`console`, `monitor_captured`, `final_dataLayer`)
- **data/content**: The actual event data
- **timestamp**: When the event was captured
- **type**: Event type (for console events)

Example output:
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

## Configuration Options

### Chrome Options
The application runs Chrome with these options by default:
- `--disable-blink-features=AutomationControlled`
- `--disable-extensions`
- `--no-sandbox`
- `--disable-dev-shm-usage`

To run in headless mode, uncomment the headless option in `DataLayerLogger.java`:
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
- Check if the website actually uses Google Tag Manager or dataLayer
- Increase wait time to allow for page loading and user interactions
- Check browser console for JavaScript errors

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