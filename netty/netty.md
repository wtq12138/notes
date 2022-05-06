# IO模型

阻塞非阻塞 应用对于TCP缓冲区获取资源是否需要等待 

阻塞，非阻塞：**进程/线程要访问的数据是否就绪，进程/线程是否需要等待；**

阻塞指用户发起io请求需要彻底完成才能返回用户空间

非阻塞指用户发起io请求后直接会得到状态值 



异步IO的优化思路是**解决了应用程序需要先后发送询问请求、发送接收数据请求两个阶段的模式，**

**在异步IO的模式下，只需要向内核发送一次请求就可以完成状态询问和数拷贝的所有操作。**

同步，异步：**访问数据的方式，同步需要用户线程的内核态主动读写数据，在读写数据的过程中还是会阻塞； **

**异步只需要I/O操作完成的通知，用户线程并不主动读写数据，由操作系统内核完成数据的读写。**

* 同步：线程自己去获取结果（一个线程）
* 异步：线程自己不去获取结果，而是由其它线程送结果（至少两个线程）



概念指用户态和内核的交互方式，传输层以上为用户，反之为内核

同步指用户发起io请求后需要等待或者轮询

异步指用户发起io请求直接继续执行，等待kernel io完成后通知用户，或者调用用户注册的回调函数



**阻塞跟非阻塞，是在网卡到内核缓冲区；同步跟非同步，指的是需不需要等待CPU进行数据的拷贝**。



异步阻塞和同步非阻塞这个实际根本不应用，太笨了

## BIO

Blocking IO （同步阻塞）

用户需要等待kernel等待数据和拷贝到用户空间

此时用户线程从runnable变为block，cpu利用率不够

![img](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bd0a4ac52aee4724ba4cfdca1b54679f~tplv-k3u1fbpfcp-zoom-in-crop-mark:1304:0:0:0.awebp)

如何去改进呢

```
while(1) {
  connfd = accept(listenfd);  // 阻塞建立连接
  pthread_create（doWork);  // 创建一个新的线程
}
```

小trick 服务器新建线程去接受阻塞这样服务器主线程不会阻塞仍然可以监听客户端

用OS实现即为NIO **为我们提供一个非阻塞的 read 函数**。

```
int n = read(connfd, buffer) != SUCCESS); //文件描述符
```

## NIO 

javaNIO其实是多路复用

Non-blocking IO（同步非阻塞）

用户可以在发起io请求后返回，但是并未读到真正数据，用户线程需要轮询，耗费大量cpu

当网卡拷贝到内核缓冲区前都属于非阻塞，需要轮询read，而拷贝后进行系统调用仍然是阻塞

![img](https://p1-tt-ipv6.byteimg.com/img/pgc-image/a63401468ccd47c3a16dd349bfcb93d0~tplv-obj.image)

小trick  我们把文件描述符放到数组中开一个线程，轮询read

是不是有点多路复用的感觉呢

但是read会造成用户态->内核态 频繁切换，故os提供了一个新的函数select

## IO多路复用

同步阻塞/非阻塞

根据select、poll、epoll的函数参数决定是否阻塞

用OS实现即为select 但是它的数组有上限1024而poll是链表

而epoll优化更猛

1. select 调用需要传入 fd 数组，需要拷贝一份到内核，高并发场景下这样的拷贝消耗的资源是惊人的。（可优化为不复制）

2. select 在内核层仍然是通过遍历的方式检查文件描述符的就绪状态，是个同步过程，只不过无系统调用切换上下文的开销。（内核层可优化为异步事件通知）

3. select 仅仅返回可读文件描述符的个数，具体哪个可读还是要用户自己遍历。（可优化为只返回给用户就绪的文件描述符，无需用户做无效的遍历）

所以 epoll 主要就是针对这三点进行了改进。

1. 内核中保存一份文件描述符集合，无需用户每次都重新传入，只需告诉内核修改的部分即可。
2. 内核不再通过轮询的方式找到就绪的文件描述符，而是通过异步 IO 事件唤醒。
3. 内核仅会将有 IO 事件的文件描述符返回给用户，用户也无需遍历整个文件描述符集合。

## AIO

异步io

## Reactor

多路复用

## Proactor

AIO

# java

## BIO

基本实战

服务器绑定端口 while() {accept得到socket  多线程handle} 

客户端建立服务器端口socket 进行io通信

```java
@Component
public class BioServer {
    @Autowired
    private ThreadPoolExecutor pool;
    @SneakyThrows
    public void handle(Socket socket) {
        try{
            byte [] bytes=new byte[1024];
            InputStream is=socket.getInputStream();
            while(true) {
                int num = is.read(bytes);
                if(num==-1) {
                    break;
                }else {
                    System.out.println(new String(bytes));
                }
            }
        }finally {
            System.out.println("socket close");
            socket.close();
        }
    }

    @SneakyThrows
    public void listen() {
        ServerSocket serverSocket=new ServerSocket(8888);
        System.out.println(pool);
        System.out.println("server start");
        while(true)
        {
            Socket socket = serverSocket.accept();
            System.out.println("client connect");
            pool.execute(() -> handle(socket));
        }
    }
}


	@Test
    void contextLoads() throws InterruptedException {
        threadPoolExecutor.submit(()->bioServer.listen());
        for(int i=1;i<8;i++) {
            Thread.sleep(1000);
            threadPoolExecutor.submit(()->{
                Socket socket= null;
                try {
                    socket = new Socket("127.0.0.1", 8888);
                    System.out.println(socket.getLocalSocketAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    OutputStream os = socket.getOutputStream();
                    os.write(Thread.currentThread().toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
```



## NIO newIO



为什么说JAVA NIO提供了基于Selector的异步网络I/O？

底层是IO多路复用，实际上是同步阻塞的，但是对于外界来看调用方法和处理方法是两个线程



组件

**selector 多路复用对应多个channel 根据事件驱动切换不同channel**

**channel  一个连接对应一个buffer  双向的体现在OS**

**buffer 底层是个数组，可以在输入输出间切换 flip() 双向的**

### buffer 

除boolean外另外七种基本数据类型均有

一开始

![](F:/资料/网课/黑马/Netty教程源码资料/讲义/Netty-讲义/img/0021.png)

写模式下，position 是写入位置，limit 等于容量，下图表示写入了 4 个字节后的状态

![](F:/资料/网课/黑马/Netty教程源码资料/讲义/Netty-讲义/img/0018.png)

flip 动作发生后，position 切换为读取位置，limit 切换为读取限制

![](F:/资料/网课/黑马/Netty教程源码资料/讲义/Netty-讲义/img/0019.png)

读取 4 个字节后，状态

![](F:/资料/网课/黑马/Netty教程源码资料/讲义/Netty-讲义/img/0020.png)

clear 动作发生后，状态

![](F:/资料/网课/黑马/Netty教程源码资料/讲义/Netty-讲义/img/0021.png)

compact 方法，是把未读完的部分向前压缩，然后切换至写模式

![](F:/资料/网课/黑马/Netty教程源码资料/讲义/Netty-讲义/img/0022.png)



```java

public abstract class Buffer {
    private int mark = -1; //标记位
	private int position = 0; //当前读写位置
	private int limit; // 终点
	private int capacity; //大小
    .../
}
flip() 将postion变为0
```

### Channel

从文件流中getChannel() 

channel从buffer读写

我们要从buffer中获取数据

NIO还提供了MappedByteBuffer，可以让文件直接在内存(堆外内存)中直接修改，而如何同步到文件中由NIO来完成

一个channel可以对应多个buffer数组 顺序读写

```java
public class FileChannelTest {
    @SneakyThrows
    public void write() {
        String str="hello";
        FileOutputStream fileOutputStream=new FileOutputStream("1.txt");
        FileChannel channel = fileOutputStream.getChannel();
        ByteBuffer byteBuffer=ByteBuffer.allocate(1024);
        byteBuffer.put(str.getBytes());
        byteBuffer.flip();
        channel.write(byteBuffer);
        fileOutputStream.close();
    }
    @SneakyThrows
    public void read() {
        FileInputStream fileInputStream=new FileInputStream("F:\\资料\\网课\\尚硅谷\\netty\\demo\\1.txt");
        FileChannel channel=fileInputStream.getChannel();
        ByteBuffer byteBuffer=ByteBuffer.allocate(1024);
        channel.read(byteBuffer);
        byteBuffer.flip();
        System.out.println(new String(byteBuffer.array()));
    }
    @SneakyThrows
    public void copy() {
        FileInputStream fileInputStream=new FileInputStream("F:\\资料\\网课\\尚硅谷\\netty\\demo\\1.txt");
        FileOutputStream fileOutputStream=new FileOutputStream("2.txt");
        FileChannel channel1=fileInputStream.getChannel();
        FileChannel channel2=fileOutputStream.getChannel();
        ByteBuffer byteBuffer=ByteBuffer.allocate(1024);
//        channel1.read(byteBuffer);
//        byteBuffer.flip();
//        channel2.write(byteBuffer);
        channel2.transferFrom(channel1, 0, channel1.size());
    }
    @SneakyThrows
    public static void main(String[] args) {
        FileChannelTest test=new FileChannelTest();
        test.write();
        test.read();
        test.copy();
    }
}
```

### Selector

select()方法返回selectedKeys集合，该集合是io就绪集合，其中聚合Channel

select方法 根据传参时间 决定阻塞非阻塞

selectNow() 非阻塞

```java
// The set of keys registered with this Selector
    private final Set<SelectionKey> keys;

    // The set of keys with data ready for an operation
    private final Set<SelectionKey> selectedKeys;

    // Public views of the key sets
    private final Set<SelectionKey> publicKeys;             // Immutable
    private final Set<SelectionKey> publicSelectedKeys;     // Removal allowed, but not addition
```

### 基本实战

服务器 先绑定ServerSocketChannel端口，然后将其注册到selector中监听accept事件 

selector.select>0 (io多路复用) 说明此时有事件需要执行

遍历selectionKeys，一个selectionKey对应一个channel对应一个事件，

如果是accept事件，注册之后，`SocketChannel socketChannel=serverSocketChannel.accept();`accept自动会将事件删除，

然后注册`socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));` 可以直接绑定buffer

如果是其他读写事件，当捕获异常时说明io异常客户端下线，手动cancel并且关闭channel

read之后记得flip

遍历selectionKeys记得remove iterator

```java
@Component
public class NioServer {
    @SneakyThrows
    public void listen() {
        //获取channel
        ServerSocketChannel serverSocketChannel=ServerSocketChannel.open();
        //获取selector
        Selector selector=Selector.open();
        //绑定socket
        serverSocketChannel.bind(new InetSocketAddress(8888));
        serverSocketChannel.configureBlocking(false);
        //注册serverSocketChannel用来监听accept事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while(true) {
            if(selector.select(1000)==0) {
                System.out.println("no connect");
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            for (; iterator.hasNext(); ) {
                SelectionKey next =  iterator.next();
                if(next.isAcceptable()) {
                    System.out.println("connect");
                    SocketChannel socketChannel=serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                }
                if(next.isReadable()) {
                    SocketChannel channel = null;
                    try {
                        channel = (SocketChannel)next.channel();
                        channel.configureBlocking(false);
                        ByteBuffer buffer=(ByteBuffer) next.attachment();
                        channel.read(buffer);
                        buffer.flip();
                        System.out.println(new String(buffer.array()));
                    }catch (IOException e) {
                        try {
                            log.debug(channel.getRemoteAddress()+"下线");
                            channel.close();
                            next.cancel();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
                iterator.remove();
            }
        }
    }
}
```

### 群聊

服务器只负责与客户端建立连接，并且读取客户端的信息，然后转发给其他客户端

> 转发时注意排除自己和ServerSocketChannel
>
> if(channel instanceof SocketChannel&&channel!=self) 

客户端只负责与服务器发消息，并且接受服务器转发的其他客户端的消息

```java
public class ChatServer {
    private Selector selector;
    private ServerSocketChannel listenChannel;

    @SneakyThrows
    public ChatServer() {
        selector=Selector.open();
        listenChannel=ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(8889));
        listenChannel.configureBlocking(false);
        listenChannel.register(selector, SelectionKey.OP_ACCEPT,ByteBuffer.allocate(1024));
    }

    @SneakyThrows
    public void listen() {
        while(true) {
            if(selector.select(1000)==0) {
                System.out.println("no user connect");
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectionKeys.iterator(); iterator.hasNext(); ) {
                SelectionKey key =  iterator.next();
                if(key.isAcceptable()) {
                    SocketChannel accept = listenChannel.accept();
                    accept.configureBlocking(false);
                    accept.register(selector,SelectionKey.OP_READ,ByteBuffer.allocate(1024));
                    System.out.println(accept.getRemoteAddress()+"  up");
                }else if(key.isReadable()) {
                    read(key);
                }else if(key.isWritable()) {

                }
                iterator.remove();
            }
        }
    }
    private void read(SelectionKey key) {
        SocketChannel channel = (SocketChannel)key.channel();
        ByteBuffer buffer = (ByteBuffer)key.attachment();
        int read = 0;
        try {
            read = channel.read(buffer);
            if(read>0) {
                String msg=new String(buffer.array());
                buffer.flip();
                System.out.println("from 客户端:"+msg);
                dispatcher(msg,channel);
            }
        } catch (IOException e) {
            try {
                System.out.println(channel.getLocalAddress()+"离线");
                key.cancel();
                channel.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    @SneakyThrows
    private void dispatcher(String message,SocketChannel self) {
        Set<SelectionKey> keys = selector.keys();
        for (Iterator<SelectionKey> iterator = keys.iterator(); iterator.hasNext(); ) {
            SelectionKey key = iterator.next();
            Channel channel = key.channel();
            if(channel instanceof SocketChannel&&channel!=self) {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                ((SocketChannel) channel).write(buffer);
            }
        }

    }
}

@Slf4j
public class ChatClient {
    private final static String HOST="127.0.0.1";
    private final static int PORT =8889;
    private Selector selector;
    private SocketChannel socketChannel;
    public String username;


    @SneakyThrows
    public ChatClient() {
        selector = Selector.open();
        socketChannel = SocketChannel.open(new InetSocketAddress(HOST,PORT));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ,ByteBuffer.allocate(1024));
        username = socketChannel.getLocalAddress().toString();
    }
    @SneakyThrows
    public void sendMessage(String info) {
        info =username+":"+info;
        socketChannel.write(ByteBuffer.wrap(info.getBytes()));
    }

    public void readInfo() {
        int select = 0;
        try {
            select = selector.select(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(select>0) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectionKeys.iterator(); iterator.hasNext(); ) {
                SelectionKey key =  iterator.next();
                if(key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    try {
                        channel.read(buffer);
                        buffer.flip();
                    } catch (IOException e) {
                        try {
                            log.debug("{}",channel.getRemoteAddress()+"下线");
                            channel.close();
                            key.cancel();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                    System.out.println(new String(buffer.array()));
                }
                iterator.remove();
            }
        }
    }
}

```

### 处理消息边界

固定长度

分隔符

TLV格式 type length value

可以手动扩容

```java
private static void split(ByteBuffer source) {
    source.flip();
    for (int i = 0; i < source.limit(); i++) {
        // 找到一条完整消息
        if (source.get(i) == '\n') {
            int length = i + 1 - source.position();
            // 把这条完整消息存入新的 ByteBuffer
            ByteBuffer target = ByteBuffer.allocate(length);
            // 从 source 读，向 target 写
            for (int j = 0; j < length; j++) {
                target.put(source.get());
            }
            debugAll(target);
        }
    }
    source.compact(); // 0123456789abcdef  position 16 limit 16 因为未调用get() pos不会++所以compact后 pos=limit
}					
						split(buffer);//处理完整消息，如果找到分隔符输出后 compact()即将pos
                        // 需要扩容
                        if (buffer.position() == buffer.limit()) {
                            ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                            buffer.flip();
                            newBuffer.put(buffer); // 0123456789abcdef3333\n
                            key.attach(newBuffer);
                        }

```

### 多线程优化

主线程selector监听accept事件，其他线程监听读写事件

问题

`selector.select();`阻塞导致无法执行register

`sc.register(selector, SelectionKey.OP_READ, null);`

```java
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector boss = Selector.open();
        SelectionKey bossKey = ssc.register(boss, 0, null);
        bossKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8888));
        // 1. 创建固定数量的 worker 并初始化
        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        AtomicInteger index = new AtomicInteger();
        while(true) {
            boss.select();
            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    log.debug("connected...{}", sc.getRemoteAddress());
                    // 2. 关联 selector
                    log.debug("before register...{}", sc.getRemoteAddress());
                    // round robin 轮询
                    workers[index.getAndIncrement() % workers.length].register(sc); // boss 调用 初始化 selector , 启动 worker-0
                    log.debug("after register...{}", sc.getRemoteAddress());
                }
            }
        }
    }
    static class Worker implements Runnable{
        private Thread thread;
        private Selector selector;
        private String name;
        private volatile boolean start = false; // 还未初始化
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        public Worker(String name) {
            this.name = name;
        }

        // 初始化线程，和 selector
        public void register(SocketChannel sc) throws IOException {
            if(!start) {
                selector = Selector.open();
                thread = new Thread(this, name);
                thread.start();
                start = true;
            }
            selector.wakeup(); // 唤醒 select 方法 boss
            sc.register(selector, SelectionKey.OP_READ, null); // boss
        }

        @Override
        public void run() {
            while(true) {
                try {
                    selector.select(); // worker-0  阻塞
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(16);
                            SocketChannel channel = (SocketChannel) key.channel();
                            log.debug("read...{}", channel.getRemoteAddress());
                            channel.read(buffer);
                            buffer.flip();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### 零拷贝

含义是不需要走java用户缓冲区

原本读写流程

* **用户态**切换至**内核态**，通过DMA读入内核缓冲区
* 从**内核态**切换回**用户态**，通过cpu将数据从**内核缓冲区**读入**用户缓冲区**，
* 将数据从**用户缓冲区**（byte[] buf）写入 **socket 缓冲区**，cpu 会参与拷贝
* 从**用户态**切换至**内核态**，调用操作系统的写能力，使用 DMA 将 **socket 缓冲区**的数据写入网卡，不会使用 cpu

用户态与内核态的切换发生了 3 次，这个操作比较重量级

拷贝四次

![](img/0024.png)

DirectByteBuf 将堆外内存映射到 jvm 内存中来直接访问使用

拷贝三次 切换三次



![](img/0025.png)

进一步优化（底层采用了 linux 2.1 后提供的 sendFile 方法）

拷贝三次 切换一次 

![](img/0026.png)

再优化（ linux 2.4）

将需要cpu拷贝的内核缓冲区到socket缓冲区中的信息大大减少

直接从内核缓冲区到网卡

拷贝两次 切换一次

![](img/0027.png)

# Netty

## Helloworld

流程

![](img/0040.png)



```java
public class HelloServer {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler());
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                super.channelRead(ctx, msg);
                                log.debug((String) msg);
                            }
                        });
                    }
                })
                .bind(8888);
    }
}

public class HelloClient {
    @SneakyThrows
    public static void main(String[] args) {
        new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect(new InetSocketAddress("localhost",8888))
                .sync()
                .channel()
                .writeAndFlush("hello world");
    }
}
```

## 组件

### EventLoop

EventLoop 

一个单线程selector

* 一条线是继承自 j.u.c.ScheduledExecutorService 因此包含了线程池中所有的方法
* 另一条线是继承自 netty 自己的 OrderedEventExecutor，
  * 提供了 boolean inEventLoop(Thread thread) 方法判断一个线程是否属于此 EventLoop
  * 提供了 parent 方法来看看自己属于哪个 EventLoopGroup

EventLoopGroup 

一组EventLoop，channel会绑定到其中一个上

继承自 netty 自己的 EventExecutorGroup

* 实现了 Iterable 接口提供遍历 EventLoop 的能力
* 另有 next 方法获取集合中下一个 EventLoop



无参构造会调用默认线程数的构造，默认线程为max(1,cpu核*2)

```java
DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
```

可以处理普通任务和定时任务，因为本质是两个线程池的继承

**如何彻底提高性能**

accept事件一个线程监听，io事件多个线程监听，具体处理时对于不同handler有着自己对应的线程处理

**如何在不同handler切换时切换线程**

```java
static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
    final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
    // 下一个 handler 的事件循环是否与当前的事件循环是同一个线程
    EventExecutor executor = next.executor();
    
    // 是，直接调用
    if (executor.inEventLoop()) {
        next.invokeChannelRead(m);
    } 
    // 不是，将要执行的代码作为任务提交给下一个事件循环处理（换人）
    else {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                next.invokeChannelRead(m);
            }
        });
    }
}
```

* 如果两个 handler 绑定的是同一个线程，那么就直接调用
* 否则，把要调用的代码封装为一个任务对象，由下一个 handler 的线程来调用

```java
EventLoopGroup group = new DefaultEventLoopGroup();
        new ServerBootstrap()
                .group(new NioEventLoopGroup(),new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new LoggingHandler());
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug(buf.toString(Charset.defaultCharset()));
                                ctx.fireChannelRead(msg); // 让消息传递给下一个handler
                            }
                        }).addLast(group, "handler2", new ChannelInboundHandlerAdapter() {
                            @Override                                         // ByteBuf
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug(buf.toString(Charset.defaultCharset()));
                            }
                        });
                    }
                })
                .bind(8888);
```

### Channel

channel 的api

* close() 可以用来关闭 channel 异步
* closeFuture() 用来处理 channel 的关闭
  * sync 方法作用是同步等待 channel 关闭
  * 而 addListener 方法是异步等待 channel 关闭
* pipeline() 方法添加处理器
* write() 方法将数据写入
* writeAndFlush() 方法将数据写入并刷出



**重新审视connect和sync**

connect是一个异步方法，交给 NioEventLoopGroup()去连接

所以需要sync同步等待连接成功后再获取channel

```java
ChannelFuture future = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect(new InetSocketAddress("localhost", 8888));
                
                future.sync();
        Channel channel = future.channel();
        channel.writeAndFlush("hello2");
        System.out.println('1');
```

**添加listener异步回调**

```java
future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.channel();
                log.debug("{}",channel+"test");
                channel.writeAndFlush("hello2");
            }
        });
```

同步关闭和异步关闭

```java
// 获取 CloseFuture 对象， 1) 同步处理关闭， 2) 异步处理关闭
        ChannelFuture closeFuture = channel.closeFuture();
        /*log.debug("waiting close...");
        closeFuture.sync();
        log.debug("处理关闭之后的操作");*/
        closeFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.debug("处理关闭之后的操作");
                group.shutdownGracefully();
            }
        });
```

### Future Promise

在异步处理时，经常用到这两个接口

netty 的 Future 继承自 jdk 的 Future，而 Promise 又继承 netty Future 

* jdk Future 只能同步等待任务结束（或成功、或失败）才能得到结果
* netty Future 可以同步等待任务结束得到结果，也可以异步方式得到结果，但都是要等任务结束
* netty Promise 不仅有 netty Future 的功能，而且脱离了任务独立存在，只作为两个线程间传递结果的容器

```java
DefaultEventLoop eventExecutors = new DefaultEventLoop();
DefaultPromise<Integer> promise = new DefaultPromise<>(eventExecutors);

eventExecutors.execute(()->{
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    log.debug("set success, {}",10);
    promise.setSuccess(10);
});

log.debug("start...");
log.debug("{}",promise.getNow()); // 还没有结果
log.debug("{}",promise.get());
```



| 功能/名称    | jdk Future                     | netty Future                                                 | Promise      |
| ------------ | ------------------------------ | ------------------------------------------------------------ | ------------ |
| cancel       | 取消任务                       | -                                                            | -            |
| isCanceled   | 任务是否取消                   | -                                                            | -            |
| isDone       | 任务是否完成，不能区分成功失败 | -                                                            | -            |
| get          | 获取任务结果，阻塞等待         | -                                                            | -            |
| getNow       | -                              | 获取任务结果，非阻塞，还未产生结果时返回 null                | -            |
| await        | -                              | 等待任务结束，如果任务失败，不会抛异常，而是通过 isSuccess 判断 | -            |
| sync         | -                              | 等待任务结束，如果任务失败，抛出异常                         | -            |
| isSuccess    | -                              | 判断任务是否成功                                             | -            |
| cause        | -                              | 获取失败信息，非阻塞，如果没有失败，返回null                 | -            |
| addLinstener | -                              | 添加回调，异步接收结果                                       | -            |
| setSuccess   | -                              | -                                                            | 设置成功结果 |
| setFailure   | -                              | -                                                            | 设置失败结果 |

### Handler & Pipeline

ChannelHandler 用来处理 Channel 上的各种事件，分为入站、出站两种。所有 ChannelHandler 被连成一串，就是 Pipeline

* 入站处理器通常是 ChannelInboundHandlerAdapter 的子类，主要用来读取客户端数据，写回结果
* 出站处理器通常是 ChannelOutboundHandlerAdapter 的子类，主要对写回结果进行加工

ChannelInboundHandlerAdapter 是按照 addLast 的顺序执行的，而 ChannelOutboundHandlerAdapter 是按照 addLast 的逆序执行的。ChannelPipeline 的实现是一个 ChannelHandlerContext（包装了 ChannelHandler） 组成的双向链表

![](img/0009.png)

**eg**

入站处理器中ctx.fireChannelRead(msg) 是 **调用下一个入站处理器**

出站处理器中，ctx.write(msg, promise) 的调用也会 **触发上一个出站处理器**

3 处的 ctx.channel().write(msg) 会 **从尾部开始触发** 后续出站处理器的执行

实际上也是

```java
super.channelRead(ctx, msg);实际上调用了ctx.fireChannelRead(msg)
super.write(ctx, msg, promise);实际上调用了ctx.write(msg, promise);
```

* 如果注释掉 6 处代码，则仅会打印 1 2 3 6

ctx.channel().write(msg) vs ctx.write(msg)

一个是从尾部开始向前找，一个是从当前向前找

```java
new ServerBootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioServerSocketChannel.class)
    .childHandler(new ChannelInitializer<NioSocketChannel>() {
        protected void initChannel(NioSocketChannel ch) {
            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    System.out.println(1);
                    ctx.fireChannelRead(msg); // 1
                }
            });
            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    System.out.println(2);
                    ctx.fireChannelRead(msg); // 2
                }
            });
            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    System.out.println(3);
                    ctx.channel().write(msg); // 3
                }
            });
            ch.pipeline().addLast(new ChannelOutboundHandlerAdapter(){
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, 
                                  ChannelPromise promise) {
                    System.out.println(4);
                    ctx.write(msg, promise); // 4
                }
            });
            ch.pipeline().addLast(new ChannelOutboundHandlerAdapter(){
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, 
                                  ChannelPromise promise) {
                    System.out.println(5);
                    ctx.write(msg, promise); // 5
                }
            });
            ch.pipeline().addLast(new ChannelOutboundHandlerAdapter(){
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, 
                                  ChannelPromise promise) {
                    System.out.println(6);
                    ctx.write(msg, promise); // 6
                }
            });
        }
    })
    .bind(8080);
```

### ByteBuf

**组成**

![](img/0010.png)

与原本Buffer订单去呗

对于读写pos有两个 所以不需要flip进行切换

可以动态扩容

**创建**

直接内存和堆内存

池化和非池化

```java
ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(10);
```

**扩容**

* 如何写入后数据大小未超过 512，则选择下一个 16 的整数倍，例如写入后大小为 12 ，则扩容后 capacity 是 16
* 如果写入后数据大小超过 512，则选择下一个 2^n，例如写入后大小为 513，则扩容后 capacity 是 2^10=1024（2^9=512 已经不够了）

**读取**

初始

```
read index:0 write index:12 capacity:16
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 01 02 03 04 00 00 00 05 00 00 00 06             |............    |
+--------+-------------------------------------------------+----------------+
```

读取

```
System.out.println(buffer.readByte());
System.out.println(buffer.readByte());
System.out.println(buffer.readByte());
System.out.println(buffer.readByte());
log(buffer);
1
2
3
4
read index:4 write index:12 capacity:16
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 00 00 00 05 00 00 00 06                         |........        |
+--------+-------------------------------------------------+----------------+
```

重复读取

还有一种方法使用get方法，不会改变readidex

```java
buffer.markReaderIndex();//标记当前ridx
System.out.println(buffer.readInt());
log(buffer);
5
read index:8 write index:12 capacity:16
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 00 00 00 06                                     |....            |
+--------+-------------------------------------------------+----------------+
buffer.resetReaderIndex();//重置ridx
log(buffer);
read index:4 write index:12 capacity:16
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 00 00 00 05 00 00 00 06                         |........        |
+--------+-------------------------------------------------+----------------+
```

**retain & release**

* UnpooledHeapByteBuf 使用的是 JVM 内存，只需等 GC 回收内存即可
* UnpooledDirectByteBuf 使用的就是直接内存了，需要特殊的方法来回收内存
* PooledByteBuf 和它的子类使用了池化机制，需要更复杂的规则来回收内存

Netty 这里采用了引用计数法来控制回收内存，每个 ByteBuf 都实现了 ReferenceCounted 接口

* 每个 ByteBuf 对象的初始计数为 1
* 调用 release 方法计数减 1，如果计数为 0，ByteBuf 内存被回收
* 调用 retain 方法计数加 1，表示调用者没用完之前，其它 handler 即使调用了 release 也不会造成回收
* 当计数为 0 时，底层内存会被回收，这时即使 ByteBuf 对象还在，其各个方法均无法正常使用

基本规则

* 起点，对于 NIO 实现来讲，在 io.netty.channel.nio.AbstractNioByteChannel.NioByteUnsafe#read 方法中首次创建 ByteBuf 放入 pipeline（line 163 pipeline.fireChannelRead(byteBuf)）
* 入站 ByteBuf 处理原则
  * 对原始 ByteBuf 不做处理，调用 ctx.fireChannelRead(msg) 向后传递，这时无须 release
  * 将原始 ByteBuf 转换为其它类型的 Java 对象，这时 ByteBuf 就没用了，必须 release
  * 如果不调用 ctx.fireChannelRead(msg) 向后传递，那么也必须 release
  * 注意各种异常，如果 ByteBuf 没有成功传递到下一个 ChannelHandler，必须 release
  * 假设消息一直向后传，那么 TailContext 实现了ChannelInboundHandler会负责释放未处理消息（原始的 ByteBuf）
* 出站 ByteBuf 处理原则
  * 出站消息最终都会转为 ByteBuf 输出，一直向前传，由 HeadContext实现了ChannelOutboundHandler  flush 后 release
* 异常处理原则
  * 有时候不清楚 ByteBuf 被引用了多少次，但又必须彻底释放，可以循环调用 release 直到返回 true

**slice**

零拷贝的体现 读写指针独立

将原本分配的ByteBuf的内存逻辑切片分成多份新的ByteBuf

如果原本的Buf release了

但是分成的小slice retain 还是不会真正释放掉

**duplicate**

零拷贝的体现 读写指针独立

截取整个Buf

**CompositeByteBuf**

零拷贝的体现 读写指针独立

将两个Buf合并为一个逻辑上的Buf

```java
CompositeByteBuf buf3 = ByteBufAllocator.DEFAULT.compositeBuffer();
// true 表示增加新的 ByteBuf 自动递增 write index, 否则 write index 会始终为 0
buf3.addComponents(true, buf1, buf2);
```

**copy**

深拷贝

## 进阶

粘包拆包问题

解决方案

短连接



固定长度

```
ch.pipeline().addLast(new FixedLengthFrameDecoder(8));
```

固定分隔符

```
ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
```

预设长度

```java
/**
     * Creates a new instance.
     *
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset  长度字段所在位置 即偏移量
     *        the offset of the length field
     * @param lengthFieldLength  长度字段长度
     *        the length of the length field
     * @param lengthAdjustment   长度是报文长度 adjustment是头长度 是额外的
     *        the compensation value to add to the value of the length field
     * @param initialBytesToStrip
     *        the number of first bytes to strip out from the decoded frame
     */
// 最大长度，长度偏移，长度占用字节，长度调整，剥离字节数
ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 1, 0, 1));


```

