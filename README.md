### 一、介绍  
&emsp;&emsp;MQTT 协议 是基于发布/订阅模式的物联网通信协议，凭借简单易实现、支持 QoS、报文小等特点，占据了物联网协议的半壁江山。  
&emsp;&emsp;常用于 IOT 物联网和一些需要服务端主动通知客户端的场景。  

### 二、使用  
**1. 导入依赖**  
```groovy
dependencies {
    implementation 'com.jinyx.mqtt:mqtt:0.0.1' 
}
```

**2. 创建 MqttHelper 辅助类，设置回调监听**  
```kotlin
private lateinit var mqttHelper: MqttHelper

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val options = MqttOptions(
        serviceUrl = "tcp://192.168.0.106:61613",   // MQTT 服务
        username = "admin",
        password = "password",
        clientId = "android-${System.currentTimeMillis()}", // MQTT 客户端ID， 唯一标识，如果存在多个 MQTT 对象使用同一个 clientId，会导致相互之间不断被挤掉再重连
        willTopic = "will/android",     // 遗嘱 Topic，不能存在通配符 # 和 +，可用于监听对方是否掉线
        willMsg = "I'm Died - $Id"      // 遗嘱消息，当客户端掉线，MQTT 服务发送pingreq包，客户端不回复 pingresp 包，MQTT 发送遗嘱消息到 订阅 willTopic 的客户端
    )
    mqttHelper = MqttHelper(this, options)
    mqttHelper.addOnMsgListener(onMqttMsgListener)
    mqttHelper.addOnStatusChangeListener(onMqttStatusChangeListener)
}
```

**3. 连接 MQTT**  
```kotlin
mqttHelper.connect()
```
&emsp;&emsp;连接成功或失败，以及中途的连接掉线，会触发 OnMqttStatusChangeListener 回调  

**4. MQTT 连接状态监听**  
```kotlin
private val onMqttStatusChangeListener = object : OnMqttStatusChangeListener {

    /**
     * [state] MQTT 连接状态改变:
     *  [MqttStatus.SUCCESS] 连接成功
     *  [MqttStatus.FAILURE] 连接失败
     *  [MqttStatus.LOST]    连接中断
     *
     * [throwable]
     *  连接失败或中断时的异常信息
     */
    override fun onChange(state: MqttStatus, throwable: Throwable?) {

    }

}
```

**5. MQTT 收发消息监听**  
```kotlin
private val onMqttMsgListener = object : OnMqttMsgListener {
    /**
     * MQTT 订阅 Topic 消息回调
     * [topic] 订阅的 Topic
     * [payload] 订阅的消息
     */
    override fun onSubMessage(topic: String, payload: ByteArray) {

    }

    /**
     * MQTT 发布消息回调
     * [payload] 发布的消息
     */
    override fun onPubMessage(payload: ByteArray) {

    }
}
```
&emsp;&emsp;onSubMessage 订阅的消息回调，因为存在订阅多个 topic 的情况，所以回调能知道是来自哪个 Topic 的消息；  
&emsp;&emsp;onPubMessage 发布的消息回调，用于确认发布的消息是否发送成功。  

**6. MQTT 订阅 Topic**  
```kotlin
/**
 * topic: String                     订阅的 Topic
 * qos: Int                          订阅的消息质量，可选值 0\1\2
 * listener: OnSubTopicListener?     订阅状态回调监听， 成功\失败
 */
mqttHelper.subscribe(topic, qos, listener)
```
&emsp;&emsp;需要在 MQTT 连接成功后才能订阅 topic，否则订阅 Topic 不成功，收不到对应消息  

**7. MQTT 取消订阅 Topic**  
```kotlin
/**
 * topic: String                    取消订阅的 Topic
 * listener: OnSubTopicListener?    取消订阅状态回调监听， 成功\失败
 */
mqttHelper.unsubscribe(topic, listener)
```

**8. MQTT 发布消息**  
```kotlin
/**
 * topic: String        发布的 topic，对应订阅该 topic 的客户端收到消息
 * payload: ByteArray   发布的消息内容
 * qos: Int             发送质量，可选值 0\1\2
 * retain: Boolean      是否保留消息
 */
mqttHelper.pubMessage(topic, payload, qos, retain)
```

**9. MQTT 断开连接**  
```kotlin
override fun onDestroy() {
    super.onDestroy()
    mqttHelper.disConnect()
}
```

**10. 通知设置**  
&emsp;&emsp;由于 MQTT 启动了一个 Service，而 Android 8.0 以上对于后台 Service 限制时长 5 秒；所以将 MqttService 绑定到 Notification 上成为了一个前台通知；通知的标题和内容显示可以在 strings.xml 中设置，对应属性如下：  
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="mqtt_notification_title">MQTT 通信标题</string>
    <string name="mqtt_notification_content">MQTT 通知内容</string>
    <bool name="mqtt_foreground_notification_low_26">false</bool>
</resources>
```
&emsp;&emsp;Android 8.0 及以上开启前台服务绑定到通知，8.0 以下默认不启用，可将 mqtt_foreground_notification_low_26 设为 true，将 8.0 以下设备也开启前台通知服务  


### 三、MQTT 相关参数说明  
&emsp;&emsp;创建 MQTT 实例时需要传送参数 MqttOptions,下面将介绍下部分参数;  
```kotlin
data class MqttOptions(
    val serviceUrl: String,     // MQTT 服务器地址,由 Scheme://IP:Port 组成, 例如 tcp://192.168.0.106:61613
    val username: String,       // MQTT 服务器管理员账号, Apollo 服务器默认 admin
    val password: String,       // MQTT 服务器密码, Apollo 服务器默认 password
    val clientId: String,       // 客户端 ID, 需要保持唯一性
    val willTopic: String = "", // 遗嘱消息 Topic
    val willMsg: String = "",   // 遗嘱消息
    val subQos: Int = 1,        // 订阅 MQTT 服务的消息质量
    val willQos: Int = 1,       // 发布遗嘱消息的服务质量
    val cleanSession: Boolean = true,   // 是否清楚 Session 会话
    val reconnectInterval: Long = 1000, // mqtt 连接失败后自动重连时间间隔(ms), 连接中断将立即重连
    val keepAliveInterval: Int = 60,    // 心跳包时间(s)
    val connectTimeOut: Int = 10        // MQTT 连接超时时间(s)
)
```

**1. Topic**  
&emsp;&emsp;MQTT 是一种发布/订阅的消息协议, 通过设定的主题 Topic,
发布者向 Topic 发送的 payload 负载消息会经过服务器, 转发到所有订阅
该 Topic 的订阅者  
&emsp;&emsp;**通配符**: 假想移动端消息推送场景,有的系统消息是全体用户接收,有的消息是 Android 或 iOS 设备接收, 又或者是某些消息具体推送到用户,当然, 对应的多种类型消息可以通过多订阅几个对应的 Topic 解决,也可以使用通配符;   
&emsp;&emsp;通配符有两个, "**+**" 和 "**#**", 与正斜杠 "**/**" 组合使用;加号只能表示一级Topic, 井号可以表示任意层级 Topic; 例如: 订阅 Topic为 "**System/+**", 发布者发布的 Topic 可以是 System、System/Android、System/iOS; 但是不能是 System/iOS/123, 而订阅的 Topic 如果是"**System/#**" 则可以收到;   
&emsp;&emsp; 注意,只有订阅的 Topic 才可以使用 通配符, 发布和遗嘱的 Topic 不能包含通配符.  


**2. ClientID**  
&emsp;&emsp;发布者和订阅者都是属于客户端, 客户端与服务端建立连接之后,发送的第一个报文消息必须是 Connect 消息,而 Connect 的消息载荷中必须包含 clientID 客户端唯一标识;  
&emsp;&emsp;如果两个客户端的 clientID 一样, 则服务端记录第一个客户端连接之后再收到第二个客户端连接请求,则会向一个客户端发送 Disconnect 报文断开连接, 并连接第二个客户端, 而如果此时设置了自动重连, 第一个客户端再次连接,服务端又断开与第二个的连接, 连上第一个客户端, 如此将导致两个客户端不断的被挤掉重连.  
&emsp;&emsp;注意: clientID 使用的字符最好是 大小写字母和数字, 长度最好限制在[1, 23] 之间;  


**3. 遗嘱消息**  
&emsp;&emsp;可选参数, 客户端没有主动向服务端发起 disconnect 断开连接消息,然而服务端检测到和客户端之间的连接已断开, 此时服务端将该客户端设置的遗嘱消息发送出去
&emsp;&emsp;应用场景: 客户端因网络等情况掉线之后, 可以及时通知到所有订阅该遗嘱 Topic 的客户端;  
&emsp;&emsp;遗嘱 Topic 中不能存在通配符.  


**4. Session**  
&emsp;&emsp;客户端和服务端之间建立的会话状态, 一般用于消息保存, 如果设置清除 Session,则每次客户端和服务端建立连接会创建一个新的会话,之前连接中的消息不能恢复,  
&emsp;&emsp;而设置不清除会话, 对应发布者发送的 qos 为 1和2 的消息,还未被订阅者接收确认,则需要保存在会话中, 以便订阅者下次连接可以恢复这些消息;  
&emsp;&emsp;注意:  Session 存储的消息是保存在内容中的, 所以如果不是重要的消息,最好是设置清除 Session, 或者设置 qos = 0;  


**5. 心跳包**  
&emsp;&emsp;标识客户端传输一次控制报文到下一次传输之间允许的空闲时间；在这段时间内，如果客户端没有其他任何报文发送，必须发送一个 PINGREQ 报文到服务器，而如果服务端在 1.5 倍心跳时间内没有收到客户端消息，则会主动断开客户端的连接，发送其遗嘱消息给所有订阅者。而服务端收到 PINGREQ 报文之后，立即返回 PINGRESP 报文给客户端  
&emsp;&emsp;心跳时间单位为秒，占用2个字节，最大 2^16 - 1 = 65535秒（18小时12分钟15秒）,设置为 0 表示不使用心跳机制； 心跳时间一般设置为几分钟或几十秒即可，时间短点可以更快的发出遗嘱消息通知掉线，但是时间短会增加消息频率，影响服务端并发； 微信长连接为 300 秒，而三大运营商貌似也有个连接时间最小的为 5 分钟。  

**6. qos**  
&emsp;&emsp;服务质量等级 qos 对应两部分,一是客户端到服务端发送的消息, 一是服务端到客户端订阅的消息; 从发布者到订阅者实际 qos 为两段路中 qos 最小的。  
&emsp;&emsp;qos 可选值 0(最多交付一次)、1(最少交付一次)、2(正好交付一次);  
&emsp;&emsp;**qos = 0**：接收方不发送响应，发送方不进行重试；发送方只管发一次，不管是否发成功，也不管接收方是否成功接收，适用于不重要的数据传输；  
&emsp;&emsp;**qos = 1**：确保消息至少有一次到达接收方，发送方向接收方发送消息，需要等待接收方返回应答消息，如果发送方在一定时间之内没有收到应答，发送方继续下一次消息发送，直到收到应答消息，删除本地消息缓存，不再发送；所以接收方可能收到1-n次消息；适用于需要收到所有消息，客户端可以处理重复消息。  
&emsp;&emsp;**qos = 2**：确保消息只一次到达接收方，发送方和接收方之间消息处理流程最复杂；  
&emsp;&emsp;[**Mqtt Qos 深度解读**](https://www.jianshu.com/p/8b0291e8ee02) 和 [**MQTT协议QoS2 准确一次送达的实现**](https://blog.csdn.net/zerooffdate/article/details/78950907)   



**7. payload 负载消息**  
&emsp;&emsp;字节流类型, 是 MQTT 通信传输的真实数据  

**8. 保留消息**  
&emsp;&emsp;发布消息时设置, 对应参数 retain, 服务端将保留对应 Topic 最新的一条消息记录; 保留消息的作用是每次客户端连接上线都会收到其 Topic 的最后一条保留消息, 所以可能存在网络不稳定,频繁掉线重连,每次重连重复收到保留消息;  
&emsp;&emsp; 可以向对应的 Topic 发送一条 空消息,用于清除保留消息。  

------

MQTT 服务搭建 [Apache Apollo 服务器 搭建 MQTT 服务](https://www.jianshu.com/p/d8b03b53acfc)  

[mqtt 协议](https://github.com/mcxiaoke/mqtt/wiki)
