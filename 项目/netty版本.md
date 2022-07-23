# 原来

tracker只存ip:port和hash

分片block大小是4k

seed  文件的信息  数据结构 hash 文件名 文件大小 分片hash  

localfile 已下载的文件  数据结构 hash  文件大小 路径 已有分片个数 bitmap 下载情况  读写本地文件 

通过ConcurrentHashMap 存key hash v localfile 知道当前本地文件的情况

每次要从本地加载过去下载的信息，固定时间落盘



downloadcontroller 聚合线程池 把给客户端服务器发的请求提交到线程池，获取返回结果

下载时先得到在线peer ip:port,

然后从任意一个peer请求得到种子

获得所有peer的bitmaps

优先获得数量较少的bit位上的资源



暂停或者下载完成之后 会将内存中localfile 修改到本地，会丢掉数据

客户端服务器 while(1) 监听端口 确保 其他客户端可以和自己交互



三种协议 封装实体类 json格式 

一 请求bitmap 请求文件的下载情况bitmap

二 请求seed   请求文件的种子

三 请求第i片byte数组 请求某片



一个下载线程池 

8

16

128

抛异常拒绝策略

一个分享带宽线程池

# 改进

**线程同步异步方式**

之前：一个下载线程中不断向clientserver发请求 

请求种子 请求bitmap  请求分片 但是这三次请求每次都需要线程池get()同步控制异步线程



之后：netty客户端 多线程监听事件， 只需要获取channel不断发请求即可

在handler中异步处理

# 协议

4字节魔数

1字节版本

1字节序列化方式

1字节 message type

4字节 序列号

1字节 填充

4字节 message length

message