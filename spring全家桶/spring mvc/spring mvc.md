## @RequestParam@PathVariable@ReuqestBody的区别

1. @RequestParam

```java
http://javam4.com/m4detail?id=111&tag=java
@RequestMapping(value = "/m4detail", method = {RequestMethod.GET,RequestMethod.POST})
public void m4detail(@RequestParam(value="id", required=true) String isId, @RequestParam String tag) {

    System.out.println("isId="+isId);
    System.out.println("tag="+tag);

}
defaultValue 如果本次请求没有携带这个参数，或者参数为空，那么就会启用默认值
name 绑定本次参数的名称，要跟URL上面的一样
required 这个参数不是必须的，如果为 true，不传参数会报错
value 跟name一样的作用，是name属性的一个别名
```

默认获取`application/x-www-form-urlencoded` 以及 `application/json` 这两种类型的参数

2. @PathVariable

```java
http://javam4.com/m4detail/111/java

@RequestMapping(value = "/m4detail/{id}/{tag}", method = {RequestMethod.GET,RequestMethod.POST})
public void m4detail(@PathVariable String id, @PathVariable String tag) {

    System.out.println("id="+id);
    System.out.println("tag="+tag);
}
name 绑定参数的名称，默认不传递时，绑定为同名的形参。 赋值但名称不一致时则报错
value 跟name一样的作用，是name属性的一个别名
required 这个参数不是必须的，如果为 true，不传参数会报错
```

3. @RequestBody

获取请求体接收这个 JSON 数据有两种方式选择，一种是建立与 JSON 数据与之对应的实体，二是直接使用 Map<string,object> 对象接收，只能使用一个 @RequestBody

## 转发和重定向区别

1、请求次数：重定向是浏览器向服务器发送一个请求并收到响应后再次向一个新地址发出请求，转发是服务器收到请求后为了完成响应跳转到一个新的地址；重定向至少请求两次，转发请求一次；

2、地址栏不同：重定向地址栏会发生变化，转发地址栏不会发生变化；

3、是否共享数据：重定向两次请求不共享数据，转发一次请求共享数据（在request级别使用信息共享，使用重定向必然出错）；

4、跳转限制：重定向可以跳转到任意URL，转发只能跳转本站点资源；

5、发生行为不同：重定向是客户端行为，转发是服务器端行为；