# spring Security

## 原理

本质是一个filter链

```
org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter
org.springframework.security.web.context.SecurityContextPersistenceFilter
org.springframework.security.web.header.HeaderWriterFilter
org.springframework.security.web.csrf.CsrfFilter
org.springframework.security.web.authentication.logout.LogoutFilter
org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter
org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter
org.springframework.security.web.savedrequest.RequestCacheAwareFilter
org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter
org.springframework.security.web.authentication.AnonymousAuthenticationFilter
org.springframework.security.web.session.SessionManagementFilter
org.springframework.security.web.access.ExceptionTranslationFilter
org.springframework.security.web.access.intercept.FilterSecurityInterceptor
```

代码底层流程：

**三个filter**：

FilterSecurityInterceptor：是一个方法级的权限过滤器, 基本位于过滤链的最底部，用于判断前面filter是否通过，以及调用真正的后台服务。

ExceptionTranslationFilter：是个异常过滤器，用来处理在认证授权过程中抛出的异常

UsernamePasswordAuthenticationFilter ：对/login的POST请求做拦截，校验表单中用户名，密码。

**两个Interface**

PasswordEncoder

用来加密解密密码的接口类，可以直接用现成的BCryptPasswordEncoder

UserDetailsService

实现此接口，重写自己的逻辑，在数据库中查出用户名密码用于验证身份以及权限

## 配置账号密码的方法

1. 在application.properties

```java
spring.security.user.name=atguigu 
spring.security.user.password=atguigu
```

2. 配置重写方法  configure(AuthenticationManagerBuilder auth)

```java
@Configuration
public class SecurityConfigTest extends WebSecurityConfigurerAdapter {
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(password());
    }
    @Bean
    PasswordEncoder password() {
        return new BCryptPasswordEncoder();
    }

    }
}
@Service("userDetailsService")
public class MyUserDetailsService implements UserDetailsService  {

    @Autowired
    private UsersMapper usersMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //调用usersMapper方法，根据用户名查询数据库
        QueryWrapper<Users> wrapper = new QueryWrapper();
        // where username=?
        wrapper.eq("username",username);
        Users users = usersMapper.selectOne(wrapper);
        //判断
        if(users == null) {//数据库没有用户名，认证失败
            throw  new UsernameNotFoundException("用户名不存在！");
        }
        List<GrantedAuthority> auths =
                AuthorityUtils.commaSeparatedStringToAuthorityList("admin,ROLE_sale");
        //从查询数据库返回users对象，得到用户名和密码，返回
        return new User(users.getUsername(),
                new BCryptPasswordEncoder().encode(users.getPassword()),auths);
    }
}
```

3. 配置重写方法 configure(HttpSecurity http)

```java
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //退出
        http.logout().logoutUrl("/logout").
                logoutSuccessUrl("/test/hello").permitAll();

        //配置没有权限访问跳转自定义页面
        http.exceptionHandling().accessDeniedPage("/unauth.html");
        http.formLogin()   //自定义自己编写的登录页面
            .loginPage("/login.html")  //登录页面设置
            .loginProcessingUrl("/user/login")   //登录访问路径
            .defaultSuccessUrl("/success.html").permitAll()  //登录成功之后，跳转路径
                .failureUrl("/unauth.html")
            .and().authorizeRequests()
                .antMatchers("/").permitAll() //设置哪些路径可以直接访问，不需要认证
                //当前登录用户，只有具有admins权限才可以访问这个路径
                //1 hasAuthority方法
               // .antMatchers("/test/index").hasAuthority("admins")
                //2 hasAnyAuthority方法
                .antMatchers("/test/index").hasAnyAuthority("admin,manager")
                //3 hasRole方法   ROLE_sale
//                .antMatchers("/test/index").hasRole("sale")
                .anyRequest().authenticated()
//                .and().rememberMe().tokenRepository(persistentTokenRepository())
//                .tokenValiditySeconds(60)//设置有效时长，单位秒
//                .userDetailsService(userDetailsService);
               // .and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
           .and().csrf().disable();  //关闭csrf防护
    }
```

## 角色和权限

![07-web权限方案-基于角色或权限的访问控制](F:\资料\八股复习\冲冲冲\spring全家桶\spring security\images\07-web权限方案-基于角色或权限的访问控制.png)