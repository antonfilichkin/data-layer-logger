package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Alternative DataLayer Logger implementation using LoggingPreferences
 * to capture console logs instead of JavaScript injection.
 * 
 * This approach is cleaner and doesn't modify the page's JavaScript environment,
 * but relies on the website actually logging dataLayer events to the console.
 */
public class DataLayerLoggerWithLogging {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLayerLoggerWithLogging.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Object> capturedEvents = new ArrayList<>();
    
    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "https://developers.google.com/";
        int waitTimeSeconds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        
        DataLayerLoggerWithLogging loggerInstance = new DataLayerLoggerWithLogging();
        loggerInstance.captureDataLayerEvents(url, waitTimeSeconds);
    }
    
    /**
     * Captures dataLayer events using browser console logs
     * @param url The URL to monitor
     * @param waitTimeSeconds How long to wait for events (in seconds)
     */
    public void captureDataLayerEvents(String url, int waitTimeSeconds) {
        WebDriver driver = null;
        
        try {
            // Setup Chrome driver with WebDriverManager
            WebDriverManager.chromedriver().setup();
            
            // Configure Chrome options with logging preferences
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-extensions");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--enable-logging");
            options.addArguments("--v=1");
            // options.addArguments("--headless"); // Uncomment for headless mode
            
            // Configure logging preferences to capture console logs
            LoggingPreferences loggingPrefs = new LoggingPreferences();
            loggingPrefs.enable(LogType.BROWSER, Level.ALL);
            loggingPrefs.enable(LogType.PERFORMANCE, Level.ALL);
            options.setCapability("goog:loggingPrefs", loggingPrefs);
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            logger.info("Starting dataLayer monitoring for URL: {}", url);
            logger.info("Using LoggingPreferences approach - capturing console logs");
            
            // Navigate to the target URL
            driver.get(url);
            
            // Wait and periodically collect logs
            logger.info("Monitoring for {} seconds...", waitTimeSeconds);
            
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (waitTimeSeconds * 1000L);
            int logCheckInterval = 1000; // Check every second
            
            while (System.currentTimeMillis() < endTime) {
                // Collect console logs
                collectConsoleLogs(driver);
                
                try {
                    Thread.sleep(logCheckInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Final log collection
            collectConsoleLogs(driver);
            
            // Display captured events
            displayCapturedEvents();
            
        } catch (Exception e) {
            logger.error("Error during dataLayer capture: ", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    /**
     * Collects and processes console logs from the browser
     */
    private void collectConsoleLogs(WebDriver driver) {
        try {
            LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
            
            for (LogEntry entry : logEntries) {
                String message = entry.getMessage();
                
                // Filter for dataLayer-related logs
                if (isDataLayerRelated(message)) {
                    logger.info("DataLayer console log captured: {}", message);
                    
                    capturedEvents.add(Map.of(
                        "source", "console_log",
                        "level", entry.getLevel().toString(),
                        "message", message,
                        "timestamp", entry.getTimestamp(),
                        "capturedAt", System.currentTimeMillis()
                    ));
                }
            }
            
            // Also collect performance logs which might contain network events
            try {
                LogEntries perfLogEntries = driver.manage().logs().get(LogType.PERFORMANCE);
                for (LogEntry entry : perfLogEntries) {
                    String message = entry.getMessage();
                    if (message.contains("gtag") || message.contains("google-analytics") || 
                        message.contains("googletagmanager")) {
                        
                        capturedEvents.add(Map.of(
                            "source", "performance_log",
                            "level", entry.getLevel().toString(),
                            "message", message,
                            "timestamp", entry.getTimestamp(),
                            "capturedAt", System.currentTimeMillis()
                        ));
                    }
                }
            } catch (Exception e) {
                // Performance logs might not be available in all configurations
                logger.debug("Performance logs not available: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.warn("Error collecting console logs: {}", e.getMessage());
        }
    }
    
    /**
     * Determines if a console log message is related to dataLayer
     */
    private boolean isDataLayerRelated(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("datalayer") ||
               lowerMessage.contains("gtag") ||
               lowerMessage.contains("google tag manager") ||
               lowerMessage.contains("gtm") ||
               lowerMessage.contains("ga(") ||
               lowerMessage.contains("google-analytics") ||
               lowerMessage.contains("_gaq") ||
               lowerMessage.contains("dataLayer.push") ||
               lowerMessage.contains("event") && lowerMessage.contains("track");
    }
    
    /**
     * Displays all captured events in a formatted way
     */
    private void displayCapturedEvents() {
        logger.info("=== CAPTURED DATALAYER EVENTS (LoggingPreferences Method) ===");
        
        if (capturedEvents.isEmpty()) {
            logger.info("No dataLayer-related console logs were captured.");
            logger.info("Note: This method depends on the website logging dataLayer events to console.");
            return;
        }
        
        for (int i = 0; i < capturedEvents.size(); i++) {
            try {
                String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(capturedEvents.get(i));
                logger.info("Event {}: {}", i + 1, json);
            } catch (Exception e) {
                logger.error("Error formatting event {}: {}", i + 1, e.getMessage());
                logger.info("Event {} (raw): {}", i + 1, capturedEvents.get(i));
            }
        }
        
        logger.info("=== END OF CAPTURED EVENTS ===");
        logger.info("Total events captured: {}", capturedEvents.size());
    }
} 