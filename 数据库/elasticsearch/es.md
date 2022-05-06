# ES

## 基本概念

数据类型

    索引（indices）----------------------Databases 数据库
    
    类型（type）--------------------------Table 数据表 //7.0后被不建议使用 因为一个索引下的数据类型相互影响
    
    文档（Document）----------------------Row 行
    
    字段（Field）-------------------------Columns 列 

**倒排索引**

从id->关键词变为从关键词->id

9200端口

## 常见操作

### 1）_CAT

（1）查看所有节点

```
GET /_cat/nodes
```

（2）查看es健康状况

```
GET /_cat/health
```

（3）查看主节点

```
GET /_cat/master
```

（4）查看所有索引 ，等价于mysql数据库的show databases;

```json
GET /_cat/indicies
```

###  2）C

保存一个数据，保存在哪个索引的哪个类型下，指定用那个唯一标识


```
POST customer/external/1
```

```json
{
 "name":"John Doe"
}
```

PUT和POST都可以

POST新增。如果不指定id，会自动生成id。指定id就会修改这个数据，并新增版本号；
PUT可以新增也可以修改。PUT必须指定id；由于PUT需要指定id，我们一般用来做修改操作，不指定id会报错。

201 created表示成功

```json
{
    "_index": "customer", 表明该数据在哪个数据库下；
    "_type": "external",  表明该数据在哪个类型下；
    "_id": "1",  表明被保存数据的id；
    "_version": 1, 被保存数据的版本
    "result": "created",这里是创建了一条数据，如果重新put一条数据，则该状态会变为updated，并且版本号也会发生变化。
    "_shards": {
        "total": 2,
        "successful": 1,
        "failed": 0
    },
    "_seq_no": 0, 并发控制字段，每次更新都会+1，用来做乐观锁
    "_primary_term": 1 同上，主分片重新分配，如重启，就会变化
}
```

### 3）R

```
GET /customer/external/1
```



```
{
    "_index": "customer",//在哪个索引
    "_type": "external",//在哪个类型
    "_id": "1",//记录id
    "_version": 3,//版本号
    "_seq_no": 6,//并发控制字段，每次更新都会+1，用来做乐观锁
    "_primary_term": 1,//同上，主分片重新分配，如重启，就会变化
    "found": true,
    "_source": {
        "name": "John Doe"
    }
}
```

### 4）U

```
PUT /customer/external/1
```

```
POST /customer/external/1
```

```json
{
    "name": "John"
}
```

同样put和post都可以

如果加POST+_update

```
POST /customer/external/1/_update
```

那么更新时发送的请求体一定是携带json格式的doc

```json
{
    "doc":{
        "name": "John"
    }
}

```

区别：带_update的如果源数据相同则不会对版本号和序列化更新，其他两种会直接更新版本号和序列号

### 5）D

```
DELETE customer/external/1
DELETE customer
```

注：elasticsearch并没有提供删除类型的操作，只提供了删除索引和文档的操作。

### 6) _BULK

语法格式：

```json
{action:{metadata}}\n
{request body  }\n

{action:{metadata}}\n
{request body  }\n
```

这里的批量操作和事务不同，当发生某一条执行发生失败时，其他的数据仍然能够接着执行，也就是说彼此之间是独立的。

bulk api以此按顺序执行所有的action（动作）。如果一个单个的动作因任何原因失败，它将继续处理它后面剩余的动作。当bulk api返回时，它将提供每个动作的状态（与发送的顺序相同），所以您可以检查是否一个指定的动作是否失败了。

实例1: 执行多条数据


```json
POST customer/external/_bulk
{"index":{"_id":"1"}}
{"name":"John Doe"}
{"index":{"_id":"2"}}
{"name":"John Doe"}
```

实例2：对于整个索引执行批量操作

```json
POST /_bulk
{"delete":{"_index":"website","_type":"blog","_id":"123"}}
{"create":{"_index":"website","_type":"blog","_id":"123"}}
{"title":"my first blog post"}
{"index":{"_index":"website","_type":"blog"}}
{"title":"my second blog post"}
{"update":{"_index":"website","_type":"blog","_id":"123"}}
{"doc":{"title":"my updated blog post"}}
```

## 进阶检索DSL

ES支持两种基本方式检索；

* 通过REST request uri 发送搜索参数 （uri +检索参数）；
* 通过REST request body 来发送它们（uri+请求体）；

DSL即第二种的请求体

一个查询语句的典型结构

```json
QUERY_NAME:{
   ARGUMENT:VALUE,
   ARGUMENT:VALUE,...
}
```

如果针对于某个字段，那么它的结构如下：

```json
{
  QUERY_NAME:{
     FIELD_NAME:{
       ARGUMENT:VALUE,
       ARGUMENT:VALUE,...
      }   
   }
}
```

eg

### 基本语法

```json
GET bank/_search
{
  "query": {
    "match_all": {}
  },
  "from": 0,
  "size": 5,
  "sort": [
    {
      "account_number": {
        "order": "desc"
      }
    }
  ]
}
```

> query定义如何查询；
>
> - match_all查询类型【代表查询所有的所有】，es中可以在query中组合非常多的查询类型完成复杂查询；
> - 除了query参数之外，我们可也传递其他的参数以改变查询结果，如sort，size；
> - from+size限定，完成分页功能；
> - sort排序，多字段排序，会在前序字段相等时后续字段内部排序，否则以前序为准；

### match

match 如果是字符串则全文检索分词匹配根据得分排序，否则精确匹配

```
GET bank/_search
{
  "query": {
    "match": {
      "address": "Kings"
    }
  }
}
```

match_phrase [短句匹配] 将给定这个当成一整个词

```
GET bank/_search
{
  "query": {
     "match_phrase": {
      "address": "mill Lane" 
    }
  }
}
```

multi_math【多字段匹配】

```
GET bank/_search
{
  "query": {
    "multi_match": {
      "query": "mill",
      "fields": [
        "state",
        "address"
      ]
    }
  }
}
```

### bool

用来做复合查询

![1](F:\资料\八股复习\冲冲冲\数据库\elasticsearch\images\1.jpg)

should：应该达到should列举的条件，如果到达会增加相关文档的评分，并不会改变查询的结果。如果query中只有should且只有一种匹配规则，那么should的条件就会被作为默认匹配条件二区改变查询结果。

```json
GET bank/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "gender": "M"
          }
        },
        {
          "match": {
            "address": "mill"
          }
        }
      ],
      "must_not": [
        {
          "match": {
            "age": "18"
          }
        }
      ],
      "should": [
        {
          "match": {
            "lastname": "Wallace"
          }
        }
      ]
    }
  }
}

```

### filter

和must_not一样不贡献得分

但是先查出数据之后再过滤，有点像grep

```
GET bank/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "address": "mill"
          }
        }
      ],
      "filter": {
        "range": {
          "balance": {
            "gte": "10000",
            "lte": "20000"
          }
        }
      }
    }
  }
}
```

### term

全文检索字段用match，其他非text字段匹配用term

精确匹配

```
GET bank/_search
{
  "query": {
    "term": {
      "address": "mill Road"
    }
  }
}
```

### range

`range` 查询找出那些落在指定区间内的数字或者时间

```json
GET /atguigu/_search
{
    "query":{
        "range": {
            "price": {
                "gte":  1000,
                "lt":   3000
            }
    	}
    }
}
```

`range`查询允许以下字符：

| 操作符 |   说明   |
| :----: | :------: |
|   gt   |   大于   |
|  gte   | 大于等于 |
|   lt   |   小于   |
|  lte   | 小于等于 |

### sort

`sort` 可以让我们按照不同的字段进行排序，并且通过`order`指定排序的方式

```json
GET /atguigu/_search
{
  "query": {
    "match": {
      "title": "小米手机"
    }
  },
  "sort": [
    {
      "price": { "order": "desc" }
    },
    {
      "_score": { "order": "desc"}
    }
  ]
}
```

### highlight

查看百度高亮的原理：

给关键字添加了<em>标签，在前端再给该标签添加样式即可。

```
GET /atguigu/_search
{
  "query": {
    "match": {
      "title": "小米"
    }
  },
  "highlight": {
    "fields": {"title": {}}, 
    "pre_tags": "<em>",
    "post_tags": "</em>"
  }
}
```

fields：高亮字段

pre_tags：前置标签

post_tags：后置标签

### _source

默认情况下，elasticsearch在搜索的结果中，会把文档中保存在`_source`的所有字段都返回。

如果我们只想获取其中的部分字段，可以添加`_source`的过滤

```json
GET /atguigu/_search
{
  "_source": ["title","price"],
  "query": {
    "term": {
      "price": 2699
    }
  }
}
```

### agg

聚合

```
"aggs":{
    "aggs_name这次聚合的名字，方便展示在结果集中":{
        "AGG_TYPE聚合的类型(avg,term,terms)":{}
     }
}，
```

查出所有年龄分布，并且这些年龄段中M的平均薪资和F的平均薪资以及这个年龄段的总体平均薪资
三层嵌套聚合查询

```json
}
GET bank/_search
{
  "query": {
    "match": {
      "address": "mill Road"
    }
  },
  "aggs": {
    "ageAgg": {
      "terms": {
        "field": "age",
        "size": 10
      },
      "aggs": {
        "genderAgg": {
          "terms": {
            "field": "gender.keyword", //gender.keyword针对text精确匹配
            "size": 10
          },
          "aggs": {
            "balancerAgg": {
              "avg": {
                "field": "balance"
              }
            }
          }
        }
      }
    }
  }
}
```

### mapping

Mapping(映射)
Maping是用来定义一个**文档**（document），以及它所包含的属性（field）是如何存储和索引的。比如：使用maping来定义：

查看文档中的映射

```
GET bank/_mapping
```

创建索引并指定映射

```json
PUT /my_index
{
  "mappings": {
    "properties": {
      "age": {
        "type": "integer"
      },
      "email": {
        "type": "keyword"
      },
      "name": {
        "type": "text"
      }
    }
  }
}
```

添加新的字段映射

```json
PUT /my_index/_mapping
{
  "properties": {
    "employee-id": {
      "type": "keyword",
      "index": false
    }
  }
}
```

这里的 "index": false，表明新增的字段不能被检索，只是一个冗余字段。

更新映射

对于已经存在的字段映射，我们不能更新。更新必须创建新的索引，进行数据迁移。

数据迁移

先创建new_twitter的正确映射。然后使用如下方式进行数据迁移。

```json
POST reindex [固定写法]
{
  "source":{
      "index":"twitter"
   },
  "dest":{
      "index":"new_twitters"
   }
}
```

将旧索引的type下的数据进行迁移

```json
POST reindex [固定写法]
{
  "source":{
      "index":"twitter",
      "twitter":"twitter"
   },
  "dest":{
      "index":"new_twitters"
   }
}
```

### 分词

```
POST _analyze
```

```
POST _analyze
{
  "text": ["我是乔碧萝"]
  , "analyzer": "ik_smart"
}
```

## 整合springboot

依赖

```maven
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.6.2</version>
</dependency>
同时修改项目中的
<properties>
    ...
    <elasticsearch.version>7.6.2</elasticsearch.version>
</properties>
```

增

```java
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest ("users");

        User user = new User();
        user.setUserName("张三");
        user.setAge(20);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        //设置要保存的内容
        indexRequest.source(jsonString, XContentType.JSON);
        //执行创建索引和保存数据
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println(index);

    }
```

查

```java
/**
 * 复杂检索:在bank中搜索address中包含mill的所有人的年龄分布以及平均年龄，平均薪资
 * @throws IOException
 */
@Test
public void searchData() throws IOException {
    //1. 创建检索请求
    SearchRequest searchRequest = new SearchRequest();

    //1.1）指定索引
    searchRequest.indices("bank");
    //1.2）构造检索条件
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.matchQuery("address","Mill"));

    //1.2.1)按照年龄分布进行聚合
    TermsAggregationBuilder ageAgg=AggregationBuilders.terms("ageAgg").field("age").size(10);
    sourceBuilder.aggregation(ageAgg);

    //1.2.2)计算平均年龄
    AvgAggregationBuilder ageAvg = AggregationBuilders.avg("ageAvg").field("age");
    sourceBuilder.aggregation(ageAvg);
    //1.2.3)计算平均薪资
    AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
    sourceBuilder.aggregation(balanceAvg);

    System.out.println("检索条件："+sourceBuilder);
    searchRequest.source(sourceBuilder);
    //2. 执行检索
    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
    System.out.println("检索结果："+searchResponse);

    //3. 将检索结果封装为Bean
    SearchHits hits = searchResponse.getHits();
    SearchHit[] searchHits = hits.getHits();
    for (SearchHit searchHit : searchHits) {
        String sourceAsString = searchHit.getSourceAsString();
        Account account = JSON.parseObject(sourceAsString, Account.class);
        System.out.println(account);

    }

    //4. 获取聚合信息
    Aggregations aggregations = searchResponse.getAggregations();

    Terms ageAgg1 = aggregations.get("ageAgg");

    for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
        String keyAsString = bucket.getKeyAsString();
        System.out.println("年龄："+keyAsString+" ==> "+bucket.getDocCount());
    }
    Avg ageAvg1 = aggregations.get("ageAvg");
    System.out.println("平均年龄："+ageAvg1.getValue());

    Avg balanceAvg1 = aggregations.get("balanceAvg");
    System.out.println("平均薪资："+balanceAvg1.getValue());
}
```

## nested

对象数组的扁平化

对于数组存进去不是按照对象存的，而是按照属性存的	

```json
{
    "user": [
        {username=A,
         pwd=B
            
        },
        {username=c,
         pwd=d  
        }
    ]
}
扁平化
user.username=[A,B]
user.pwd=[C,D]
如果查询"A,D"，也会被检索到
```

如果不想扁平处理，需要修改mapping，即将user属性的type改为nested

nested查询

```json
GET blog/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "nested": {
            "path": "comments",
            "query": {
              "bool": {
                "must": [
                  {
                    "match": {
                      "comments.name": "steve"
                    }
                  },
                  {
                    "match": {
                      "comments.age": 24
                    }
                  }
                ]
              }
            }
          }
        }
      ]
    }
  }
}
```

nested聚合

```json
POST es_latent_buy_brands_frequency/_search
{
  "aggs": {
    "buy_goods_brand": {
      "nested": {
        "path": "嵌入属性名"
      },
      "aggs": {
        "agg_by_buy_goods_brand": {
          "terms": {
            "field": "latent_buy_brand_frequency.name",
            "size": 10
          }
        }
      }
    }
  }
}
```

