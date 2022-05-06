## association和collection

连表查询时，可能出现A对象聚合B对象的情况，是一对一或者多对一的关系

使用association进行结果的封装

```xml
<resultMap type="com.pjf.mybatis.po.Hotel" id="myHotel">
    <!--result 指定非主键的封装规则 column：数据库中列名 property：javaBean的属性名 -->
    <result column="hotel_name" property="hotelName" jdbcType="VARCHAR" />
    <result column="hotel_address" property="hotelAddress"
            jdbcType="VARCHAR" />
    <result column="price" property="price" jdbcType="INTEGER" />

    <!--association 关联的表
               property 指被关联的类成员变量  
               javaType 指被关联的类成员变量的全类名   -->
    <association property="city" javaType="com.pjf.mybatis.po.City">
        <id column="city_code" property="cityCode" jdbcType="INTEGER"/>
        <result column="city_name" property="cityName" jdbcType="VARCHAR"/>
    </association>
</resultMap>
```

而如果出现一对多的情况

使用collection进行封装

```xml
<resultMap id="spuAttrGroup" type="com.wtq12138.gulimall.product.vo.SpuItemAttrGroupVo">
    <result property="groupName" column="attr_group_name"/>
    <collection property="attrs" ofType="com.wtq12138.gulimall.product.vo.Attr">
        <result property="attrId" column="attr_id"></result>
        <result property="attrName" column="attr_name"></result>
        <result property="attrValue" column="attr_value"></result>
    </collection>
</resultMap>
```

## 缓存

1、一级缓存：（本地缓存）SqlSession级别的缓存，默认一直开启的 ，

​         与数据库同一次会话期间的数据会放到本地缓存中，以后如果需要相同的数据，直接从缓存中拿，不再查询数据库。

​         当 Session flush 或 close 之后，该Session中的所有 Cache 就将清空。

　　　　当进行增删改之后，该Session中的所有 Cache 就将清空。

2、二级缓存：基于namespace级别的缓存，一个namespace对应一个二级缓存

　　　　当 Session flush 或 close 之后，二级缓存仍然可用。

　　　　当进行增删改之后，该namespace的所有 Cache 就将清空。
