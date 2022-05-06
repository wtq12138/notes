# nacos

## 注册中心

依赖

```xml
<dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

在每个模块的application.yml中添加模块名和nacos主机的ip

```yml
  application:
    name: gulimall-coupon
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.137.14
```

在springboot启动类添加注释@EnableDiscoveryClient

## 配置中心

依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

创建bootstrap.properties文件，该配置文件会优先于“application.yml”加载。

```properties
spring.application.name=gulimall-coupon
spring.cloud.nacos.config.server-addr=192.168.137.14:8848
```

根据命名空间来配置

```properties
spring.application.name=gulimall-third-party
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.namespace=7b0e01b0-4a67-45fd-ac95-af4df992a92c

spring.cloud.nacos.config.extension-configs[0].data-id=spring.yaml
spring.cloud.nacos.config.extension-configs[0].group=DEFAULT_GROUP
spring.cloud.nacos.config.extension-configs[0].refresh=true
```



# open feign

声明式、模板化的HTTP请求客户端

1)、引入open-feign

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
```



2)、在member模块中编写一个接口，告诉SpringCLoud这个接口需要调用coupon的远程服务

在coupon添加以下controller方法：

```java
    @RequestMapping("/member/list")
    public R memberCoupons(){
        CouponEntity couponEntity = new CouponEntity();
        couponEntity.setCouponName("discount 20%");
        return R.ok().put("coupons",Arrays.asList(couponEntity));
    }
```

在member模块中新建CouponFeignService接口

```java
@FeignClient("gulimall_coupon")
public interface CouponFeignService {
    @RequestMapping("/coupon/coupon/member/list")
    public R memberCoupons();
}
```

修改GulimallMemberApplication类，添加上"@EnableFeignClients"：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.bigdata.gulimall.member.feign")
public class GulimallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }
}
```

3)、开启远程调用功能

在member模块中调用CouponFeignService

```java
    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity=new MemberEntity();
        memberEntity.setNickname("zhangsan");
        R memberCoupons = couponFeignService.memberCoupons();

        return memberCoupons.put("member",memberEntity).put("coupons",memberCoupons.get("coupons"));
    }

```

# gateway

依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

application.yml配置

 uri: lb://gulimall-product 负载均衡到该模块

predicates:
            --Path=/api/product/** 声明url

filters:

​			将携带/api路径的url重写为想要的路径

```yml
spring:
  cloud:
  	gateway:
      routes:
        - id: product_route
          uri: lb://gulimall-product
          predicates:
            - Path=/api/product/**
          filters:
            - RewritePath=/api/(?<segment>/?.*),/$\{segment}
        - id: third_party_route
          uri: lb://gulimall-third-party
          predicates:
            - Path=/api/thirdparty/**
          filters:
            - RewritePath=/api(?<segment>/?.*),/$\{segment}
        - id: admin_route
          uri: lb://renren-fast
          predicates:
            - Path=/api/**
          filters:
            - RewritePath=/api/(?<segment>/?.*),/renren-fast/$\{segment}
   application:
    name: gulimall-gateway
server:
  port: 88
```

配置跨域

跨域流程

发送非简单请求前先发送预检请求，只有回复可以，才可以发送真实请求

```java
@Configuration
public class GulimallCorsConfiguration {
    @Bean
    public CorsWebFilter corsWebFilter() {
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration=new CorsConfiguration();
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.setAllowCredentials(true);
        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter((CorsConfigurationSource) source);
    }

}
```

# alibaba Seata

分布式事务 不适合高并发       适合后台事务

配置

```java
@Configuration
public class MySeataConfig {

    @Autowired
    DataSourceProperties dataSourceProperties;


    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {

        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (StringUtils.hasText(dataSourceProperties.getName())) {
            dataSource.setPoolName(dataSourceProperties.getName());
        }

        return new DataSourceProxy(dataSource);
    }

}
```



```
大事务
@GlobalTransactional
调用其他微服务的小事务
@Transactional
```
