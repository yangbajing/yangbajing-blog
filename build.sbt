name := "yangbajing-blog"

homepage := Some(url("https://www.yangbajing.me"))

startYear := Some(2019)

scalaVersion := "2.13.1"

scalafmtOnCompile := true

resolvers ++= Seq("Bintray helloscala".at("https://helloscala.bintray.com/maven"), Resolver.sonatypeRepo("snapshots"))

val versionFusion = "2.0.6"

libraryDependencies ++= Seq(
  "com.helloscala.fusion" %% "fusion-cluster" % versionFusion,
  "com.helloscala.fusion" %% "fusion-json-jackson" % versionFusion,
  "com.helloscala.fusion" %% "fusion-core" % versionFusion,
  "com.helloscala.fusion" %% "fusion-testkit" % versionFusion % Test)
