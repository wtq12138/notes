# 常见API介绍

## Runnable和Thread

thread聚合runnable,而Runnable只是一个重写run方法的接口，在thread的构造函数中传递Runnable进行赋值，

这样在thread.start时调用native方法start0调用c++中pthread_create进行操作系统层的线程创建，在jvm中为线程分配内存空间，并且在c++中回调threar的run方法，

而thread的run方法也很简单，有runnable就用它的run方法，没有就用自己的重写的run方法，这样线程和run方法实现分离

## FutureTask,Future和Callable

FutureTask同时实现Runnable和Future

FutureTask 能够接收 Callable 类型的参数，用来处理有返回结果的情况

```java
public static void main(String[] args) throws ExecutionException, InterruptedException {
    FutureTask futureTask = new FutureTask<>(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            log.debug("多线程任务");
            Thread.sleep(100);
            return 100;
        }
    });
    // 主线程阻塞，同步等待 task 执行完毕的结果
    new Thread(futureTask,"我的名字").start();
    log.debug("主线程");
    log.debug("{}",futureTask.get());
}
```

## CompleteFuture

runAsync

supplyAsync 

whenComplete 可以处理正常和异常的计算结果，exceptionally 处理异常情况。
whenComplete 和whenCompleteAsync 的区别：
whenComplete：是执行当前任务的线程执行继续执行whenComplete 的任务。
whenCompleteAsync：是执行把whenCompleteAsync 这个任务继续提交给线程池
来进行执行。

thenApply 方法：当一个线程依赖另一个线程时，获取上一个任务返回的结果，并返回当前
任务的返回值。
thenAccept 方法：消费处理结果。接收任务的处理结果，并消费处理，无返回结果。
thenRun 方法：只要上面的任务执行完成，就开始执行thenRun，只是处理完任务后，执行
thenRun 的后续操作

和complete 一样，可对结果做最后的处理（可处理异常），可改变返回值。



两个任务必须都完成，触发该任务。
thenCombine：组合两个future，获取两个future 的返回结果，并返回当前任务的返回值
thenAcceptBoth：组合两个future，获取两个future 任务的返回结果，然后处理任务，没有
返回值。
runAfterBoth：组合两个future，不需要获取future 的结果，只需两个future 处理完任务后，
处理该任务。

applyToEither：两个任务有一个执行完成，获取它的返回值，处理任务并有新的返回值。
acceptEither：两个任务有一个执行完成，获取它的返回值，处理任务，没有新的返回值。
runAfterEither：两个任务有一个执行完成，不需要获取future 的结果，处理任务，也没有返
回值。

allOf：等待所有任务完成
anyOf：只要有一个任务完成

## run()和start()

如果只调用run方法，则不会开启线程，只有start方法才能进行多线程，且同一个start方法只能调用一次

## sleep()和yield()

sleep()

Thread 静态方法,不会释放对象锁,不需要强制和 synchronized 配合使用

1. 调用 sleep 会让当前线程从 Running 进入 Timed Waiting 状态
2. 其它线程可以使用 interrupt 方法打断正在睡眠的线程，这时 sleep 方法会抛出 InterruptedException
4. 建议用 TimeUnit 的 sleep 代替 Thread 的 sleep 来获得更好的可读性
4. 睡醒后进入runnable状态

yield

Thread 静态方法


1. 调用 yield 会让当前线程从 Running 进入 Runnable 就绪状态，然后调度执行其它线程
2. 具体的实现依赖于操作系统的任务调度器

## wait()和notify()

Object 的方法,需要和 synchronized 一起用，在等待的时候会释放对象锁

![image-20220212104123024](..\juc\images\image-20220212104123024.png)

- Owner 线程发现条件不满足，调用 wait 方法，即可进入 WaitSet 变为 WAITING 状态
- BLOCKED 和 WAITING 的线程都处于阻塞状态，不占用 CPU 时间片
- BLOCKED 线程会在 Owner 线程释放锁时唤醒
- WAITING 线程会在 Owner 线程调用 notify 或 notifyAll 时唤醒，但唤醒后并不意味者立刻获得锁，仍需进入
  EntryList 重新竞争

### 保护性暂停

即 Guarded Suspension，用在一个线程等待另一个线程的执行结果，要点：

1. 有一个结果需要从一个线程传递到另一个线程，让他们关联同一个 GuardedObject
   1. 如果有结果不断从一个线程到另一个线程那么可以使用消息队列（见生产者/消费者）

2. JDK 中，join 的实现、Future 的实现，采用的就是此模式
3. 因为要等待另一方的结果，因此归类到同步模式

如果设置等待超时时间millis，则在循环等待结果的线程里记录经过的时间passtime，每次wait(millis-passtime)

```java
class GuardedObject {
    private Object response;
    private final Object lock = new Object();
    public Object get(long millis) {
        synchronized (lock) {
// 1) 记录最初时间
            long begin = System.currentTimeMillis();
// 2) 已经经历的时间
            long timePassed = 0;
            while (response == null) {
// 4) 假设 millis 是 1000，结果在 400 时唤醒了，那么还有 600 要等
                long waitTime = millis - timePassed;
                log.debug("waitTime: {}", waitTime);
                if (waitTime <= 0) {
                    log.debug("break...");
                    break;
                }
                try {
                    lock.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
// 3) 如果提前被唤醒，这时已经经历的时间假设为 400
                timePassed = System.currentTimeMillis() - begin;
                log.debug("timePassed: {}, object is null {}",
                        timePassed, response == null);
            }
            return response;
        }
    }
    public void complete(Object response) {
        synchronized (lock) {
// 条件满足，通知等待线程
            this.response = response;
            log.debug("notify...");
            lock.notifyAll();
        }
    }
}
```

### 生产消费者模型

while循环和wait充当P操作,++和notify充当V操作

```java
class MessageQueue {
    private LinkedList<Message> queue;
    public Message take() {
        synchronized (queue) {
            while (queue.isEmpty()) {
                log.debug("没货了, wait");
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Message message = queue.removeFirst();
            queue.notifyAll();
            return message;
        }
    }
    public void put(Message message) {
        synchronized (queue) {
            while (queue.size() == capacity) {
                log.debug("库存已达上限, wait");
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(message);
            queue.notifyAll();
        }
    }
}
```

## join()

t.join()等待t线程结束后才可以继续运行之后的代码，如果传参时间的话为最多等待时间

应用了超时保护性暂停模式，每次等待的时间是剩余时间

```java
public final synchronized void join(long millis)
    throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (millis == 0) {
        while (isAlive()) {
            wait(0);
        }
    } else {
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}
```

## interrupt()

执行t.interrupt()后只是将其标志位修改为1，并未真正打断，如果想要真正进行打断，需要在内部逻辑中编写。

特别的是，打断waiting timeed_waiting blocked的线程和已经interrupt的线程被阻塞会抛出异常，且会将标志位置零，从阻塞态转为catch异常的runnable态

`isInterrupted()` `Interrupted()`都会判断标志位是否为1，但是后者判断完后会将其置0

### 两阶段终止模式

通过interrupt实现监控

还可以通过自定义flag标记并加上volatile使其可见，在判断退出时直接用自定义的标记

```java
@Slf4j
public class Test11 {
    public static void main(String[] args) throws InterruptedException {
        TwoParseTermination twoParseTermination = new TwoParseTermination();
        twoParseTermination.start();
        Thread.sleep(3000);  // 让监控线程执行一会儿
        twoParseTermination.stop(); // 停止监控线程
    }
}


@Slf4j
class TwoParseTermination{
    Thread thread ;
    public void start(){
        thread = new Thread(()->{
            while(true){
                if (Thread.currentThread().isInterrupted()){
                    log.debug("线程结束。。正在料理后事中");
                    break;
                }
                try {
                    Thread.sleep(500);
                    log.debug("正在执行监控的功能");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
    public void stop(){
        thread.interrupt();
    }
}
```



## park()和unpark()

LockSupport的静态方法，如果线程`isInterrupted()`为假就暂停当前线程，否则无影响，

可以通过`Interrupted()`或`Interrupt()`来恢复线程运行

**原理**

每个线程都有自己的一个 Parker 对象，由三部分组成 _counter ， _cond 和 _mutex

counter是01标记位，mutex是互斥锁，它和condition的关系类似monitor和entrylist

调用park(),检查counter为0，则阻塞，为1则消耗掉设置为0，不阻塞

调用unpark()，设置counter为1，唤醒mutex中的condition的阻塞线程，唤醒成功后counter设置为0。

如果先unpark()，后park()则无影响，且无论unpark多少次，只会抵消一次。

## java六种线程状态

![image-20220214100744141](C:\Users\雷神\AppData\Roaming\Typora\typora-user-images\image-20220214100744141.png)

java的runnable状态包含操作系统层面的三种状态，就绪态：可执行但未被cpu时间片占领，运行态：正在执行，阻塞态：线程运行中遭遇BIO阻塞


1.  NEW --> RUNNABLE
   当调用 t.start() 方法时，由 NEW --> RUNNABLE

2. RUNNABLE <--> WAITING

   t 线程用 synchronized(obj) 获取了对象锁后
   调用 obj.wait() 方法时，t 线程从 RUNNABLE --> WAITING
   调用 obj.notify() ， obj.notifyAll() ， t.interrupt() 时
   竞争锁成功，t 线程从 WAITING --> RUNNABLE
   竞争锁失败，t 线程从 WAITING --> BLOCKED

3. RUNNABLE <--> WAITING
   当前线程调用 t.join() 方法时，当前线程从 RUNNABLE --> WAITING
   注意是当前线程在t 线程对象的监视器上等待
   t 线程运行结束，或调用了当前线程的 interrupt() 时，当前线程从 WAITING --> RUNNABLE

4. RUNNABLE <--> WAITING
   当前线程调用 LockSupport.park() 方法会让当前线程从 RUNNABLE --> WAITING
   调用 LockSupport.unpark(目标线程) 或调用了线程 的 interrupt() ，会让目标线程从 WAITING -->
   RUNNABLE

5. RUNNABLE <--> TIMED_WAITING

   和2类似不过调用的是wait(time)方法

6. RUNNABLE <--> TIMED_WAITING

   和3类似不过调用的是join(time)方法

7. RUNNABLE <--> TIMED_WAITING
   当前线程调用 Thread.sleep(long n) ，当前线程从 RUNNABLE --> TIMED_WAITING
   当前线程等待时间超过了 n 毫秒，当前线程从 TIMED_WAITING --> RUNNABLE

8. RUNNABLE <--> TIMED_WAITING

   和4类似

   当前线程调用 LockSupport.parkNanos(long nanos) 或 LockSupport.parkUntil(long millis) 时，当前线
   程从 RUNNABLE --> TIMED_WAITING
   调用 LockSupport.unpark(目标线程) 或调用了线程 的 interrupt() ，或是等待超时，会让目标线程从
   TIMED_WAITING--> RUNNABLE

9. RUNNABLE <--> BLOCKED
   t 线程用 synchronized(obj) 获取了对象锁时如果竞争失败，从 RUNNABLE --> BLOCKED
   持 obj 锁线程的同步代码块执行完毕，会唤醒该对象上所有 BLOCKED 的线程重新竞争，如果其中 t 线程竞争
   成功，从 BLOCKED --> RUNNABLE ，其它失败的线程仍然 BLOCKED

10. RUNNABLE <--> TERMINATED
      当前线程所有代码运行完毕，进入TERMINATED

# ThreadLocal

# 锁

## synchronized

```java
class Test{
    public synchronized void test() {
    }
}
等价于
class Test{
    public void test() {
        synchronized(this) {
        }
    }
}
```

当synchronized加在方法上时，如果有static则锁的是类对象，否则锁的是实例也就是this

## 变量的线程安全问题

1. 成员变量和静态变量线程安全？
 - 如果它们没有共享，则线程安全
 - 如果它们被共享了，根据它们的状态是否能够改变，又分两种情况
 - 如果只有读操作，则线程安全
 - 如果有读写操作，则这段代码是临界区，需要考虑线程安全

2. 局部变量线程安全？

- 局部变量是线程安全的

- 但局部变量引用的对象则未必

- 如果该对象没有逃离方法的作用访问，它是线程安全的

- 如果该对象逃离方法的作用范围，需要考虑线程安全

  情况1：有其它线程调用  方法1和方法2  
  情况2：为 ThreadSafe 类添加子类，子类重写方法时开启新的线程调用

## 各种锁

![img](https://pica.zhimg.com/80/v2-29784bd4ae4d9786d943710fe06499af_720w.jpg?source=1940ef5c)

### 重量级锁 monitor 

悲观锁 针对不同线程竞争

每个java对象的对象头中的mark word中有monitor地址标记，monitor是操作系统层面的东西

mark word不同状态图示

![image-20220212111122283](..\juc\images\image-20220212111122283.png)

如果使用 synchronized 给对象上锁（重量级）之后，该对象头的Mark Word 中就被设置指向 Monitor 对象的指针

![image-20220212104123024](..\juc\images\image-20220212104123024.png)

- 刚开始 Monitor 中 Owner 为 null
- 当 Thread-2 执行 synchronized(obj) 就会将 Monitor 的所有者 Owner 置为 Thread-2，Monitor中只能有一
  个 Owner
- 在 Thread-2 上锁的过程中，如果 Thread-3，Thread-4，Thread-5 也来执行 synchronized(obj)，就会进入
  EntryList BLOCKED
- Thread-2 执行完同步代码块的内容，然后唤醒 EntryList 中等待的线程来竞争锁，竞争的时是非公平的
- 图中 WaitSet 中的 Thread-0，Thread-1 是之前获得过锁，但条件不满足进入 WAITING 状态的线程，后面讲
  wait-notify 时会分析

#### 自旋优化

阻塞和唤醒需要CPU从用户态转为核心态，所以通过自旋减少无谓的阻塞

当多个线程因为monitor已经有owner时进入entrylist前，会进行自旋，即**不直接进入entrylist**而是判断owner是否已经释放，

好处是减少阻塞次数，减少上下文切换，

注意必须要**多核cpu**才可以发挥作用

### 轻量级锁 

乐观锁 针对的是不同线程使用对象，但是无竞争

解决加monitor消耗大的问题

cas之后 object和lock record互相指向对方

1. 每次指向到synchronized代码块时，都会创建锁记录（Lock Record）对象，**每个线程都**会包括一个锁记录的结构，锁记录内部可以储存对象的Mark Word和对象引用reference

   1. ![1583755737580](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309200902-382362.png)
2. 让锁记录中的Object reference指向对象，并且尝试用cas(compare and swap)替换Object对象的Mark Word ，将Mark Word 的值存入锁记录中

   1. ![1583755888236](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309201132-961387.png)
3. 如果cas替换成功，那么对象的对象头储存的就是锁记录的地址和状态01，如下所示

   1. ![1583755964276](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309201247-989088.png)
4. 如果cas失败，有两种情况
            1. 如果是其它线程已经持有了该Object的轻量级锁，那么表示有竞争，将进入锁膨胀阶段
         1. 如果是自己的线程已经执行了synchronized进行加锁，那么那么再添加一条 Lock Record 作为重入的计数

#### 锁重入

对象头中mark word的lock record地址指向的是自己当前线程的地址导致cas失败，即为锁重入

1. ![1583756190177](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309201634-451646.png)

   1. 当线程退出synchronized代码块的时候，**如果获取的是取值为 null 的锁记录 **，表示有重入，这时重置锁记录，表示重入计数减一

   1. ![1583756357835](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309201919-357425.png)

   2. 当线程退出synchronized代码块的时候，如果获取的锁记录取值不为 null，那么使用cas将Mark Word的值恢复给对象

   1. 成功则解锁成功
   2. 失败，则说明轻量级锁进行了锁膨胀或已经升级为重量级锁，进入重量级锁解锁流程



#### 锁膨胀

如果在尝试加轻量级锁的过程中，cas操作无法成功，这是有一种情况就是其它线程已经为这个对象加上了轻量级锁，这是就要进行锁膨胀，将轻量级锁变成重量级锁。

1. 当 Thread-1 进行轻量级加锁时，Thread-0 已经对该对象加了轻量级锁
   1. ![1583757433691](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309203715-909034.png)
2. 这时 Thread-1 加轻量级锁失败，进入锁膨胀流程
   1. 即为对象申请Monitor锁，让Object指向重量级锁地址，然后自己进入Monitor 的EntryList 变成BLOCKED状态
   2. ![1583757586447](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200309203947-654193.png)
3. 当Thread-0 退出synchronized同步块时，使用cas将Mark Word的值恢复给对象头，失败，那么会进入重量级锁的解锁过程，即按照Monitor的地址找到Monitor对象，将Owner设置为null，唤醒EntryList 中的Thread-1线程

### 偏向锁

针对的是 只有一个线程使用对象，不存在竞争与其他线程使用

解决重入cas的消耗问题

轻量级锁重入多次cas消耗较大，java6开始引入了偏向锁，只有第一次使用CAS时将对象的Mark Word头设置为入锁线程ID，**之后这个入锁线程再进行重入锁时，发现线程ID是自己的，那么就不用再进行CAS了**

偏向锁默认是延迟的，不会在程序启动的时候立刻生效

**何时撤销偏向锁**	

1. 调用对象的hashcode方法 变为轻量级锁
2. 两个线程非竞争使用同一个对象  变为轻量级锁
3. wait notify 直接升级为重量级

**批量撤销和批量重偏向均是针对类的优化**

#### 批量重偏向

撤销次数到达第一个阈值20时触发，导致之后的该类的撤销变为不升级为轻量锁，而是改变对象头mark word的线程id，即改变偏向

#### 批量撤销

撤销次数到达第二个阈值40时触发，导致之后该类的对象都改为不可偏向，新建的对象也为不可偏向

### 编译器的锁优化

**锁粗化**和**锁消除**

锁化指将多个加锁中的操作合并为同一个锁内执行，减少上下文切换的时间损耗

锁消除指不会出现线程安全问题的加锁操作优化为不加锁执行，

## 活跃性问题

**死锁**

两把锁a,b，t1线程占领a,需要b，t2线程占领b需要a

**活锁**

两个线程不停改变对方线程结束的条件，导致都在运行但是无法结束

**饥饿**

经常得不到或始终得不到cpu调度

## 如何多线程循环控制输出

1. wait()和notify()

自定义不同标记位输出不同字符，

P操作是while() wait，V操作是修改标记位，notifyall

```java
class Solution {
    private int cnt;
    private int flag;
    Solution(int cnt,int flag) {
        this.cnt=cnt;
        this.flag=flag;
    }
    public void print(int cnt ,int cur,int next) {
        for(int i=0;i<cnt;i++) {
            synchronized (this) {
                while(flag!=cur) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(cur);
                flag=next;
                this.notifyAll();
            }
        }
    }
}
```

2. await()和signal()

因为有条件控制，则不需要自定义flag了而是利用condition精确控制线程阻塞和唤醒,缺点是一开始是在阻塞状态，需要先手动唤醒

p操作是cur.awit()，V操作是next.signal

```java
public class Test2 {
    public static void main(String[] args) {
        Solution solution=new Solution(5);
        var lock=solution.reentrantLock;
        Condition a=lock.newCondition();
        Condition b=lock.newCondition();
        Condition c=lock.newCondition();
        new Thread(() -> solution.print('a',a,b)).start();
        new Thread(() -> solution.print('b',b,c)).start();
        new Thread(() -> solution.print('c',c,a)).start();
        lock.lock();
        try {
            Thread.sleep(1000);
            a.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
class Solution {
    ReentrantLock reentrantLock=new ReentrantLock();
    private int cnt;

    public Solution(int cnt) {
        this.cnt = cnt;
    }
    public void print(char c, Condition cur, Condition next) {
        for(int i=0;i<cnt;i++) {
            reentrantLock.lock();
            try {
                cur.await();
                System.out.println(c);
                next.signal();
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
    }
}
```

3. park()和unpark()

和2同理，需要告知下一个唤醒线程是哪个，且需要一开始手动唤醒

```java
class ParkUnpark {
    public void print(String str, Thread next) {
        for (int i = 0; i < loopNumber; i++) {
            LockSupport.park();
            System.out.print(str);
            LockSupport.unpark(next);
        }
    }

    private int loopNumber;

    public ParkUnpark(int loopNumber) {
        this.loopNumber = loopNumber;
    }
}
```

## 内存模型之内存三特性

JMM即 Java Memory Model,JMM  多线程并发情况下对于共享变量读写**屏蔽不同的操作系统、不同的硬件的差异，从而解决多线程可见性、原子性等问题**。

JMM 抽象了**主内存和本地内存**

happens before 原则

- 单线程 happen-before 原则：在同一个线程中，书写在前面的操作 happen-before 后面的操作。 有依赖关系的！

```java
int a = 3;      //1
int b = a + 1; //2
```

- 锁的 happen-before 原则：同一个锁的 unlock 操作 happen-before 此锁的 lock 操作。
- volatile 的 happen-before 原则：对一个 volatile 变量的写操作 happen-before 对此变量的任意操作(当然也包括写操作了)。
- happen-before 的传递性原则：如果 A 操作 happen-before B 操作，B 操作 happen-before C 操作，那么 A 操作 happen-before C 操作。
- 线程启动的 happen-before 原则：同一个线程的 start 方法 happen-before 此线程的其它方法。
- 线程中断的 happen-before 原则：对线程 interrupt 方法的调用 happen-before 被中断线程的检测到中断发送的代码。
- 线程终结的 happen-before 原则：线程中的所有操作都 happen-before 线程的终止检测。
- 对象创建的 happen-before 原则：一个对象的初始化完成先于他的 finalize 方法调用。

体现在以下几个方面

1. 原子性 - 保证指令不会受到线程上下文切换的影响
2. 可见性 - 保证指令不会受 cpu 缓存的影响
3. 有序性 - 保证指令不会受 cpu 指令并行优化的影响

**可见性问题**

由于JIT优化，当线程t1的常用变量x会从主存缓存到高速缓存中，而在线程t2修改了x不会影响高速缓存中的值，这样导致，修改了却不可见。

**解决方法**

volatile，它可以用来修饰成员变量和静态成员变量，他可以避免线程从自己的工作缓存中查找变量的值，必须到主存中获取它的值，线程操作 volatile 变量都是直接操作主存 

synchronized,在Java内存模型中，synchronized规定，线程在加锁时， 先清空工作内存→在主内存中拷贝最新变量的副本到工作内存 →执行完代码→将更改后的共享变量的值刷新到主内存中→释放互斥锁。

**原子性问题**

指令交错++和--

**解决方法**

synchronized 语句块既可以保证代码块的原子性，也同时保证代码块内变量的可见性。但缺点是
synchronized 是属于重量级操作，性能相对更低。

**有序性**

不相关指令乱序执行

**解决方法**

volatile 修饰的变量，可以禁用指令重排

synchronized并不能解决有序性问题，但是如果是该变量整个都在synchronized代码块的保护范围内，那么变量就不会被多个线程同时操作，也不用考虑有序性问题！在这种情况下相当于解决了重排序问题

## 共享模型之不可变

为了使并发时不出错，定义类时就使其只读，这样就不存在线程安全问题

**使用final**

属性用 final 修饰保证了该属性是只读的，不能修改

类用 final 修饰保证了该类中的方法不能被覆盖，防止子类无意间破坏不可变性

**保护性拷贝**

例如String.substr()方法

构造新字符串对象时，会生成新的 char[] value，对内容进行复制。

## volatile

volatile 的底层实现原理是内存屏障，Memory Barrier（Memory Fence）

1. 对 volatile 变量的写指令后会加入写屏障（sfence）
2. 对 volatile 变量的读指令前会加入读屏障（lfence）

**如何保证可见性**

写屏障（sfence）保证在该屏障之前的，对共享变量的改动，都同步到主存当中（刷新）

读屏障（lfence）保证在该屏障之后，对共享变量的读取，加载的是主存中最新数据(读新)

**如何保证有序性**

写屏障会确保指令重排序时，不会将写屏障之前的代码排在写屏障之后

读屏障会确保指令重排序时，不会将读屏障之后的代码排在读屏障之前

**缺陷**

只能解决本线程指令交错问题，无法解决多线程指令交错：

1. 写屏障仅仅是保证之后的读能够读到最新的结果，但不能保证其它线程的读跑到它前面去
2. 而有序性的保证也只是保证了本线程内相关代码不被重排序

## 无锁

CAS(compareAndSet)

实现是通过JNI使用c++编写，底层是lock cmpxchg 指令，该指令是原子的，所以可以实现无锁

```java
    @Override
    public void withdraw(Integer amount) {
        // 核心代码
        // 需要不断尝试，直到成功为止
        while (true){
            // 比如拿到了旧值 1000
            int pre = getBalance();
            // 在这个基础上 1000-10 = 990
            int next = pre - amount;
            /*
             compareAndSet 正是做这个检查，在 set 前，先比较 prev 与当前值
             - 不一致了，next 作废，返回 false 表示失败
             比如，别的线程已经做了减法，当前值已经被减成了 990
             那么本线程的这次 990 就作废了，进入 while 下次循环重试
             - 一致，以 next 设置为新值，返回 true 表示成功
			 */
            if (atomicInteger.compareAndSet(pre,next)){
                break;
            }
        }
    }
```

## 原子类型

**AtomicInteger**

常用api

```java
    public static void main(String[] args) {
        AtomicInteger i = new AtomicInteger(0);
        // 获取并自增（i = 0, 结果 i = 1, 返回 0），类似于 i++
        System.out.println(i.getAndIncrement());
        // 自增并获取（i = 1, 结果 i = 2, 返回 2），类似于 ++i
        System.out.println(i.incrementAndGet());
        // 自减并获取（i = 2, 结果 i = 1, 返回 1），类似于 --i
        System.out.println(i.decrementAndGet());
        // 获取并自减（i = 1, 结果 i = 0, 返回 1），类似于 i--
        System.out.println(i.getAndDecrement());
        // 获取并加值（i = 0, 结果 i = 5, 返回 0）
        System.out.println(i.getAndAdd(5));
        // 加值并获取（i = 5, 结果 i = 0, 返回 0）
        System.out.println(i.addAndGet(-5));
        // 获取并更新（i = 0, p 为 i 的当前值, 结果 i = -2, 返回 0）
        // 函数式编程接口，其中函数中的操作能保证原子，但函数需要无副作用
        System.out.println(i.getAndUpdate(p -> p - 2));
        // 更新并获取（i = -2, p 为 i 的当前值, 结果 i = 0, 返回 0）
        // 函数式编程接口，其中函数中的操作能保证原子，但函数需要无副作用
        System.out.println(i.updateAndGet(p -> p + 2));
        // 获取并计算（i = 0, p 为 i 的当前值, x 为参数1, 结果 i = 10, 返回 0）
        // 函数式编程接口，其中函数中的操作能保证原子，但函数需要无副作用
        // getAndUpdate 如果在 lambda 中引用了外部的局部变量，要保证该局部变量是 final 的
        // getAndAccumulate 可以通过 参数1 来引用外部的局部变量，但因为其不在 lambda 中因此不必是 final
        System.out.println(i.getAndAccumulate(10, (p, x) -> p + x));
        // 计算并获取（i = 10, p 为 i 的当前值, x 为参数1值, 结果 i = 0, 返回 0）
        // 函数式编程接口，其中函数中的操作能保证原子，但函数需要无副作用
        System.out.println(i.accumulateAndGet(-10, (p, x) -> p + x));
    }
```

**AtomicReference**

```java
AtomicReference<BigDecimal> ref;// 泛型自己封装数据类型
```

**AtomicIntegerArray**

```java
AtomicIntegerArray atomicIntegerArray=new AtomicIntegerArray(10);
atomicIntegerArray.getAndIncrement(index);//第i位++
```

**AtomicIntegerFieldUpdater**

字段更新器只能配合 volatile 修饰的字段使用

```java
AtomicReferenceFieldUpdater updater =
    AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");;//所属类，成员变量类，成员变量名
updater.compareAndSet(stu, null, "张三")
```



## CAS缺点

### ABA问题

cas是否成功依据get()获取的值与当前内存中是否一致，而这个值是否被其他线程修改，它无法感知，也就会出现，一开始获取的是A但是在其他线程里变成了B又变成了A，然后依然cas成功

解决方案是，多传入一个参数记录值改变的版本号,只有当值和版本号均一致时，才会cas成功修改值和版本号

**AtomicStampedReference**

```java
static AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);
public static void main(String[] args) throws InterruptedException {
    log.debug("main start...");
    // 获取值 A
    String prev = ref.getReference();
    // 获取版本号
    int stamp = ref.getStamp();
    log.debug("版本 {}", stamp);
    // 如果中间有其它线程干扰，发生了 ABA 现象
    other();
    sleep(1);
    // 尝试改为 C
    log.debug("change A->C {}", ref.compareAndSet(prev, "C", stamp, stamp + 1));
}
```

**AtomicMarkableReference**

当无需知道修改几次时，只需要知道改还是没改，即bool值判断

### **自旋时间长**

设置自旋次数

### 只能对一个对象进行操作

- 使用互斥锁来保证原子性；
- 将多个变量封装成对象，通过AtomicReference来保证原子性。

## LongAdder

和普通AtomicLong实现累加相比性能提升

原因是就是在有竞争时，设置多个累加单元(但不会超过cpu的核心数)，Therad-0 累加 Cell[0]，而 Thread-1 累加Cell[1]... 最后将结果汇总。这样它们在累加时操作的不同的 Cell 变量，因此减少了 CAS 重试失败，从而提高性能。

源码

```java
// 累加单元数组, 懒惰初始化
transient volatile Cell[] cells;
// 基础值, 如果没有竞争, 则用 cas 累加这个域
transient volatile long base;
// 在 cells 创建或扩容时, 置为 1, 表示加锁
transient volatile int cellsBusy;
```

Cell类

```java
// 防止缓存行伪共享
@sun.misc.Contended
static final class Cell {
    volatile long value;
    Cell(long x) { value = x; }
    // 最重要的方法, 用来 cas 方式进行累加, prev 表示旧值, next 表示新值
    final boolean cas(long prev, long next) {
        return UNSAFE.compareAndSwapLong(this, valueOffset, prev, next);
    }
    // 省略不重要代码
}
```

@sun.misc.Contended注解作用

累加时，因为要提高速度，不能一直从内存中读取，则从多级cache中读取，而缓存以缓存行为单位，每个缓存行对应着一块内存，一般是 64 byte。

缓存的加入会造成数据副本的产生，即同一份数据会缓存在不同核心的缓存行中。

CPU 要保证数据的一致性，如果某个 CPU 核心更改了数据，其它 CPU 核心对应的**整个缓存行**必须失效。

因为 Cell 是数组形式，在内存中是连续存储的，一个 Cell 为 24 字节（16 字节的对象头和 8 字节的 value），因此一个缓存行中有两个

Cell对象，那么只要修改一个，导致对方的缓存行失效。

@sun.misc.Contended 用来解决这个问题，它的原理是在使用此注解的对象或字段的前后各增加 128 字节大小的padding，从而让 CPU 将对象预读至缓存时占用不同的缓存行，这样，不会造成对方缓存行的失效

## UnSafe

Unsafe 对象提供了非常底层的，操作内存、线程的方法，Unsafe 对象不能直接调用，只能通过反射获得。LockSupport的park方法，cas相关的方法底层都是通过Unsafe类来实现的。

```java
// 获取unsafe
Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
theUnsafe.setAccessible(true);
Unsafe unsafe = (Unsafe) theUnsafe.get(null);

//获取teacher类中id的偏移地址,然后cas
long idOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("id"));
unsafe.compareAndSwapInt(t, idOffset, 0, 1);
```



## 手写线程池

**生产者消费者模型实现阻塞队列**

```java
@Slf4j(topic = "c.BlockingQueue")
class BlockingQueue<T> {
    // 1. 任务队列
    private Deque<T> queue = new ArrayDeque<>();

    // 2. 锁
    private ReentrantLock lock = new ReentrantLock();

    // 3. 生产者条件变量
    private Condition fullWaitSet = lock.newCondition();

    // 4. 消费者条件变量
    private Condition emptyWaitSet = lock.newCondition();

    // 5. 容量
    private int capcity;

    public BlockingQueue(int capcity) {
        this.capcity = capcity;
    }

    // 带超时阻塞获取
    public  T poll(long timeout, TimeUnit unit) {
        lock.lock();
        try {
            // 将 timeout 统一转换为 纳秒
            long nanos = unit.toNanos(timeout);
            while (queue.isEmpty()) {
                try {
                    // 返回值是剩余时间
                    if (nanos <= 0) {
                        return null;
                    }
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 带超时时间阻塞添加
    public boolean offer(T task, long timeout, TimeUnit timeUnit) {
        lock.lock();
        try {
            long nanos = timeUnit.toNanos(timeout);
            while (queue.size() == capcity) {
                try {
                    if(nanos <= 0) {
                        return false;
                    }
                    log.debug("等待加入任务队列 {} ...", task);
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列 {}", task);
            queue.addLast(task);
            emptyWaitSet.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            // 判断队列是否满
            if(queue.size() == capcity) {
                rejectPolicy.reject(this, task);
            } else {  // 有空闲
                log.debug("加入任务队列 {}", task);
                queue.addLast(task);
                emptyWaitSet.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}
```

**线程池聚合阻塞队列**

`ThreadPool.execute(Runnable)`直接将线程池与Runnable联系起来，

如果正在运行的线程小于cpu核心数，则直接运行，否则尝试加入线程池，如果线程池满，则通过拒绝策略拒绝，否则加入线程池

**内部定义线程类重写run方法**

execute方法中调用了线程start方法，正常是直接调用Runnable开启线程

而这里需要等待线程池分配线程才能运行，所以重写run逻辑

`while(task != null || (task = taskQueue.poll(timeout, timeUnit)) != null)`

```java
class ThreadPool {
    // 任务队列
    private BlockingQueue<Runnable> taskQueue;

    // 线程集合
    private HashSet<Worker> workers = new HashSet<>();

    // 核心线程数
    private int coreSize;

    // 获取任务时的超时时间
    private long timeout;

    private TimeUnit timeUnit;

    private RejectPolicy<Runnable> rejectPolicy;

    // 执行任务
    public void execute(Runnable task) {
        // 当任务数没有超过 coreSize 时，直接交给 worker 对象执行
        // 如果任务数超过 coreSize 时，加入任务队列暂存
        synchronized (workers) {
            if(workers.size() < coreSize) {
                Worker worker = new Worker(task);
                log.debug("新增 worker{}, {}", worker, task);
                workers.add(worker);
                worker.start();
            } else {
                taskQueue.tryPut(rejectPolicy, task);
            }
        }
    }

    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapcity, RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.taskQueue = new BlockingQueue<>(queueCapcity);
        this.rejectPolicy = rejectPolicy;
    }

    class Worker extends Thread{
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            // 执行任务
            // 1) 当 task 不为空，执行任务
            // 2) 当 task 执行完毕，再接着从任务队列获取任务并执行
//            while(task != null || (task = taskQueue.take()) != null) {
            while(task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
                try {
                    log.debug("正在执行...{}", task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            synchronized (workers) {
                log.debug("worker 被移除{}", this);
                workers.remove(this);
            }
        }
    }
}
```

**拒绝策略**

即在线程池中再聚合一个 RejectPolicy，构造方法中就决定当线程池满时进行的操作，

具体这些操作也封装在Pool中，这样就可以不直接在代码中写死，而是在构造实例时自己声明想要的拒绝策略，可扩展性强

```java
ThreadPool threadPool = new ThreadPool(2,
                5000, TimeUnit.MILLISECONDS, 1, (queue, task)->{
            // 1. 死等
//            queue.put(task);
            // 2) 带超时等待
            queue.offer(task, 1500, TimeUnit.MILLISECONDS);
            // 3) 让调用者放弃任务执行
//            log.debug("放弃{}", task);
            // 4) 让调用者抛出异常
//            throw new RuntimeException("任务执行失败 " + task);
            // 5) 让调用者自己执行任务
//            task.run();
        });
@FunctionalInterface // 拒绝策略
interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue, T task);
}
```

## ThreadPoolExecutor

###线程池状态

ThreadPoolExecutor 使用 int 的高 3 位来表示线程池状态，低 29 位表示线程数量

这些信息存储在一个原子变量 ctl 中，目的是将线程池状态与线程个数合二为一，这样就可以用一次 cas 原子操作
进行赋值

![1594949019952](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200717092345-119571.png)

### 源码

#### 重要属性

```java
public class ThreadPoolExecutor extends AbstractExecutorService {

    // 控制变量-存放状态和线程数
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    // 任务队列，必须是阻塞队列
    private final BlockingQueue<Runnable> workQueue;

    // 工作线程集合，存放线程池中所有的（活跃的）工作线程，只有在持有全局锁mainLock的前提下才能访问此集合
    private final HashSet<Worker> workers = new HashSet<>();
    
    // 全局锁
    private final ReentrantLock mainLock = new ReentrantLock();

    // awaitTermination方法使用的等待条件变量
    private final Condition termination = mainLock.newCondition();

    // 记录峰值线程数
    private int largestPoolSize;
    
    // 记录已经成功执行完毕的任务数
    private long completedTaskCount;
    
    // 线程工厂，用于创建新的线程实例
    private volatile ThreadFactory threadFactory;

    // 拒绝执行处理器，对应不同的拒绝策略
    private volatile RejectedExecutionHandler handler;
    
    // 空闲线程等待任务的时间周期，单位是纳秒
    private volatile long keepAliveTime;
    
    // 是否允许核心线程超时，如果为true则keepAliveTime对核心线程也生效
    private volatile boolean allowCoreThreadTimeOut;
    
    // 核心线程数
    private volatile int corePoolSize;

    // 线程池容量
    private volatile int maximumPoolSize;

    // 省略其他代码
}    
```



####  构造方法

```java
public ThreadPoolExecutor(int corePoolSize,
 int maximumPoolSize,
 long keepAliveTime,
 TimeUnit unit,
 BlockingQueue<Runnable> workQueue,
 ThreadFactory threadFactory,
RejectedExecutionHandler handler){
}
```

1. corePoolSize 核心线程数目 (最多保留的线程数)
2. maximumPoolSize 最大线程数目(核心线程数加上救急线程数)
3. keepAliveTime 救急线程的生存时间(核心线程没有生存时间这个东西，核心线程会一直运行) 
4. unit 时间单位 - 针对救急线程
5. workQueue 阻塞队列
6. threadFactory 线程工厂 - 可以为线程创建时起个好名字
7. handler 拒绝策略

#### **执行流程**

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    int c = ctl.get();
    // 如果当前woker的数量小于 核心线程数 则该判断为true
    if (workerCountOf(c) < corePoolSize) {
        /*
        * 使用核心线程数的限制去开worker来执行这个任务 
        * 注意没有核心线程和非核心线程这一说 worker是没有区别的
        * addWorker会失败 这里只需要了解 可能是因为线程池状态或者worker的数量引起addWorker失败
        * 因为可能不只是一个线程在操作线程池可能其他线程也在操作
        */
        if (addWorker(command, true))
            return;
        // 如果addWorker失败则ctl要重新获取因为不管是状态变还是worker数量变ctl都已经变了你需要重新获取最新值
        c = ctl.get();
    }
    /*
    * 在当前worker数量大于等于corePoolSize或者上面的addWorker失败之后才会走到这里
    * 经过上面分析可能有两种可能 1、线程的状态发生改变  2、当前worker数量不小于核心线程数
    * 第一个判断是查看一下当前线程的状态是否是running状态
    * 在满足第一条件下会尝试往工作队列里面添加这个任务 但是有可能失败 工作队列可能满了
    */ 
    if (isRunning(c) && workQueue.offer(command)) {
        // 走到这里说明offer之前是running状态 放入工作队列成功了 需要重新获取当前状态 因为有可能放进去之后线程状态变了
        int recheck = ctl.get();
        /*
        * 如果offer之后线程池不是running了 需要尝试remove刚才的任务
        * 不是running的状态下 remove也有可能失败，他可能被执行了
        */
        if (! isRunning(recheck) && remove(command))
            // 如果remove成功了需要拒绝这个任务
            reject(command);
        /*
        * 因为走到这里一定是 offer成功了 
        * 这个判断是为了防止没有worker 但是队列里面有任务 没人执行
        * 这个是有可能的 工作一段时间后worker的数量为0 和 allowCoreThreadTimeOut()这个方法有关系
        * 
        * 上一个if判断的!isRuning 是true remove失败的时候 有可能 workerCountOf(recheck) == 0 为true
        * 这个时候线程池肯定是不让你再添加线程的
        */
        else if (workerCountOf(recheck) == 0)
            /* 
            * 如果出现 线程池是running worker是0 队列有任务 需要添加一个worker执行这些任务
            * 如果出现 线程池不是running 但是remove失败 worker是0 线程池是不允许添加worker的这个逻辑在addworker方法里面
            *
            */
            addWorker(null, false);
    }
    /*
    * 如果用核心线程数限制开worker执行任务失败
    * 或者 线程池状态不是running
    * 或者 工作队列已经满了
    * 使用最大线程数限制开worker执行任务
    */
    else if (!addWorker(command, false))
        // 失败的原因有 1、worker达到非核心线程数 2、线程池的状态变了不是running了 则拒绝这个任务
        reject(command);
}
```

1. 如果当前工作线程总数小于`corePoolSize`，addworker()
2. 如果当前工作线程总数大于等于`corePoolSize`，检查状态，workQueue.offer，因为执行判断语句后可能存在线程池状态改变，需要二次判断，如果不是running了workQueue.remove,拒绝策略，检查worker数量是否为0，防止出现workqueue中有任务，worker为0无法执行
3. 走到这里说明队列以及worker达到最大值或者线程池不是runing reject

#### addworker

```java
// 添加工作线程，如果返回false说明没有新创建工作线程，如果返回true说明创建和启动工作线程成功
private boolean addWorker(Runnable firstTask, boolean core) {
    retry:  
    // 注意这是一个死循环 - 最外层循环
    //CAS进行cnt++成功break
    for (int c = ctl.get();;) {
        
    }
    // 标记工作线程是否启动成功
    boolean workerStarted = false;
    // 标记工作线程是否创建成功
    boolean workerAdded = false;
    Worker w = null;
    try {
        // 传入任务实例firstTask创建Worker实例，Worker构造里面会通过线程工厂创建新的Thread对象，所以下面可以直接操作Thread t = w.thread
        // 这一步Worker实例已经创建，但是没有加入工作线程集合或者启动它持有的线程Thread实例
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            // 这里需要全局加锁，因为会改变一些指标值和非线程安全的集合
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                int c = ctl.get();
                // 这里主要在加锁的前提下判断ThreadFactory创建的线程是否存活或者判断获取锁成功之后线程池状态是否已经更变为SHUTDOWN
                // 1. 如果线程池状态依然为RUNNING，则只需要判断线程实例是否存活，需要添加到工作线程集合和启动新的Worker
                // 2. 如果线程池状态小于STOP，也就是RUNNING或者SHUTDOWN状态下，同时传入的任务实例firstTask为null，则需要添加到工作线程集合和启动新的Worker
                // 对于2，换言之，如果线程池处于SHUTDOWN状态下，同时传入的任务实例firstTask不为null，则不会添加到工作线程集合和启动新的Worker
                // 这一步其实有可能创建了新的Worker实例但是并不启动（临时对象，没有任何强引用），这种Worker有可能成功下一轮GC被收集的垃圾对象
                if (isRunning(c) ||
                    (runStateLessThan(c, STOP) && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    // 把创建的工作线程实例添加到工作线程集合
                    workers.add(w);
                    int s = workers.size();
                    // 尝试更新历史峰值工作线程数，也就是线程池峰值容量
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    // 这里更新工作线程是否启动成功标识为true，后面才会调用Thread#start()方法启动真实的线程实例
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            // 如果成功添加工作线程，则调用Worker内部的线程实例t的Thread#start()方法启动真实的线程实例
            if (workerAdded) {
                t.start();
                // 标记线程启动成功
                workerStarted = true;
            }
        }
    } finally {
        // 线程启动失败，需要从工作线程集合移除对应的Worker
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}

// 添加Worker失败
private void addWorkerFailed(Worker w) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        // 从工作线程集合移除之
        if (w != null)
            workers.remove(w);
        // wc数量减1    
        decrementWorkerCount();
        // 基于状态判断尝试终结线程池
        tryTerminate();
    } finally {
        mainLock.unlock();
    }
}
```

cas修改workercount

加一把全局大锁

判断线程池状态，workers.add以及worker内置线程t.start

### 常见线程池

**newFixedThreadPool**

Executors类提供的工厂方法来创建线程池

特点

1. 核心线程数 == 最大线程数（没有救急线程被创建），因此也无需超时时间
2. 阻塞队列是无界的，可以放任意数量的任务
3. 适用于任务量已知，相对耗时的任务

**newCachedThreadPool**

特点

1. 核心线程数是 0， 最大线程数是 Integer.MAX_VALUE，救急线程的空闲生存时间是 60s，意味着
   1. 全部都是救急线程（60s 后可以回收）
   2. 救急线程可以无限创建
2. 队列采用了 SynchronousQueue 实现特点是，它没有容量，没有线程来取是放不进去的（一手交钱、一手交
   货）SynchronousQueue测试代码  Test20.java
3. 整个线程池表现为线程数会根据任务量不断增长，没有上限，当任务执行完毕，空闲 1分钟后释放线
   程。 适合任务数比较密集，但每个任务执行时间较短的情况

**newSingleThreadExecutor**

```java
public static ExecutorService newSingleThreadExecutor() {
 return new FinalizableDelegatedExecutorService
 (new ThreadPoolExecutor(1, 1,0L, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>()));
}
```

使用场景：

1. 希望多个任务排队执行。线程数固定为 1，任务数多于 1 时，会放入无界队列排队。任务执行完毕，这唯一的线程也不会被释放。
2. 区别：
   1. 和自己创建单线程执行任务的区别：自己创建一个单线程串行执行任务，如果任务执行失败而终止那么没有任何补救措施，而线程池还会新建一个线程，保证池的正常工作
   2. Executors.newSingleThreadExecutor() 线程个数始终为1，不能修改
      1. FinalizableDelegatedExecutorService 应用的是装饰器模式，只对外暴露了 ExecutorService 接口，因
         此不能调用 ThreadPoolExecutor 中特有的方法
   3. 和Executors.newFixedThreadPool(1) 初始时为1时的区别：Executors.newFixedThreadPool(1) 初始时为1，以后还可以修改，对外暴露的是 ThreadPoolExecutor 对象，可以强转后调用 setCorePoolSize 等方法进行修改

### 提交任务

```java
// 执行任务
void execute(Runnable command);
// 提交任务 task，用返回值 Future 获得任务执行结果，Future的原理就是利用我们之前讲到的保护性暂停模式来接受返回结果的，主线程可以执行 FutureTask.get()方法来等待任务执行完成
<T> Future<T> submit(Callable<T> task);
// 提交 tasks 中所有任务
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
 throws InterruptedException;
// 提交 tasks 中所有任务，带超时时间
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
 long timeout, TimeUnit unit)
 throws InterruptedException;
// 提交 tasks 中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其它任务取消
<T> T invokeAny(Collection<? extends Callable<T>> tasks)
 throws InterruptedException, ExecutionException;
// 提交 tasks 中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其它任务取消，带超时时间
<T> T invokeAny(Collection<? extends Callable<T>> tasks,
 long timeout, TimeUnit unit)
 throws InterruptedException, ExecutionException, TimeoutException;
```

### 关闭线程池

```java
void shutdown();// 状态变为000即不接收新线程，把阻塞队列中的任务执行完
void shutdownNow();// 状态变为001 不接收新任务，把阻塞队列任务抛弃，interrupt执行任务
```

### 任务调度线程池

在『任务调度线程池』功能加入之前，可以使用 java.util.Timer 来实现定时功能，Timer 的优点在于简单易用，但
由于所有任务都是由同一个线程来调度，因此所有任务都是串行执行的，同一时间只能有一个任务在执行，前一个
任务的延迟或异常都将会影响到之后的任务。

**延时调用**

```java
ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        pool.schedule(() -> {
            try {
                log.debug("task1");
                int i = 1 / 0;
            } catch (Exception e) {
                log.error("error:", e);
            }
        }, 1, TimeUnit.SECONDS);
```

**固定间隔调用**

```java
//如果线程耗时大于间隔时间，则直接按照线程耗时走
pool.scheduleAtFixedRate(() -> {
    log.debug("running...");
    Thread.sleep(3000);
}, 1, 1, TimeUnit.SECONDS);

//线程耗时+间隔时间
pool.scheduleWithFixedDelay(() -> {
    try {
        log.debug("task1");
        Thread.sleep(1000);
    } catch (Exception e) {
        log.error("error:", e);
    }
}, 1,1, TimeUnit.SECONDS);
```

### 异常处理

**主动处理**

**使用Future**

错误信息都被封装进submit方法的返回方法中

```java
ExecutorService pool = Executors.newFixedThreadPool(1);
Future<Boolean> f = pool.submit(() -> {
 log.debug("task1");
 int i = 1 / 0;
 return true;
});
log.debug("result:{}", f.get());
```

## AQS

https://www.cnblogs.com/yewy/p/13773799.html

概述：全称是 AbstractQueuedSynchronizer，是阻塞式锁和相关的同步器工具的框架

重要方法

acquire和release，决定了加锁和释放锁，在Lock类中调用

特点：

1. 用 state 属性来表示资源的状态（分独占模式和共享模式），子类需要定义如何维护这个状态，控制如何获取锁和释放锁
   1. getState - 获取 state 状态
   2. setState - 设置 state 状态
   3.  **cas 机制设置 state 状态保证修改状态是原子的**
   4. 独占模式是只有一个线程能够访问资源，而共享模式可以允许多个线程访问资源
2. 提供了基于 FIFO 的等待队列，类似于 Monitor 的 EntryList
3. 条件变量来实现等待、唤醒机制，支持多个条件变量，类似于 Monitor 的 WaitSet

## ReentrantLock

用法和synchronized关键字类似，不过Object对象变为了ReentrantLock实例，同时加锁后一定要解锁

```java
private  ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    log.debug("获得到锁");
} finally {
    lock.unlock();
}
```

**特性**

1. 可重入:t1线程获得锁后，调用t2线程再次加锁时不会被挡住
2. 可打断：调用`lock.lockInterruptibly()`之后，如果被打断则会catch到不会一直等待
3. 锁超时：调用`lock.tryLock(time, TimeUnit.SECONDS)`,在一定时间的等待锁，得到了返回true，得不到返回false
4. 锁公平：构造方法中传入true可创建公平锁，默认非公平，解决饥饿问题
5. 条件变量 对于synchronized 其monitor中只有一个waitset，即notify()和notifyall()只能粗化控制，而condition可以创建多个，通过`condition.await()`和`condition.signal()`达到精确控制，和synchronized类似，必须先得到锁才能wait()

### 非公平锁流程

双向链表维护线程结点，每个结点前一个结点设置为-1负责叫醒当前结点，

lock方法主要会进入两个方法，一个是tryAcquire，另一个是acquireQueued，前者主要用CAS去获取锁，后者是加入等待队列

重要方法 **acquireQueued**，加入队列中的结点调用此方法，如果是老二结点（dummy后的结点）则 tryAcquire,cas修改state和exclusiveOwnerThread，

否则进入 shouldParkAfterFailedAcquire 逻辑，将前驱 node，即 head 的 waitStatus 改为 -1，之后才会park掉线程,

而unlock时，只要链表不为空，则unpark老二结点，这时再次进入tryacquire，如果有竞争，外部线程可能优先tryacquire导致不公平



![1595045253140](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718120751-947446.png)

出现竞争后

![1595045270516](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718120756-909534.png)

Thread-1 执行了

1. lock方法中CAS 尝试将 state 由 0 改为 1，结果失败
2. lock方法中进一步调用acquire方法，进入 tryAcquire 逻辑，这里我们认为这时 state 已经是1，结果仍然失败   
3. 接下来进入 acquire方法的addWaiter 逻辑，构造 Node 队列
   1. 图中黄色三角表示该 Node 的 waitStatus 状态，其中 0 为默认正常状态
   2. Node 的创建是懒惰的
   3. 其中第一个 Node 称为 Dummy（哑元）或哨兵，用来占位，并不关联线程

![1595045451872](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718121606-851927.png)

当前线程进入  acquire方法的 acquireQueued 逻辑

1. acquireQueued 会在一个死循环中不断尝试获得锁，失败后进入 park 阻塞

2. 如果自己是紧邻着 head（排第二位），那么再次 tryAcquire 尝试获取锁，我们这里设置这时 state 仍为 1，失败

3. 进入 shouldParkAfterFailedAcquire 逻辑，将前驱 node，即 head 的 waitStatus 改为 -1，这次返回 false

![1595046768331](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718123250-843710.png)

1. shouldParkAfterFailedAcquire 执行完毕回到 acquireQueued ，再次 tryAcquire 尝试获取锁，当然这时
   state 仍为 1，失败
2. 当再次进入 shouldParkAfterFailedAcquire 时，这时因为其前驱 node 的 waitStatus 已经是 -1，这次返回
   true
3. 进入 parkAndCheckInterrupt， Thread-1 park（灰色表示已经阻塞）

​	![1595046786213](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718123306-587166.png)

Thread-0 调用unlock方法里的release方法释放锁，进入tryRelease(使用ctrl+alt+b查看tryRelease方法的具体`ReentrantLock`实现) 流程，如果成功，设置 exclusiveOwnerThread 为 null，state = 0

![1595046828330](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718123350-483378.png)

unlock方法里的release方法方法中，如果当前队列不为 null，并且 head 的 waitStatus = -1，进入 unparkSuccessor 流程：
unparkSuccessor中会找到队列中离 head 最近的一个 Node（没取消的），unpark 恢复其运行，本例中即为 Thread-1
回到 Thread-1 的 acquireQueued 流程

![1595046840247](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718123401-464896.png)

如果加锁成功（没有竞争），会设置  （acquireQueued 方法中）

1. exclusiveOwnerThread 为 Thread-1，state = 1
2. head 指向刚刚 Thread-1 所在的 Node，该 Node 清空 Thread
3. 原本的 head 因为从链表断开，而可被垃圾回收

如果这时候有其它线程来竞争（非公平的体现），例如这时有 Thread-4 来了

![1595046854757](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718123416-757446.png)

如果不巧又被 Thread-4 占了先

1. Thread-4 被设置为 exclusiveOwnerThread，state = 1
2. Thread-1 再次进入 acquireQueued 流程，获取锁失败，重新进入 park 阻塞

### 锁重入原理

有一个变量state记录当前锁被占领线程调用的次数

### 可打断原理

可打断模式下，只修改interrupted

不可打断模式下，会抛出InterruptedException()

### 公平锁原理

非公平锁原理，当tryaccquire时，不去检查aqs的等待队列直接去CAS

公平锁原理，正好相反，先检查aqs队列

### 条件变量原理

每个条件变量其实就对应着一个等待队列，其实现类是 ConditionObject

await 流程
开始 Thread-0 持有锁，调用 await，进入 ConditionObject 的 addConditionWaiter 流程
创建新的 Node 状态为 -2（Node.CONDITION），关联 Thread-0，加入等待队列尾部

![1595079373121](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718213616-149322.png)

接下来进入 AQS 的 fullyRelease 流程，释放同步器上的锁

![1595079397323](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718213639-301396.png)

unpark AQS 队列中的下一个节点，竞争锁，假设没有其他竞争线程，那么 Thread-1 竞争成功

![1595079457815](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718213740-553926.png)

park 阻塞 Thread-0

![1595079481112](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718213802-887995.png)

signal 流程

假设 Thread-1 要来唤醒 Thread-0

![1595079499939](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718213825-402537.png)

进入 ConditionObject 的 doSignal 流程，取得等待队列中第一个 Node，即 Thread-0 所在 Node

![1595079518690](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718214223-163008.png)



执行 transferForSignal 流程，将该 Node 加入 AQS 队列尾部，将 Thread-0 的 waitStatus 改为 0，Thread-3 的waitStatus 改为 -1

![1595079772187](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200718214253-269715.png)

Thread-1 释放锁，进入 unlock 流程，略

### 源码

加锁

```java
// Sync 继承自 AQS
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = 7316153563782823691L;

     // 加锁实现
    final void lock() {
        // 首先用 cas 尝试（仅尝试一次）将 state 从 0 改为 1, 如果成功表示获得了独占锁
        if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            // 如果尝试失败，进入 ㈠
            acquire(1);
    }

    // ㈠ AQS 继承过来的方法, 方便阅读, 放在此处
    public final void acquire(int arg) {
        // ㈡ tryAcquire
        if (
                !tryAcquire(arg) &&
            	// 当 tryAcquire 返回为 false 时, 先调用 addWaiter ㈣, 接着 acquireQueued ㈤
                 acquireQueued(addWaiter(Node.EXCLUSIVE), arg)
        ) {
            selfInterrupt();
        }
    }

    // ㈡ 进入 ㈢
    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }

    // ㈢ Sync 继承过来的方法, 方便阅读, 放在此处
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        // 如果还没有获得锁
        if (c == 0) {
            // 尝试用 cas 获得, 这里体现了非公平性: 不去检查 AQS 队列
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        // 如果已经获得了锁, 线程还是当前线程, 表示发生了锁重入
        else if (current == getExclusiveOwnerThread()) {
            // state++
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        // 获取失败, 回到调用处
        return false;
    }

    // ㈣ AQS 继承过来的方法, 方便阅读, 放在此处
    private Node addWaiter(Node mode) {
// 将当前线程关联到一个 Node 对象上, 模式为独占模式，新建的Node的waitstatus默认为0，因为waitstatus是成员变量，默认被初始化为0
        Node node = new Node(Thread.currentThread(), mode);
        // 如果 tail 不为 null, cas 尝试将 Node 对象加入 AQS 队列尾部
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                // 双向链表
                pred.next = node;
                return node;
            }
        }
        //如果tail为null，尝试将 Node 加入 AQS, 进入 ㈥
        enq(node);
        return node;
    }

    // ㈥ AQS 继承过来的方法, 方便阅读, 放在此处
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) {
                // 还没有, 设置 head 为哨兵节点（不对应线程，状态为 0）
                if (compareAndSetHead(new Node())) {
                    tail = head;
                }
            } else {
                // cas 尝试将 Node 对象加入 AQS 队列尾部
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    // ㈤ AQS 继承过来的方法, 方便阅读, 放在此处
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                // 上一个节点是 head, 表示轮到自己（当前线程对应的 node）了, 尝试获取
                if (p == head && tryAcquire(arg)) {
                    // 获取成功, 设置自己（当前线程对应的 node）为 head
                    setHead(node);
                    // 上一个节点 help GC
                    p.next = null;
                    failed = false;
                    // 返回中断标记 false
                    return interrupted;
                }
                if (
                    // 判断是否应当 park, 进入 ㈦
                    shouldParkAfterFailedAcquire(p, node) &&
                    // park 等待, 此时 Node 的状态被置为 Node.SIGNAL ㈧
                    parkAndCheckInterrupt()
                ) {
                    interrupted = true;
                }
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // ㈦ AQS 继承过来的方法, 方便阅读, 放在此处
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // 获取上一个节点的状态
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL) {
            // 上一个节点都在阻塞, 那么自己也阻塞好了
            return true;
        }
        // > 0 表示取消状态
        if (ws > 0) {
            // 上一个节点取消, 那么重构删除前面所有取消的节点, 返回到外层循环重试
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            // 这次还没有阻塞
            // 但下次如果重试不成功, 则需要阻塞，这时需要设置上一个节点状态为 Node.SIGNAL
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    // ㈧ 阻塞当前线程
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }
}
```

解锁

```java
// Sync 继承自 AQS
static final class NonfairSync extends Sync {
    // 解锁实现
    public void unlock() {
        sync.release(1);
    }

    // AQS 继承过来的方法, 方便阅读, 放在此处
    public final boolean release(int arg) {
        // 尝试释放锁, 进入 ㈠
        if (tryRelease(arg)) {
            // 队列头节点 unpark
            Node h = head;
            if (
                // 队列不为 null
                h != null &&
                // waitStatus == Node.SIGNAL 才需要 unpark
                h.waitStatus != 0
            ) {
                // unpark AQS 中等待的线程, 进入 ㈡
                unparkSuccessor(h);
            }
            return true;
        }
        return false;
    }

    // ㈠ Sync 继承过来的方法, 方便阅读, 放在此处
    protected final boolean tryRelease(int releases) {
        // state--
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        // 支持锁重入, 只有 state 减为 0, 才释放成功
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }

    // ㈡ AQS 继承过来的方法, 方便阅读, 放在此处
    private void unparkSuccessor(Node node) {
        // 如果状态为 Node.SIGNAL 尝试重置状态为 0, 如果线程获取到了锁那么后来头结点会被抛弃掉
        // 不成功也可以
        int ws = node.waitStatus;
        if (ws < 0) {
            compareAndSetWaitStatus(node, ws, 0);
        }
        // 找到需要 unpark 的节点, 但本节点从 AQS 队列中脱离, 是由唤醒节点完成的
        Node s = node.next;
        // 不考虑已取消的节点, 从 AQS 队列从后至前找到队列最前面需要 unpark 的节点
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            LockSupport.unpark(s.thread);
    }
}
```



## ReentrantReadWriteLock

1. 读锁不支持条件变量
2. 重入时升级不支持：即持有读锁的情况下去获取写锁，会导致获取写锁永久等待
3. 重入时降级支持：即持有写锁的情况下去获取读锁

类似操作系统读者写者问题

```
semaphore rw=1; //用于实现共享文件的互斥访问
int count = 0; //记录当前有几个进程在访问文件
semaphore mutex = 1; //用于保证对count变量的互斥访问
semaphore w = 1; //用于实现“写优先”
writer (){
        while(1){
            P(w);
            P(rw);
            写文件…
            V(rw);
            V(w);
        }
    }
reader (){
        while(1){
            P(w);
            P(mutex);
            if(count==0)
                P(rw);
            count++;
            V(mutex);
            V(w);
            写
            P(mutex);
            count--;
            if(count==0)
                V(rw);
            V(mutex);
        }
    }
```



### 读写锁应用数据库缓存

查询时，通过读锁保证并发查，通过写锁保证往缓存中放入查出的结果，在写锁中也要检查缓存中是否已经有，因为可以出现其他线程先通过写锁放入的情况

更改时，通过写锁保证数据库和缓存一致

```java
    @Override
    public <T> T queryOne(Class<T> beanClass, String sql, Object... args) {
        // 先从缓存中找，找到直接返回
        SqlPair key = new SqlPair(sql, args);;
        rw.readLock().lock();
        try {
            T value = (T) map.get(key);
            if(value != null) {
                return value;
            }
        } finally {
            rw.readLock().unlock();
        }
        rw.writeLock().lock();
        try {
            // 多个线程
            T value = (T) map.get(key);
            if(value == null) {
                // 缓存中没有，查询数据库
                value = dao.queryOne(beanClass, sql, args);
                map.put(key, value);
            }
            return value;
        } finally {
            rw.writeLock().unlock();
        }
    }

    @Override
    public int update(String sql, Object... args) {
        rw.writeLock().lock();
        try {
            // 先更新库
            int update = dao.update(sql, args);
            // 清空缓存
            map.clear();
            return update;
        } finally {
            rw.writeLock().unlock();
        }
    }

```

### 原理

简单理解

同样双向链表维护线程结点，不同的是读锁是shared，写锁是exclusive，同样每个结点前一个结点设置为-1负责叫醒当前结点，只有加写锁的时候，才会把exclusiveOwnerThread指向占领线程，当shared线程唤醒时，会唤醒链表第一个exclusive结点前的所有shared结点，累加到state上，当unlock时先修改state后unpark

读写锁用的是同一个 Sycn 同步器，因此等待队列、state 等也是同一个  

 下面执行：t1 w.lock，t2 r.lock

1） t1 成功上锁，流程与 ReentrantLock 加锁相比没有特殊之处，不同是写锁状态占了 state 的低 16 位，而读锁
使用的是 state 的高 16 位

![1595149666861](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719170758-795058.png)

2）t2 执行 r.lock，这时进入读锁的 sync.acquireShared(1) 流程，首先会进入 tryAcquireShared 流程。如果有写
锁占据，那么 tryAcquireShared 返回 -1 表示失败

> tryAcquireShared 返回值表示
>
> 1. -1 表示失败
> 2. 0 表示成功，但后继节点不会继续唤醒
> 3. 正数表示成功，而且数值是还有几个后继节点需要唤醒，我们这里的读写锁返回 1

![1595149816131](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719171017-881527.png)

3）这时会进入 sync.doAcquireShared(1) 流程，首先也是调用 addWaiter 添加节点，不同之处在于节点被设置为
Node.SHARED 模式而非 Node.EXCLUSIVE 模式，注意此时 t2 仍处于活跃状态

![1595149862569](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719171103-934763.png)



4）t2 会看看自己的节点是不是老二，如果是，还会再次调用 tryAcquireShared(1) 来尝试获取锁

5）如果没有成功，在 doAcquireShared 内 for (;;) 循环一次，把前驱节点的 waitStatus 改为 -1，再 for (;;) 循环一
次尝试 tryAcquireShared(1) 如果还不成功，那么在 parkAndCheckInterrupt() 处 park

![1595150020844](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719171346-311272.png)



**又继续执行：t3 r.lock，t4 w.lock**
这种状态下，假设又有 t3 加读锁和 t4 加写锁，这期间 t1 仍然持有锁，就变成了下面的样子

![1595150111679](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719171513-717513.png)

**继续执行t1 w.unlock**
这时会走到写锁的 sync.release(1) 流程，调用 sync.tryRelease(1) 成功，变成下面的样子

![1595152703040](F:/资料/八股复习/java-construct/java并发编程/assets/1595152703040.png)



接下来执行唤醒流程 sync.unparkSuccessor，即让老二恢复运行，这时 t2 在 doAcquireShared 内
parkAndCheckInterrupt() 处恢复运行，图中的t2从黑色变成了蓝色（注意这里只是恢复运行而已，并没有获取到锁！）
这回再来一次 for (;;) 执行 tryAcquireShared 成功则让读锁计数加一

![1595152000565](F:/资料/八股复习/java-construct/java并发编程/assets/1595152000565.png)





这时 t2 已经恢复运行，接下来 t2 调用 setHeadAndPropagate(node, 1)，它原本所在节点被置为头节点

![1595152203229](F:/资料/八股复习/java-construct/java并发编程/assets/1595152203229.png)

事情还没完，在 setHeadAndPropagate 方法内还会检查下一个节点是否是 shared，如果是则调用
doReleaseShared() 将 head 的状态从 -1 改为 0 并唤醒老二，这时 t3 在 doAcquireShared 内
parkAndCheckInterrupt() 处恢复运行

![1595152506026](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719175507-103788.png)

这回再来一次 for (;;) 执行 tryAcquireShared 成功则让读锁计数加一

![1595152518613](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719175519-539342.png)

这时 t3 已经恢复运行，接下来 t3 调用 setHeadAndPropagate(node, 1)，它原本所在节点被置为头节点

![1595152534234](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719175534-17620.png)

下一个节点不是 shared 了，因此不会继续唤醒 t4 所在节点



**再继续执行t2 r.unlock，t3 r.unlock**
t2 进入 sync.releaseShared(1) 中，调用 tryReleaseShared(1) 让计数减一，但由于计数还不为零

![1595153460990](F:/资料/八股复习/java-construct/java并发编程/assets/1595153460990.png)

t3 进入 sync.releaseShared(1) 中，调用 tryReleaseShared(1) 让计数减一，这回计数为零了，进入
doReleaseShared() 将头节点从 -1 改为 0 并唤醒老二，即

![1595153473005](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719181114-328967.png)

之后 t4 在 acquireQueued 中 parkAndCheckInterrupt 处恢复运行，再次 for (;;) 这次自己是老二，并且没有其他
竞争，tryAcquire(1) 成功，修改头结点，流程结束

![1595153528383](https://gitee.com/gu_chun_bo/picture/raw/master/image/20200719181211-256827.png)

## StampedLock

JDK 8 加入，配合【戳】使用

加解读锁

```java
long stamp = lock.readLock();
lock.unlockRead(stamp);
```

加解写锁

```java
long stamp = lock.writeLock();
lock.unlockWrite(stamp);
```

乐观读，StampedLock 支持 tryOptimisticRead() 方法（乐观读），读取完毕后需要做一次 戳校验 如果校验通
过，表示这期间确实没有写操作，数据可以安全使用，如果	校验没通过，需要重新获取读锁，保证数据安全。

```java
long stamp = lock.tryOptimisticRead();
// 验戳
if(!lock.validate(stamp)){
 // 锁升级
}
```

> StampedLock 不支持条件变量
> StampedLock 不支持可重入

## Semaphore

信号量，用来限制能同时访问共享资源的**线程上限**。

```java
public static void main(String[] args) {
        // 1. 创建 semaphore 对象
        Semaphore semaphore = new Semaphore(3);
        // 2. 10个线程同时运行
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                // 3. 获取许可
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    log.debug("running...");
                    sleep(1);
                    log.debug("end...");
                } finally {
                    // 4. 释放许可
                    semaphore.release();
                }
            }).start();
        }
    }
```

原理

和ReentrantLock类似，维护双向链表队列，不过state是自己设置信号量大小，其他同理

![1595168704315](..\juc\images\1595168704315.png)

## CountdownLatch

加强版join，可以自己设置多少个等待多少个线程

CountDownLatch是共享锁的一种实现,它默认构造 AQS 的 state 值为 count。当线程使用countDown方法时,其实使用了`tryReleaseShared`方法以CAS的操作来减少state,直至state为0就代表所有的线程都调用了countDown方法。当调用await方法的时候，如果state不为0，就代表仍然有线程没有调用countDown方法，那么就把已经调用过countDown的线程都放入阻塞队列Park,并自旋CAS判断state  == 0，直至最后一个线程调用了countDown，使得state == 0，于是阻塞的线程便判断成功，全部往下执行。

用来进行线程同步协作，等待所有线程完成倒计时。
其中构造参数用来初始化等待计数值，await() 用来等待计数归零，countDown() 用来让计数减一 

```java
 private static void test5() {
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService service = Executors.newFixedThreadPool(4);
        service.submit(() -> {
            log.debug("begin...");
            sleep(1);
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        });
        service.submit(() -> {
            log.debug("begin...");
            sleep(1.5);
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        });
        service.submit(() -> {
            log.debug("begin...");
            sleep(2);
            latch.countDown();
            log.debug("end...{}", latch.getCount());
        });
        service.submit(()->{
            try {
                log.debug("waiting...");
                latch.await();
                log.debug("wait end...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
```

## CyclicBarrier

适合多次循环同步，当count减为0后，自己会再将其赋值为count，这就不需要创建多个countdownlatch对象



## 线程安全集合类

![1601031651136](..\juc\images\1601031651136.png)

1. `Blocking`  ：大部分实现基于锁，并提供用来阻塞的方法

2. `CopyOnWrite` 之类容器：修改开销相对较重

3. `Concurrent` 类型的容器

   1. 内部很多操作使用 cas 优化，一般可以提供较高吞吐量
   2. 弱一致性

   > 1. 遍历时弱一致性，例如，当利用迭代器遍历时，如果容器发生修改，迭代器仍然可以继续进行遍
   >    历，这时内容是旧的(fast-safe机制)
   > 2. 求大小弱一致性，size 操作未必是 100% 准确
   > 3. 读取弱一致性

## ConcurrentHashMap

### 1.7中hashmap的死链问题

JDK1.7HashMap，加入hash表用头插法，JDK1.8用尾插法，当扩容时

![img](https://pic1.zhimg.com/80/v2-5f78d8ac8af1efc79e7d79a1f3fdc154_720w.jpg?source=1940ef5c)



这时t2执行自己的线程时，调用头插法时，就会将A.next=B,newTable[i]=A,这时就会出现A,b互相指向对方成死链

### 重要属性

node结点有四个子类 ForwardingNode,ReservationNode,TreeBin,TreeNode

```java
// 默认为 0
// 当初始化时, 为 -1
// 当扩容时, 为 -(1 + 扩容线程数)
// 当初始化或扩容完成后，为 下一次的扩容的阈值大小
private transient volatile int sizeCtl;
// 整个 ConcurrentHashMap 就是一个 Node[]
static class Node<K,V> implements Map.Entry<K,V> {}
// hash 表
transient volatile Node<K,V>[] table;
// 扩容时的 新 hash 表
private transient volatile Node<K,V>[] nextTable;
// 扩容时如果某个 bin 迁移完毕, 用 ForwardingNode 作为旧 table bin 的头结点
static final class ForwardingNode<K,V> extends Node<K,V> {}
// 用在 compute 以及 computeIfAbsent 时, 用来占位, 计算完成后替换为普通 Node
static final class ReservationNode<K,V> extends Node<K,V> {}
// 作为 treebin 的头节点, 存储 root 和 first
static final class TreeBin<K,V> extends Node<K,V> {}
// 作为 treebin 的节点, 存储 parent, left, right
static final class TreeNode<K,V> extends Node<K,V> {}
```

### 构造器分析

可以看到实现了懒惰初始化，在构造方法中仅仅计算了 table 的大小，以后在第一次使用时才会真正创建

```java
    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel) // Use at least as many bins
            initialCapacity = concurrencyLevel; // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        // tableSizeFor方法仍然是保证cap计算的大小是 2^n, 即 16,32,64 ...
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }
```

### get

hash负数表示是树结点或在扩容中

根据node重写方法进行不同策略的find

判断头结点是否等于hash

判断hash是否小于0  进行 去另一个正在扩容的hash表查找或者红黑树查找 **多态体现**

正常链表遍历查找

```java
 public V get(Object key) {
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
        // spread 方法能确保返回结果是正数
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (e = tabAt(tab, (n - 1) & h)) != null) {
            // 1.如果头结点已经是要查找的 key
            if ((eh = e.hash) == h) {
                //先使用==比较，再用 equals 比较
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }
            //2. hash 为负数表示该 bin 在扩容中或是 treebin, 这时调用 find 方法来查找
            else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            //3. 正常遍历链表, 先使用==比较，再用 equals 比较
            while ((e = e.next) != null) {
                if (e.hash == h &&
                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }
```

### put

1. 根据 key 计算出 hash值。
2. 判断是否需要进行初始化。 CAS初始化 其他线程会yield
3. 定位到 Node，拿到首节点 f，判断首节点 f：
   - 如果为 null ，则通过cas的方式尝试添加。
   - 如果为 `f.hash = MOVED = -1` ，说明其他线程在扩容，参与一起扩容。
   - 如果都不满足 ，synchronized 锁住 f 节点，判断是链表还是红黑树，遍历插入。
4. 当在链表长度达到8的时候，数组扩容或者将链表转换为红黑树。

## BlockingQueue

ReentrantLock



两把锁 putlock takelock，锁队列的tail和head

消费者和生产者可以并行

各自串行
