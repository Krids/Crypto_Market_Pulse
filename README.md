# Crypto Pulse — Flink (Java) → Kafka → Spark (Scala) → Delta/Parquet → Dashboard

A weekend portfolio project that ingests **free crypto market data** from CoinGecko with **Flink (Java)**, streams it into **Kafka** (Redpanda), models it with **Spark (Scala)** into **Bronze/Silver/Gold** tables, and visualizes **Gold** outputs on a tiny **Streamlit** dashboard.

This repo is designed to demonstrate (and help you practice) the core Spark concepts interviewers ask about: **coalesce, repartition, shuffle, skew, executors, drivers**. It also gives you a clean data-engineering structure you can extend later.

---

## Architecture

```
CoinGecko (free REST)
   │ (Flink Java HTTP poll, every 60s)
   ▼
Kafka topic: coingecko.markets  (Redpanda single-node)
   ▼
Spark (Scala)
  1) Bronze  — raw JSON snapshots from Kafka + ingest metadata
  2) Silver  — normalized hourly prices (coin_id, ts, price, market_cap, volume)
  3) Gold    — features: ret_1h, MA-24/72, vol_24h, zscore_1h; plus top_movers_24h
   ▼
Dashboard (Streamlit + Plotly) reads Gold Parquet/Delta
```

---

## Folder Structure

```
crypto-pulse/
├─ README.md
├─ .gitignore
├─ infra/
│  ├─ docker-compose.yml            # Redpanda (Kafka) single-node
│  └─ topics.sh                     # helper script (optional)
├─ flink-java-ingest/
│  ├─ pom.xml
│  └─ src/main/java/com/example/pulse/
│     ├─ model/MarketTick.java
│     ├─ source/CoinGeckoSource.java
│     └─ MarketsToKafkaJob.java
├─ spark-scala-analytics/
│  ├─ build.sbt
│  └─ src/main/scala/com/example/pulse/
│     ├─ BronzeIngest.scala
│     ├─ SilverTransform.scala
│     ├─ GoldFeatures.scala
│     └─ SkewLab.scala              # skew demo (salting & AQE skew-join)
├─ dashboard/
│  ├─ app.py
│  ├─ requirements.txt
│  └─ .streamlit/config.toml
├─ conf/
│  └─ settings.yaml                 # coin list, paths, intervals (optional)
├─ data/
│  ├─ bronze/
│  ├─ silver/
│  ├─ gold/
│  └─ gold_parquet/
└─ scripts/
   ├─ build_all.sh
   ├─ run_flink.sh
   ├─ run_spark_all.sh
   └─ dev_clean.sh
```

> Versions tested: **Java 17**, **Scala 2.12**, **Spark 3.5.x**, **Flink 1.19.x**.

---

## Why this project is interview‑friendly

- **Flink (Java)**: custom HTTP polling `SourceFunction` → Kafka; delivery guarantees; JSON serialization.
- **Spark (Scala)**: window + groupBy (forces **shuffles**), `repartition` to spread hot keys, `coalesce` to compact files, **AQE** to mitigate skew joins.
- **Data modeling**: clear **Bronze/Silver/Gold** separation; schemas you can evolve.
- **Ops reality**: explicit **driver vs executor** resources via `spark-submit`; tuning `spark.sql.shuffle.partitions`.

---

## Prerequisites

- Docker & Docker Compose
- Java **17** (JDK)
- Maven 3.9+
- Scala **2.12** toolchain & **sbt**
- Python 3.10+ (for the dashboard)
- Apache Spark **3.5.x** available on PATH (`spark-submit --version`)

---

## Quickstart

### 1) Start Kafka (Redpanda)
```bash
docker compose -f infra/docker-compose.yml up -d
# Redpanda exposes Kafka at localhost:9092
```

### 2) Run Flink (Java): CoinGecko → Kafka
```bash
cd flink-java-ingest
mvn -q -DskipTests package
flink run -c com.example.pulse.MarketsToKafkaJob target/flink-java-ingest-1.0.0-shaded.jar
# Leave running for ~30–120 minutes to accumulate snapshots
```

### 3) Run Spark (Scala): Bronze → Silver → Gold
Open a new terminal:

```bash
cd spark-scala-analytics
sbt package

# Bronze: read earliest Kafka offsets, persist raw to Delta
spark-submit \
  --class com.example.pulse.BronzeIngest \
  target/scala-2.12/spark-scala-analytics_2.12-0.1.0.jar

# Silver: normalize hourly, repartition & coalesce
spark-submit \
  --class com.example.pulse.SilverTransform \
  target/scala-2.12/spark-scala-analytics_2.12-0.1.0.jar

# Gold: rolling features & top movers (forces shuffles; AQE enabled)
spark-submit \
  --class com.example.pulse.GoldFeatures \
  target/scala-2.12/spark-scala-analytics_2.12-0.1.0.jar
```

### 4) Dashboard (Streamlit)
```bash
cd ../dashboard
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
streamlit run app.py
# http://localhost:8501
```

---

## Core Spark Concepts (where they show up)

| Concept | Where | Why it matters |
|---|---|---|
| **Driver vs Executors** | `spark-submit` flags; `SkewLab.scala` prints executor memory | Driver plans & schedules; executors run tasks & hold shuffle state |
| **repartition** | `SilverTransform.scala` → `.repartition(col("coin_id"))` | Wide op (**shuffle**) that distributes hot keys (BTC/ETH) to avoid skew |
| **coalesce** | `SilverTransform.scala` → `.coalesce(1)` before write | Narrow op (no shuffle) to reduce small output files for the demo |
| **shuffle** | `GoldFeatures.scala` (window + groupBy), joins in `SkewLab.scala` | Inspect in Spark UI: shuffle read/write MB and skewed task durations |
| **skew** | `SkewLab.scala` (naive join vs salted join; AQE skew join) | Hot keys create stragglers; fix with salting and/or AQE |
| **AQE** | `GoldFeatures.scala` & `SkewLab.scala` configs | Splits skewed partitions, coalesces small ones at runtime |

Useful knobs you’ll tune:
```bash
--num-executors 4 --executor-cores 3 --executor-memory 4g --driver-memory 2g \
--conf spark.sql.shuffle.partitions=200 \
--conf spark.sql.adaptive.enabled=true \
--conf spark.sql.adaptive.skewJoin.enabled=true
```

---

## Data Model

- **Bronze**: raw JSON from Kafka with metadata (`ingest_ts`) → `data/bronze/markets_snapshot`
- **Silver**: `prices_hourly(coin_id, ts, price, market_cap, volume)` partitioned by `coin_id`
- **Gold**:
  - `features_hourly`: add `ret_1h`, `ma_24`, `ma_72`, `vol_24h`, `zscore_1h`
  - `top_movers_24h`: last 24h performance & volatility summary

> For the dashboard, we additionally export compact **Parquet** copies under `data/gold_parquet/*` for fast reads without Spark.

---

## Skew Lab (prove you understand skew)

Run twice and compare Spark UI:
```bash
# Painful path (AQE off)
spark-submit --class com.example.pulse.SkewLab \
  --conf spark.sql.adaptive.enabled=false \
  target/scala-2.12/spark-scala-analytics_2.12-0.1.0.jar

# Mitigated path (AQE on + salting)
spark-submit --class com.example.pulse.SkewLab \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.skewJoin.enabled=true \
  target/scala-2.12/spark-scala-analytics_2.12-0.1.0.jar
```

What to look for in Spark UI:
- Unbalanced tasks and long tails on the naive join
- Reduced skewed partition sizes with AQE
- Smaller number of output partitions after AQE coalescing

---

## Troubleshooting

- **No data in Bronze**: ensure Flink job is running and Redpanda is up (`docker ps`). Check topic name `coingecko.markets`.
- **Spark Kafka connector version mismatch**: use **Scala 2.12** and **Spark 3.5.x**; the build already targets those.
- **Delta vs Parquet**: If you prefer not to use Delta, switch format to `parquet`. Dashboard can read either; Parquet is simpler.
- **Windows WSL**: run Docker and Spark inside WSL2 for better networking.

---

## License
MIT (for learning/demo purposes). CoinGecko data is subject to their terms of service.
