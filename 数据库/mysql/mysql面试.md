https://www.cnblogs.com/ZhuChangwu/p/13953209.html

# 原理

## 三大范式

1.列不可再分 2. 非主属性依赖主属性 3.非主属性不互相依赖

## MyISAM 和 InnoDB 的区别有哪些

后者有事务 外键 行级锁 MVCC

前者有 表的行数

InnoDB 是聚集索引，数据文件是和索引绑在一起的，必须要有主键，通过主键索引效率很高；

MyISAM 是非聚集索引，数据文件是分离的，索引保存的是数据文件的指针，主键索引和辅助索引是独立的。

## 超键、候选键、主键、外键分别是什么？

候选键 最小超键

超键 只要能标明身份的推出其他属性的

主键 唯一标识 任一个候选键

外键 其他表的主键

## 表文件结构

![img](https://oss-emcsprod-public.modb.pro/wechatSpider/modb_20210905_8350a3dc-0de7-11ec-9c8b-38f9d3cd240d.png)

## 三大日志

![QQ截图20220320111101](F:\资料\八股复习\冲冲冲\数据库\mysql\images\QQ截图20220320111101.png)

redo_log innodb特有 物理日志  环形存储  无事务的概念 一句sql写一句

为了实现内存和磁盘一致，一直随机写磁盘浪费资源，随机io并且一次修改只会修改一页中的一小部分数据十分浪费，redo_log相当于一种改进顺序写而且存储小

redo_log要先写到redo_log buffer中再刷盘 默认是提交事务后刷盘 刷盘也不是直接到磁盘而是先到os page cache，然后fsync

> UNIX系统提供了三个系统调用：sync、fsync、fdatasync
>
> sync,将缓冲区排入写队列就返回，不保证写磁盘操作结束
>
> fsync，传fd参数，写磁盘结束返回，更可靠

**三种刷盘时机**

- **0** ：设置为 0 的时候，表示每次事务提交时不进行刷盘操作
- **1** ：设置为 1 的时候，表示每次事务提交时都将进行刷盘操作（默认值）
- **2** ：设置为 2 的时候，表示每次事务提交时都只把 redo log buffer 内容写入 page cache
- 后台线程每秒刷盘一次

**环形存储**

- **write pos** 是当前记录的位置，一边写一边后移
- **checkpoint** 是当前要擦除的位置，也是往后推移

wp 追赶 cp



undo_log innodb特有 逻辑日志  环形存储  无事务的概念 一句sql前写一句反向操作 

采用rollback segment管理，存储在.ibd文件即聚簇索引文件，对应1024个 undo log segment  分为insert和update两种undo_log

日志中的行信息有三个隐藏字段 db_row_id 隐藏主键 db_trx-id 事务id db_roll_ptr回滚指针 头插法 链表串联历史版本

因为insert刚插入行数据，所以回滚指针指向空，如果update了，那么就会将b+树指针指向修改后的行数据，然后回滚指针指向原来那条数据

同样有undo_log_buffer



binlog mysql特有 逻辑日志(增删改sql) 主从复制和数据恢复 事务commit后才将其状态修改

 三种格式 statement 原sql 部分函数会不一致 row 精确数据 mixed 根据语句选择

要写到binlog_cache

## 两阶段提交

![MySQL数据更新流程](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3ddd109ee25242aba5c176c6af58ae2d~tplv-k3u1fbpfcp-zoom-1.image)

两阶段提交 

先写入redo_log此时属于prepare状态，再写入binlog，提交事务后才能把redo_log状态改为commit

**挂掉恢复原理**

如果redo_log是commit直接提交

如果是redo_log是prepared，判断binlog是否完整，是的话commit，否则rollback

## Buffer Pool和change buffer

https://www.cnblogs.com/myseries/p/11307204.html

buffer pool 缓冲池	 定期刷盘

命中 修改缓冲池内存 写入磁盘redo_log buffer 未命中 增加一步磁盘随机读 ，从磁盘读到缓冲池

**lru链表 淘汰缓存页原理**

没有采取传统lru淘汰缓冲页，因为有预读操作，线性预读，可能有的页面不是真正有效的，即预读--失败

所以稍微复杂一点采用**新生代和老年代**， 预读的放在老年代，而真正调用之后放在新生代

**新问题** 缓冲池污染，全表扫描，就需要访问大量的页，所有的数据页都会被加载到新生代的头部，但只会访问一次，真正的热数据被大量换出。

![img](https://img2018.cnblogs.com/blog/885859/201908/885859-20190806094655989-736215797.png)



**时间窗口机制** 只有**满足**“**被访问**”并且“**在老生代停留时间大于T**，才会被放入新生代头部；

free链表双向链表存空闲页

flush链表记录脏页(内存中修改了但是没有写入磁盘)

刷盘时机 1.每秒后台线程刷盘2.关闭mysql 时刷盘

change buffer  写缓冲   也可以定期刷盘

目的是**懒加载**

当写操作时，buffer pool未命中，先不磁盘随机读，而是先记录此次修改，之后真正读的时候，读到bufferpool，将修改合并到其中

> 唯一索引无法使用写缓冲，因为需要检验唯一索引的唯一性，必须将页读到bufferpool 中

写操作 修改写缓冲内存 顺序写入磁盘redo_log

读操作 此时缓冲池还没有 磁盘随机读 写缓冲读取 加载到缓冲池中

> 写缓冲只有当真正查询时才会将修改合并到缓冲池

## mysql语句执行过程

连接器--->缓存(8.0删除)--->解析器------>优化器-------->执行器------>存储引擎

# 语法

## varchar和char

最后一位存len

检索效率后者高

## drop truncate delete

![image-20210822203927822](http://blog-img.coolsen.cn/img/image-20210822203927822.png)

## exists和in

哪个表小就用哪个表来驱动，A表小就用EXISTS, B表小就用IN。

`in` 在查询的时候，首先查询子查询的表，然后将内表和外表做一个笛卡尔积，然后按照条件进行筛选。

exists遍历循环外表，检查外表中的记录有没有和内表的的数据一致的。

```
SELECT * FROM A WHERE EXISTS (SELECT cc FROM B WHERE B. cc=A.cc)
for(i in A)
	for(j in B)
		if j.cc == i.cc then . ..
		
SELECT * FROM A WHERE cc IN (SELECT cc FROM B)
for(i in B)
	for(j in A)
		if j.cc == i.cc then . ..


```

## 约束有哪些

unique pk fk check not null

## count(*),count(1)和count(列)

前两个没有本质区别，不会忽略null，count(列)会忽略null

如果在MyISAM中，是O(1)复杂度，因为存储了row_count，而innodb需要mvcc无法维护变量，是O(n)复杂度

如果count(列)尽量使用二级索引，而对于前两个优化器会自动选择空间更小的索引统计

## select *

内部会进行转换浪费资源和时间

无法使用覆盖索引



# 索引

## 索引类型

数据结构分 B+索引 hash索引 rtree 全文索引

应用层次 唯一索引 主键索引 普通索引

物理存储上 聚簇索引和非聚簇索引

## 底层原理

hash 对于索引列计算hashcode保存

b树 多路平衡树 每个结点都保存数据

b+ 在b树基础上的改进 数据都在叶子结点上且叶子有指针顺序访问

## b+树相较其他的优势

内部结点无数据只有索引，而mysql页的大小固定16kb，一个结点是一个页，那么页面中可以装更多索引，使得树矮胖，减少io次数

适合区间查询 因为叶子结点有指针 而b树的话需要中序遍历 

Hash：只适合等值查找 不支持范围 且只有memory引擎 hash碰撞

平衡二叉树和红黑树 树的高度较高 减少io次数

## 聚簇索引和非聚簇索引

根据主键或者非空唯一索引建立，如果都没有就建立虚拟索引

前者叶子结点有完整数据，后者只有主键，所以需要回表  （也可以是覆盖索引）

## 联合索引

必须按照顺序挨个使用因为索引的排序是按照声明的先后顺序

因此要 注意最左前缀原则 即把使用频繁的由高到低放在左边 如果indx(a,b,c) where a=xx and c=xx c不会用 如果a=xx and b>xx and c=xx b会用但是c不会用了

而in的话等价于or，会在优化器优化为最佳索引匹配

## 覆盖索引

查询的所有字段均在索引中出现，即不需要回表直接可以返回

## 前缀索引

字段前几个字节作为索引，好处是压缩了空间

坏处是前缀长度过小的话区分度不高导致出现类似哈希碰撞的情况增加回表次数，同时无法用覆盖索引进行优化

## 索引下推

对于index(A,B)如果查询条件是 A=xx,B like "%xx%" 那么只能走A索引，因为B不满足最左前缀原则

如果不开启索引下推，那么会找到满足A条件的直接回表，然后返回服务器上层判断

开启之后可以在存储引擎层直接判断 AB两个条件，不满足直接跳过不会频繁回表

## 为什么主键自增作为索引

减少插入时B+树的分裂

## 建立索引的注意事项

非空 =和in可以乱序 最左前缀匹配原则 索引列不要计算

将区分度高的放在联合索引前面 

索引字段的大小要尽量小

尽量扩展索引不要新建索引

频繁更新慎重建立

## 索引失效的情况

列用来 函数 计算 类型转换

or连接不同类型 like %xx  范围条件 会导致后边失效

不等于 is null is not null 都可能会导致失效

not in not exists

## 索引优化

连表查询时 尽量被驱动表使用索引 小表作为驱动表

子查询创建的临时表不会有索引

where 排序和分组 是where后排序再分组，索引按照这个顺序建立

**分页优化**

order by走索引 查询很快，连表查询时也是走索引 所有优化效率很高

```sql
SELECT * FROM student t, (SELECT id FROM student ORDER BY id LIMIT 2000000,10) a
WHERE t.id = a.id; 
```

## 普通索引和唯一索引的区别

https://segmentfault.com/a/1190000038321537

唯一索引的好处是不可能出现哈希碰撞的情况，但是普通索引基本也不会出现哈希碰撞导致占满一整页，不得不多次io

而普通索引却可以使用change buffer，省去了将数据页读到buffer pool的过程中，而唯一索引必须判断唯一性不能省去此过程，唯一索引无法使用change buffer故普通索引更新效率要高

# 事务

## acid

原子性 不可再分

一致性 保证我们定义的事务逻辑一致 

隔离性 

持久性

除了隔离性是锁实现，其他都是redo日志和undo日志实现

## 四种隔离级别

读未提交    脏读   a读数据 b修改此时不提交 a读多次 数据不一致

读已提交	不可重复读  a读数据 b修改并提交多次 a读多次 数据不一致

可重复读	幻读 a读数据 b插入数据，a读多次，发现有新数据

可串行

## mvcc的原理

multivesion concurrent control 多版本并发控制， 数据某一时刻的快照实现一个事务一张视图ReadView

ReadView四个概念

creator_trx_id 当前事务id

trxs_ids 活跃事务id列表

up_limit_id 列表中最小id

low_limit_id 应该分配的下一个事务id

**如果访问的是版本链(表中的一行)中trx_id是当前事务，说明可以访问，如果小于最小id，说明可以访问，如果大于等于下一个事务id，说明不能访问**

**如果位于最小id和下一个id区间内 需要判断是否是活跃id，是的话不能访问，否则可以**



读已提交是每个sql一个快照

可重复读的原理就是一个事务中同样的查询语句只会开启一个ReadView

通过 undo log 版本链和 ReadView 机制，可以保证一个事务只可以读到该事务自己修改的数据或该事务开始之前的数据。

## 如何解决幻读

快照读 select   mvcc快照一个事务同一个查询一张快照

实时读 for update 、in share mode、 update 、insert 、delete 加行锁 不让其他事务插入数据

其实没有完全解决 如果全是快照读，那么自然解决了，但是其实只要快照读和当前读混用就会出问题

![img](https://pic2.zhimg.com/80/v2-ab809605698a47bf3c42174e63859935_720w.jpg)

# 锁

## 分类

### 粒度划分

**行级锁INNODB引擎**

记录锁(record lock) 把行锁住 读锁写锁

间隙锁(gap lock)  RR级别下解决幻读问题，作用在索引上，锁的是间隙

临键锁(next key lock) 前两个合体 innodb默认行锁 左开右闭

插入意向锁(insert intention lock) 对于gap锁范围内的插入，插入前的等待会产生该锁，也是一种gap锁

行锁都是基于索引实现的

**1、当使用唯一索引来等值查询的语句时,降级为记录锁。**

**2、当使用唯一索引来等值查询的语句时, 如果这行数据不存在，会产生间隙锁。**

**3、当使用唯一索引来范围查询的语句时，对于满足查询条件但不存在的数据产生间隙(gap)锁，如果查询存在的记录就会产生记录锁，加在一起就是临键锁(next-key)锁。**

**4、当使用普通索引不管是锁住单条，还是多条记录，都会产生间隙锁；同时会在唯一索引产生记录锁**

**5、在没有索引上不管是锁住单条，还是多条记录，都会产生表锁；**

**页级锁BDB引擎**



**表级锁MYISAM引擎**

表级别 x锁s锁

意向(intention)锁 ix锁is锁 为了使得加表级锁时迅速判断是否已有行级锁而出现的 存储引擎自己维护 加了说明不能再加大锁了

自增锁(auto inc) 对于自增主键的操作

元数据锁（meta data) 对于增删改查时加读锁，其他进行表结构修改加写锁

### 数据类型操作划分

S锁X锁

读时加s锁x锁都行

写时

delete 先在聚簇索引中找到记录，获取x锁，然后将其移入垃圾链表

update 存储空间不变时，x锁，变化时先delete再insert

insert 不加锁 有隐式锁

### 态度上

悲观锁

乐观锁

乐观锁添加vesion字段

多读写少的环境

开启事务

先查出id和version

```
update t_goods 
        set status=#{status},name=#{name},version=version+1 
        where id=#{id} and version=#{version} 
```

如果两个事务同时进行操作那么会出现一个事务update后加了临键锁，这样另一个事务update就会卡住

原来那个事务提交后，卡住的update就会发现version变化，更新失败。

### 显式隐式

如果是聚簇索引 有trx_id，插入一条记录，其他事务如果想对其加锁，会先看trx_id是否在活跃列表中，如果是为其创建x锁，

如果是二级索引 页面内有个属性是最大事务id，如果小于最活跃id说明事务已提交，否则定位到聚簇索引，并按照其逻辑执行

# 高可用

## 主从复制原理

三个线程 

binlog dump thread 主库线程负责将主库的binlog发给从库，当主库读时加锁

从库 io thread 连接主库 拉取主库的binlog，放进自己的relay log 中继日志

从库 sql 执行relay log线程中的sql

## 一致性问题

异步复制 主库写完就返回客户端，导致从库有延迟

半同步复制 主库写通知从库也写，至少一个从库写完返回客户端

组复制  分布式paxos算法实现

# 分库分表

## 水平拆分和垂直拆分

水平 多个库存一张表中数据  横着拆分

垂直  多个库存一张表中数据竖着拆分
