// Package declaration - defines the namespace for this class
package com.example.pulse.model;

// Jackson annotation to ignore unknown JSON fields (defensive programming)
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// Jackson annotation to map JSON field names to Java field names
import com.fasterxml.jackson.annotation.JsonProperty;

// Required for Flink serialization across the cluster
import java.io.Serializable;
// For timestamp handling when creating ingestion metadata
import java.time.Instant;

/**
 * Represents a single cryptocurrency market data point from CoinGecko API.
 * This is a POJO (Plain Old Java Object) that maps to the JSON structure 
 * returned by the CoinGecko /coins/markets endpoint.
 * 
 * Key design decisions:
 * - Implements Serializable for Flink distributed processing
 * - Uses Jackson annotations for JSON deserialization
 * - Includes metadata fields for ingestion tracking
 * - Uses wrapper types (Double, Long) to handle null values from API
 */
// Tells Jackson to ignore any JSON fields not mapped to Java fields
// This makes our code resilient to API changes that add new fields
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketTick implements Serializable {

    // Required for Java serialization - ensures compatibility across versions
    private static final long serialVersionUID = 1L;

    // Maps JSON field "id" to Java field "coinId" (e.g., "bitcoin", "ethereum")
    @JsonProperty("id")
    private String coinId;

    // Maps JSON field "symbol" to Java field "symbol" (e.g., "BTC", "ETH")
    @JsonProperty("symbol")
    private String symbol;

    // Maps JSON field "name" to Java field "name" (e.g., "Bitcoin", "Ethereum")
    @JsonProperty("name")
    private String name;

    // Current price in USD - using Double to handle null values and decimals
    @JsonProperty("current_price")
    private Double currentPrice;

    // Total market capitalization in USD - using Long for large numbers
    @JsonProperty("market_cap")
    private Long marketCap;

    // Ranking by market cap (1 = largest, 2 = second largest, etc.)
    @JsonProperty("market_cap_rank")
    private Integer marketCapRank;

    // Market cap if all possible coins were in circulation
    @JsonProperty("fully_diluted_valuation")
    private Long fullyDilutedValuation;

    // 24-hour trading volume in USD
    @JsonProperty("total_volume")
    private Long totalVolume;

    // Highest price in the last 24 hours
    @JsonProperty("high_24h")
    private Double high24h;

    // Lowest price in the last 24 hours
    @JsonProperty("low_24h")
    private Double low24h;

    // Absolute price change in USD over 24 hours (can be negative)
    @JsonProperty("price_change_24h")
    private Double priceChange24h;

    // Percentage price change over 24 hours (e.g., 5.23 for +5.23%)
    @JsonProperty("price_change_percentage_24h")
    private Double priceChangePercentage24h;

    // Absolute market cap change in USD over 24 hours
    @JsonProperty("market_cap_change_24h")
    private Long marketCapChange24h;

    // Percentage market cap change over 24 hours
    @JsonProperty("market_cap_change_percentage_24h")
    private Double marketCapChangePercentage24h;

    // Number of coins currently in circulation
    @JsonProperty("circulating_supply")
    private Double circulatingSupply;

    // Total number of coins that exist (including locked/reserved)
    @JsonProperty("total_supply")
    private Double totalSupply;

    // Maximum number of coins that will ever exist (null if unlimited)
    @JsonProperty("max_supply")
    private Double maxSupply;

    // All-time high price in USD
    @JsonProperty("ath")
    private Double ath;

    // Percentage change from all-time high (usually negative)
    @JsonProperty("ath_change_percentage")
    private Double athChangePercentage;

    // Date when all-time high was reached (ISO 8601 format)
    @JsonProperty("ath_date")
    private String athDate;

    // All-time low price in USD
    @JsonProperty("atl")
    private Double atl;

    // Percentage change from all-time low (usually positive)
    @JsonProperty("atl_change_percentage")
    private Double atlChangePercentage;

    // Date when all-time low was reached (ISO 8601 format)
    @JsonProperty("atl_date")
    private String atlDate;

    // When CoinGecko last updated this data (ISO 8601 format)
    @JsonProperty("last_updated")
    private String lastUpdated;

    // Additional metadata fields for ingestion tracking
    // When our Flink job ingested this record (Unix timestamp in milliseconds)
    private long ingestTimestamp;
    // URL of the API endpoint we fetched this data from
    private String sourceUrl;

    // Default constructor required by Jackson for JSON deserialization
    public MarketTick() {
        // Set ingestion timestamp to current time when object is created
        // Instant.now() gets current time, toEpochMilli() converts to Unix timestamp
        this.ingestTimestamp = Instant.now().toEpochMilli();
    }

    // Getters and setters - required for Jackson serialization/deserialization
    // and for accessing private fields from other classes
    public String getCoinId() {
        return coinId;
    }

    public void setCoinId(String coinId) {
        this.coinId = coinId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Long marketCap) {
        this.marketCap = marketCap;
    }

    public Integer getMarketCapRank() {
        return marketCapRank;
    }

    public void setMarketCapRank(Integer marketCapRank) {
        this.marketCapRank = marketCapRank;
    }

    public Long getFullyDilutedValuation() {
        return fullyDilutedValuation;
    }

    public void setFullyDilutedValuation(Long fullyDilutedValuation) {
        this.fullyDilutedValuation = fullyDilutedValuation;
    }

    public Long getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(Long totalVolume) {
        this.totalVolume = totalVolume;
    }

    public Double getHigh24h() {
        return high24h;
    }

    public void setHigh24h(Double high24h) {
        this.high24h = high24h;
    }

    public Double getLow24h() {
        return low24h;
    }

    public void setLow24h(Double low24h) {
        this.low24h = low24h;
    }

    public Double getPriceChange24h() {
        return priceChange24h;
    }

    public void setPriceChange24h(Double priceChange24h) {
        this.priceChange24h = priceChange24h;
    }

    public Double getPriceChangePercentage24h() {
        return priceChangePercentage24h;
    }

    public void setPriceChangePercentage24h(Double priceChangePercentage24h) {
        this.priceChangePercentage24h = priceChangePercentage24h;
    }

    public Long getMarketCapChange24h() {
        return marketCapChange24h;
    }

    public void setMarketCapChange24h(Long marketCapChange24h) {
        this.marketCapChange24h = marketCapChange24h;
    }

    public Double getMarketCapChangePercentage24h() {
        return marketCapChangePercentage24h;
    }

    public void setMarketCapChangePercentage24h(Double marketCapChangePercentage24h) {
        this.marketCapChangePercentage24h = marketCapChangePercentage24h;
    }

    public Double getCirculatingSupply() {
        return circulatingSupply;
    }

    public void setCirculatingSupply(Double circulatingSupply) {
        this.circulatingSupply = circulatingSupply;
    }

    public Double getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(Double totalSupply) {
        this.totalSupply = totalSupply;
    }

    public Double getMaxSupply() {
        return maxSupply;
    }

    public void setMaxSupply(Double maxSupply) {
        this.maxSupply = maxSupply;
    }

    public Double getAth() {
        return ath;
    }

    public void setAth(Double ath) {
        this.ath = ath;
    }

    public Double getAthChangePercentage() {
        return athChangePercentage;
    }

    public void setAthChangePercentage(Double athChangePercentage) {
        this.athChangePercentage = athChangePercentage;
    }

    public String getAthDate() {
        return athDate;
    }

    public void setAthDate(String athDate) {
        this.athDate = athDate;
    }

    public Double getAtl() {
        return atl;
    }

    public void setAtl(Double atl) {
        this.atl = atl;
    }

    public Double getAtlChangePercentage() {
        return atlChangePercentage;
    }

    public void setAtlChangePercentage(Double atlChangePercentage) {
        this.atlChangePercentage = atlChangePercentage;
    }

    public String getAtlDate() {
        return atlDate;
    }

    public void setAtlDate(String atlDate) {
        this.atlDate = atlDate;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getIngestTimestamp() {
        return ingestTimestamp;
    }

    public void setIngestTimestamp(long ingestTimestamp) {
        this.ingestTimestamp = ingestTimestamp;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @Override
    public String toString() {
        return "MarketTick{" +
                "coinId='" + coinId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", marketCap=" + marketCap +
                ", marketCapRank=" + marketCapRank +
                ", totalVolume=" + totalVolume +
                ", priceChangePercentage24h=" + priceChangePercentage24h +
                ", ingestTimestamp=" + ingestTimestamp +
                '}';
    }
}
