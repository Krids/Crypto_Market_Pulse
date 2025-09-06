ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version := "1.0.0"
ThisBuild / organization := "com.crypto.pulse"

lazy val root = (project in file("."))
  .settings(
    name := "crypto-pulse-spark-analytics",
    
    libraryDependencies ++= Seq(
      // Apache Spark core dependencies
      "org.apache.spark" %% "spark-core" % "3.5.0",
      "org.apache.spark" %% "spark-sql" % "3.5.0", 
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.0",
      
      // Configuration management
      "com.typesafe" % "config" % "1.4.3",
      
      // Testing framework
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    ),
    
    // Scala compiler options
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature", 
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
    ),
    
    // Test configuration with Java 17 compatibility
    Test / fork := true
  )

// JVM options for Java 17 compatibility
Test / javaOptions ++= Seq(
  "-Xmx4g",
  "-XX:+UseG1GC",
  "--enable-native-access=ALL-UNNAMED",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
  "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED",
  "--add-opens=java.base/javax.security.auth=ALL-UNNAMED",
  "-Djdk.security.auth.subject.getSubject.compat=true"
)

// Run configuration
run / fork := true 
run / javaOptions ++= Seq(
  "-Xmx4g",
  "-XX:+UseG1GC",
  "--enable-native-access=ALL-UNNAMED", 
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
  "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
  "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED",
  "--add-opens=java.base/javax.security.auth=ALL-UNNAMED",
  "-Djdk.security.auth.subject.getSubject.compat=true"
)
