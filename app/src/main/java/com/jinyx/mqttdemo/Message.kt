package com.jinyx.mqttdemo

data class Message(
    val message: String = "",
    val isPublish: Boolean = false
)