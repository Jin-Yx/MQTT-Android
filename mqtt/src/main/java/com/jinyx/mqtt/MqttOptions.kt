package com.jinyx.mqtt

data class MqttOptions(

    val serviceUrl: String,
    val username: String,
    val password: String,
    val clientId: String,
    val willTopic: String = "",
    val willMsg: String = "",
    val subQos: Int = 1,
    val willQos: Int = 1,
    val cleanSession: Boolean = true,
    val reconnectInterval: Long = 1000,
    val keepAliveInterval: Int = 20,
    val connectTimeOut: Int = 10

)