# spring源码浅尝辄止

https://www.cnblogs.com/lifullmoon/p/14452795.html

## IOC

### 启动流程

### refresh

```java
public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			// Prepare this context for refreshing.
            允许收集早期的容器事件，等待事件派发器可用之后，即可进行发布。
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
            创建了一个BeanFactory对象（DefaultListableBeanFactory类型的），并为其设置好了一个序列化id。
            由于AnnotationConfigApplicationContext extends GenericApplicationContext，而父类构造方法里对于工厂已经初始化过了	
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context
            一堆set方法
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
                一个protected的空方法
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
				// Invoke factory processors registered as beans in the context.
                执行BeanDefinitionRegistryPostProcessor和BeanFactoryPostProcessors的方法
				invokeBeanFactoryPostProcessors(beanFactory);
				
				// Register bean processors that intercept bean creation.
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

                国际化信息？？不太懂
				// Initialize message source for this context.
				initMessageSource();

                获取多播器
				// Initialize event multicaster for this context.
				initApplicationEventMulticaster();
				
                一个protected的空方法
				// Initialize other special beans in specific context subclasses.
				onRefresh();

                注册listener
				// Check for listener beans and register them.
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
               	初始化所有剩下的单实例bean
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}
```



### bean的前身

Spring Bean 的“前身”为 BeanDefinition 对象，里面包含了 Bean 的元信息，后续在 Bean 的生命周期中会根据该对象进行实例化和初始化等工作

BeanDefinition 接口的实现类主要根据 Bean 的定义方式进行区分，如下：

1. XML 定义 Bean：GenericBeanDefinition
2. @Component 以及派生注解定义 Bean：ScannedGenericBeanDefinition
3. 借助于 @Import 导入 Bean：AnnotatedGenericBeanDefinition
4. @Bean 定义的方法：ConfigurationClassBeanDefinition 私有静态类

上面的 `1`、`2`、`3` 三种 BeanDefinition 实现类具有层次性，在 Spring BeanFactory 初始化 Bean 的前阶段，会根据 BeanDefinition 生成一个合并后的 RootBeanDefinition 对象

### xml加载解析

我们在 Spring 中通常以这两种方式定义一个 Bean：**面向资源（XML、Properties）**、**面向注解**，对于第一种方式如果定义的是一个 XML 文件，Spring 会通过 XmlBeanDefinitionReader 加载该 XML 文件，获取该 Resource 资源的 `org.w3c.dom.Document` 对象，这个过程会经过校验、解析两个步骤

将document解析为BeanDefinition,注册

### 注解加载

1. ClassPathBeanDefinitionScanner 会去扫描到包路径下所有的 .class 文件
2. 通过 **ASM**（Java 字节码操作和分析框架）获取 .class 对应类的所有元信息
3. 根据元信息判断是否符合条件（带有 `@Component 注解或其派生注解`），符合条件则根据这个类的元信息生成一个 BeanDefinition 进行注册

### Bean加载 get

`dogetBean()`

对于单例模式 三级缓存获取到 调用`getObjectForBeanInstance()`获取，其中方法会直接返回bean或者去getObjectFromFactoryBean获取

如果未获取到，

这里检查如果非单例模式，threadlocal中存一个set检查是否正在创建，说明循环依赖，抛异常

则尝试从beanfactory 层次进行bean加载

加载后将其加入`alreadyCreated集合`中防止多线程创建和循环依赖 这里用双重检查锁

从获取mergedBeanDefinitions concurenthashmap缓存中获取BD(bean definition)

处理depends-on注解

对于不同的作用域，进行不同bean的创建

最后进行类型转换

```java
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
        @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    // <1> 获取 `beanName`
    // 因为入参 `name` 可能是别名，也可能是 FactoryBean 类型 Bean 的名称（`&` 开头，需要去除）
    // 所以需要获取真实的 beanName
    final String beanName = transformedBeanName(name);
    Object bean;

    // <2> 先从缓存（仅缓存单例 Bean ）中获取 Bean 对象，这里缓存指的是 `3` 个 Map
    // 缓存中也可能是正在初始化的 Bean，可以避免**循环依赖注入**引起的问题
    Object sharedInstance = getSingleton(beanName);
    // <3> 若从缓存中获取到对应的 Bean，且 `args` 参数为空
    if (sharedInstance != null && args == null) {
        .../
        // <3.1> 获取 Bean 的目标对象，`scopedInstance` 非 FactoryBean 类型直接返回
        // 否则，调用 FactoryBean#getObject() 获取目标对象
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }
    // 缓存中没有对应的 Bean，则开启 Bean 的加载
    else {
        // <4> 如果**非单例模式**下的 Bean 正在创建，这里又开始创建，表明存在循环依赖，则直接抛出异常
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        BeanFactory parentBeanFactory = getParentBeanFactory();
        // <5> 如果从当前容器中没有找到对应的 BeanDefinition，则从父容器中加载（如果存在父容器）
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // Not found -> check parent.
            // <5.1> 获取 `beanName`，因为可能是别名，则进行处理
            // 和第 `1` 步不同，不需要对 `&` 进行处理，因为进入父容器重新依赖查找
            String nameToLookup = originalBeanName(name);
            // <5.2> 若为 AbstractBeanFactory 类型，委托父容器的 doGetBean 方法进行处理
            // 否则，就是非 Spring IoC 容器，根据参数调用相应的 `getBean(...)`方法
            if (parentBeanFactory instanceof AbstractBeanFactory) {
                return ((AbstractBeanFactory) parentBeanFactory).doGetBean(nameToLookup, requiredType, args, typeCheckOnly);
            }
            else if (args != null) {
                return (T) parentBeanFactory.getBean(nameToLookup, args);
            }
            else if (requiredType != null) {
                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
            else {
                return (T) parentBeanFactory.getBean(nameToLookup);
            }
        }

        // <6> 如果不是仅仅做类型检查，则表示需要创建 Bean，将 `beanName` 标记为已创建过
        if (!typeCheckOnly) {
            markBeanAsCreated(beanName);
        }

        try {
            // <7> 从容器中获取 `beanName` 对应的的 RootBeanDefinition（合并后）
            final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            // 检查是否为抽象类
            checkMergedBeanDefinition(mbd, beanName, args);

            // Guarantee initialization of beans that the current bean depends on.
            // <8> 获取当前正在创建的 Bean 所依赖对象集合（`depends-on` 配置的依赖）
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                for (String dep : dependsOn) {
                    // <8.1> 检测是否存在循环依赖，存在则抛出异常
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    // <8.2> 将 `beanName` 与 `dep` 之间依赖的关系进行缓存
                    registerDependentBean(dep, beanName);
                    try {
                        // <8.3> 先创建好依赖的 Bean（重新调用 `getBean(...)` 方法）
                        getBean(dep);
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                    }
                }
            }

            // Create bean instance.
            // <9> 开始创建 Bean，不同模式创建方式不同
            if (mbd.isSingleton()) { // <9.1> 单例模式
                /*
                 * <9.1.1> 创建 Bean，成功创建则进行缓存，并移除缓存的早期对象
                 * 创建过程实际调用的下面这个 `createBean(...)` 方法
                 */
                sharedInstance = getSingleton(beanName,
                        // ObjectFactory 实现类
                        () -> {
                            try {
                                // **【核心】** 创建 Bean
                                return createBean(beanName, mbd, args);
                            } catch (BeansException ex) {
                                // Explicitly remove instance from singleton cache: It might have been put there
                                // eagerly by the creation process, to allow for circular reference resolution.
                                // Also remove any beans that received a temporary reference to the bean.
                                // 如果创建过程出现异常，则显式地从缓存中删除当前 Bean 相关信息
                                // 在单例模式下为了解决循环依赖，创建过程会缓存早期对象，这里需要进行删除
                                destroySingleton(beanName);
                                throw ex;
                            }
                });
                // <9.1.2> 获取 Bean 的目标对象，`scopedInstance` 非 FactoryBean 类型直接返回
                // 否则，调用 FactoryBean#getObject() 获取目标对象
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }
            // <9.2> 原型模式
            else if (mbd.isPrototype()) {
                // It's a prototype -> create a new instance.
                Object prototypeInstance = null;
                try {
                    // <9.2.1> 将 `beanName` 标记为原型模式正在创建
                    beforePrototypeCreation(beanName);
                    // <9.2.2> **【核心】** 创建 Bean
                    prototypeInstance = createBean(beanName, mbd, args);
                }
                finally {
                    // <9.2.3> 将 `beanName` 标记为不在创建中，照应第 `9.2.1` 步
                    afterPrototypeCreation(beanName);
                }
                // <9.2.4> 获取 Bean 的目标对象，`scopedInstance` 非 FactoryBean 类型直接返回
                // 否则，调用 FactoryBean#getObject() 获取目标对象
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            }
            // <9.3> 其他模式
            else {
                // <9.3.1> 获取该模式的 Scope 对象 `scope`，不存在则抛出异常
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    // <9.3.1> 从 `scope` 中获取 `beanName` 对应的对象（看你的具体实现），不存在则执行**原型模式**的四个步骤进行创建
                    Object scopedInstance = scope.get(beanName, () -> {
                        // 将 `beanName` 标记为原型模式正在创建
                        beforePrototypeCreation(beanName);
                        try {
                            // **【核心】** 创建 Bean
                            return createBean(beanName, mbd, args);
                        }
                        finally {
                            // 将 `beanName` 标记为不在创建中，照应上一步
                            afterPrototypeCreation(beanName);
                        }
                    });
                    // 获取 Bean 的目标对象，`scopedInstance` 非 FactoryBean 类型直接返回
                    // 否则，调用 FactoryBean#getObject() 获取目标对象
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                }
                catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                            "Scope '" + scopeName + "' is not active for the current thread; consider " +
                            "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                            ex);
                }
            }
        }
        catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }
    }

    // Check if required type matches the type of the actual bean instance.
    // <10> 如果入参 `requiredType` 不为空，并且 Bean 不是该类型，则需要进行类型转换
    if (requiredType != null && !requiredType.isInstance(bean)) {
        try {
            // <10.1> 通过类型转换机制，将 Bean 转换成 `requiredType` 类型
            T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
            // <10.2> 转换后的 Bean 为空则抛出异常
            if (convertedBean == null) {
                // 转换失败，抛出 BeanNotOfRequiredTypeException 异常
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            // <10.3> 返回类型转换后的 Bean 对象
            return convertedBean;
        }
        catch (TypeMismatchException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to convert bean '" + name + "' to required type '" +
                        ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }
    // <11> 返回获取到的 Bean
    return (T) bean;
}

```

### bean创建 create

先create  在真正create之前让InstantiationAwareBeanPostProcessor拦截进行aop返回代理对象

如果没返回则doCreateBean

单例 从缓存里拿

instanceWrapper = createBeanInstance(beanName, mbd, args); 使用合适的实例化策略来创建 Bean 的实例：工厂方法、构造函数自动注入、简单初始化

对 RootBeanDefinition（合并后）进行加工处理

提前暴露这个 `bean`，如果可以的话，目的是解决单例模式 Bean 的循环依赖注入

populateBean(beanName, mbd, instanceWrapper);此方法先被InstantiationAwareBeanPostProcessors拦截执行postProcessAfterInstantiation和postProcessPropertyValues，最后才赋值，赋值通过CommonAnnotationBeanPostProcessor 和AutowiredAnnotationBeanPostProcessor

之后initializeBean中 生命周期中的初始化

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		.../
		//在真正create之前让InstantiationAwareBeanPostProcessor拦截进行aop返回代理对象
		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		try {
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		.../
	}
}
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
        throws BeanCreationException {

    // Instantiate the bean.
    /**
     * <1> Bean 的实例化阶段，会将 Bean 的实例对象封装成 {@link BeanWrapperImpl} 包装对象
     * BeanWrapperImpl 承担的角色：
     * 1. Bean 实例的包装
     * 2. {@link org.springframework.beans.PropertyAccessor} 属性编辑器
     * 3. {@link org.springframework.beans.PropertyEditorRegistry} 属性编辑器注册表
     * 4. {@link org.springframework.core.convert.ConversionService} 类型转换器（Spring 3+，替换了之前的 TypeConverter）
     */
    BeanWrapper instanceWrapper = null;
    // <1.1> 如果是单例模式，则先尝试从 `factoryBeanInstanceCache` 缓存中获取实例对象，并从缓存中移除
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    // <1.2> 使用合适的实例化策略来创建 Bean 的实例：工厂方法、构造函数自动注入、简单初始化
    // 主要是将 BeanDefinition 转换为 BeanWrapper 对象
    if (instanceWrapper == null) {
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    // <1.3> 获取包装的实例对象 `bean`
    final Object bean = instanceWrapper.getWrappedInstance();
    // <1.4> 获取包装的实例对象的类型 `beanType`
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    // Allow post-processors to modify the merged bean definition.
    // <2> 对 RootBeanDefinition（合并后）进行加工处理
    synchronized (mbd.postProcessingLock) { // 加锁，线程安全
        // <2.1> 如果该 RootBeanDefinition 没有处理过，则进行下面的处理
        if (!mbd.postProcessed) {
            try {
                /**
                 * <2.2> 对 RootBeanDefinition（合并后）进行加工处理
                 * 调用所有 {@link MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition}
                 * 【重要】例如有下面两个处理器：
                 * 1. AutowiredAnnotationBeanPostProcessor 会先解析出 @Autowired 和 @Value 注解标注的属性的注入元信息，后续进行依赖注入；
                 * 2. CommonAnnotationBeanPostProcessor 会先解析出 @Resource 注解标注的属性的注入元信息，后续进行依赖注入，
                 * 它也会找到 @PostConstruct 和 @PreDestroy 注解标注的方法，并构建一个 LifecycleMetadata 对象，用于后续生命周期中的初始化和销毁
                 */
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Post-processing of merged bean definition failed", ex);
            }
            // <2.3> 设置该 RootBeanDefinition 被处理过，避免重复处理
            mbd.postProcessed = true;
        }
    }

    // Eagerly cache singletons to be able to resolve circular references
    // even when triggered by lifecycle interfaces like BeanFactoryAware.
    // <3> 提前暴露这个 `bean`，如果可以的话，目的是解决单例模式 Bean 的循环依赖注入
    // <3.1> 判断是否可以提前暴露
    boolean earlySingletonExposure = (mbd.isSingleton() // 单例模式
            && this.allowCircularReferences // 允许循环依赖，默认为 true
            && isSingletonCurrentlyInCreation(beanName)); // 当前单例 bean 正在被创建，在前面已经标记过
    if (earlySingletonExposure) {
        if (logger.isTraceEnabled()) {
            logger.trace("Eagerly caching bean '" + beanName +
                    "' to allow for resolving potential circular references");
        }
        /**
         * <3.2>
         * 创建一个 ObjectFactory 实现类，用于返回当前正在被创建的 `bean`，提前暴露，保存在 `singletonFactories` （**三级 Map**）缓存中
         *
         * 可以回到前面的 {@link AbstractBeanFactory#doGetBean#getSingleton(String)} 方法
         * 加载 Bean 的过程会先从缓存中获取单例 Bean，可以避免单例模式 Bean 循环依赖注入的问题
         */
        addSingletonFactory(beanName,
                // ObjectFactory 实现类
                () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // Initialize the bean instance.
    // 开始初始化 `bean`
    Object exposedObject = bean;
    try {
        // <4> 对 `bean` 进行属性填充，注入对应的属性值
        populateBean(beanName, mbd, instanceWrapper);
        // <5> 初始化这个 `exposedObject`，调用其初始化方法
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
            throw (BeanCreationException) ex;
        }
        else {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
        }
    }

    // <6> 循环依赖注入的检查
    if (earlySingletonExposure) {
        // <6.1> 获取当前正在创建的 `beanName` 被依赖注入的早期引用
        // 注意，这里有一个入参是 `false`，不会调用上面第 `3` 步的 ObjectFactory 实现类
        // 也就是说当前 `bean` 如果出现循环依赖注入，这里才能获取到提前暴露的引用
        Object earlySingletonReference = getSingleton(beanName, false);
        // <6.2> 如果出现了循环依赖注入，则进行接下来的检查工作
        if (earlySingletonReference != null) {
            // <6.2.1> 如果 `exposedObject` 没有在初始化阶段中被改变，也就是没有被增强
            // 则使用提前暴露的那个引用
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            // <6.2.2> 否则，`exposedObject` 已经不是被别的 Bean 依赖注入的那个 Bean
            else if (!this.allowRawInjectionDespiteWrapping // 是否允许注入未加工的 Bean，默认为 false，这里取非就为 true
                    && hasDependentBean(beanName)) { // 存在依赖 `beanName` 的 Bean（通过 `depends-on` 配置）
                // 获取依赖当前 `beanName` 的 Bean 们的名称（通过 `depends-on` 配置）
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                // 接下来进行判断，如果依赖 `beanName` 的 Bean 已经创建
                // 说明当前 `beanName` 被注入了，而这里最终的 `bean` 被包装过，不是之前被注入的
                // 则抛出异常
                for (String dependentBean : dependentBeans) {
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                if (!actualDependentBeans.isEmpty()) {
                    throw new BeanCurrentlyInCreationException(beanName,
                            "Bean with name '" + beanName + "' has been injected into other beans [" +
                            StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                            "] in its raw version as part of a circular reference, but has eventually been " +
                            "wrapped. This means that said other beans do not use the final version of the " +
                            "bean. This is often the result of over-eager type matching - consider using " +
                            "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                }
            }
        }
    }

    // Register bean as disposable.
    try {
        /**
         * <7> 为当前 `bean` 注册 DisposableBeanAdapter（如果需要的话），用于 Bean 生命周期中的销毁阶段
         * 可以看到 {@link DefaultSingletonBeanRegistry#destroySingletons()} 方法
         */
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }
    // <8> 返回创建好的 `exposedObject` 对象
    return exposedObject;
}
```



### Aware接口

不同Aware接口下是不同set方法，用处是给bean注入容器中的相关Context和内部组件

## AOP

@EnableAspectJAutoProxy开启注解aop功能 

主要流程是向ioc容器中注入**AnnotationAwareAspectJAutoProxyCreator**，而此组件继承了SmartInstantiationAwareBeanPostProcessor和BeanFactoryAware

所以根本流程其实是向容器中注入BeanPostProcessor

流程

```java
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		this();
		register(componentClasses);
		refresh();
	}

public void refresh() throws BeansException, IllegalStateException {
    .../
        注册后置处理器
        registerBeanPostProcessors(beanFactory);
    	完成初始化
    	finishBeanFactoryInitialization(beanFactory);
    .../
}
public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
    分批次注册,实现PriorityOrdered、Ordered接口、和没实现的，三个批次注册
}
注册其实是beanFactory.getBean()
之后将BeanPostProcessors（是一个list）中加入BeanPostProcessor
初始化其实就是来创建剩下的单实例bean
挨个dogetBean
获取不到时就会去createBean
InstantiationAwareBeanPostProcessor类型的后置处理器会在实例化对象前试图返回

protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
     .../
        
       Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
    .../
}
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		.../
					if (targetType != null) {
					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
					if (bean != null) {
						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
		.../
		return bean;
	}

protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
		for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
			Object result = bp.postProcessBeforeInstantiation(beanClass, beanName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}	
在实例化前兜兜转转调用了这个方法  实例化前进行一个切面和切入点的准备
    advisedBeans里是需要增强的连接点
    isInfrastructureClass判断是否带@aspect可以
public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}
在实例化后调用after这个方法
public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}
包装即创建代理对象，先找到所有advice，然后找到所有增强bean的advice，保存到代理工厂（即proxyFactory），最后创建代理对象
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		.../

		// Create proxy if we have advice.
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		.../
		return bean;
	}
代理对象执行方法时会进入intercept方法，增强的advice包装为intercept链，并包装一个CglibMethodInvocation(传入代理对象，方法，intercept链)执行proceed方法
主要干两个事儿 索引++ invoke方法，invoke方法通过多态执行不同的intercept中invoke方法，以此达到一个效果是每一个拦截器等待下一个拦截器执行完成返回以后再来执行
public Object proceed() throws Throwable {
		.../
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		.../
		else {
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}
```

至此 从@EnableAspectJAutoProxy开启注解aop功能到注入AnnotationAwareAspectJAutoProxyCreator再到通过此InstantiationAwareBeanPostProcessor在每个对象实例化前进行拦截做好切面的准备，实例化进行包装生成代理类，执行方法进入intercept方法，包装一个CglibMethodInvocation执行proceed方法，通过拦截器链进行对象方法的增强

## 事务原理

**@EnableTransactionManagement**

TransactionManagementConfigurationSelector导入组件AutoProxyRegistrar和ProxyTransactionManagementConfiguration

AutoProxyRegistrar注入InfrastructureAdvisorAutoProxyCreator这个也是后置处理器   会导入环境 比如aop的AnnotationAwareAspectJAutoProxyCreator

ProxyTransactionManagementConfiguration来提供一个advisor  里面有两个TransactionAttributeSource和TransactionInterceptor

transactionAttributeSource其中有parser用来解析相关事务注解，

transactionInterceptor 实现 MethodInterceptor用来拦截对象执行方法

invokeWithinTransaction实际上是获取 事务管理器 通过它来进行事务的提交

```java
public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
			TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {

		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		advisor.setTransactionAttributeSource(transactionAttributeSource);
		advisor.setAdvice(transactionInterceptor);
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}
其invoke方法
public Object invoke(MethodInvocation invocation) throws Throwable {
		// Work out the target class: may be {@code null}.
		// The TransactionAttributeSource should be passed the target class
		// as well as the method, which may be from an interface.
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

		// Adapt to TransactionAspectSupport's invokeWithinTransaction...
		return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
	}
```

## 扩展组件

### BeanFactoryPostProcessor

在context.refresh()方法中可以清除看到执行顺序

**BeanFactoryPostProcessor的调用时机是在BeanFactory标准初始化之后，这样一来，我们就可以来定制和修改BeanFactory里面的一些内容了，此时，所有的bean定义已经保存加载到BeanFactory中了，但是bean的实例还未创建。**

invoke方法中是从beanFatory中获取BeanFactoryPostProcessor类型的组件，然后执行方法

```java
public void refresh() throws BeansException, IllegalStateException {
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				invokeBeanFactoryPostProcessors(beanFactory);
            }
}
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		.../
	}

```

### BeanDefinitionRegistryPostProcessor

extends BeanFactoryPostProcessor

优先于 BeanFactoryPostProcessor

```java
public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
    从IOC容器中获取到所有的BeanDefinitionRegistryPostProcessor组件，
    并依次触发它们的postProcessBeanDefinitionRegistry方法，然后再来触发它们的postProcessBeanFactory方法
	再来从IOC容器中获取到所有的BeanFactoryPostProcessor组件，并依次触发它们的postProcessBeanFactory方法
}
```

### ApplicationListener



多播器在容器refresh时会注入容器中

我猜是一个事件通过多播器映射到不同listener上，然后回调

```java
publishEvent发布事件
protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		.../
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}
		.../
		
	}
多播器->遍历Listener 异步或同步执行 
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		Executor executor = getTaskExecutor();
		for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
			if (executor != null) {
				executor.execute(() -> invokeListener(listener, event));
			}
			else {
				invokeListener(listener, event);
			}
		}
	}
callback 回调listener的onApplicationEvent方法
private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
		try {
			listener.onApplicationEvent(event);
		}
		.../
	}
```

### @EventListener 鸽

# SprigMVC原理

## 前置知识

Servlet 一个java类运行在tomcat中接受请求 响应数据，

JSP(Java Server Page) jsp其实就是个Servlet

DispatcherServlet 的继承结构如下

Httpservlet原始Servlet的Service方法中通过判断选择执行自己内部的doGet或者doPost方法，

而FrameworkServlet中的doGet或者doPost方法全部调用了其内部的processRequest方法，其中调用了抽象方法doService

而只有DispatcherServlet重写此方法，并调用其内部doDispatch进行分发

![FrameworkServlet](F:\资料\八股复习\冲冲冲\spring全家桶\spring\images\FrameworkServlet.png)

## 源码



```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		.../

				// 获取handler执行链 除了handler还有interceptor链
				mappedHandler = getHandler(processedRequest);
				// Determine handler adapter for the current request.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Actually invoke the handler.
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
	
				processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
	}
//从List<handlerMapping> handlerMappings遍历即可
因为有不同种实现的handlerMapping 低版本的源码有两种
    高版本的有五种 
    核心原理就是从handlerMapping LinkedHashMap中获取
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.handlerMappings != null) {
			for (HandlerMapping mapping : this.handlerMappings) {
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
		}
		return null;
	}
//同理 handlerAdapters遍历
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		if (this.handlerAdapters != null) {
			for (HandlerAdapter adapter : this.handlerAdapters) {
				if (adapter.supports(handler)) {
					return adapter;
				}
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

```



# SpringBoot

https://www.yuque.com/atguigu/springboot/qb7hy2

```

```



## 原理

```java
@SpringBootApplication等于

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
重点在@EnableAutoConfiguration

@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
}

其中两个注解一个@AutoConfigurationPackage
@Import(AutoConfigurationPackages.Registrar.class)将@SpringBootApplication标注注解的包名中的组件注册进来
public @interface AutoConfigurationPackage {
}

static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
			register(registry, new PackageImports(metadata).getPackageNames().toArray(new String[0]));
		}
	}
另一个@Import(AutoConfigurationImportSelector.class)
Selector导入
最终从META-INF/spring.factories中获取需要加载的Configuration
但是实际加载时按需加载，通过@Conditional注解实现
public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
		.../
		try {
			Enumeration<URL> urls = (classLoader != null ?
					classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
			.../
		}
	}
```



## web开发中常见

 @ConfigurationProperties(prefix="xxx")
 读取配置文件中的属性但是必须配合容器中的组件

 在类上加@Component 把类注册到里面
 也可以在Config上加@EnableConfigurationProperties(.class) 指定某类注册进去，这种做法可以将第三方包的属性与配置绑定

spring.mvc.static-path-pattern指定静态资源匹配路径必须携带前缀如  /res/a.jpg 注意这里本地的位置不需要改为/res

好处是将动态请求和静态请求的路径作分离方便拦截

spring.resources.static-locations指定本地静态资源的目录，默认是/static` (or `/public` or `/resources` or `/META-INF/resources

将其都改为resources下的haha目录中

```properties
spring.mvc.static-path-pattern=/res/**
spring.resources.static-locations= classpath:/haha/
```



Rest原理

- 表单提交会带上**_method=PUT**
- **请求过来被**HiddenHttpMethodFilter拦截
- - - **包装模式requesWrapper重写了getMethod方法，返回的是传入的值。**
      - **过滤器链放行的时候用wrapper。以后的方法调用getMethod是调用****requesWrapper的。**