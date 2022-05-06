

tracker只存ip:port和hash

分片block大小是4k

seed  文件的信息  数据结构 hash 文件名 文件大小 分片hash  

localfile 已下载的文件  数据结构 hash  文件大小 路径 已有分片个数 bitmap 下载情况  读写本地文件 

通过ConcurrentHashMap 存key hash v localfile 知道当前本地文件的情况

每次要从本地加载过去下载的信息



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



线程池 

8

16

128

抛异常拒绝策略
