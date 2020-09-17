package com.jinyx.mqtt

interface OnMqttMsgListener {

    /**
     * 订阅的 MQTT 消息
     */
    fun onSubMessage(topic: String, payload: ByteArray)

    /**
     * 发布的 MQTT 消息
     */
    fun onPubMessage(payload: ByteArray)

}