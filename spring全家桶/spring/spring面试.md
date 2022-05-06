### 

# 面试

## IOC DI AOP

IOC **Inversion of Control**

思想  类不去依赖耦合其他类，而是通过spring容器获取

实现方式是xml解析+工厂+反射

依然需要组件主动去pull

DI **Dependency Injection**

IOC的一种实现方式

容器主动去push

AOP **Aspect-Oriented Programming**

能够将那些与业务无关，却为业务模块所共同调用的逻辑或责任（例如事务处理、日志管理、权限控制等）封装起来

接口  **JDK Proxy** 效率高

无接口 **Cglib** 效率低

## Spring 框架中用到了哪些设计模式？

工厂模式

代理模式

单例模式

模板方法模式 jdbcTemplate

观察者模式 事件驱动模式 

## Spring AOP 和 AspectJ AOP 有什么区别？

**Spring AOP 属于运行时增强，而 AspectJ 是编译时增强。** Spring AOP 基于代理(Proxying)，而 AspectJ 基于字节码操作，在编译期间将advice代码切入切入点

## Spring相关用法

### DI的几种方式，对比

- 基于 field 注入（属性注入）
- 基于 setter 注入
- 基于 constructor 注入（构造器注入）

属性注入的缺点也是其他的优点

不能使用属性注入的方式构建不可变对象(`final` 修饰的变量)

只有在调用时才知道注入是否成功

·**区分构造注入和属性注入**

| 构造函数注入               | setter 注入                |
| -------------------------- | -------------------------- |
| 没有部分注入               | 有部分注入                 |
| 不会覆盖 setter 属性       | 会覆盖 setter 属性         |
| 任意修改都会创建一个新实例 | 任意修改不会创建一个新实例 |
| 适用于设置很多属性         | 适用于设置少量属性         |

### BeanFactory 和 ApplicationContext

| BeanFactory                | ApplicationContext       |
| -------------------------- | ------------------------ |
| 它使用懒加载               | 它使用即时加载           |
| 它使用语法显式提供资源对象 | 它自己创建和管理资源对象 |
| 不支持国际化               | 支持国际化               |
| 不支持基于依赖的注解       | 支持基于依赖的注解       |

### 几种配置方式

xml

注解 @Component

javaapi @configuration和@Bean 和

import

1@ import(类.class) 2importSelector 3importBeanDefinitionRegistar

通过BeanFactory

### Bean作用域

singleton 单例applicationcontext容器创建好时就会将单例全部创建，除非lazy

prototype

request

session

### 几种Bean注解

@repository @service @controller 

@bean @component  

@component 是任意层随便都可以作用在类上

@bean作用在方法上，多用于第三方类库注入

```java
@Configuration
public class AppConfig {
    @Bean
public TransferService transferService() {
return new TransferServiceImpl();
    }
}
```

### 几种注入注解

@Resource 非spring库 ByName

@Autowired 自动注入 ByType,如果多个type相等，将@Autowired指定的属性名称作为组件id查找，如果找不到抛异常

如果加上@Qualifier()则指定name必须相等

如果在Bean上加@Primary 则指定该Bean在注入时首选用这个

可以加在构造方法 set方法 参数 属性上

如果在@Bean+方法参数，参数是从容器中获取的，参数上等同于写Autowired，效果一样

## Aop相关用法

### 概念

连接点 joinpoint所有切入点

切入点 pointcut具体被增强的方法

切面  aspect 将通知应用到切入点

通知 advice增强的逻辑

切面类 通过注解将advice和pointcut连接起来

```java
@Aspect
@Component
public class LogAspect {
    @Pointcut("execution(public int com.wtq12138.aop.Cal.*(..))")
    public void pointCut() {

    }
    @Before("pointCut()")
    public void logStart() {
        System.out.println("除法开始");
    }
    @After("pointCut()")
    public void logEnd() {
        System.out.println("除法结束");
    }
    @AfterReturning("pointCut()")
    public void logNormal() {
        System.out.println("除法正常结束");
    }
    @AfterThrowing("pointCut()")
    public void logException() {
        System.out.println("除法异常");
    }
}

```



## ObjectFactory、FactoryBean 和 BeanFactory 的区别

三者都是工厂模式获取对象 BeanFactory是底层容器，

FactoryBean是一种特殊的Bean，**实现了 FactoryBean 接口的 Bean**，从BeanFactory获取bean时，如果通过此Bean的id获取，**实际上是 FactoryBean 的 getObject() 返回的对象，而不是 FactoryBean 本身，如果要获取 FactoryBean 对象，请在 id 前面加一个 & 符号来获取。**

ObjectFactory 提供的是延迟依赖查找，想要获取某一类型的 Bean，需要调用其 getObject() 方法才能依赖查找到目标 Bean 对象。ObjectFactory 就是一个对象工厂，想要获取该类型的对象，需要调用其 getObject() 方法生产一个对象。

## 生命周期

1. 实例化（Instantiation）
2. 属性赋值（Populate）
3. 初始化（Initialization）
4. 销毁（Destruction）

初始化和销毁的顺序是一样的

BeanPostProcessor  重写postProcessBeforeInitialization和postProcessAfterInitialization

InitializingBean 重写afterPropertiesSet  DisposableBean重写destroy

init-method destory-method 自己指定方法



![img](https://p1-jj.byteimg.com/tos-cn-i-t2oaga2asx/gold-user-assets/2020/2/15/1704860a4de235aa~tplv-t2oaga2asx-zoom-in-crop-mark:1304:0:0:0.awebp)

## bean线程安全吗

单例不安全，Protype安全

解决方案是不定义可变成员变量以及threadlocal

## 同名bean优先级

- 同一个配置文件内同名的Bean，以最上面定义的为准
- 不同配置文件中存在同名Bean，后解析的配置文件会覆盖先解析的配置文件

- 同文件中ComponentScan和@Bean出现同名Bean。同文件下@Bean的会生效，@ComponentScan扫描进来不会生效。通过@ComponentScan扫描进来的优先级是最低的，原因就是它扫描进来的Bean定义是最先被注册的~

## 循环依赖问题

1. prototype循环依赖：dogetBean中会检查 如果创建对象正在创建，在set中，抛异常
2. 构造器的循环依赖：这种依赖spring是处理不了的，直 接抛出BeanCurrentlylnCreationException异常 因为未初始化无法暴露对象

dogetBean中会检测 如果A依赖的B，而B依赖A，对于B依赖的bean会检测是否依赖A

3. 单例模式下的setter field循环依赖

（1）createBeanInstance：实例化，其实也就是调用对象的构造方法实例化对象

（2）populateBean：填充属性，这一步主要是多bean的依赖属性进行填充



![img](http://blog-img.coolsen.cn/img/1584758309616_10.png)

1. **【一级 Map】**从单例缓存 `singletonObjects` （concurrentHashmap）中获取 beanName 对应的 Bean
2. 如果一级 Map中不存在，且当前 beanName 正在创建
   1. 对 `singletonObjects` 加锁
   2. **【二级 Map】**从 `earlySingletonObjects` 集合中获取，里面会保存从 **三级 Map** 获取到的正在初始化的 Bean
   3. 如果二级 Map中不存在，且允许提前创建
      1. **【三级 Map】**从 `singletonFactories` 中获取对应的 ObjectFactory 实现类，如果从**三级 Map** 中存在对应的对象，则进行下面的处理
      2. 调用 ObjectFactory#getOject() 方法，获取目标 Bean 对象（早期半成品）
      3. 将目标对象放入**二级 Map**
      4. 从**三级 Map**移除 beanName
3. 返回从缓存中获取的对象

## 事务传播规则

- PROPAGATION_REQUIRED: 支持当前事务，如果当前没有事务，就新建一个事务。这是最常见的选择。
- PROPAGATION_SUPPORTS: 支持当前事务，如果当前没有事务，就以非事务方式执行。
- PROPAGATION_MANDATORY: 支持当前事务，如果当前没有事务，就抛出异常。
- PROPAGATION_REQUIRES_NEW: 新建事务，如果当前存在事务，把当前事务挂起。
- PROPAGATION_NOT_SUPPORTED: 以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。
- PROPAGATION_NEVER: 以非事务方式执行，如果当前存在事务，则抛出异常。
- PROPAGATION_NESTED:如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，则进行与PROPAGATION_REQUIRED类似的操作。

## 事务注解失效的场景

1. 方法不是 public 的
2. 代理失效场景 本类方法中调用另一个本类方法，导致第二个方法失去代理

##  SpringMVC流程

`DispatcherServlet`->HandlerMapping->HandlerAdapter->ModelAndView->ViewResolver

1. 客户端（浏览器）发送请求，直接请求到 `DispatcherServlet`。
2. `DispatcherServlet` 根据请求信息调用 `HandlerMapping`，解析请求对应的 `Handler`。
3. 解析到对应的 `Handler`（也就是我们平常说的 `Controller` 控制器）后，开始由 `HandlerAdapter` 适配器处理。
4. `HandlerAdapter` 会根据 `Handler`来调用真正的处理器开处理请求，并处理相应的业务逻辑。
5. 处理器处理完业务后，会返回一个 `ModelAndView` 对象，`Model` 是返回的数据对象，`View` 是个逻辑上的 `View`。
6. `ViewResolver` 会根据逻辑 `View` 查找实际的 `View`。
7. `DispaterServlet` 把返回的 `Model` 传给 `View`（视图渲染）。
8. 把 `View` 返回给请求者（浏览器）

## 九大组件

| 组件                        | 说明                                                         |
| --------------------------- | ------------------------------------------------------------ |
| DispatcherServlet           | Spring MVC 的核心组件，是请求的入口，负责协调各个组件工作    |
| MultipartResolver           | 内容类型( `Content-Type` )为 `multipart/*` 的请求的解析器，例如解析处理文件上传的请求，便于获取参数信息以及上传的文件 |
| HandlerMapping              | 请求的处理器匹配器，负责为请求找到合适的 `HandlerExecutionChain` 处理器执行链，包含处理器（`handler`）和拦截器们（`interceptors`） |
| HandlerAdapter              | 处理器的适配器。因为处理器 `handler` 的类型是 Object 类型，需要有一个调用者来实现 `handler` 是怎么被执行。Spring 中的处理器的实现多变，比如用户处理器可以实现 Controller 接口、HttpRequestHandler 接口，也可以用 `@RequestMapping` 注解将方法作为一个处理器等，这就导致 Spring MVC 无法直接执行这个处理器。所以这里需要一个处理器适配器，由它去执行处理器 |
| HandlerExceptionResolver    | 处理器异常解析器，将处理器（ `handler` ）执行时发生的异常，解析( 转换 )成对应的 ModelAndView 结果 |
| RequestToViewNameTranslator | 视图名称转换器，用于解析出请求的默认视图名                   |
| LocaleResolver              | 本地化（国际化）解析器，提供国际化支持                       |
| ThemeResolver               | 主题解析器，提供可设置应用整体样式风格的支持                 |
| ViewResolver                | 视图解析器，根据视图名和国际化，获得最终的视图 View 对象     |
| FlashMapManager             | FlashMap 管理器，负责重定向时，保存参数至临时存储（默认 Session） |

## springboot启动原理

- Spring Boot 在启动时会去依赖的 Starter 包中寻找 resources/META-INF/spring.factories 文件，然后根据文件中配置的 Jar 包去扫描项目所依赖的 Jar 包。
- 根据 spring.factories 配置加载 AutoConfigure 类
- 根据 @Conditional 注解的条件，进行自动配置并将 Bean 注入 Spring Context
