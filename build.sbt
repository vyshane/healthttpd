// Copyright 2018 Vy-Shane Xie Sin Fat

name := "Healthttpd"
version := sys.env.get("VERSION").getOrElse("0.1-SNAPSHOT")
description := "A tiny Scala library that provides a lightweight health and readiness status server"
organization := "mu.node"
licenses += ("Apache-2.0", url("https://choosealicense.com/licenses/apache-2.0/"))

scalaVersion := "2.12.4"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "org.nanohttpd" % "nanohttpd" % "2.3.1",

  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  // Test
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.jsoup" % "jsoup" % "1.11.2" % Test,
)

// Shade dependencies
assemblyShadeRules in assembly ++= Seq(
  ShadeRule.rename("fi.iki.elonen.**" -> "mu.node.shaded.@0").inAll,
  ShadeRule.rename("org.slf4j.**" -> "mu.node.shaded.@0").inAll,
  ShadeRule.rename("ch.qos.logback.**" -> "mu.node.shaded.@0").inAll
)

// Code formatting
scalafmtConfig := file(".scalafmt.conf")
