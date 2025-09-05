// TDD STEP 2: Test the MapFunction and Kafka Serializer components
// These are critical parts of our Flink pipeline that transform data
package com.example.pulse;

// Testing imports
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

// Flink testing utilities
import org.apache.flink.api.common.functions.util.ListCollector;

// Our classes under test
import com.example.pulse.model.MarketTick;
import com.example.pulse.MarketsToKafkaJob.MarketTickToJsonMapper;
import com.example.pulse.MarketsToKafkaJob.MarketTickKafkaSerializer;

// Kafka and JSON
import org.apache.kafka.clients.producer.ProducerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

// Collections
import java.util.ArrayList;
import java.util.List;

/**
 * TDD tests for the main Flink job components.
 * 
 * Testing Strategy:
 * 1. Test MapFunction (MarketTick → JSON String transformation)
 * 2. Test Kafka Serializer (JSON String → Kafka ProducerRecord)
 * 3. Test error handling and edge cases
 * 4. Test data flow integration
 * 
 * Why test these components?
 * - MapFunction: Core data transformation logic
 * - Kafka Serializer: Partitioning and message structure
 * - Integration: End-to-end data flow verification
 */
@DisplayName("Flink Job Components Tests - TDD Approach")
class MarketsToKafkaJobTest {

    // Test fixtures - shared across multiple tests
    private MarketTickToJsonMapper jsonMapper;
    private MarketTickKafkaSerializer kafkaSerializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // ARRANGE: Initialize test fixtures with fresh instances
        // This ensures test isolation - each test starts with clean state
        jsonMapper = new MarketTickToJsonMapper();
        kafkaSerializer = new MarketTickKafkaSerializer();
        objectMapper = new ObjectMapper();
    }

    // TDD TEST 1: MapFunction should convert MarketTick to valid JSON
    @Test
    @DisplayName("MapFunction should convert MarketTick object to JSON string")
    void mapFunctionShouldConvertMarketTickToJson() throws Exception {
        // ARRANGE: Create a sample MarketTick object
        MarketTick marketTick = new MarketTick();
        marketTick.setCoinId("bitcoin");
        marketTick.setSymbol("btc");
        marketTick.setName("Bitcoin");
        marketTick.setCurrentPrice(50000.0);
        marketTick.setMarketCap(1000000000000L);
        marketTick.setSourceUrl("https://api.coingecko.com/test");
        
        // ACT: Transform MarketTick to JSON using our MapFunction
        String jsonResult = jsonMapper.map(marketTick);
        
        // ASSERT: Verify the JSON is valid and contains expected data
        assertThat(jsonResult)
            .as("MapFunction should produce valid JSON string")
            .isNotNull()
            .isNotEmpty();
            
        // Parse the JSON back to verify it's valid JSON
        MarketTick parsedTick = objectMapper.readValue(jsonResult, MarketTick.class);
        
        assertThat(parsedTick.getCoinId())
            .as("Coin ID should be preserved in JSON transformation")
            .isEqualTo("bitcoin");
            
        assertThat(parsedTick.getCurrentPrice())
            .as("Price should be preserved in JSON transformation")  
            .isEqualTo(50000.0);
            
        assertThat(parsedTick.getSourceUrl())
            .as("Source URL should be preserved in JSON transformation")
            .isEqualTo("https://api.coingecko.com/test");
    }

    // TDD TEST 2: MapFunction should handle null values gracefully
    @Test
    @DisplayName("MapFunction should handle MarketTick with null values")
    void mapFunctionShouldHandleNullValues() throws Exception {
        // ARRANGE: Create MarketTick with some null values (real-world scenario)
        MarketTick marketTick = new MarketTick();
        marketTick.setCoinId("test-coin");
        marketTick.setCurrentPrice(null);  // null price
        marketTick.setMarketCap(null);     // null market cap
        marketTick.setName("Test Coin");
        
        // ACT: Transform to JSON
        String jsonResult = jsonMapper.map(marketTick);
        
        // ASSERT: Should handle nulls without throwing exception
        assertThat(jsonResult)
            .as("Should produce JSON even with null values")
            .isNotNull()
            .contains("\"id\":\"test-coin\"")  // Note: using CoinGecko field name "id"
            .contains("\"current_price\":null")  // Note: using CoinGecko field name
            .contains("\"market_cap\":null");    // Note: using CoinGecko field name
    }

    // TDD TEST 3: Kafka Serializer should create ProducerRecord with correct partitioning
    @Test
    @DisplayName("Kafka Serializer should create ProducerRecord with coin_id as key")
    void kafkaSerializerShouldCreateProducerRecordWithKey() throws Exception {
        // ARRANGE: JSON string representing a MarketTick (using CoinGecko field names)
        String jsonInput = """
            {
                "id": "ethereum",
                "symbol": "eth", 
                "current_price": 3000.0,
                "market_cap": 350000000000
            }
        """;
        
        // ACT: Serialize to Kafka ProducerRecord
        // Note: KafkaSinkContext parameter can be null for testing, timestamp is optional
        ProducerRecord<byte[], byte[]> record = kafkaSerializer.serialize(jsonInput, null, null);
        
        // ASSERT: Verify ProducerRecord structure
        assertThat(record.topic())
            .as("Should use correct Kafka topic")
            .isEqualTo("coingecko.markets");
            
        // Verify the key (used for partitioning)
        assertThat(record.key())
            .as("Should have a key for partitioning")
            .isNotNull();
            
        String keyString = new String(record.key());
        assertThat(keyString)
            .as("Key should be the coin ID for proper partitioning")
            .isEqualTo("ethereum");
            
        // Verify the value (message content)
        assertThat(record.value())
            .as("Should have message value")
            .isNotNull();
            
        String valueString = new String(record.value());
        assertThat(valueString)
            .as("Value should contain the original JSON")
            .contains("ethereum")
            .contains("3000.0");
    }

    // TDD TEST 4: Kafka Serializer should handle malformed JSON gracefully
    @Test
    @DisplayName("Kafka Serializer should handle malformed JSON without crashing")
    void kafkaSerializerShouldHandleMalformedJson() {
        // ARRANGE: Invalid JSON string
        String malformedJson = "{ invalid json structure";
        
        // ACT: Attempt to serialize malformed JSON
        // Testing error handling - malformed JSON should be handled gracefully
        ProducerRecord<byte[], byte[]> record = kafkaSerializer.serialize(malformedJson, null, null);
        
        // ASSERT: Should not crash, should fallback gracefully
        assertThat(record)
            .as("Should not return null even for malformed JSON")
            .isNotNull();
            
        assertThat(record.topic())
            .as("Should still use correct topic")
            .isEqualTo("coingecko.markets");
            
        assertThat(record.key())
            .as("Should have no key when JSON parsing fails (fallback behavior)")
            .isNull();
            
        // Value should still contain the original string (even if malformed)
        String valueString = new String(record.value());
        assertThat(valueString)
            .as("Should preserve original JSON string even if malformed")
            .isEqualTo(malformedJson);
    }

    // TDD TEST 5: Integration test - full pipeline simulation
    @Test
    @DisplayName("Integration: MarketTick → JSON → Kafka Record pipeline")
    void shouldWorkEndToEndFromMarketTickToKafkaRecord() throws Exception {
        // ARRANGE: Start with MarketTick object (simulates data from CoinGecko source)
        MarketTick originalTick = new MarketTick();
        originalTick.setCoinId("cardano");
        originalTick.setSymbol("ada");
        originalTick.setName("Cardano");
        originalTick.setCurrentPrice(0.45);
        originalTick.setMarketCap(15000000000L);
        originalTick.setPriceChangePercentage24h(-2.3);
        
        // ACT: Simulate the complete Flink pipeline
        // Step 1: MarketTick → JSON (MapFunction)
        String jsonString = jsonMapper.map(originalTick);
        
        // Step 2: JSON → Kafka ProducerRecord (Kafka Serializer)
        // Simulating the full Flink pipeline transformation
        ProducerRecord<byte[], byte[]> kafkaRecord = kafkaSerializer.serialize(jsonString, null, System.currentTimeMillis());
        
        // ASSERT: Verify end-to-end data integrity
        assertThat(kafkaRecord.topic())
            .as("Final Kafka record should use correct topic")
            .isEqualTo("coingecko.markets");
            
        // Verify partitioning key (might be null if JSON parsing failed)
        assertThat(kafkaRecord.key())
            .as("Partition key should not be null")
            .isNotNull();
            
        String partitionKey = new String(kafkaRecord.key());
        assertThat(partitionKey)
            .as("Partition key should match original coin ID")
            .isEqualTo("cardano");
            
        // Verify message content by parsing it back
        String kafkaMessageJson = new String(kafkaRecord.value());
        MarketTick reconstructedTick = objectMapper.readValue(kafkaMessageJson, MarketTick.class);
        
        assertThat(reconstructedTick.getCoinId())
            .as("Coin ID should survive the complete pipeline")
            .isEqualTo("cardano");
            
        assertThat(reconstructedTick.getCurrentPrice())
            .as("Price should survive the complete pipeline")
            .isEqualTo(0.45);
            
        assertThat(reconstructedTick.getPriceChangePercentage24h())
            .as("Price change should survive the complete pipeline")
            .isEqualTo(-2.3);
    }

    // TDD TEST 6: Test Flink MapFunction with ListCollector (Flink testing pattern)
    @Test
    @DisplayName("MapFunction should work with Flink's ListCollector")
    void mapFunctionShouldWorkWithFlinkListCollector() throws Exception {
        // ARRANGE: Set up Flink test collector and sample data
        List<String> collectedResults = new ArrayList<>();
        ListCollector<String> collector = new ListCollector<>(collectedResults);
        
        MarketTick testTick = new MarketTick();
        testTick.setCoinId("polkadot");
        testTick.setSymbol("dot");
        testTick.setCurrentPrice(5.75);
        
        // ACT: Use MapFunction in Flink-style testing
        String result = jsonMapper.map(testTick);
        collector.collect(result);
        
        // ASSERT: Verify Flink collector behavior
        assertThat(collectedResults)
            .as("Collector should have received one result")
            .hasSize(1);
            
        String collectedJson = collectedResults.get(0);
        assertThat(collectedJson)
            .as("Collected JSON should contain expected data")
            .contains("polkadot")
            .contains("5.75");
    }
}

/*
 * TDD LEARNINGS FROM THIS TEST CLASS:
 * 
 * 1. ISOLATION: Each test focuses on one specific component/behavior
 * 
 * 2. REALISTIC DATA: We use real-world-like test data that matches 
 *    what CoinGecko API actually returns
 * 
 * 3. ERROR SCENARIOS: We test not just happy path but also edge cases
 *    like null values and malformed JSON
 * 
 * 4. INTEGRATION: We test how components work together in the pipeline
 * 
 * 5. FLINK-SPECIFIC: We use Flink testing utilities like ListCollector
 *    to simulate how the code runs in the actual Flink environment
 * 
 * 6. ASSERTIONS: We use meaningful assertion messages that help 
 *    diagnose failures quickly
 */
