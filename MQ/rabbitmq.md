# MQ的好处

流量消峰      应用解耦       异步处理

# Connection和Channel

connecttion是tcp物理连接，channel是归属于connection的逻辑连接

# exchange分发消息的四种策略

direct  headers 点对点  前者完全匹配routing key   后者性能差匹配的是head

fanout topic  点对多 前者是广播exchange连接的所有queue   后者通配符匹配

exchange 四种类型通过routing key 绑定 queue 

发消息带上routing key 根据类型消息路由到 queue

# 消息应答的方式

Consumer->Queue ack机制

basicAck	肯定确认

basicNack 否定确认

basicReject 拒绝丢弃

multiple含义批量应答 true 5,6,7,8 回复8等于5,6,7,8都回复了 

只要消费者不应答，队列中持久化的消息就不会删除

# 发布确认

Broker->Publisher confirm

单个 批量 异步

异步 通过concurrenthashmap进行存储消息

# 消息顺序如何保证

乱序的情况

一个queue 多个consumer 消费速度不确定顺序无法保证 一个consumer 起多个线程消费 同样消费速度不确定顺序无法保证，只能保证队列中的消息顺序

解决措施

多个queue但是一个queue去对应一个consumer，

# 可靠传输实现

大招 事务 性能下降250倍

Publisher----->Broker[exchange------->Queue]-------->Consumer

p->b confirmcallback 确认机制

e->q returncallback 回退机制

q->c ack机制 如果没收到还会消费者的ack消息会重新排队

# prefetchcount

用来设置消费者的预取值，即queue与consumer 之间channel上未确认的消息数量，来实现不公平分发

# 死信队列

满足如下条件消息会进入死信路由

TTL 取queue和message两者TTL的较小值

> queue的TTL时间一到就会直接扔到死信队列中
>
> 而message的判断ttl时间是在到消费者时才判断，如果消息积压，已过期的消息还会在队列中呆较长时间

消息被拒绝且停止重新入队

队列长度达到上限

通过一个无人监听的队列和被人监听的死信队列实现延迟队列

需要设置死信路由和路由键即ttl才会将消息放入死信路由

```
arguments.put("x-dead-letter-exchange", "order-event-exchange");
arguments.put("x-dead-letter-routing-key", "order.release.order");
arguments.put("x-message-ttl", 60000); // 消息过期时间 1分钟
```

# 延迟队列实现

先正常路由到无人监听的队列，导致进入死信路由

再死信路由到死信队列

# 消息可靠性

消息丢失

生产者丢消息 

发布确认 Broker->publisher confirm通过concurrenthashmap异步处理 ,这里也可以发消息写库

当confirmcallback和returncallback时

如果失败 则重新投递修改对应状态，定时扫库发送

消息队列丢消息

发布确认 Broker->publisher confirm如果rabbitmq直接挂掉可以集群镜像队列

消费者丢消息

消息应答 Consumer->Queue 

队列自己有重试机制

```sql
CREATE TABLE `mq_message` (
  `message_id` char(32) NOT NULL,
  `content` text,
  `to_exchane` varchar(255) DEFAULT NULL,
  `routing_key` varchar(255) DEFAULT NULL,
  `class_type` varchar(255) DEFAULT NULL,
  `message_status` int(1) DEFAULT '0' COMMENT '0-新建 1-已发送 2-错误抵达 3-已抵达',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```



消费者

•2、消息重复

•消息消费成功，事务已经提交，ack时，机器宕机。导致没有ack成功，Broker的消息 重新由unack变为ready，并发送给其他消费者

•消息消费失败，由于重试机制，自动又将消息发送出去

•成功消费，ack时宕机，消息由unack变为ready，Broker又重新发送

•消费者的业务消费接口应该设计为**幂等性**的。比如扣库存有 工作单的状态标志

•使用**防重表**（redis/mysql），发送消息每一个都有业务的唯 一标识，处理过就不用处理

•rabbitMQ的每一个消息都有redelivered字段，可以获取**是****否** **是被重新投递过来的**，而不是第一次投递过来的



•3、消息积压

•消费者宕机积压

•消费者消费能力不足积压

•发送者发送流量太大

•上线更多的消费者，进行正常消费

•上线专门的队列消费服务，将消息先批量取出来，记录数据库，离线慢慢处理



# 整合springboot

## conf

```properties
# 虚拟主机配置
spring.rabbitmq.virtual-host=/
# 开启发送端消息抵达Broker确认
spring.rabbitmq.publisher-confirms=true
# 开启发送端消息抵达Queue确认
spring.rabbitmq.publisher-returns=true
# 只要消息抵达Queue，就会异步发送优先回调returnfirm
spring.rabbitmq.template.mandatory=true
# 手动ack消息，不使用默认的消费端确认
spring.rabbitmq.listener.simple.acknowledge-mode=manual
```

## Test

```java
class GulimallOrderApplicationTests {

    //用来建立binding queue exchange
    @Autowired
    AmqpAdmin amqpAdmin;

    //用来发消息
    @Autowired
    RabbitTemplate rabbitTemplate;
    
    @Test
    void sendMessage() {
        OrderEntity orderEntity=new OrderEntity();
        rabbitTemplate.convertAndSend("testAdmin","testAdmin",orderEntity,new CorrelationData("1"));
    }
    @Test
    void contextLoads() {
       amqpAdmin.declareExchange(new DirectExchange("testAdmin",true,false));
       Queue queue=new Queue("testAdmin" ,true,false,false);
       amqpAdmin.declareQueue(queue);
        Binding binding=new Binding("testAdmin", Binding.DestinationType.QUEUE,"testAdmin","testAdmin",null);
       amqpAdmin.declareBinding(binding);
        log.debug("success");
    }

}
	@RabbitHandler//只能在方法上 可以通过参数重载 三种参数
	//Message message,  //消息的原始类型
	//Channel channel,	//客户端与rabbitmq服务器建立的通道
	//Entity entity		// convert之后的
    @RabbitListener(queues = {"testAdmin"}) //可在方法上和类上
    @RabbitListener(queues = {"testAdmin"})
    public void receiveMessage(Message message, Channel channel,Entity entity) {
        System.out.println(message);
        if(message.getMessageProperties().getDeliveryTag()%2==0) {
            try {
                // 序列号 是否批量操作
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            try {
                // 序列号  是否批量操作 是否回到队列
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```

