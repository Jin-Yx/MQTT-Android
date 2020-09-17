package com.jinyx.mqtt

interface OnMqttStatusChangeListener {

    fun onChange(state: MqttStatus, throwable: Throwable?)

}