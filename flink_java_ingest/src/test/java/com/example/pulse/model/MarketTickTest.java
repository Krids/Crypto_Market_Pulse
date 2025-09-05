// TDD STEP 1 (RED): Write failing tests FIRST, before any production code
// This forces us to think about the API and requirements upfront
package com.example.pulse.model;

// JUnit 5 imports for modern testing
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

// AssertJ for fluent, readable assertions (better than JUnit assertions)
import static org.assertj.core.api.Assertions.*;

// Jackson for JSON testing
import com.fasterxml.jackson.databind.ObjectMapper;

// Standard Java imports
import java.time.Instant;

/**
 * TDD Test class for MarketTick model.
 * 
 * TDD Philosophy:
 * 1. Write tests FIRST (they will fail - RED phase)
 * 2. Write minimal code to make tests pass (GREEN phase)
 * 3. Refactor while keeping tests green (REFACTOR phase)
 * 
 * Testing Strategy:
 * - Test JSON serialization (Java object → JSON string)
 * - Test JSON deserialization (JSON string → Java object)
 * - Test field mappings and data types
 * - Test edge cases (null values, missing fields)
 * - Test metadata generation (timestamps)
 */
@DisplayName("MarketTick Model Tests - TDD Approach")
class MarketTickTest {

    // Test fixture - ObjectMapper instance used across multiple tests
    // BeforeEach ensures fresh instance for each test (test isolation)
    private ObjectMapper objectMapper;
    
    // Sample JSON data that mimics CoinGecko API response
    // This is our "test data" that represents real-world input
    private String sampleCoinGeckoJson;
    
    @BeforeEach
    void setUp() {
        // ARRANGE phase - Set up test fixtures before each test
        // Fresh ObjectMapper prevents test interference
        objectMapper = new ObjectMapper();
        
        // Real CoinGecko JSON structure (simplified for testing)
        // Notice: Testing with realistic data catches real-world issues
        sampleCoinGeckoJson = """
            {
                "id": "bitcoin",
                "symbol": "btc",
                "name": "Bitcoin",
                "current_price": 45000.50,
                "market_cap": 850000000000,
                "market_cap_rank": 1,
                "total_volume": 25000000000,
                "price_change_percentage_24h": 2.5,
                "circulating_supply": 19500000.0,
                "last_updated": "2024-01-15T10:30:00.000Z"
            }
        """;
    }

    // TDD TEST 1: JSON Deserialization (CoinGecko JSON → MarketTick object)
    @Test
    @DisplayName("Should deserialize CoinGecko JSON to MarketTick object")
    void shouldDeserializeJsonToMarketTick() throws Exception {
        // ARRANGE: JSON string is already set up in setUp()
        
        // ACT: Convert JSON string to Java object
        // This is the core functionality we're testing
        MarketTick marketTick = objectMapper.readValue(sampleCoinGeckoJson, MarketTick.class);
        
        // ASSERT: Verify all fields were mapped correctly
        // Using AssertJ fluent assertions for better readability
        assertThat(marketTick.getCoinId())
            .as("Coin ID should be mapped from 'id' field")
            .isEqualTo("bitcoin");
            
        assertThat(marketTick.getSymbol())
            .as("Symbol should be mapped correctly")
            .isEqualTo("btc");
            
        assertThat(marketTick.getName())
            .as("Name should be mapped correctly")
            .isEqualTo("Bitcoin");
            
        assertThat(marketTick.getCurrentPrice())
            .as("Price should be mapped as Double")
            .isEqualTo(45000.50);
            
        assertThat(marketTick.getMarketCap())
            .as("Market cap should be mapped as Long")
            .isEqualTo(850000000000L);
            
        assertThat(marketTick.getMarketCapRank())
            .as("Market cap rank should be mapped as Integer")
            .isEqualTo(1);
            
        assertThat(marketTick.getTotalVolume())
            .as("Total volume should be mapped as Long")
            .isEqualTo(25000000000L);
            
        assertThat(marketTick.getPriceChangePercentage24h())
            .as("24h price change should be mapped as Double")
            .isEqualTo(2.5);
            
        assertThat(marketTick.getCirculatingSupply())
            .as("Circulating supply should be mapped as Double")
            .isEqualTo(19500000.0);
            
        assertThat(marketTick.getLastUpdated())
            .as("Last updated should be mapped as String")
            .isEqualTo("2024-01-15T10:30:00.000Z");
    }

    // TDD TEST 2: JSON Serialization (MarketTick object → JSON string)
    @Test
    @DisplayName("Should serialize MarketTick object to JSON")
    void shouldSerializeMarketTickToJson() throws Exception {
        // ARRANGE: Create a MarketTick object with test data
        MarketTick marketTick = new MarketTick();
        marketTick.setCoinId("ethereum");
        marketTick.setSymbol("eth");
        marketTick.setName("Ethereum");
        marketTick.setCurrentPrice(3000.75);
        marketTick.setMarketCap(360000000000L);
        marketTick.setMarketCapRank(2);
        
        // ACT: Convert Java object to JSON string
        String jsonResult = objectMapper.writeValueAsString(marketTick);
        
        // ASSERT: Verify JSON contains expected fields and values
        // Note: We test that serialization works, not exact JSON format
        assertThat(jsonResult)
            .as("JSON should contain id field (matching CoinGecko API format)")
            .contains("\"id\":\"ethereum\"");
            
        assertThat(jsonResult)
            .as("JSON should contain symbol field")
            .contains("\"symbol\":\"eth\"");
            
        assertThat(jsonResult)
            .as("JSON should contain current price (using CoinGecko field name)")
            .contains("\"current_price\":3000.75");
            
        assertThat(jsonResult)
            .as("JSON should contain market cap (using CoinGecko field name)")
            .contains("\"market_cap\":360000000000");
    }

    // TDD TEST 3: Handle missing/null JSON fields gracefully
    @Test
    @DisplayName("Should handle missing JSON fields without errors")
    void shouldHandleMissingJsonFields() throws Exception {
        // ARRANGE: JSON with some fields missing (real-world scenario)
        String incompleteJson = """
            {
                "id": "dogecoin",
                "symbol": "doge",
                "current_price": null,
                "market_cap_rank": 10
            }
        """;
        
        // ACT: Deserialize incomplete JSON
        MarketTick marketTick = objectMapper.readValue(incompleteJson, MarketTick.class);
        
        // ASSERT: Should not throw exception and handle nulls properly
        assertThat(marketTick.getCoinId())
            .as("Available field should be populated")
            .isEqualTo("dogecoin");
            
        assertThat(marketTick.getSymbol())
            .as("Available field should be populated")
            .isEqualTo("doge");
            
        assertThat(marketTick.getCurrentPrice())
            .as("Null price should be handled gracefully")
            .isNull();
            
        assertThat(marketTick.getName())
            .as("Missing field should be null")
            .isNull();
            
        assertThat(marketTick.getMarketCapRank())
            .as("Available field should be populated")
            .isEqualTo(10);
    }

    // TDD TEST 4: Test metadata generation (ingestion timestamp)
    @Test
    @DisplayName("Should automatically set ingestion timestamp on creation")
    void shouldSetIngestionTimestampOnCreation() {
        // ARRANGE: Record current time before object creation
        long beforeCreation = Instant.now().toEpochMilli();
        
        // ACT: Create new MarketTick object
        MarketTick marketTick = new MarketTick();
        
        // ARRANGE: Record current time after object creation
        long afterCreation = Instant.now().toEpochMilli();
        
        // ASSERT: Ingestion timestamp should be set automatically
        // and should be between our before/after timestamps
        assertThat(marketTick.getIngestTimestamp())
            .as("Ingestion timestamp should be set automatically")
            .isNotZero()
            .isGreaterThanOrEqualTo(beforeCreation)
            .isLessThanOrEqualTo(afterCreation);
    }

    // TDD TEST 5: Test round-trip conversion (JSON → Object → JSON)
    @Test
    @DisplayName("Should maintain data integrity in round-trip conversion")
    void shouldMaintainDataIntegrityInRoundTrip() throws Exception {
        // ARRANGE: Start with original JSON
        String originalJson = sampleCoinGeckoJson;
        
        // ACT: JSON → Object → JSON
        MarketTick marketTick = objectMapper.readValue(originalJson, MarketTick.class);
        String convertedBackJson = objectMapper.writeValueAsString(marketTick);
        MarketTick secondMarketTick = objectMapper.readValue(convertedBackJson, MarketTick.class);
        
        // ASSERT: Key data should survive round-trip conversion
        assertThat(secondMarketTick.getCoinId())
            .as("Coin ID should survive round-trip")
            .isEqualTo("bitcoin");
            
        assertThat(secondMarketTick.getCurrentPrice())
            .as("Price should survive round-trip")
            .isEqualTo(45000.50);
            
        assertThat(secondMarketTick.getMarketCap())
            .as("Market cap should survive round-trip")
            .isEqualTo(850000000000L);
    }

    // TDD TEST 6: Test data types and edge cases
    @Test
    @DisplayName("Should handle edge cases and data type boundaries")
    void shouldHandleEdgeCasesAndDataTypeBoundaries() throws Exception {
        // ARRANGE: JSON with edge case values
        String edgeCaseJson = """
            {
                "id": "test-coin",
                "current_price": 0.0,
                "market_cap": 0,
                "market_cap_rank": null,
                "price_change_percentage_24h": -99.99,
                "max_supply": null
            }
        """;
        
        // ACT: Deserialize edge case JSON
        MarketTick marketTick = objectMapper.readValue(edgeCaseJson, MarketTick.class);
        
        // ASSERT: Should handle zero values, negatives, and nulls correctly
        assertThat(marketTick.getCurrentPrice())
            .as("Zero price should be handled")
            .isEqualTo(0.0);
            
        assertThat(marketTick.getMarketCap())
            .as("Zero market cap should be handled")
            .isEqualTo(0L);
            
        assertThat(marketTick.getMarketCapRank())
            .as("Null rank should be handled")
            .isNull();
            
        assertThat(marketTick.getPriceChangePercentage24h())
            .as("Negative percentage should be handled")
            .isEqualTo(-99.99);
            
        assertThat(marketTick.getMaxSupply())
            .as("Null max supply should be handled")
            .isNull();
    }
}

/*
 * TDD LEARNING NOTES:
 * 
 * 1. RED PHASE: All these tests will FAIL initially because we haven't 
 *    implemented the MarketTick class yet (or it's incomplete)
 *    
 * 2. GREEN PHASE: We'll implement just enough code in MarketTick to make 
 *    these tests pass
 *    
 * 3. REFACTOR PHASE: We'll clean up the MarketTick implementation while 
 *    keeping all tests green
 *    
 * BENEFITS OF TDD:
 * - Tests serve as living documentation
 * - Forces us to think about edge cases upfront
 * - Prevents regression bugs
 * - Makes refactoring safer
 * - Improves code design (testable code is usually better designed)
 */
