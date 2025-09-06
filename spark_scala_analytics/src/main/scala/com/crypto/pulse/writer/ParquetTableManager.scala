package com.crypto.pulse.writer // Package declaration for data writing and persistence components

// Import our TDD-developed MarketTick model for type-safe data writing
import com.crypto.pulse.model.MarketTick
// Import Spark SQL components for DataFrame and Dataset operations
import org.apache.spark.sql.{Dataset, SparkSession} // Dataset for typed data operations, SparkSession for Spark context
// Import Spark SQL functions for data transformation and partitioning
import org.apache.spark.sql.functions.{col, year, month, dayofmonth, hour} // Functions for time-based partitioning
// Import Java time utilities for timestamp processing and partitioning
import java.time.Instant // Instant for epoch timestamp conversion

/**
 * ParquetTableManager - Parquet File Writing and Management
 * 
 * Handles writing MarketTick data to Parquet files with time-based partitioning.
 * Implements data validation, partitioning strategy, and performance optimization.
 * 
 * Built with TDD methodology for reliable data persistence.
 */
// Parquet table manager case class for writing and managing cryptocurrency market data in Parquet format
case class ParquetTableManager(spark: SparkSession, config: Map[String, String]) { // SparkSession for operations, Map for configuration

  // Configuration validation to ensure required storage settings are present
  validateConfig() // Immediate configuration validation for fail-fast behavior

  /**
   * Validates that all required configuration keys are present
   * Throws IllegalArgumentException with detailed error messages for missing configuration
   */
  // Private configuration validation method ensuring all required storage settings are present
  private def validateConfig(): Unit = { // Unit return type - validation method with side effects
    // Define sequence of required configuration keys for Parquet writer operation
    val requiredKeys = Seq( // Sequence of string keys that must be present in configuration
      "output.path"        // Output directory path for Parquet files
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
   * Writes MarketTick data to Parquet files with partitioning
   * Implements time-based partitioning for optimal query performance
   */
  // Public method to write MarketTick Dataset to Parquet files with time-based partitioning
  def writeMarketData(marketData: Dataset[MarketTick], outputPath: String): Unit = { // Unit return - side effect method
    
    // Import Spark SQL implicits for Dataset operations
    import spark.implicits._ // Enable Dataset operations and automatic encoders
    
    // Add partitioning columns based on processing timestamp for optimal query performance
    val partitionedData = marketData.map { marketTick => // Transform each MarketTick to add partition columns
      // Get processing timestamp or use current time as fallback
      val timestamp = marketTick.processingTimestamp.getOrElse(System.currentTimeMillis()) // Processing time or current time
      
      // Convert epoch timestamp to LocalDateTime for partition extraction
      val dateTime = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).toLocalDateTime // Convert to UTC LocalDateTime
      
      // Create tuple with MarketTick and partition information
      (marketTick, dateTime.getYear, dateTime.getMonthValue, dateTime.getDayOfMonth, dateTime.getHour) // Tuple with partition data
    }.toDF("marketTick", "year", "month", "day", "hour") // Convert to DataFrame with partition columns
      .select(col("marketTick.*"), col("year"), col("month"), col("day"), col("hour")) // Flatten MarketTick fields with partitions

    // Write to Parquet with partitioning for optimal query performance
    partitionedData.write // Access DataFrame writer
      .mode("append") // Append mode for continuous data accumulation
      .option("compression", config.getOrElse("compression", "snappy")) // Compression for storage efficiency
      .partitionBy("year", "month", "day", "hour", "coinId") // Time and coin-based partitioning
      .parquet(outputPath) // Write to Parquet format at specified path
  } // End of Parquet writing method
} // End of ParquetTableManager case class

/**
 * ParquetTableManager Companion Object
 * Provides factory methods for creating writer instances
 */
// Companion object for ParquetTableManager with factory methods and utilities
object ParquetTableManager { // Factory object for Parquet writer creation

  /**
   * Factory method to create writer from configuration map
   * Validates configuration and returns configured writer instance
   */
  // Factory method to create Parquet writer from configuration map with validation
  def apply(spark: SparkSession, config: Map[String, String]): ParquetTableManager = { // Returns configured writer
    // Create writer instance with immediate configuration validation
    new ParquetTableManager(spark, config) // Constructor with configuration validation
  } // End of factory method

  /**
   * Factory method to create writer from AppConfig
   * Uses centralized application configuration for writer creation
   */
  // Factory method to create Parquet writer using centralized application configuration
  def fromAppConfig(spark: SparkSession): ParquetTableManager = { // Returns writer with app configuration
    // Import centralized application configuration
    import com.crypto.pulse.config.AppConfig // Import for accessing application configuration
    
    // Create writer using application configuration factory method
    ParquetTableManager(spark, AppConfig.localStorageConfig()) // Create writer with app config
  } // End of AppConfig factory method
} // End of ParquetTableManager companion object
