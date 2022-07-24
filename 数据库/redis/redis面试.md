# 综合

## redis为什么快

内存数据库

单线程实现 避免切换上下文和锁竞争 因为cpu不是瓶颈 网络io才是瓶颈

epoll 多路复用 非阻塞

内部高效数据结构

## Redis为什么做缓存，对比memcache和map

高并发 承受请求远大于数据库

高性能 快

memcache 数据结构单一 字符串 不支持持久化 无集群 多线程非阻塞io

map jvm大小限制 不支持持久化 分布式 过期

## 应用场景

分布式锁，session，缓存

排行榜 zset

计数器 incr

社交网络 点赞，共同好友

## redis为什么引入多线程

1. Redisv4.0（引入多线程处理异步任务）
2. Redis 6.0（在网络模型中实现多线程 I/O ）

异步任务，防止耗时命令阻塞

Redis6.0引入**多线程I/O**，只是用来**处理网络数据的读写和协议的解析**，而**执行命令依旧是单线程**。

利用多核优势分摊同步io

## 线程模型

![202105092153019692.png](http://blog-img.coolsen.cn/img/202105092153019692.png)

# 锁原理

## 分布式锁原理

cas+uuid+超时+加锁线程启动守护线程去延时锁

可重入 使用hash 

## redlock原理

防止主从切换 主机获取锁，挂了，从机成为master，但是不知道

1. 客户端在多个 Redis 实例上申请加锁
2. 必须保证大多数节点加锁成功
3. 大多数节点加锁的总耗时，要小于锁设置的过期时间
4. 释放锁，要向全部节点发起释放锁请求

# 底层

## key的结构

redis的key的结构

```c++
typedef struct redisObject {
    unsigned type:4;//对象类型（4位=0.5字节）
    unsigned encoding:4;//编码（4位=0.5字节）
    unsigned lru:LRU_BITS;//记录对象最后一次被应用程序访问的时间（24位=3字节）
    int refcount;//引用计数。等于0时表示可以被垃圾回收（32位=4字节）
    void *ptr;//指向底层实际的数据存储结构，如：SDS等(8字节)
} robj;
16字节
```

## string实现

SDS simple dynamic string

```c++
struct sdshdr {

    // buf 已占用长度
    unsigned int  len;

    // buf 剩余可用长度
    unsigned int  free;

    // 实际保存字符串数据的地方
    char buf[];
};
16字节+4字节+4字节+'/0' 故一开始embstr的分界线是39后来redis版本更新后
数据结构优化为sdshdrN N为8、16、32、64
struct __attribute__ ((__packed__)) sdshdr8 {
    //buf 已经使用的长度
    uint8_t len; /* used */
    //buf 分配的长度，等于buf[]的总长度-1，因为buf有包括一个/0的结束符
    uint8_t alloc; /* excluding the header and null terminator */
    //只有3位有效位，因为类型的表示就是0到4，所有这个8位的flags 有5位没有被用到
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    //实际的字符串存在这里
    char buf[];
};  
而embstr显然使用的是sdshdr8 即为1字节+1字节+1字节相较于39多剩余5个
```

O(1)求len

二进制安全

空间预分配和惰性空间释放

**空间预分配**

- 对SDS修改后，如果长度小于`1MB`,那么就会`free`属性就会分配和`len`属性同样大小的长度
- 对SDS修改后，如果长度大于`1MB`，那么会额外分配`1MB`的空间。

**惰性空间释放**

- 当需要缩小SDS字符长度的时候，不会立即释放空闲的字符串，而是会使用`free`记录起来。

encoding可以是**int(8位longlong)、embstr或者raw**

embstr存储小于等于44字节的字符串 sds与redisobject 连续 内存只分配一次

可以用 `long double` 类型表示的浮点数在 Redis 中也是作为字符串值来保存的

raw 存大于的 sds与redisobject 不连续 内存分配两次

## list实现

双向链表quicklist 每个节点是一个ziplist 即一块连续的内存存储

- 获取长度是O(1)的

## Hash实现

```c++
typedef struct dict {

    // 类型特定函数
    dictType *type;

    // 私有数据
    void *privdata;

    // 哈希表
    dictht ht[2];

    // rehash 索引
    // 当 rehash 不在进行时，值为 -1
    int rehashidx; /* rehashing not in progress if rehashidx == -1 */

} dict;
```

一开始是ziplist，之后变为dict

一个字典有两个哈希表，多的那个用来扩容，进行渐进式rehash，采用链地址法进行hash碰撞处理

1. 为 `ht[1]` 分配空间， 让字典同时持有 `ht[0]` 和 `ht[1]` 两个哈希表。
2. 在字典中维持一个索引计数器变量 `rehashidx` ， 并将它的值设置为 `0` ， 表示 rehash 工作正式开始。
3. 在 rehash 进行期间， 每次对字典执行添加、删除、查找或者更新操作时， 程序除了执行指定的操作以外， 还会顺带将 `ht[0]` 哈希表在 `rehashidx` 索引上的所有键值对 rehash 到 `ht[1]` ， 当 rehash 工作完成之后， 程序将 `rehashidx` 属性的值增一。
4. 随着字典操作的不断执行， 最终在某个时间点上， `ht[0]` 的所有键值对都会被 rehash 至 `ht[1]` ， 这时程序将 `rehashidx` 属性的值设为 `-1` ， 表示 rehash 操作已完成。

渐进式hash开始后，rud会在两个表上进行，而c只在ht[1]进行

## set实现

如果只有整数且不多，使用intset一个有序数组

否则升级为val值为null的字典

## zset实现

小的时候ziplist，有序链表

大的时候skiplist 使用跳表和字典 

跳表实现有序，字典实现O(1)查找

跳表插入是随机化插入多少层

# 事务

## 如何实现事务

multi 开启事务 discard结束事务 中间插入指令 exec 执行事务 如果指令语法出错 则事务取消 ，如果执行出错，则单条语句不执行 

这种事务类似于脚本执行 还可以用lua脚本代替

watch 乐观锁 监视key版本号 unwatch 取消监视 如果exec或者discard后就不用执行unwatch了

## 满足acid吗

满足隔离性 因为单线程不并发

一致性 执行前后保持一致性

# 持久化

## aof rdb

rdb 

快照技术 主进程fork子进程进程存入临时文件，然后将dump.rdb覆盖，

写时复制技术，当bgsave时，redis执行写操作，会复制一个副本，主线程对副本进行写操作，而原来那个会一直被子进程进行bgsave操作

`fork()` 之后，内核会把父进程的所有内存页都标记为**只读**。一旦其中一个进程尝试写入某个内存页，就会触发一个保护故障（缺页异常），此时会陷入内核。

内核将拦截写入，并为尝试写入的进程创建这个页面的一个**新副本**，恢复这个页面的**可写权限**，然后重新执行这个写操作，这时就可以正常执行了。

aof

日志技术 将写操作记录到日志里，有重写机制，如果日志过大，会fork出一个子进程将rdb快照二进制保存到日志头部以代替原来的写操作

## 过期键的删除策略

定时删除 每个key一个定时器 到期自动删除 内存友好 cpu不友好

惰性删除 用的时候取检查是否过期 cpu友好 内存友好

定期删除 定期对一批设置过期时间的key进行检查

## 内存淘汰策略

lru

lfu

random 

三种对应两个数据集合 一个是所有数据，一个是已设置过期时间的数据

还有一个是最接近过期的ttl 策略 以及不淘汰策略

# 集群

## 主从复制原理

salve 启动成功向master发送一个psync 

进行判断是否需要重新连接 根据三个概念 **offset buffer  服务器runid**

判断 先判断服务器runid是否相同，如果相同判断offset的数据是否在buffer中

是的话要进行一次全量复制，**master执行bgsave生成.rdb**，使用复制buffer记录从现在开始执行的写命令，然后将.rdb发送给从slave

slave清除旧数据，使用rdb更新为master bgsave时的状态

master将buffer中的命令发给slave，slave执行保持数据同步

## 主从延迟删除问题

slave不会主动淘汰过期key

1. 主动scan扫库 利用惰性删除
2. 升级版本 在读取数据时增加了过期判断

## 主从数据丢失问题

**异步复制同步丢失：**

主从复制时master挂掉，重新上线时已变为slave且数据被刷掉

**集群产生脑裂：**

网络分区故障，客户端操作旧master，但是sentinel选出新master，故障恢复后旧master变为slave,数据丢失

**如何解决**

修改配置文件 减少主从复制的延迟时间，避免大量数据丢失

## 哨兵模式原理

哨兵集群监控主从集群

三个监控任务

每10秒每个sentinel会对master和slave执行info命令获取主从关系和slave节点，当master下线后会变为1s一次  自动发现机制

每2秒每个sentinel通过master节点的channel交换信息（pub/sub）

每1秒每个sentinel对其他sentinel和redis节点执行ping操作（相互监控）

**监控** 每个sentinel向其他Master，Slave以及其他 Sentinel心跳检测1s 如果master主观下线，其他sentinel一秒一次确认它

一个节点没检测到是主观下线，大于等于一半节点是客观下线

**通知** 可以通过api通知客户端 集群故障

**故障转移**

master客观下线后，sentinel通过raft算法选举出leader进行master选举

基于四个条件 **slave与master断开时间** 超过指定值不选举，**优先级**  值越小越高  0不参加选举，**offse**t越大说明和master数据越接近，**运行id**越小说明运行时间越长

sentinel通知 新的master slaveof no one成为master，通知其他slave slaveof leader 

## 集群模式

16384个插槽 每个key计算CRC(16)%16384 确定属于哪个节点

增删节点 通过移动插槽

节点通信机制 gossip 每个节点一个元数据，更新慢慢扩散到所有节点，减轻压力

# 异常

## 写时一致性问题

先删再改

问题：删掉 后别人查旧数据并且写到缓存中，然后自己才改掉数据库

延时双删

改掉后隔一段时间把缓存删掉



先改再删

监听binlog

可以使用消息队列或者cannal进行删除同步

失败的话一直重试

## 读时并发问题

### 缓存穿透

原因 恶意攻击 出现大量无效url redis无法命中导致全部去数据库查询

**解决方案**

1. 空值缓存，redis缓存垃圾的数据
2. 布隆过滤器 概率型数据结构 对于一个key进行n次hash操作映射到bit数组的n位上，对于查询的key进行计算，如果映射的位置上不存在，则一定不存在，否则可能存在
3. 黑名单

### 缓存击穿

原因 热点key过期

**解决方案**

1. 不过期
2. 加锁
3. 实时调整

### 缓存雪崩

原因 大量key同时过期

**解决方案**

1. 不过期
2. 加锁
3. 随机时间

### 缓存预热

上线前把缓存加载进去

比如秒杀前先把商品信息加载到缓存中

