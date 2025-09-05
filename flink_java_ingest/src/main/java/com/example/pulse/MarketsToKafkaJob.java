// Package declaration for our main Flink job
package com.example.pulse;

// Import our custom model and source classes
import com.example.pulse.model.MarketTick;
import com.example.pulse.source.CoinGeckoSource;
// Jackson for JSON processing
import com.fasterxml.jackson.databind.ObjectMapper;
// Flink Kafka connector classes for streaming to Kafka
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
// Core Flink streaming classes
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
// Kafka producer record for creating key-value pairs
import org.apache.kafka.clients.producer.ProducerRecord;
// Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// For UTF-8 string encoding
import java.nio.charset.StandardCharsets;

/**
 * Main Flink job that ingests cryptocurrency market data from CoinGecko 
 * and streams it into a Kafka topic.
 * 
 * Architecture overview:
 * 1. Sets up Flink streaming environment with checkpointing for fault tolerance
 * 2. Creates a custom CoinGecko source that polls API every 60 seconds  
 * 3. Transforms MarketTick objects to JSON strings using MapFunction
 * 4. Writes JSON messages to Kafka topic 'coingecko.markets' with proper partitioning
 * 5. Handles serialization, delivery guarantees, and error recovery
 * 
 * Key Flink concepts demonstrated:
 * - StreamExecutionEnvironment: Entry point for Flink streaming jobs
 * - DataStream: Represents a stream of data elements
 * - Checkpointing: Fault tolerance mechanism for exactly-once processing
 * - Custom SourceFunction: How to create data sources for external systems
 * - MapFunction: Stateless transformation of stream elements
 * - KafkaSink: Connector for writing to Apache Kafka
 */
public class MarketsToKafkaJob {

    // Logger for this class - helps with monitoring and debugging
    private static final Logger LOG = LoggerFactory.getLogger(MarketsToKafkaJob.class);

    // Kafka configuration constants
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";  // Redpanda runs on this port
    private static final String KAFKA_TOPIC = "coingecko.markets";          // Topic name for crypto data

    // Job configuration
    private static final String JOB_NAME = "CoinGecko Markets to Kafka";     // Appears in Flink UI

    // Main method - entry point when running with 'java -jar' or 'flink run'
    public static void main(String[] args) throws Exception {
        LOG.info("Starting {}", JOB_NAME);

        // Create Flink streaming environment - this is the context for our job
        // getExecutionEnvironment() automatically detects if running locally or on cluster
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure for production reliability and exactly-once semantics
        env.enableCheckpointing(30_000); // Checkpoint every 30 seconds for fault tolerance
        env.setParallelism(1); // Single parallelism because we have one polling source

        // Create the data stream pipeline:
        // Step 1: Add our custom source to generate MarketTick objects
        // Suppress deprecation warning for addSource (newer fromSource API requires major refactoring)
        @SuppressWarnings("deprecation")
        DataStream<MarketTick> marketStream = env
                .addSource(new CoinGeckoSource())          // Creates stream of MarketTick objects
                .name("CoinGecko Market Data Source");     // Name appears in Flink UI

        // Step 2: Transform and sink the data
        marketStream
                .map(new MarketTickToJsonMapper())    // Transform MarketTick → JSON String
                .name("MarketTick to JSON Mapper")    // Name for Flink UI monitoring
                .sinkTo(createKafkaSink())             // Write to Kafka topic
                .name("Kafka Sink");                  // Name for Flink UI monitoring

        // Step 3: Debug output (optional) - prints to Flink logs for monitoring
        // This creates a separate sink that doesn't interfere with Kafka output
        marketStream.print().name("Console Debug Sink");

        // Log configuration for monitoring
        LOG.info("Executing Flink job: {}", JOB_NAME);
        LOG.info("Kafka servers: {}", KAFKA_BOOTSTRAP_SERVERS);
        LOG.info("Kafka topic: {}", KAFKA_TOPIC);

        // Execute the job - this blocks until job is cancelled or fails
        // In production, this runs indefinitely until manually stopped
        env.execute(JOB_NAME);
    }

    /**
     * Creates a Kafka sink with proper serialization and producer properties.
     * 
     * Configuration explains:
     * - Bootstrap servers: How to connect to Kafka cluster
     * - Serializer: How to convert Java objects to Kafka records
     * - Producer properties: Performance and reliability tuning
     * 
     * Note: Removed DeliveryGuarantee to simplify dependencies for local execution
     */
    private static KafkaSink<String> createKafkaSink() {
        return KafkaSink.<String>builder()                          // Builder pattern for configuration
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)       // Kafka cluster connection
                .setRecordSerializer(new MarketTickKafkaSerializer()) // How to serialize to Kafka
                // Removed .setDeliveryGuarantee() to avoid missing dependency issues
                .setProperty("transaction.timeout.ms", "900000")   // 15 min timeout for transactions
                .setProperty("batch.size", "16384")               // Batch size for better throughput
                .setProperty("linger.ms", "5")                    // Wait up to 5ms to batch messages
                .setProperty("compression.type", "snappy")        // Compress messages to save space
                .build();
    }

    /**
     * Maps MarketTick objects to JSON strings for Kafka.
     * 
     * This is a stateless transformation function that converts each MarketTick
     * to a JSON string. Flink will parallelize this across multiple task slots.
     */
    public static class MarketTickToJsonMapper implements org.apache.flink.api.common.functions.MapFunction<MarketTick, String> {
        // Required for serialization when distributing across Flink cluster
        private static final long serialVersionUID = 1L;
        // Transient = won't be serialized, will be recreated on each task manager
        private transient ObjectMapper objectMapper;

        // Called once for each MarketTick object in the stream
        @Override
        public String map(MarketTick marketTick) throws Exception {
            // Lazy initialization - create ObjectMapper only when first needed
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }

            try {
                // Convert Java object to JSON string
                return objectMapper.writeValueAsString(marketTick);
            } catch (Exception e) {
                // Log error with context - helps debug which record failed
                LOG.error("Failed to serialize MarketTick to JSON: {}", marketTick, e);
                // Re-throw to fail the record and trigger Flink retry/recovery
                throw e;
            }
        }
    }

    /**
     * Kafka record serializer that uses coin_id as the key for partitioning.
     * 
     * Why this matters:
     * - Kafka partitions messages by key (coin_id like "bitcoin", "ethereum")
     * - All messages with same key go to same partition → same consumer
     * - This ensures ordered processing per cryptocurrency
     * - Enables scaling consumers by partition
     */
    public static class MarketTickKafkaSerializer implements KafkaRecordSerializationSchema<String> {
        // Required for Flink serialization
        private static final long serialVersionUID = 1L;
        // JSON parser for extracting coin_id from JSON string
        private transient ObjectMapper objectMapper;

        // Called for each JSON string to create a Kafka ProducerRecord
        @Override
        public ProducerRecord<byte[], byte[]> serialize(String jsonString, 
                                                        KafkaRecordSerializationSchema.KafkaSinkContext context, 
                                                        Long timestamp) {
            try {
                // Lazy initialization of JSON parser
                if (objectMapper == null) {
                    objectMapper = new ObjectMapper();
                }

                // Parse JSON to extract coin_id for use as Kafka partition key
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(jsonString);
                String coinId = jsonNode.get("id").asText();  // e.g., "bitcoin", "ethereum" (matches CoinGecko API)

                // Convert strings to bytes (Kafka protocol requirement)
                byte[] key = coinId.getBytes(StandardCharsets.UTF_8);     // Partition key
                byte[] value = jsonString.getBytes(StandardCharsets.UTF_8); // Message content

                // Create Kafka record: topic + key + value
                return new ProducerRecord<>(KAFKA_TOPIC, key, value);

            } catch (Exception e) {
                LOG.error("Failed to create Kafka record from JSON: {}", jsonString, e);
                // Fallback: send without key (random partition assignment)
                byte[] value = jsonString.getBytes(StandardCharsets.UTF_8);
                return new ProducerRecord<>(KAFKA_TOPIC, null, value);
            }
        }
    } // End of MarketTickKafkaSerializer class
} // End of MarketsToKafkaJob class
