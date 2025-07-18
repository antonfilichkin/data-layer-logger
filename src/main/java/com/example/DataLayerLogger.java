package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v118.runtime.Runtime;
import org.openqa.selenium.devtools.v118.runtime.model.ConsoleAPICalled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A minimal Java-Selenium project to fetch dataLayer.push() event logs from web pages.
 * This class demonstrates how to capture Google Tag Manager dataLayer events using Selenium WebDriver.
 */
public class DataLayerLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLayerLogger.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Object> capturedEvents = new ArrayList<>();
    
    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "https://tagmanager.google.com/";
        int waitTimeSeconds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        
        DataLayerLogger logger = new DataLayerLogger();
        logger.captureDataLayerEvents(url, waitTimeSeconds);
    }
    
    /**
     * Captures dataLayer events from a specified URL
     * @param url The URL to monitor
     * @param waitTimeSeconds How long to wait for events (in seconds)
     */
    public void captureDataLayerEvents(String url, int waitTimeSeconds) {
        WebDriver driver = null;
        
        try {
            // Setup Chrome driver with WebDriverManager
            WebDriverManager.chromedriver().setup();
            
            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-extensions");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            // options.addArguments("--headless"); // Uncomment for headless mode
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            logger.info("Starting dataLayer monitoring for URL: {}", url);
            
            // Method 1: Using DevTools to listen for console events
            setupDevToolsConsoleListener(driver);
            
            // Method 2: Inject JavaScript to monitor dataLayer
            injectDataLayerMonitor(driver);
            
            // Navigate to the target URL
            driver.get(url);
            
            // Wait for the specified time to capture events
            logger.info("Waiting {} seconds to capture dataLayer events...", waitTimeSeconds);
            Thread.sleep(waitTimeSeconds * 1000L);
            
            // Method 3: Extract current dataLayer content
            extractCurrentDataLayer(driver);
            
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
     * Sets up DevTools to listen for console API calls (including dataLayer.push)
     */
    private void setupDevToolsConsoleListener(WebDriver driver) {
        try {
            ChromeDriver chromeDriver = (ChromeDriver) driver;
            DevTools devTools = chromeDriver.getDevTools();
            devTools.createSession();
            
            // Enable Runtime domain
            devTools.send(Runtime.enable());
            
            // Listen for console API calls
            devTools.addListener(Runtime.consoleAPICalled(), this::handleConsoleEvent);
            
            logger.info("DevTools console listener setup completed");
            
        } catch (Exception e) {
            logger.warn("Could not setup DevTools listener: {}", e.getMessage());
        }
    }
    
    /**
     * Handles console events from DevTools
     */
    private void handleConsoleEvent(ConsoleAPICalled event) {
        try {
            if (event.getArgs() != null && !event.getArgs().isEmpty()) {
                String firstArg = event.getArgs().get(0).getValue().map(Object::toString).orElse("");
                if (firstArg.contains("dataLayer") || firstArg.contains("gtag")) {
                    logger.info("Console event captured: {}", firstArg);
                    capturedEvents.add(Map.of(
                        "source", "console",
                        "type", event.getType().toString(),
                        "args", event.getArgs().toString(),
                        "timestamp", System.currentTimeMillis()
                    ));
                }
            }
        } catch (Exception e) {
            logger.warn("Error processing console event: {}", e.getMessage());
        }
    }
    
    /**
     * Injects JavaScript to monitor dataLayer pushes
     */
    private void injectDataLayerMonitor(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String monitorScript = 
            "// Store original dataLayer.push method\n" +
            "window.originalDataLayerPush = window.dataLayer ? window.dataLayer.push : null;\n" +
            "window.capturedDataLayerEvents = [];\n" +
            "\n" +
            "// Initialize dataLayer if it doesn't exist\n" +
            "if (typeof window.dataLayer === 'undefined') {\n" +
            "    window.dataLayer = [];\n" +
            "}\n" +
            "\n" +
            "// Override dataLayer.push to capture events\n" +
            "window.dataLayer.push = function() {\n" +
            "    // Call original push method\n" +
            "    if (window.originalDataLayerPush) {\n" +
            "        window.originalDataLayerPush.apply(window.dataLayer, arguments);\n" +
            "    } else {\n" +
            "        Array.prototype.push.apply(window.dataLayer, arguments);\n" +
            "    }\n" +
            "    \n" +
            "    // Capture the pushed data\n" +
            "    for (let i = 0; i < arguments.length; i++) {\n" +
            "        const eventData = {\n" +
            "            data: arguments[i],\n" +
            "            timestamp: Date.now(),\n" +
            "            source: 'dataLayer.push'\n" +
            "        };\n" +
            "        window.capturedDataLayerEvents.push(eventData);\n" +
            "        console.log('DataLayer Event Captured:', eventData);\n" +
            "    }\n" +
            "    \n" +
            "    return window.dataLayer.length;\n" +
            "};\n" +
            "\n" +
            "console.log('DataLayer monitor injected successfully');";
        
        try {
            js.executeScript(monitorScript);
            logger.info("DataLayer monitor script injected successfully");
        } catch (Exception e) {
            logger.error("Failed to inject dataLayer monitor script: {}", e.getMessage());
        }
    }
    
    /**
     * Extracts current dataLayer content and captured events
     */
    private void extractCurrentDataLayer(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        try {
            // Get current dataLayer content
            Object dataLayerContent = js.executeScript("return window.dataLayer || [];");
            if (dataLayerContent != null) {
                capturedEvents.add(Map.of(
                    "source", "final_dataLayer",
                    "content", dataLayerContent,
                    "timestamp", System.currentTimeMillis()
                ));
                logger.info("Current dataLayer content extracted: {} events", 
                    ((List<?>) dataLayerContent).size());
            }
            
            // Get captured events from our monitor
            Object capturedByMonitor = js.executeScript("return window.capturedDataLayerEvents || [];");
            if (capturedByMonitor != null) {
                capturedEvents.add(Map.of(
                    "source", "monitor_captured",
                    "events", capturedByMonitor,
                    "timestamp", System.currentTimeMillis()
                ));
                logger.info("Monitor captured events: {}", ((List<?>) capturedByMonitor).size());
            }
            
        } catch (Exception e) {
            logger.error("Error extracting dataLayer content: {}", e.getMessage());
        }
    }
    
    /**
     * Displays all captured events in a formatted way
     */
    private void displayCapturedEvents() {
        logger.info("=== CAPTURED DATALAYER EVENTS ===");
        
        if (capturedEvents.isEmpty()) {
            logger.info("No dataLayer events were captured.");
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
    }
} 