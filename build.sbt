name := "yangbajing-blog"

homepage := Some(url("https://www.yangbajing.me"))

startYear := Some(2019)

scalaVersion := "2.13.1"

scalafmtOnCompile := true

resolvers ++= Seq("Bintray akka-fusion".at("https://akka-fusion.bintray.com/maven"), Resolver.sonatypeRepo("snapshots"))

val versionFusion = "2.0.2"
val versionAkka = "2.6.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-typed" % versionAkka,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % versionAkka,
  "com.akka-fusion" %% "fusion-json" % versionFusion,
  "com.akka-fusion" %% "fusion-core" % versionFusion,
  "com.akka-fusion" %% "fusion-testkit" % versionFusion % Test)
