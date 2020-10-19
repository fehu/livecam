import sbt._

object Dependencies {
  lazy val `cats-core`      = "org.typelevel"     %% "cats-core"      % Version.cats
  lazy val `cats-effect`    = "org.typelevel"     %% "cats-effect"    % Version.catsEffect
  lazy val `fs2-core`       = "co.fs2"            %% "fs2-core"       % Version.fs2
  lazy val `log4cats-slf4j` = "io.chrisdavenport" %% "log4cats-slf4j" % Version.log4cats
  lazy val `scala-test`     = "org.scalatest"     %% "scalatest"      % Version.scalaTest
  lazy val `slf4j-simple`   = "org.slf4j"          % "slf4j-simple"   % Version.slf4j
  lazy val xuggler          = "xuggle"             % "xuggle-xuggler" % Version.xuggler

  object Plugin {
    lazy val `kind-projector`     = "org.typelevel" %% "kind-projector"     % Version.kindProjector cross CrossVersion.full
    lazy val `better-monadic-for` = "com.olegpy"    %% "better-monadic-for" % Version.betterMonadicFor
  }

  object Repos {
    lazy val dcm4che = "Dcm4Che Repository" at "https://www.dcm4che.org/maven2/"
  }

  protected object Version {
    lazy val betterMonadicFor = "0.3.1"
    lazy val cats             = "2.2.0"
    lazy val catsEffect       = "2.2.0"
    lazy val fs2              = "2.4.4"
    lazy val kindProjector    = "0.11.0"
    lazy val log4cats         = "1.1.1"
    lazy val scalaTest        = "3.1.1"
    lazy val slf4j            = "1.7.30"
    lazy val xuggler          = "5.4"
  }
}
