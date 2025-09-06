package com.crypto.pulse.model // Package declaration for cryptocurrency market data model

// Import Java time utilities for precise timestamp handling in cryptocurrency data
import java.time.{Instant, LocalDateTime, ZoneOffset} // Instant for epoch timestamps, LocalDateTime for human-readable time, ZoneOffset for UTC conversion

/**
 * MarketTick - Cryptocurrency market data case class
 * 
 * Represents a single cryptocurrency market data point with complete market information.
 * This case class is designed to be immutable and type-safe for Spark processing.
 * 
 * TDD-developed with comprehensive validation and metadata support.
 */
// Cryptocurrency market data case class with comprehensive field definitions and type safety
case class MarketTick(
  coinId: String,                    // Unique cryptocurrency identifier (e.g., "bitcoin", "ethereum")
  symbol: String,                    // Trading symbol (e.g., "BTC", "ETH")  
  name: String,                      // Human-readable cryptocurrency name (e.g., "Bitcoin", "Ethereum")
  currentPrice: Option[Double],      // Current market price in USD (Option for handling missing data)
  marketCap: Option[Double] = None,  // Total market capitalization (Optional field)
  marketCapRank: Option[Int] = None, // Market cap ranking among cryptocurrencies (Optional field)
  totalVolume: Option[Double] = None, // 24h trading volume (Optional field)
  priceChangePercentage24h: Option[Double] = None, // 24h price change percentage (Optional field)
  
  // Kafka metadata for data lineage and debugging - added when processing streaming data
  kafkaPartition: Option[Int] = None,    // Kafka partition number for message tracking
  kafkaOffset: Option[Long] = None,      // Kafka offset for exactly-once processing
  kafkaTimestamp: Option[Long] = None,   // Kafka message timestamp for event time processing
  
  // Processing metadata for monitoring and quality assurance
  ingestTimestamp: Option[Long] = None,     // Timestamp when data was first ingested by Flink
  processingTimestamp: Option[Long] = None  // Timestamp when data was processed by Spark
) { // End of case class field definitions

  /**
   * Validates if this MarketTick contains the minimum required fields for processing
   * Used for data quality assurance in the streaming pipeline
   */
  // Data validation method to ensure minimum required fields for processing quality
  def isValid: Boolean = { // Boolean return type indicating data validity
    // Check that essential fields are non-empty and non-null for data quality
    coinId.nonEmpty && // CoinId must be present for identification
    symbol.nonEmpty && // Symbol must be present for trading identification  
    name.nonEmpty &&   // Name must be present for human readability
    currentPrice.isDefined // Current price must be present for meaningful market data
  } // End of validation method

  /**
   * Creates a new MarketTick with Kafka metadata added for data lineage tracking
   * Used when enriching data with Kafka message metadata during streaming processing
   */
  // Method to enrich market tick with Kafka metadata for data lineage and exactly-once processing
  def withKafkaMetadata(partition: Int, offset: Long, timestamp: Long): MarketTick = { // Returns enriched MarketTick
    // Create new MarketTick instance with Kafka metadata added for tracking
    this.copy( // Copy current instance with additional metadata
      kafkaPartition = Some(partition),  // Add Kafka partition for message tracking
      kafkaOffset = Some(offset),        // Add Kafka offset for exactly-once semantics
      kafkaTimestamp = Some(timestamp)   // Add Kafka timestamp for event time processing
    ) // End of copy operation with Kafka metadata
  } // End of Kafka metadata enrichment method

  /**
   * Creates a new MarketTick with processing timestamp for monitoring and debugging
   * Used to track when Spark processed this data for performance monitoring
   */
  // Method to enrich market tick with processing timestamp for monitoring and performance tracking
  def withProcessingTimestamp(timestamp: Long = Instant.now().toEpochMilli): MarketTick = { // Returns MarketTick with processing time
    // Create new MarketTick instance with current processing timestamp added
    this.copy(processingTimestamp = Some(timestamp)) // Copy with processing timestamp for monitoring
  } // End of processing timestamp enrichment method
} // End of MarketTick case class

/**
 * MarketTick companion object with utility methods and factory functions
 * Provides additional functionality for MarketTick creation and manipulation
 */
// Companion object for MarketTick with utility methods and JSON field mapping
object MarketTick { // MarketTick utility object

  /**
   * JSON field mappings for CoinGecko API response parsing
   * Maps CoinGecko JSON field names to MarketTick case class fields
   */
  // JSON field name mappings for parsing CoinGecko API responses into MarketTick objects
  val jsonFieldMappings: Map[String, String] = Map( // String to String mapping for JSON parsing
    "id" -> "coinId",                           // CoinGecko "id" maps to our "coinId" field
    "symbol" -> "symbol",                       // Direct mapping for symbol field
    "name" -> "name",                          // Direct mapping for name field
    "current_price" -> "currentPrice",         // Snake case to camel case conversion
    "market_cap" -> "marketCap",              // Snake case to camel case conversion
    "market_cap_rank" -> "marketCapRank",     // Snake case to camel case conversion
    "total_volume" -> "totalVolume",          // Snake case to camel case conversion
    "price_change_percentage_24h" -> "priceChangePercentage24h" // Complex field name mapping
  ) // End of JSON field mappings
} // End of MarketTick companion object
