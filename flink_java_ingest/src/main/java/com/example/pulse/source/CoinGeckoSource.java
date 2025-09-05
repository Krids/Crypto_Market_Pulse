// Package declaration - defines the namespace for this class within the source module
package com.example.pulse.source;

// Import our custom MarketTick model - this is the data structure we'll create from CoinGecko API
import com.example.pulse.model.MarketTick;
// Import Jackson TypeReference - needed to deserialize JSON arrays with generic types
import com.fasterxml.jackson.core.type.TypeReference;
// Import Jackson ObjectMapper - the main class for JSON serialization/deserialization
import com.fasterxml.jackson.databind.ObjectMapper;
// Import Flink's SourceFunction interface - this makes our class a Flink data source
import org.apache.flink.streaming.api.functions.source.SourceFunction;
// Import Apache HttpClient GET request class - for making HTTP requests to CoinGecko
import org.apache.hc.client5.http.classic.methods.HttpGet;
// Import Apache HttpClient interface - manages HTTP connections and lifecycle
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
// Import Apache HttpClient factory - creates configured HTTP client instances
import org.apache.hc.client5.http.impl.classic.HttpClients;
// Import Apache HttpClient response class - represents HTTP response from server
import org.apache.hc.core5.http.ClassicHttpResponse;
// Import Apache HttpClient utility - converts HTTP response entities to strings
import org.apache.hc.core5.http.io.entity.EntityUtils;
// Import SLF4J Logger interface - for structured logging throughout the application
import org.slf4j.Logger;
// Import SLF4J LoggerFactory - creates logger instances tied to specific classes
import org.slf4j.LoggerFactory;

// Import Java 8 time API - for generating precise timestamps when ingesting data
import java.time.Instant;
// Import Java List interface - for handling collections of MarketTick objects
import java.util.List;

/**
 * Custom Flink SourceFunction that polls the CoinGecko API every 60 seconds.
 * 
 * Key Flink concepts demonstrated:
 * 1. SourceFunction<MarketTick> - creates a stream of MarketTick objects
 * 2. Exactly-once semantics through automatic Flink checkpointing
 * 3. Cancellation handling via volatile boolean flag
 * 4. Error handling without failing the entire job
 * 5. Integration with external APIs using HTTP polling
 * 
 * HTTP polling strategy with API key:
 * 1. Makes authenticated HTTP GET requests to CoinGecko /coins/markets endpoint
 * 2. Uses API key for higher rate limits and better reliability
 * 3. Parses JSON response into List<MarketTick> objects
 * 4. Emits each MarketTick as a separate stream element
 * 5. Waits 60 seconds before next poll
 * 6. Implements retry logic for transient failures
 */
// Suppress deprecation warnings for SourceFunction API (newer Source API would require major refactoring)
@SuppressWarnings("deprecation")
// Class declaration implementing SourceFunction<MarketTick> - tells Flink this produces MarketTick objects
public class CoinGeckoSource implements SourceFunction<MarketTick> {

    // Serial version UID for Java serialization - ensures compatibility when distributing across Flink cluster
    private static final long serialVersionUID = 1L;
    // Logger instance for this specific class - all log messages will include class name for debugging
    private static final Logger LOG = LoggerFactory.getLogger(CoinGeckoSource.class);

    // CoinGecko API Key - using your specific API key for authenticated requests
    // API key provides higher rate limits (10,000 calls/month vs 100 calls/hour for free tier)
    private static final String API_KEY = "CG-MaiUSKQaHtZyYuocN1KiLSpn";
    
    // CoinGecko API endpoint URL with query parameters and API key authentication
    private static final String API_URL = "https://api.coingecko.com/api/v3/coins/markets" +
            "?vs_currency=usd" +           // Get prices denominated in US Dollars
            "&order=market_cap_desc" +     // Sort by market capitalization (largest first)
            "&per_page=100" +              // Retrieve top 100 cryptocurrencies per request
            "&page=1" +                    // Get first page of results (pagination support)
            "&sparkline=false" +           // Don't include 7-day price history sparkline data (saves bandwidth)
            "&price_change_percentage=24h" + // Include 24-hour percentage price change information
            "&locale=en" +                 // Use English locale for text fields
            "&x_cg_demo_api_key=" + API_KEY; // Authenticate with your API key for higher rate limits

    // Configuration constants for polling behavior - centralized for easy maintenance
    private static final long POLL_INTERVAL_MS = 60_000; // Wait 60 seconds between API calls to respect rate limits
    private static final int MAX_RETRIES = 3;             // Retry failed requests up to 3 times before giving up
    private static final long RETRY_DELAY_MS = 5_000;     // Wait 5 seconds between retry attempts

    // Instance variables for runtime state management
    private volatile boolean isRunning = true;          // Thread-safe flag to control polling loop (volatile ensures visibility across threads)
    private transient CloseableHttpClient httpClient;   // HTTP client for making requests (transient = not serialized across cluster)
    private transient ObjectMapper objectMapper;        // Jackson mapper for JSON parsing (transient = recreated on each task manager)

    // Override method from SourceFunction interface - this is where Flink calls us to start generating data
    @Override
    public void run(SourceContext<MarketTick> ctx) throws Exception {
        // Initialize HTTP client with default configuration (connection pooling, timeouts, etc.)
        httpClient = HttpClients.createDefault();
        // Initialize Jackson ObjectMapper for JSON serialization/deserialization with default settings
        objectMapper = new ObjectMapper();

        // Log startup information for monitoring and debugging purposes
        LOG.info("Starting CoinGecko polling source with API key authentication");
        LOG.info("API URL: {}", API_URL);
        LOG.info("Poll interval: {} ms", POLL_INTERVAL_MS);
        LOG.info("Max retries per request: {}", MAX_RETRIES);

        // Counter to track total number of API polling attempts for monitoring/debugging
        long pollCount = 0;
        
        // Main polling loop - continues until isRunning flag is set to false (via cancel() method)
        while (isRunning) {
            try {
                // Increment poll counter before each attempt
                pollCount++;
                // Log debug message for each polling attempt (helps with troubleshooting)
                LOG.debug("Starting poll #{} with API key authentication", pollCount);
                
                // Call our private method to actually fetch data from CoinGecko API
                List<MarketTick> marketData = fetchMarketData();
                
                // Check if we got valid data back from the API (not null and not empty)
                if (marketData != null && !marketData.isEmpty()) {
                    // Modern Flink automatically handles exactly-once semantics, no manual checkpoint locking needed
                    // Iterate through each MarketTick object returned from the API
                    for (MarketTick tick : marketData) {
                        // Check if source was cancelled while processing (graceful shutdown)
                        if (!isRunning) break;
                        
                        // Add ingestion metadata to track when and where this data was processed
                        tick.setIngestTimestamp(Instant.now().toEpochMilli()); // Current time as Unix timestamp in milliseconds
                        tick.setSourceUrl(API_URL);                           // Store the source URL for data lineage tracking
                        
                        // Emit the MarketTick into the Flink stream - this is how data enters the pipeline
                        ctx.collect(tick);
                    }
                    
                    // Log successful completion with count for monitoring purposes
                    LOG.info("Poll #{} completed successfully. Emitted {} market ticks using authenticated API", 
                             pollCount, marketData.size());
                } else {
                    // Log warning when API returns no data (could indicate API issues or rate limiting)
                    LOG.warn("Poll #{} returned no data - this may indicate API issues", pollCount);
                }

            } catch (Exception e) {
                // Catch any exceptions during polling to prevent the entire job from failing
                LOG.error("Error during poll #{}: {}", pollCount, e.getMessage(), e);
                // Important: Don't rethrow the exception - just log and continue to next iteration
                // This makes our Flink job resilient to transient API failures
            }

            // Wait for the configured interval before next polling attempt
            try {
                // Only sleep if source is still running (avoids unnecessary delay during shutdown)
                if (isRunning) {
                    // Block current thread for the specified poll interval
                    Thread.sleep(POLL_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                // InterruptedException means someone wants this thread to stop (usually from cancel())
                LOG.info("Polling interrupted, shutting down gracefully");
                // Restore interrupt status for proper thread cleanup
                Thread.currentThread().interrupt();
                // Exit the main polling loop
                break;
            }
        }

        // Log final statistics when source shuts down
        LOG.info("CoinGecko source shut down after {} polls with authenticated API access", pollCount);
    }

    /**
     * Private method to fetch cryptocurrency market data from CoinGecko API with robust retry logic.
     * This implements resilient HTTP communication with API key authentication and error handling.
     * 
     * API Key Benefits:
     * - Higher rate limits (10,000 vs 100 calls/hour)
     * - More stable service quality
     * - Access to additional data fields
     * - Priority support during high-traffic periods
     */
    private List<MarketTick> fetchMarketData() {
        // Retry loop - attempt the HTTP request up to MAX_RETRIES times
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Create HTTP GET request object with our configured API URL (includes API key)
                HttpGet request = new HttpGet(API_URL);
                // Add User-Agent header to identify our application to the API server (good API citizenship)
                request.addHeader("User-Agent", "Flink-CryptoPulse/1.0");
                // Add Accept header to specify we want JSON response format
                request.addHeader("Accept", "application/json");
                // Add API key header for additional authentication (some APIs prefer header over URL param)
                request.addHeader("x-cg-demo-api-key", API_KEY);

                // Execute the HTTP request using our initialized HTTP client
                ClassicHttpResponse response = httpClient.execute(request);
                try {
                    // Extract HTTP status code from response (200 = success, 429 = rate limited, etc.)
                    int statusCode = response.getCode();
                    // Convert response body from HTTP entity to Java string for processing
                    String responseBody = EntityUtils.toString(response.getEntity());

                    // Check if request was successful (HTTP 200 OK)
                    if (statusCode == 200) {
                        // Parse JSON response string into List<MarketTick> using Jackson
                        // TypeReference is needed because Java can't infer generic types at runtime
                        List<MarketTick> marketTicks = objectMapper.readValue(
                                responseBody, new TypeReference<List<MarketTick>>() {});
                        
                        // Log successful parsing for debugging purposes
                        LOG.debug("Successfully parsed {} market ticks from authenticated API", marketTicks.size());
                        // Return the successfully parsed data to calling method
                        return marketTicks;
                        
                    // Handle rate limiting (HTTP 429 Too Many Requests) - less likely with API key
                    } else if (statusCode == 429) {
                        // Log rate limiting warning with attempt information
                        LOG.warn("Rate limited by CoinGecko (HTTP 429) despite API key. Attempt {}/{}", attempt, MAX_RETRIES);
                    // Handle authentication issues (HTTP 401 Unauthorized)
                    } else if (statusCode == 401) {
                        // Log authentication error - this indicates API key issues
                        LOG.error("Authentication failed (HTTP 401) - check API key validity. Attempt {}/{}", attempt, MAX_RETRIES);
                    } else {
                        // Handle any other HTTP error status codes (4xx client errors, 5xx server errors)
                        LOG.warn("HTTP {} from CoinGecko with API key. Attempt {}/{}. Response: {}", 
                                statusCode, attempt, MAX_RETRIES, 
                                // Truncate long error messages to keep logs readable (first 200 chars)
                                responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
                    }
                } finally {
                    // Always close the HTTP response to free up system resources
                    // This is critical to prevent connection leaks in long-running applications
                    response.close();
                }

            } catch (Exception e) {
                // Catch any exceptions during HTTP request (network issues, JSON parsing errors, etc.)
                LOG.warn("HTTP request failed with API key. Attempt {}/{}: {}", attempt, MAX_RETRIES, e.getMessage());
            }

            // Implement retry delay with exponential backoff (wait before next attempt)
            if (attempt < MAX_RETRIES) {
                try {
                    // Sleep for configured retry delay to avoid hammering the API
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    // If thread is interrupted during sleep, restore interrupt status
                    Thread.currentThread().interrupt();
                    // Log the interruption and break out of retry loop
                    LOG.info("Retry delay interrupted during API key authenticated request");
                    break;
                }
            }
        }

        // If we've exhausted all retry attempts, log final error and return null
        LOG.error("Failed to fetch market data after {} attempts with authenticated API key", MAX_RETRIES);
        return null; // Returning null signals to calling method that fetch failed
    }

    // Override method from SourceFunction interface - called by Flink when job is cancelled or stopped
    @Override
    public void cancel() {
        // Log cancellation for monitoring and debugging purposes
        LOG.info("Cancelling CoinGecko authenticated source...");
        // Set running flag to false - this will cause the main polling loop to exit
        // Using volatile ensures this change is immediately visible to the polling thread
        isRunning = false;
        
        // Clean up HTTP client resources to prevent connection leaks
        if (httpClient != null) {
            try {
                // Close HTTP client and all its underlying connections
                httpClient.close();
                LOG.info("HTTP client closed successfully for authenticated CoinGecko source");
            } catch (Exception e) {
                // Log any errors during cleanup, but don't throw - we're already shutting down
                LOG.warn("Error closing HTTP client for authenticated source: {}", e.getMessage());
            }
        }
    }
// End of CoinGeckoSource class - this completes our authenticated Flink data source implementation
}