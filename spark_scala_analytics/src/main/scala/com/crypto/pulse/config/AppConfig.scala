package com.crypto.pulse.config // Package declaration for application configuration management

// Import Typesafe Config library for application configuration management
import com.typesafe.config.{Config, ConfigFactory} // Config interface and ConfigFactory for loading configuration files

/**
 * AppConfig - Centralized Application Configuration
 * 
 * Provides type-safe access to application configuration from application.conf
 * and local.conf override files. Built with TDD principles for reliable configuration management.
 */
// Application configuration object providing centralized access to all configuration settings
object AppConfig { // Singleton object for application-wide configuration access

  // Load configuration with local overrides for development flexibility
  private val config: Config = { // Private immutable configuration loaded at startup
    // Load application.conf from resources as base configuration
    val baseConfig = ConfigFactory.load("application") // Load base application configuration
    
    // Attempt to load local.conf for development overrides
    val localConfig = try { // Exception handling for optional local configuration
      ConfigFactory.load("local") // Load local development overrides
    } catch { // Handle case where local.conf doesn't exist
      case _: Exception => ConfigFactory.empty() // Return empty config if local.conf missing
    } // End of local configuration loading
    
    // Merge local overrides with base configuration - local takes precedence
    localConfig.withFallback(baseConfig) // Combine configurations with local overrides
  } // End of configuration loading

  /**
   * Kafka Configuration Object
   * Provides type-safe access to Kafka connection and topic settings
   */
  // Kafka configuration nested object providing connection and topic settings
  object Kafka { // Kafka-specific configuration object
    // Get Kafka broker addresses from configuration
    val bootstrapServers: String = config.getString("crypto-pulse.kafka.bootstrap-servers") // Kafka broker connection string
    
    // Get Kafka topic name containing cryptocurrency data
    val topic: String = config.getString("crypto-pulse.kafka.topic") // Kafka topic for market data
    
    // Get consumer group name for offset management
    val consumerGroup: String = config.getString("crypto-pulse.kafka.consumer-group") // Consumer group identifier
    
    // Get auto offset reset policy for consumer initialization
    val autoOffsetReset: String = config.getString("crypto-pulse.kafka.auto-offset-reset") // Offset reset strategy
    
    // Get auto commit setting for exactly-once processing
    val enableAutoCommit: Boolean = config.getBoolean("crypto-pulse.kafka.enable-auto-commit") // Auto commit flag
  } // End of Kafka configuration object

  /**
   * Storage Configuration Object  
   * Provides type-safe access to data storage paths and settings
   */
  // Storage configuration nested object providing data persistence settings
  object Storage { // Storage-specific configuration object
    // Get base storage directory path
    val basePath: String = config.getString("crypto-pulse.storage.base-path") // Base storage directory
    
    // Get Parquet files base directory
    val parquetPath: String = config.getString("crypto-pulse.storage.parquet-path") // Parquet storage path
    
    // Get market data specific storage path
    val marketDataPath: String = config.getString("crypto-pulse.storage.market-data-path") // Market data storage location
    
    // Get checkpoint location for fault tolerance
    val checkpointLocation: String = config.getString("crypto-pulse.storage.checkpoint-location") // Checkpoint storage path
    
    // Get compression algorithm for storage optimization
    val compression: String = config.getString("crypto-pulse.storage.compression") // Storage compression format
    
    // Get storage format specification
    val format: String = config.getString("crypto-pulse.storage.format") // Data storage format
  } // End of Storage configuration object

  /**
   * Spark Configuration Object
   * Provides type-safe access to Spark processing and streaming settings
   */
  // Spark configuration nested object providing streaming and processing settings
  object Spark { // Spark-specific configuration object
    // Get Spark application name for identification
    val appName: String = config.getString("crypto-pulse.spark.app-name") // Spark application identifier
    
    /**
     * Spark Streaming Configuration
     * Settings specific to Spark structured streaming
     */
    // Nested object for Spark streaming-specific configuration
    object Streaming { // Streaming configuration nested object
      // Get streaming trigger interval for batch processing
      val triggerInterval: String = config.getString("crypto-pulse.spark.streaming.trigger-interval") // Batch processing interval
      
      // Get checkpoint interval for state persistence
      val checkpointInterval: String = config.getString("crypto-pulse.spark.streaming.checkpoint-interval") // Checkpoint frequency
      
      // Get maximum files per trigger for performance tuning
      val maxFilesPerTrigger: Int = config.getInt("crypto-pulse.spark.streaming.max-files-per-trigger") // File processing limit
    } // End of Streaming configuration object

    /**
     * Spark Partitioning Configuration
     * Settings for data partitioning and file organization
     */
    // Nested object for Spark partitioning and data organization configuration
    object Partitioning { // Partitioning configuration nested object
      // Get partitioning column names for data organization
      val columns: List[String] = { // List of column names for partitioning
        import scala.jdk.CollectionConverters._ // Import for Java collection conversion
        config.getStringList("crypto-pulse.spark.partitioning.columns").asScala.toList // Convert Java list to Scala list
      } // End of partitioning columns
      
      // Get maximum records per file for optimal file sizes
      val maxRecordsPerFile: Int = config.getInt("crypto-pulse.spark.partitioning.max-records-per-file") // File size limit
    } // End of Partitioning configuration object
  } // End of Spark configuration object

  /**
   * Data Quality Configuration Object
   * Provides settings for data validation and quality monitoring
   */
  // Data quality configuration nested object for validation and monitoring settings
  object DataQuality { // Data quality configuration object
    
    /**
     * Data Validation Configuration
     * Settings for data validation rules and policies
     */
    // Nested object for data validation configuration
    object Validation { // Validation configuration nested object
      // Check if data validation is enabled
      val enabled: Boolean = config.getBoolean("crypto-pulse.data-quality.validation.enabled") // Validation enable flag
      
      // Check if invalid records should be rejected
      val rejectInvalidRecords: Boolean = config.getBoolean("crypto-pulse.data-quality.validation.reject-invalid-records") // Rejection policy
      
      // Get maximum allowed null percentage for data quality
      val maxNullPercentage: Int = config.getInt("crypto-pulse.data-quality.validation.max-null-percentage") // Null data threshold
    } // End of Validation configuration object

    /**
     * Data Quality Metrics Configuration
     * Settings for quality metrics collection and reporting
     */
    // Nested object for data quality metrics configuration
    object Metrics { // Metrics configuration nested object
      // Check if quality metrics collection is enabled
      val enabled: Boolean = config.getBoolean("crypto-pulse.data-quality.metrics.enabled") // Metrics enable flag
      
      // Get metrics collection interval for quality monitoring
      val collectionInterval: String = config.getString("crypto-pulse.data-quality.metrics.collection-interval") // Metrics frequency
    } // End of Metrics configuration object
  } // End of DataQuality configuration object

  /**
   * Logging Configuration Object
   * Provides settings for application logging and monitoring
   */
  // Logging configuration nested object for application logging settings
  object Logging { // Logging configuration object
    // Get logging level for application output control
    val level: String = config.getString("crypto-pulse.logging.level") // Log level setting
    
    // Get logging pattern for consistent log formatting
    val pattern: String = config.getString("crypto-pulse.logging.pattern") // Log format pattern
  } // End of Logging configuration object

  /**
   * Factory method to create Kafka consumer configuration Map
   * Returns configuration suitable for KafkaMarketDataConsumer creation
   */
  // Factory method for creating Kafka consumer configuration map
  def kafkaConsumerConfig(): Map[String, String] = { // Returns String to String configuration map
    // Build comprehensive Kafka configuration map from application settings
    Map( // Configuration map for Kafka consumer creation
      "kafka.bootstrap-servers" -> Kafka.bootstrapServers,     // Kafka broker addresses
      "kafka.topic" -> Kafka.topic,                           // Topic containing market data
      "kafka.consumer-group" -> Kafka.consumerGroup,          // Consumer group for offset management
      "kafka.auto-offset-reset" -> Kafka.autoOffsetReset,     // Offset reset policy
      "kafka.enable-auto-commit" -> Kafka.enableAutoCommit.toString // Auto commit setting as string
    ) // End of Kafka configuration map
  } // End of Kafka consumer configuration factory

  /**
   * Factory method to create local storage configuration Map
   * Returns configuration suitable for ParquetTableManager creation
   */
  // Factory method for creating local storage configuration map
  def localStorageConfig(): Map[String, String] = { // Returns String to String configuration map
    // Build comprehensive storage configuration map from application settings
    Map( // Configuration map for storage manager creation
      "output.path" -> Storage.marketDataPath,                    // Output directory for market data
      "partitioning.columns" -> Spark.Partitioning.columns.mkString(","), // Partitioning columns as comma-separated string
      "compression" -> Storage.compression,                       // Compression algorithm for storage
      "max-records-per-file" -> Spark.Partitioning.maxRecordsPerFile.toString, // File size limit as string
      "write-mode" -> "append",                                  // Write mode for continuous data accumulation
      "checkpoint-location" -> Storage.checkpointLocation        // Checkpoint directory for fault tolerance
    ) // End of storage configuration map
  } // End of local storage configuration factory
} // End of AppConfig object
