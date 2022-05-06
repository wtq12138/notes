# redis

## 原理	

Redis是单线程+多路IO复用技术

套接字两种状态可读可写

![19.png](http://dockone.io/uploads/article/20211128/9e9adb183f51be8c67e9d7c053a1778f.png)



多路复用是指使用一个线程来检查多个文件描述符（Socket）的就绪状态，比如调用select和poll函数，传入多个文件描述符，如果有一个文件描述符就绪，则返回，否则阻塞直到超时。得到就绪状态后进行真正的操作可以在同一个线程里执行，也可以启动线程执行（比如使用线程池）

## key操作

keys *查看当前库所有key  (匹配：keys *1)

exists key判断某个key是否存在

type key 查看你的key是什么类型

del key    删除指定的key数据

unlink key  根据value选择非阻塞删除,异步删除

仅将keys从keyspace元数据中删除，真正的删除会在后续异步操作。

expire key 10  10秒钟：为给定的key设置过期时间

ttl key 查看还有多少秒过期，-1表示永不过期，-2表示已过期



select命令切换数据库

dbsize查看当前数据库的key的数量

flushdb清空当前库

flushall通杀全部库

## 五大数据类型

### String 

**数据结构**

Simple Dynamic String类似ArrayList

最大长度为512M，每次扩容翻倍，最多扩容+1m

二进制安全，即有专门的len属性记录长度，而不是通过’\0‘来判断

**常见操作** 

**原子性**

get <key>

set <key> <val>

append <key> 

strlen <key>

**setnx** <key> <val> 只有在 key 不存在时  设置 key 的值

**incr** <key>

decr <key>

incrby/decrby <key> <步长>

mset <key1><value1><key2><value2> ..... 同时设置一个或多个 key-value对 

mget <key1><key2><key3> .....

msetnx <key1><value1><key2><value2> ..... 

getrange <key><起始位置><结束位置> substr

setrange <key><起始位置><value>用 <value> 覆写<key>所储存的字符串值，从<起始位置>开始(**索引从0****开始**)。

setex <key><过期时间><value>设置键值的同时，设置过期时间，单位秒。

getset <key><value>以新换旧，设置了新值同时获得旧值。

### List

单键多值

数据结构

**数据结构**

双向链表

首先在列表元素较少的情况下会使用一块连续的内存存储，即ziplist

数量多时改成quicklist，即将多个ziplist用指针串联起来

**常见操作**

lpush/rpush <key><value1><value2><value3>

lpop/rpop <key> 值在键在，值光键亡。

rpoplpush <key1><key2>从<key1>列表右边吐出一个值，插到<key2>列表左边。

lrange <key><start><stop>按照索引下标获得元素(从左到右) stop可以用-1代表末尾

lindex <key><index>按照索引下标获得元素(从左到右)

llen <key>获得列表长度 

linsert <key> before <value><newvalue> 在<value>的后面插入<newvalue>插入值

lrem <key><n><value>从左边删除n个value(从左到右)

lset<key><index><value>将列表key下标为index的值替换成value

### Set

**数据结构**

Redis的Set是string类型的无序集合。它底层其实是一个value为null的hash表

**常见操作**

sadd <key><value1><value2> ..... 将一个或多个 member 元素加入到集合 key 中，已经存在的 member 元素将被忽略

smembers <key>取出该集合的所有值。

sismember <key><value>判断集合<key>是否为含有该<value>值，有1，没有0

scard<key>返回该集合的元素个数。

srem <key><value1><value2> .... 删除集合中的某个元素。

spop <key>随机从该集合中吐出一个值。

srandmember <key><n>随机从该集合中取出n个值。不会从集合中删除 。

smove <source><destination>value把集合中一个值从一个集合移动到另一个集合

sinter <key1><key2>返回两个集合的交集元素。

sunion <key1><key2>返回两个集合的并集元素。

sdiff <key1><key2>返回两个集合的差集元素(key1中的，不包含key2中的)

### Hash

**数据结构**

Hash类型对应的数据结构是两种：ziplist（压缩列表），hashtable（哈希表）。当field-value长度较短且个数较少时，使用ziplist，否则使用hashtable。

**常见操作**

hset <key><field><value>给<key>集合中的 <field>键赋值<value>

hget <key1><field>从<key1>集合<field>取出 value 

hmset <key1><field1><value1><field2><value2>... 批量设置hash的值

hexists<key1><field>查看哈希表 key 中，给定域 field 是否存在。 

hkeys <key>列出该hash集合的所有field

hvals <key>列出该hash集合的所有value

hincrby <key><field><increment>为哈希表 key 中的域 field 的值加上增量 1  -1

hsetnx <key><field><value>将哈希表 key 中的域 field 的值设置为 value ，当且仅当域 field 不存在 .

### Zset

**数据结构**

zset底层使用了两个数据结构

（1）hash，hash的作用就是关联元素value和权重score，保障元素value的唯一性，可以通过元素value找到相应的score值。

（2）跳跃表，跳跃表的目的在于给元素value排序，根据score的范围获取元素列表。常见操作

**常见操作**

zadd <key><score1><value1><score2><value2>…将一个或多个 member 元素及其 score 值加入到有序集 key 当中。

zrange <key><start><stop> [WITHSCORES]  返回有序集 key 中，下标在<start><stop>之间的元素带WITHSCORES，可以让分数一起和值返回到结果集。

zrangebyscore key minmax [withscores] [limit offset count] 返回有序集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的成员。有序集成员按 score 值递增(从小到大)次序排列。 

zrevrangebyscore key maxmin [withscores] [limit offset count]        同上，改为从大到小排列。 

zincrby <key><increment><value>   为元素的score加上增量

zrem <key><value>删除该集合下，指定值的元素

zcount <key><min><max>统计该集合，分数区间内的元素个数 

zrank <key><value>返回该值在集合中的排名，从0开始。

## 新的三个数据类型

### Bitmaps

**数据结构**

01字符串数组

**常见操作**

setbit<key><offset><value>设置Bitmaps中某个偏移量的值（0或1）

getbit<key><offset>获取Bitmaps中某个偏移量的值

bitcount

bitop and(or/not/xor) <destkey> [key…]，并交取反亦或

### HyperLogLog

**数据结构**

？？

**常见操作**

pfadd <key>< element> [element ...]  添加指定元素到 HyperLogLog 中

pfcount<key> [key ...] 计算HLL的近似基数，可以计算多个HLL

pfmerge<destkey><sourcekey> [sourcekey ...] 将一个或多个HLL合并后的结果存储在另一个HLL中，比如每月活跃用户可以使用每天的活跃用户来合并计算可得

### Geospatial

**数据结构**

二维坐标

**常见操作**

geoadd<key>< longitude><latitude><member> [longitude latitude member...]  添加地理位置（经度，纬度，名称）

geopos <key><member> [member...] 获得指定地区的坐标值

geodist<key><member1><member2> [m|km|ft|mi ] 获取两个位置之间的直线距离

georadius<key>< longitude><latitude>radius m|km|ft|mi  以给定的经纬度为中心，找出某一半径内的元素

## 发布和订阅

多个客户端连接一个redis服务器时，客户端可以订阅channel，未订阅的客户端可以向channel发送信息

```
SUBSCRIBE channel1
publish channel1 hello
非持久化，无法漫游历史记录
```



## 整合springboot

配置类

```java
@EnableCaching
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setConnectionFactory(factory);
//key序列化方式
        template.setKeySerializer(redisSerializer);
//value序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
//value hashmap序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
//解决查询缓存转换异常的问题
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
// 配置序列化（解决乱码的问题）,过期时间600秒
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(600))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();
        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
        return cacheManager;
    }
}
```

注入之后使用它进行操作

```java
@Autowired
private RedisTemplate redisTemplate;
```

## Redis事务

### Multi、Exec、discard

排队、执行、放弃排队

### 错误处理

组队中某个命令出现了报告错误，执行时整个的所有队列都会被取消。

如果执行阶段某个命令报出了错误，则只有报错的命令不会被执行，而其他的命令都会执行，不会回滚。

### watch

乐观锁原理，可以监视多个key，如果被监视的key在别的事务中执行，则version变化，导致exec failed

### 三大特性

1. 单独隔离操作 序列化操作不会被打断
2. 没有隔离级别 exec一次性执行
3. 非原子性 失败不回滚

### 和mysql事务区别

acid中只满足ci不满足ad

### 秒杀场景

使用key-String记录库存，key-Set记录成功名单，事务解决超卖问题，连接池解决连接超时问题

lua脚本解决库存遗留问题

库存遗留问题是因为乐观锁修改版本号后导致虽然有库存但是买不完的情况

lua脚本可以解决的原因是：redis保证lua脚本的执行是原子性的但是又不依靠版本号的原理所以可以解决

## 持久化

### RDB

Snapshot原理

内存中redis主进程不进行io操作而是fork一个子进程将数据**存入一个临时文件然后将之前保存的dump.rdb覆盖** 

fork在linux中引进写时复制技术，父进程和子进程共用一块物理内存，只有写时才会复制一份给子进程

### AOF

写操作日志重现	

aof文件过大后rewrite原理

fork进程写时复用，4.0前是将数据重新用精简指令恢复，4.0后是将rdb快照中的数据二进制保存到aof头部来代替原有的写操作日志指令

重写触发机制

AOF当前大小>= base_size +base_size*100% (默认)且当前大小>=64mb(默认)

### 主从复制

#### docker如何配置

创建三个配置文件run三个redis进程，分别从主机6379,6380,6381映射到容器6379端口，将三个配置文件也挂载进去

slaveof host 6379

这里需要容器间网络通信，docker容器启动采用虚拟网桥桥接，docker inspect redis查看6379的容器在虚拟ip地址，此地址即为host

 info replication查看当前redis的主从配置

#### 原理

salve 启动成功向master发送一个sync

如果重连后要进行一次全量复制，master执行bgsave生成.rdb，使用复制buffer记录从现在开始执行的写命令，然后将.rdb发送给从slave

slave清除旧数据，使用rdb更新为master bgsave时的状态

master将buffer中的命令发给slave，slave执行保持数据同步

## 哨兵模式

配置sentinel.conf

redis-sentinel  sentinel.conf启动哨兵，记得打开26379防火墙

当master down掉之后该哨兵会主观认为sdown了，再由其他哨兵发起ping确认，如果认为主观sdown的哨兵大于n

从而由主观sdown变为客观odown

从而发起选举

选举条件 优先级靠前 偏移量最大 runid最小

选举成功后 sentinel向原来的slave发送slave of 新host命令 复制新master

当原master上线后 sentinel同上

```
优先级在redis.conf中默认：slave-priority 100，值越小优先级越高
偏移量是指获得原主机数据最全的
每个redis实例启动后都会随机生成一个40位的runid
```



```
sentinel monitor mymaster 172.17.0.6 6379 n   n为至少有多少个哨兵同意迁移的数量。
sentinel auth-pass mymaster 023176
```

## 集群

### 两种模式

代理访问

**无中心化**

Redis 集群实现了对Redis的水平扩容，即启动N个redis节点，将整个数据库分布存储在这N个节点中，每个节点存储总数据的1/N。

slots 对于每个操作需要计算出key需要放在哪个结点中，即slots,不在一个slots中的值不能使用mget等多键操作

{}来定义组的概念，从而使key中{}内相同内容的键值对放到一个slot中去。

master挂掉后，slave变为master，master恢复后依然是slave

当一个结点的master和slave都挂掉后，有两种策略 一种是集群挂掉，一种是结点挂掉

## 应用问题

### 缓存穿透

原因 恶意攻击 出现大量无效url redis无法命中导致全部去数据库查询

**解决方案**

1. 空值缓存，redis缓存垃圾的数据
2. bitmap白名单
3. 布隆过滤器 概率型数据结构 对于一个key进行n次hash操作映射到bit数组的n位上，对于查询的key进行计算，如果映射的位置上不存在，则一定不存在，否则可能存在
4. 黑名单

### 缓存击穿

原因 某一key过期后，导致短时间并发全部查库

**解决方案**

1. 预热 将热门数据的expire时间延长
2. 实时调整
3. 加锁

### 缓存雪崩

原因 大量key集中过期，导致短时间并发全部查库

**解决方案**

1. 多级缓存
2. 加锁
3. expire时间随机分散
4. 设置提前量，过期触发线程去更新缓存

### redis实现分布式锁

通过设置uuid防止误删锁，lua脚本保证删除的原子性

```java
@GetMapping("testLockLua")
public void testLockLua() {
    //1 声明一个uuid ,将做为一个value 放入我们的key所对应的值中
    String uuid = UUID.randomUUID().toString();
    //2 定义一个锁：lua 脚本可以使用同一把锁，来实现删除！
    String skuId = "25"; // 访问skuId 为25号的商品 100008348542
    String locKey = "lock:" + skuId; // 锁住的是每个商品的数据

    // 3 获取锁
    Boolean lock = redisTemplate.opsForValue().setIfAbsent(locKey, uuid, 3, TimeUnit.SECONDS);

    // 第一种： lock 与过期时间中间不写任何的代码。
    // redisTemplate.expire("lock",10, TimeUnit.SECONDS);//设置过期时间
    // 如果true
    if (lock) {
        // 执行的业务逻辑开始
        // 获取缓存中的num 数据
        Object value = redisTemplate.opsForValue().get("num");
        // 如果是空直接返回
        if (StringUtils.isEmpty(value)) {
            return;
        }
        // 不是空 如果说在这出现了异常！ 那么delete 就删除失败！ 也就是说锁永远存在！
        int num = Integer.parseInt(value + "");
        // 使num 每次+1 放入缓存
        redisTemplate.opsForValue().set("num", String.valueOf(++num));
        /*使用lua脚本来锁*/
        // 定义lua 脚本
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        // 使用redis执行lua执行
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        // 设置一下返回值类型 为Long
        // 因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，
        // 那么返回字符串与0 会有发生错误。
        redisScript.setResultType(Long.class);
        // 第一个要是script 脚本 ，第二个需要判断的key，第三个就是key所对应的值。
        redisTemplate.execute(redisScript, Arrays.asList(locKey), uuid);
    } else {
        // 其他线程等待
        try {
            // 睡眠
            Thread.sleep(1000);
            // 睡醒了之后，调用方法。
            testLockLua();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

```

### 缓存一致问题

**是否真的有必要改缓存**

先写数据库再改缓存

会出现写数据库1 --写数据库2 -----写缓存2-----写数据库1的问题

先改缓存再写数据库

会出现改缓存后，改数据库失败的问题

缓存真正使用次数不多，但是修改次数很多



先删再改

问题：删掉后还没写完，另一个查询在数据库查到旧值并将缓存覆盖为旧值

解决方案

延时双删

即删掉缓存改掉数据库后等待一段时间再删掉缓存

这一段时间大于从数据库读+写入缓存的时间

先改再删

问题：改数据库时，读到的是旧值

解决方案

 消息队列或者binlog同步

**要求不高时**

最简单的设置超时时间

**终极办法**

**读写锁**



## 6.0新特性

ACL

Access Control List

用户可执行的key与命令

IO多线程

## 缓存和Redisson的逻辑

查看redis中有无缓存？

命中->直接返回

未命中->查询数据库->是否能抢到锁

抢到锁->执行业务逻辑

否则->自旋等待

```java
public Map<String, List<Catelog2Vo>> getCatalogJson2() {
    ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
    String catalogJson = ops.get("catalogJson");
    if (StringUtils.isEmpty(catalogJson)) {
        System.out.println("缓存不命中...查询数据库...");
        //2、缓存中没有数据，查询数据库
        Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();

        return catalogJsonFromDb;
    }

    System.out.println("缓存命中...直接返回...");
    //转为指定的对象
    Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson,new TypeReference<Map<String, List<Catelog2Vo>>>(){});

    return result;
}

public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

    //1、占分布式锁。去redis占坑
    //（锁的粒度，越细越快:具体缓存的是某个数据，11号商品） product-11-lock
    //RLock catalogJsonLock = redissonClient.getLock("catalogJson-lock");
    //创建读锁
    RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("catalogJson-lock");

    RLock rLock = readWriteLock.readLock();

    Map<String, List<Catelog2Vo>> dataFromDb = null;
    try {
        rLock.lock();
        //加锁成功...执行业务
        dataFromDb = getDataFromDb();
    } finally {
        rLock.unlock();
    }
    //先去redis查询下保证当前的锁是自己的
    //获取值对比，对比成功删除=原子性 lua脚本解锁
    // String lockValue = stringRedisTemplate.opsForValue().get("lock");
    // if (uuid.equals(lockValue)) {
    //     //删除我自己的锁
    //     stringRedisTemplate.delete("lock");
    // }

    return dataFromDb;

}
```

## @Cacheable

spring的缓存机制

如果缓存有就直接拿，没有先写库再写缓存

```
@Cacheable(value = {"category"},key = "#root.method.name",sync = true)
```

先更新数据库后删掉缓存

```
@CacheEvict(value = "category",allEntries = true)       //删除某个分区下的所有数据
```

批量操作

```
// @Caching(evict = {
//         @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
//         @CacheEvict(value = "category",key = "'getCatalogJson'")
// })
```

不管有没有缓存，先写库再写缓存

```
@CachePut
```

```
* 4、Spring-Cache的不足之处：
*  1）、读模式
*      缓存穿透：查询一个null数据。解决方案：缓存空数据
*      缓存击穿：大量并发进来同时查询一个正好过期的数据。解决方案：加锁 ? 默认是无加锁的;使用sync = true来解决击穿问题
*      缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间
*  2)、写模式：（缓存与数据库一致）
*      1）、读写加锁。
*      2）、引入Canal,感知到MySQL的更新去更新Redis
*      3）、读多写多，直接去数据库查询就行
*
*  总结：
*      常规数据（读多写少，即时性，一致性要求不高的数据，完全可以使用Spring-Cache）：写模式(只要缓存的数据有过期时间就足够了)
*      特殊数据：特殊设计
```
