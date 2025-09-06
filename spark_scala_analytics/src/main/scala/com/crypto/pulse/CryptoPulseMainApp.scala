package com.crypto.pulse // Main package for Crypto Pulse application

// Import our TDD-developed components for production streaming
import com.crypto.pulse.consumer.KafkaMarketDataConsumer // TDD-tested Kafka consumer
import com.crypto.pulse.writer.ParquetTableManager // TDD-tested Parquet writer
import com.crypto.pulse.config.AppConfig // Centralized configuration management
// Import Spark SQL for streaming session management
import org.apache.spark.sql.SparkSession

/**
 * CryptoPulseMainApp - Production Main Application
 * 
 * Entry point for running Crypto Pulse in production.
 * Processes real cryptocurrency data from Kafka and creates bronze layer.
 */
object CryptoPulseMainApp {

  def main(args: Array[String]): Unit = {
    
    println("🚀 Starting Crypto Pulse Production Application")
    println("=" * 80)
    println("📊 Real-time Cryptocurrency Market Data Analytics")
    println("🔄 Continuous Bronze Layer Creation") 
    println("📈 Built with Apache Spark + Kafka + Parquet")
    println("=" * 80)
    
    // Create production SparkSession
    val spark = SparkSession.builder()
      .appName("CryptoPulse-Production")
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.sql.streaming.metricsEnabled", "true")
      .config("spark.sql.streaming.stopGracefullyOnShutdown", "true")
      .getOrCreate()
    
    try {
      println(s"✅ Spark Session Created: ${spark.sparkContext.appName}")
      println(s"📍 Spark Master: ${spark.sparkContext.master}")
      println(s"📊 Spark UI: ${spark.sparkContext.uiWebUrl.getOrElse("Not Available")}")
      
      // Create components
      val kafkaConsumer = KafkaMarketDataConsumer.fromAppConfig(spark)
      val parquetWriter = ParquetTableManager.fromAppConfig(spark)
      
      println("\n🎯 Starting Continuous Cryptocurrency Streaming...")
      
      // Create streaming pipeline
      val kafkaStream = kafkaConsumer.createKafkaStream()
      val marketTickStream = kafkaConsumer.parseJsonToMarketTick(
        kafkaStream.selectExpr("CAST(value AS STRING) as value")
      )
      
      // Start streaming query
      val streamingQuery = marketTickStream.writeStream
        .outputMode("append")
        .trigger(org.apache.spark.sql.streaming.Trigger.ProcessingTime(AppConfig.Spark.Streaming.triggerInterval))
        .queryName("CryptoPulse-Production-Stream")
        .option("checkpointLocation", AppConfig.Storage.checkpointLocation)
        .foreachBatch { (batchDF: Dataset[MarketTick], batchId: Long) =>
          val recordCount = batchDF.count()
          if (recordCount > 0) {
            println(s"\n📦 Batch $batchId: $recordCount records")
            batchDF.show(5, truncate = false)
            parquetWriter.writeMarketData(batchDF, AppConfig.Storage.marketDataPath)
            println(s"✅ Batch $batchId written to bronze layer")
          }
        }
        .start()
      
      println(s"🎯 Streaming started - Query ID: ${streamingQuery.id}")
      println(s"📊 Spark UI: http://localhost:4041")
      
      // Wait for termination
      streamingQuery.awaitTermination()
      
    } catch {
      case e: Exception =>
        println(s"❌ Production Error: ${e.getMessage}")
        e.printStackTrace()
        System.exit(1)
    } finally {
      spark.stop()
    }
  }
}
