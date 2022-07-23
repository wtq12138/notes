# framework

timer

node client server address 

message 

commend result

模拟网络通信 反射实现 messageHandler 和OnTimer

# lab1

实现 at least once 和exactly once

at least once  

timer重试

exactly once

timer重试+hashmap序列号去重

# lab2

实现ViewServer和primary、backup 

ViewServer 

```
/* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    //分情况讨论
    // 只有Primary返回的viewNum等于当前curView的viewNum才能进行View的状态切换
    // 新建状态 任意server ping之后成为primary
    // 有P无B状态 任意非P Server ping之后 变为next 等待状态转换

 /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    // 检测server是否断连接
    // 1如果 cur.P 断联 next变为 {B,null}
    // 2如果 cur.B 断联 next变为 {P,null}
    // 然时后检测next的B
    // 如果存在说明handlePing 中任意非P Server ping之后 变为next 需要检测是否断连接 以免无意义转换
    // 如果不存在说明是1或2情况 需要从idlerServer中替补一个连接的
```





# lab3

paxos

通过ping 同步firstUnchosenIndex 进行日志GC

通过success 实现