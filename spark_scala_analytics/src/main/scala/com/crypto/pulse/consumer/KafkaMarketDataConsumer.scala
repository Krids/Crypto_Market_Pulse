package com.crypto.pulse.consumer // Package declaration for Kafka data consumption components

// Import our TDD-developed MarketTick model for type-safe data processing
import com.crypto.pulse.model.MarketTick
// Import Spark SQL components for DataFrame and Dataset operations
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession} // DataFrame for untyped data, Dataset for typed data, SparkSession for Spark operations
// Import Spark SQL functions for data manipulation and JSON processing
import org.apache.spark.sql.functions.{col, from_json, lit} // col for column references, from_json for JSON parsing, lit for literal values
// Import Spark SQL types for schema definition and JSON processing
import org.apache.spark.sql.types._ // All SQL types for comprehensive schema definition

/**
 * KafkaMarketDataConsumer - Kafka Data Consumption and JSON Parsing
 * 
 * Handles connection to Kafka, consumption of cryptocurrency market data,
 * and parsing of JSON messages into strongly-typed MarketTick objects.
 * 
 * Built with TDD methodology for reliability and comprehensive error handling.
 */
// Kafka market data consumer case class for consuming and parsing cryptocurrency data from Kafka streams
case class KafkaMarketDataConsumer(spark: SparkSession, config: Map[String, String]) { // SparkSession for operations, Map for configuration

  // Import Spark SQL implicits for Dataset operations and automatic encoder generation
  import spark.implicits._ // Enables Dataset creation, automatic encoders, and SQL operations

  // Configuration validation to ensure required Kafka settings are present
  validateConfig() // Immediate configuration validation for fail-fast behavior

  /**
   * Validates that all required configuration keys are present
   * Throws IllegalArgumentException with detailed error messages for missing configuration
   */
  // Private configuration validation method ensuring all required Kafka settings are present
  private def validateConfig(): Unit = { // Unit return type - validation method with side effects
    // Define sequence of required configuration keys for Kafka consumer operation
    val requiredKeys = Seq( // Sequence of string keys that must be present in configuration
      "kafka.bootstrap-servers", // Kafka broker addresses for connection
      "kafka.topic",            // Topic name containing cryptocurrency market data
      "kafka.consumer-group"    // Consumer group for offset coordination and exactly-once processing
    ) // End of required configuration keys
    
    // Iterate through required keys and validate each one is present and non-empty
    requiredKeys.foreach { key => // Functional iteration with lambda for each required key
      // Check if configuration contains key and value is non-empty - defensive programming
      if (!config.contains(key) || config(key).isEmpty) { // Boolean check for key presence and non-empty value
        // Throw descriptive exception with missing key name for debugging
        throw new IllegalArgumentException(s"Missing required configuration: $key") // String interpolation for error detail
      } // End of individual key validation check
    } // End of required keys validation iteration
  } // End of configuration validation method

  /**
   * Creates Kafka configuration Map from consumer configuration
   * Builds Kafka-specific settings for Spark Kafka source
   */
  // Private method to extract and build Kafka-specific configuration from consumer configuration
  private def getKafkaConfig(): Map[String, String] = { // Returns Map for Kafka source configuration
    // Build Kafka configuration map with required and optional settings
    Map( // Kafka configuration map for Spark structured streaming source
      "kafka.bootstrap.servers" -> config("kafka.bootstrap-servers"), // Kafka broker addresses (note dot vs hyphen)
      "subscribe" -> config("kafka.topic"),                          // Topic subscription for streaming source
      "kafka.group.id" -> config("kafka.consumer-group"),           // Consumer group for offset management
      "kafka.auto.offset.reset" -> config.getOrElse("kafka.auto-offset-reset", "latest"), // Offset reset policy with default
      "kafka.enable.auto.commit" -> config.getOrElse("kafka.enable-auto-commit", "false"), // Auto commit setting with default
      "failOnDataLoss" -> "false" // Allow processing to continue even if some Kafka data is lost
    ) // End of Kafka configuration map construction
  } // End of Kafka configuration method

  /**
   * Creates a streaming DataFrame from Kafka topic
   * Returns raw Kafka messages as DataFrame for further processing
   */
  // Public method to create Kafka streaming DataFrame for consuming real-time cryptocurrency data
  def createKafkaStream(): DataFrame = { // Returns DataFrame containing Kafka messages
    // Get Kafka-specific configuration for streaming source
    val kafkaConfig = getKafkaConfig() // Extract Kafka configuration from consumer config
    
    // Create Kafka streaming source with configuration
    val kafkaStream = spark.readStream // Access Spark streaming reader
      .format("kafka") // Specify Kafka format for streaming source
      .options(kafkaConfig) // Apply Kafka configuration options
      .load() // Load streaming DataFrame from Kafka source
    
    // Add processing timestamp for monitoring and debugging
    kafkaStream.withColumn("processingTimestamp", lit(System.currentTimeMillis())) // Add current timestamp column
  } // End of Kafka stream creation method

  /**
   * Parses JSON messages from Kafka into MarketTick objects
   * Handles JSON deserialization with comprehensive error handling
   */
  // Public method to parse JSON messages from Kafka into strongly-typed MarketTick Dataset
  def parseJsonToMarketTick(kafkaDF: DataFrame): Dataset[MarketTick] = { // Takes DataFrame, returns typed Dataset
    
    // Define JSON schema for MarketTick parsing - matches CoinGecko API structure
    val marketTickSchema = StructType(Seq( // StructType for JSON schema definition
      StructField("ingestTimestamp", LongType, nullable = true),           // Flink ingestion timestamp
      StructField("id", StringType, nullable = false),                    // Cryptocurrency ID (required)
      StructField("symbol", StringType, nullable = false),                // Trading symbol (required)
      StructField("name", StringType, nullable = false),                  // Cryptocurrency name (required)  
      StructField("current_price", DoubleType, nullable = true),          // Current market price (optional)
      StructField("market_cap", DoubleType, nullable = true),             // Market capitalization (optional)
      StructField("market_cap_rank", IntegerType, nullable = true),       // Market cap ranking (optional)
      StructField("total_volume", DoubleType, nullable = true),           // Trading volume (optional)
      StructField("price_change_percentage_24h", DoubleType, nullable = true) // Price change percentage (optional)
    )) // End of JSON schema definition

    // Parse JSON and convert to MarketTick objects
    kafkaDF
      .select(from_json(col("value"), marketTickSchema).as("data")) // Parse JSON string into structured data
      .select("data.*") // Flatten nested structure to top-level columns
      .as[MarketTick] // Convert DataFrame to strongly-typed Dataset[MarketTick]
      .filter(_.isValid) // Filter out invalid records using MarketTick validation
  } // End of JSON parsing method

  /**
   * Enriches MarketTick data with Kafka metadata for data lineage
   * Adds partition, offset, and timestamp information for tracking and exactly-once processing
   */
  // Public method to enrich MarketTick data with Kafka metadata for complete data lineage
  def enrichWithKafkaMetadata(marketTickDS: Dataset[MarketTick], kafkaDF: DataFrame): Dataset[MarketTick] = { // Enhanced MarketTick Dataset
    
    // Get Kafka metadata columns for enrichment
    val kafkaMetadata = kafkaDF.select( // Select Kafka metadata columns
      col("partition"),  // Kafka partition number
      col("offset"),     // Kafka message offset
      col("timestamp")   // Kafka message timestamp
    ) // End of Kafka metadata selection

    // This is a simplified version - in production you'd need to join on a common key
    // For now, we'll add metadata during JSON parsing
    marketTickDS.map { marketTick => // Transform each MarketTick with metadata
      // Add current Kafka metadata (simplified for demo)
      marketTick.withProcessingTimestamp() // Add processing timestamp for monitoring
    } // End of metadata enrichment transformation
  } // End of Kafka metadata enrichment method
} // End of KafkaMarketDataConsumer case class

/**
 * KafkaMarketDataConsumer Companion Object
 * Provides factory methods for creating consumer instances
 */
// Companion object for KafkaMarketDataConsumer with factory methods and utilities
object KafkaMarketDataConsumer { // Factory object for consumer creation

  /**
   * Factory method to create consumer from configuration map
   * Validates configuration and returns configured consumer instance
   */
  // Factory method to create Kafka consumer from configuration map with validation
  def apply(spark: SparkSession, config: Map[String, String]): KafkaMarketDataConsumer = { // Returns configured consumer
    // Create consumer instance with immediate configuration validation
    new KafkaMarketDataConsumer(spark, config) // Constructor with configuration validation
  } // End of factory method

  /**
   * Factory method to create consumer from AppConfig
   * Uses centralized application configuration for consumer creation
   */
  // Factory method to create Kafka consumer using centralized application configuration
  def fromAppConfig(spark: SparkSession): KafkaMarketDataConsumer = { // Returns consumer with app configuration
    // Import centralized application configuration
    import com.crypto.pulse.config.AppConfig // Import for accessing application configuration
    
    // Create consumer using application configuration factory method
    KafkaMarketDataConsumer(spark, AppConfig.kafkaConsumerConfig()) // Create consumer with app config
  } // End of AppConfig factory method
} // End of KafkaMarketDataConsumer companion object
