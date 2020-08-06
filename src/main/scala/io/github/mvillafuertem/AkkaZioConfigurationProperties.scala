package io.github.mvillafuertem

import com.typesafe.config.{ Config, ConfigFactory }

final case class AkkaZioConfigurationProperties(
  name: String,
  interface: String,
  port: Int
) {

  def withName(name: String): AkkaZioConfigurationProperties =
    copy(name = name)

  def withInterface(interface: String): AkkaZioConfigurationProperties =
    copy(interface = interface)

  def withPort(port: Int): AkkaZioConfigurationProperties =
    copy(port = port)

}

object AkkaZioConfigurationProperties {

  def apply(): AkkaZioConfigurationProperties = {
    val applicationConfig: Config    = ConfigFactory.load().getConfig("application")
    val infrastructureConfig: Config = ConfigFactory.load().getConfig("infrastructure")
    new AkkaZioConfigurationProperties(
      name = applicationConfig.getString("name"),
      interface = infrastructureConfig.getString("server.interface"),
      port = infrastructureConfig.getInt("server.port")
    )
  }

}
