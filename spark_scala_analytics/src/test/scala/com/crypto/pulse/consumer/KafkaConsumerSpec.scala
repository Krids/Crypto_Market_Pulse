package com.crypto.pulse.consumer // Package declaration for Kafka consumer testing

// Import our TDD-developed components for testing
import com.crypto.pulse.consumer.KafkaMarketDataConsumer // Component under test
import com.crypto.pulse.model.MarketTick // Data model for testing
// Import Spark components for testing environment
import org.apache.spark.sql.SparkSession // SparkSession for test environment
// Import ScalaTest framework for behavior testing
import org.scalatest.BeforeAndAfterAll // Test lifecycle management
import org.scalatest.funspec.AnyFunSpec // Function-style testing
import org.scalatest.matchers.should.Matchers // Assertion matchers

/**
 * KafkaConsumerSpec - TDD Test Suite for KafkaMarketDataConsumer
 * 
 * Comprehensive test suite following TDD principles for Kafka consumer functionality.
 * Tests configuration, JSON parsing, error handling, and streaming capabilities.
 */
class KafkaConsumerSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll {

  private var spark: SparkSession = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    
    spark = SparkSession.builder()
      .appName("KafkaConsumerTest")
      .master("local[4]")
      .config("spark.sql.shuffle.partitions", "8")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("WARN")
  }

  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
    super.afterAll()
  }

  describe("KafkaMarketDataConsumer") {

    describe("connection and configuration") {
      
      it("should create a consumer with correct Kafka configuration") {
        val kafkaConfig = Map(
          "kafka.bootstrap-servers" -> "localhost:19092",
          "kafka.topic" -> "test-topic",
          "kafka.consumer-group" -> "test-group"
        )
        
        val consumer = KafkaMarketDataConsumer(spark, kafkaConfig)
        
        consumer should not be null
        consumer.config("kafka.topic") shouldBe "test-topic"
      }

      it("should handle missing configuration gracefully") {
        val incompleteConfig = Map(
          "kafka.topic" -> "test-topic"
        )
        
        an[IllegalArgumentException] should be thrownBy {
          KafkaMarketDataConsumer(spark, incompleteConfig)
        }
      }
    }

    describe("JSON deserialization") {
      
      it("should deserialize valid JSON market tick messages to MarketTick case class") {
        val kafkaConfig = Map(
          "kafka.bootstrap-servers" -> "localhost:19092",
          "kafka.topic" -> "coingecko.markets",
          "kafka.consumer-group" -> "test-group"
        )
        
        val consumer = KafkaMarketDataConsumer(spark, kafkaConfig)
        
        // Create test DataFrame with valid JSON
        val validJson = """{"id":"bitcoin","symbol":"btc","name":"Bitcoin","current_price":50000.0}"""
        
        val sparkSession = spark
        import sparkSession.implicits._
        
        val testDF = Seq(validJson).toDF("value")
        val marketTicks = consumer.parseJsonToMarketTick(testDF)
        
        val result = marketTicks.collect()
        result.length shouldBe 1
        result.head.coinId shouldBe "bitcoin"
        result.head.symbol shouldBe "btc"
        result.head.currentPrice shouldBe Some(50000.0)
      }
    }
  }
}
