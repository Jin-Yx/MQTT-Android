package com.jinyx.mqtt

import android.content.Context
import com.jinyx.mqtt.paho.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlin.math.max

class MqttHelper(context: Context, private val mqttOptions: MqttOptions) : IMqtt {

    private var mState = MqttStatus.FAILURE

    private val mqttConnectOptions: MqttConnectOptions by lazy {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = mqttOptions.cleanSession
        options.connectionTimeout = mqttOptions.connectTimeOut    // 超时时间会是设置的 2 倍
        options.keepAliveInterval = mqttOptions.keepAliveInterval
        options.userName = mqttOptions.username
        options.password = mqttOptions.password.toCharArray()
        options.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        options
    }

    private var mqttAndroidClient: MqttAndroidClient? = null

    private var mqttReconnectHandler: MqttReconnectHandler? = null

    private var mMsgListener: OnMqttMsgListener? = null
    private var mStatusListener: OnMqttStatusChangeListener? = null

    init {
        if (mqttOptions.willTopic.isNotEmpty() && mqttOptions.willTopic.isNotBlank()
            && !mqttOptions.willTopic.contains(MqttTopic.MULTI_LEVEL_WILDCARD)
            && !mqttOptions.willTopic.contains(MqttTopic.SINGLE_LEVEL_WILDCARD)) {
            mqttConnectOptions.setWill(mqttOptions.willTopic, mqttOptions.willMsg.toByteArray(), mqttOptions.willQos, false)
        }
        mqttAndroidClient = MqttAndroidClient(context.applicationContext, mqttOptions.serviceUrl, mqttOptions.clientId)
        mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {
            var lastMessage: MqttMessage? = null
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if (reconnect) {
                    subscribeToService()
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (!topic.isNullOrEmpty() && message != null && message != lastMessage && message.payload != null) {
                    lastMessage = message
                    mMsgListener?.onSubMessage(topic, message.payload)
                }
            }

            override fun connectionLost(cause: Throwable?) {
                changeState(MqttStatus.LOST, cause)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                val payload = token?.message?.payload
                if (payload != null) {
                    mMsgListener?.onPubMessage(payload)
                }
            }
        })
        mqttReconnectHandler = MqttReconnectHandler(this, context.applicationContext, mqttOptions.reconnectInterval)
        connect()
    }

    override fun connect() {
        try {
            mqttAndroidClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient?.setBufferOpts(disconnectedBufferOptions)
                    subscribeToService()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    changeState(MqttStatus.FAILURE, exception)
                }
            })
        } catch (ex: MqttException) {
        }
    }

    protected fun reconnect() {
        connect()
    }

    override fun disConnect() {
        mMsgListener = null
        mStatusListener = null
        if (mqttAndroidClient != null) {
            if (mqttAndroidClient?.isConnected == true) {
                mqttAndroidClient?.disconnect()
            }
            mqttAndroidClient?.setCallback(null)
            mqttAndroidClient?.unregisterResources()
            mqttAndroidClient = null
        }
    }

    private fun subscribeToService() {
        try {
            mqttAndroidClient?.subscribe(mqttOptions.serviceUrl, mqttOptions.subQos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    changeState(MqttStatus.SUCCESS, null)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    changeState(MqttStatus.FAILURE, exception)
                }
            })
        } catch (e: Exception) {
            changeState(MqttStatus.FAILURE, e)
        }
    }

    override fun subscribe(topic: String, qos: Int, listener: OnSubTopicListener?) {
        try {
            mqttAndroidClient?.subscribe(topic, qos, null, listener)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun unsubscribe(topic: String, listener: OnSubTopicListener?) {
        try {
            mqttAndroidClient?.unsubscribe(topic, null, listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun pubMessage(topic: String, payload: ByteArray, qos: Int, retain: Boolean) {
        if (mState != MqttStatus.SUCCESS) return
        try {
            mqttAndroidClient?.publish(topic, payload, qos, retain)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun addOnMsgListener(listener: OnMqttMsgListener?) {
        mMsgListener = listener
    }

    override fun addOnStatusChangeListener(listener: OnMqttStatusChangeListener?) {
        mStatusListener = listener
    }

    private fun changeState(state: MqttStatus, throwable: Throwable?) {
        mState = state
        mStatusListener?.onChange(state, throwable)
        if (mState == MqttStatus.LOST) {
            reconnect()
        }
        if (mState == MqttStatus.SUCCESS) {
            mqttReconnectHandler?.removeCallbacksAndMessages(null)
        } else {
            mqttReconnectHandler?.sendEmptyMessageDelayed(MqttReconnectHandler.RECONNECT, max(0, mqttOptions.reconnectInterval))
        }
    }

}