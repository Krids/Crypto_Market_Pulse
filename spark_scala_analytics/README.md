# 🚀 Crypto Pulse - Spark Analytics

Real-time cryptocurrency market data analytics built with Apache Spark, following TDD principles.

## ✅ Features

- **Real-time Streaming**: Processes live cryptocurrency data from Kafka
- **Bronze Layer**: Creates Parquet files with time-based partitioning  
- **100+ Cryptocurrencies**: Handles complete CoinGecko dataset
- **Production Ready**: Fault tolerance, monitoring, error handling
- **TDD Developed**: Comprehensive test coverage
- **Java 17 Compatible**: Modern JVM compatibility

## 🛠️ Quick Start

### Prerequisites
- Java 17+ (OpenJDK 17 recommended)
- SBT 1.9+
- Apache Spark 3.5+
- Running Kafka with `coingecko.markets` topic

### Build and Test
```bash
cd spark_scala_analytics

# Compile
sbt clean compile

# Run tests  
sbt test

# Run production application
sbt "runMain com.crypto.pulse.CryptoPulseMainApp"
```

## 📊 Architecture

- **Data Source**: Kafka topic `coingecko.markets` 
- **Processing**: Apache Spark Structured Streaming
- **Storage**: Parquet files partitioned by time and coinId
- **Monitoring**: Spark UI at http://localhost:4041

## 🎯 Data Pipeline

1. **Flink** ingests data from CoinGecko API → Kafka
2. **Spark** consumes from Kafka → parses JSON → validates data
3. **Bronze Layer** stores raw data in Parquet format with partitioning
4. **Monitoring** tracks processing metrics and data quality

## 📁 Data Location

- **Bronze Layer**: `../data/parquet/market-data/`
- **Partitioning**: `year/month/day/hour/coinId`
- **Format**: Parquet with Snappy compression

## 🔧 Configuration

Configuration is managed through:
- `src/main/resources/application.conf` - Production settings
- `src/main/resources/local.conf` - Development overrides

## 🚀 Production Deployment

The system is designed for production use with:
- Continuous 24/7 streaming operation
- Fault tolerance with checkpointing
- Error handling and recovery
- Performance monitoring and metrics

Built with enterprise software engineering practices and comprehensive TDD test coverage.
