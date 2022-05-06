

## 通用语句分类

**DQL** **Data Query Language**

**DML** **Data Manipulation Language**

**DDL** **Data definition language**

**TCL** **Transaction Control Language**

**DCL** **Data control language**

## 导入数据

**注**

**不区分大小写**

**\c结束一条语句**

**exit 退出mysql**

### 1**登录**

mysql -uroot -p

密码前六位

### 2**查看数据库**

show databases; (mysql命令)

**+--------------------+**
**| Database           |**
**+--------------------+**
**| information_schema |**
**| mysql              |**
**| performance_schema |**
**| sys                |**
**+--------------------+**

### 3**创建自己的数据库**

create database wtq12138;(mysql命令)

### 4**使用自己的数据库**

use wtq12138;(mysql命令)

### 5**查看当前数据库的表**

show tables;(mysql命令)

### 6**执行sql脚本**

source 把文件拖进来

### 7**删库**

 drop database wtq;

### 8**查看表结构**

**+---------------+**
**| Tables_in_wtq |**
**+---------------+**
**| dept          |**       部门表
**| emp           |**      员工表
**| salgrade      | **  等级表
**+---------------+**

**mysql> desc dept;**
**+--------+-------------+------+-----+---------+-------+**
**| Field  | Type        | Null | Key | Default | Extra |**
**+--------+-------------+------+-----+---------+-------+**
**| DEPTNO | int         | NO   | PRI | NULL    |       |**                      编号
**| DNAME  | varchar(14) | YES  |     | NULL    |       | **                 名称
**| LOC    | varchar(13) | YES  |     | NULL    |       | **                     位置
**+--------+-------------+------+-----+---------+-------+**

**mysql> desc emp;**
**+----------+-------------+------+-----+---------+-------+**
**| Field    | Type        | Null | Key | Default | Extra |**
**+----------+-------------+------+-----+---------+-------+**
**| EMPNO    | int         | NO   | PRI | NULL    |       |**              员工编号
**| ENAME    | varchar(10) | YES  |     | NULL    |       | **         名称
**| JOB      | varchar(9)  | YES  |     | NULL    |       |**                工作
**| MGR      | int         | YES  |     | NULL    |       | **                    上级领导编号
**| HIREDATE | date        | YES  |     | NULL    |       | **             入职日期
**| SAL      | double(7,2) | YES  |     | NULL    |       | **               月薪
**| COMM     | double(7,2) | YES  |     | NULL    |       |**           补助
**| DEPTNO   | int         | YES  |     | NULL    |       |**                  部门编号
**+----------+-------------+------+-----+---------+-------+**

****

**mysql> desc salgrade;**
**+-------+------+------+-----+---------+-------+**
**| Field | Type | Null | Key | Default | Extra |**
**+-------+------+------+-----+---------+-------+**
**| GRADE | int  | YES  |     | NULL    |       |**
**| LOSAL | int  | YES  |     | NULL    |       |**
**| HISAL | int  | YES  |     | NULL    |       |**
**+-------+------+------+-----+---------+-------+**

## DQL语句

### 查询

select 字段名1 ,字段名2 ,字段名3...... from 表名;

字段可以参与数学运算并且可以为新量更改名字（别名如果中文用**单引号**）

select SAL*12 as YearSAL from emp;

### 全部查询 

效率较低一般不会正式用于开发

select * from 表名 ;

### 条件查询 

select 字段名1 ,字段名2 ,字段名3...... from 表名 where 字段+条件;

条件符号有

-------------------------

**between and**  **闭区间**  数字和字符串都可以用 **（字符串左闭右开但用的很少)**

**is null** 判空   (null不是一个值 就是空 不等于0)

**is not null**    判空只能用 is 只有值可以用 =

**and**  and优先级大于or 不行就用（）

**or**

**in**  类似于集合属于 也可以用多个or替换 in( 集合内容a,b,c,d)  不是区间是**具体值**

**not in**  不属于 

**like**  **模糊查询** like '%a%'任意有一个a的字符 ‘_a%'任意第二个为a的字符 

select ename from emp e where e.ename like '%A%';

%代表任意多字符 _代表1个字符

查询有下划线和百分号的   用转义字符\ 

--------------------------

### 排序

select 字段名 from 表名 order by 字段名 排序方法 

排序方法 asc 升序 desc 降序 

排序方法  用,并列 只有当靠前排列字段相等时 再依靠后面的排序方法排序 

### 分组函数

注

1一般与group by 一块使用 且一定在group by 后执行

2分组函数不可直接用于where条件查询  原因是**group by 在where条件语句结束后执行**

eg 查询大于平均工资的员工

错误写法 select ename from emp where sal>ave(sal);

正确写法 **select ename from emp where sal>(select avg(sal) from emp);** 其实是**子查询**

-------------------------

**count**

ps：

count(*):记录条数

count(字段):具体字段的不为null的总数

**sum** 

**avg**

**max**

**min**

------------

### 多行处理函数和单行处理函数的差别

多行处理函数 输入多行 输出一行  如分组函数 给出多组数据求出一种结果

**自动忽略NULL**

单行处理函数 输入一行 输出一行  如查询

**不会忽略NULL**

除分组函数外其他函数中数据和null运算后 结果都是null

**ifnull函数**

语法 ifnull(可能为null的数据,如果为null被当成的数据)

eg1

select sal*12+comm as yearsal from emp;

为单行处理函数由于comm中有null则部分结果运算为null

**select sal*12+ifnull(comm,0) as yearsal from emp;**

可以用ifnull函数来解决

eg2

而 select sum (comm) from emp;

为多行处理函数所以忽略NULL

### 分组

**group by**

选出每个职位中的最高薪资

select job,max(sal) from emp group by job;

注

1**多字段分组** 每个部门中每个职位最高薪资select deptno,job,max(sal) from emp group by deptno ,job;

2select后只能出现参加分组的字段以及分组函数 不能 **出现其他字段** 如ename 出现无意义会随机选取，

**having**

分组后进行过滤

选出每个部门中的最高薪资，且显示大于2900的薪资

两种选择 where和having  且显然由于执行顺序 where的效率要大于having

 1select max(sal) from emp where sal>2900 group by  deptno; 

2select max(sal) from emp group by deptno having max(sal) >2900;	

### 执行顺序

select ..             5

from..                1

join on              1.5

where ..            2

group by..         3

having..             4

order by..          6

limit                    7

 1）from子句组装来自不同数据源的数据；

  2）使用on进行join连接的数据筛选

  3）where子句基于指定的条件对记录行进行筛选；

  4）group by子句将数据划分为多个分组；

  5）cube， rollup

  6）使用聚集函数进行计算；

  7）使用having子句筛选分组；

  8）计算所有的表达式；

  9）计算select的字段；

  10）使用distinct 进行数据去重

  11）使用order by对结果集进行排序。

  12）选择TOPN的数据

### 查询结果去重

selcet distinct  字段 from emp;

如果字段数大于1 则distinct放在最前面 表示 联合去

重

注 distinct只能放在最前面

### 连接查询

根据年份 

sql 92

sql 99

根据表的连接方式来划分 包括

#### **内连接**

##### 等值连接

特点是**等量关系**

92语法

 select

 e.ename,d.dname

 from emp e,dept d 

where e.deptno=d.deptno

99语法 优越性在于将where条件查询和等值连接剥离更有可读性

select

e.ename,d.dname

from emp e

(**inner**)join dept d 可省略inner加上可读性好

on e.deptno=d.deptno

where...

##### 非等值连接

特点是**不等量关系**

 select e.ename,e.sal,s.grade from emp e join salgrade s on e.sal between s.losal and s.hisal;

##### 自连接

特点是一张表当两张表，自己连自己

select a.ename as '员工', b.ename as'领导' from emp a join emp b on a.mgr=b.empno;

#### **外连接**

select A.xxx,B.xxx from A  left/right  join B on A.xxx=B.xxx;

其中 from 挨着的A是左表

join 挨着的B是右表

##### 左连接

表示左边是主表

select a.ename,b.ename from emp a left  join emp b on a.mgr=b.empno;

##### 右连接

表示右边是主表

select a.ename,b.ename from emp b right join emp a on a.mgr=b.empno;

#### 内连接和外连接的区别

内连接：

AB两张表是平等的，查询出两张表可以匹配上的记录 这就是内连接。

外连接：

AB两张表一张主表一张副表，主要查询主表，如果副表中的数据未能与主表匹配则自动模拟出null与之匹配

eg找出所有员工的领导

内连接 CEO没有领导查不出来

外连接  CEO没有领导匹配出NULL

**笛卡尔积现象** ：当两张表进行连接查询时，没有任何条件限制，最终的查询结果是两张表的乘积

**关于表的别名**

eg：select e.ename,d.dname from emp e,dept d;

**如何避免笛卡尔积现象？**

加条件过滤 故匹配次数依然为两张表乘积 只不过显示的是有效记录

 select e.ename,d.dname from emp e,dept d where e.deptno=d.deptno; **sql92 已弃用**

#### 多张表连接

```mysql
 select  
	e.ename,d.dname, s.grade,e1.ename
 from 
	emp  e
 join  
	dept d
 on  
	e.deptno=d.deptno
 join 
	salgrade s
 on  
	e.sal between s.losal and s.hisal 
left join 
	emp e1  
on 
     e.mgr=e1.empno;
```



### 子查询

#### where 后子查询

 select ename from emp where sal>(select avg(sal) from emp);

#### from后子查询

**查询部门平均薪资的等级水平**

**思路** 先查出来平均薪资 再用查出来的这张表中去和等级水平表连接

```mysql
select
	 t.*,s.grade 
from
	 (select deptno,avg(sal) as avgsal from emp group by deptno) t 
join 
	salgrade s 
on
	 t.avgsal between s.losal and s.hisal;
```

**查询部门薪资的平均等级水平**            

**思路** 直接拿emp 表去和等级水平表连接 然后可以直接分组求出平均等级水平                            

```mysql
select 
	e.deptno,avg(s.grade) 
from 
	emp e
 join
 	salgrade s 
on 
	e.sal between s.losal and s.hisal 
group by 
	e.deptno
	
```

#### select 后子查询

**查询每个员工所在的部门名称**

不用连接查询的话

 正常查出员工名字 然后 从dept查出部门名并起别名  

```mysql
select 
	e.ename,
(select d.dname from dept d where e.deptno=d.deptno) as dname 
from 
	emp e;
```

### union

找出工作岗位是salsman和manger的员工

1select e.ename,job from emp e where job='manager'or job='salesman';

2select e.ename,job from emp e where job in('manager','salesman');

**好处是可以拼接无关的表**

**要求是字段名数量要相等 拼接两列就必须都要查两列 但是字段名取的是第一行查出来的**

3select ename,job from emp  where job='manager' 

union 

select ename,job from emp  where job='salesman'; 

### **limit

mysql特有

取结果集中的部分数据

语法

**limit startIndex，length**

起始位置 长度



取工资前五名的员工

select e.ename,e.job,e.sal from emp e order by  sal desc limit 5; 不写startIndex 缺省为0

select e.ename,e.job,e.sal from emp e order by  sal desc limit 0,5;

取工资4到9的员工

select e.ename,e.job,e.sal from emp e order by  sal desc limit 3,6;

### 通用的标准分页sql

每页显示三条记录

limit 语句

第1页 0       3 

第2页 3      3 

第3页 6       3 

第4页 9       3 

每页显示pagesize记录

第pagenumber页  pagesize*（pagenumber-1） pagesize

### **concat()**

1、功能：将多个字符串连接成一个字符串。

2、语法：concat(str1, str2,...)

返回结果为连接参数产生的字符串，如果有任何一个参数为null，则返回值为null。

### group_concat

将group by产生的同一个分组中的值连接起来，返回一个字符串结果。	

## DML语句

### 插入数据

语法1

insert into 表名		 (字段名,字段名。。。。。) 		values(值1，值2.。。。。。);

字段和值数量要求相等且前后要对应

字段名顺序不做要求 

insert into t_student(no,name,sex,classno,birth) values(1,'wtq','男',1,'2001-06-01');

insert into t_student( name) values('wtq');缺省的字段名默认为NULL或建表时已确定的缺省值

插入数据缺省的null值之后无法再进行插入修改 只能update修改

语法2

insert into 表名  values(值1，值2.。。。。。);

数量和顺序均要与字段相符

语法3

insert into 表名(字段名,字段名。。。。。) values(值1，值2.。。。。。)，(值1，值2.。。。。。);

插入多行数据

### 修改数据

语法

update	表名	set	字段名1=值1，字段名2=值2.。。。where条件;    **,不要写成and** 因为and相当于逻辑运算符 会使得

SET age = (1 and address = '6') 变成这样

不加条件 修改全部

### 删除数据

delete from 表名 where 条件;

可恢复效率慢

不加条件 删除全部

删除大表 不可恢复

truncate table t_student;

## DDL语句

### 创建表

**语法**

create 表名 (

字段名1 数据类型,

字段名2 数据类型,

...........

);

关于常见数据类型

int		整型

bigint 		长整型

float		浮点型

char 		定长字符串  效率高适用于性别 生日等

varcahr		可变长字符串  适用简介 姓名等等

date		日期类型

BLOB		二进制大对象（图片，视频等流媒体信息） Binary Large Object

CLOB		字符大对象  (较大文本 比如4个G的字符串)  Character Large Object

InnoDB存储引擎的表索引的前缀长度最长是767字节(bytes) 你如果需要建索引,就不能超过

767 bytes;utf8编码时 255*3=765bytes ,恰恰是能建索引情况下的最大值。

```mysql
create table t_student (         表名建议以t_ tbl_开始

no bigint, 

name varchar(255) ,

sex char(1),  default '男'  缺省时为男

classno varchar(255),

birth char (10)

);

```

### 删除表

drop table if exists t_student;

### 表的复制

将查询结果创建新表

语法

create table 表名 as select 语句；

将查询结果插入到一张表中

语法

insert into 表名 select 语句;

## 约束

约束是创建表的时候为字段添加的条件 以此保证表中数据的合法性,有效性,完整性。

非空约束 **not null**

唯一约束 **unique**

主键约束 **primary key**  满足非空和唯一

外键约束 **foreign key**

检查约束check **mysql不支持**

### 非空约束

```mysql
create table t_student(

id int,

username varchar(255)  not null,

password varchar(255)

);

insert into t_student(id,password) values(1,123);报错因为缺省值为null

```

### 唯一约束

不能重复，但是可以为null 且多个null不等于重复

```mysql
create table t_student(

id int,

username varchar(255) unique,

password varchar(255)

);

```

**多个字段联合unique写法**

```mysql
create table t_student(

id int,

username varchar(255) ,

password varchar(255),

unique(username,password)相当于把括号中的东西当成一整个数据 ，只有每个都相同的时候才不满足unique

);表级约束

```

```mysql
create table t_student(

id int,

username varchar(255) unique,

password varchar(255) unique

);列级约束

```

### 主键约束

```mysql
create table t_student(

id int,

username varchar(255)  primary key,列级约束

password varchar(255)

primary key(username)  表级约束

);

```

**相关定义**

**主键约束 ** primary key

**主键字段** 添加主键约束后的字段

**主键值**  主键字段的每个值

#### 主键的作用

主键值是这行记录在这张表中的唯一标识。

#### 主键的分类

1根据主键字段的字段数量来划分

单一主键

复合主键 **即用表级约束多字段联合添加一个主键且不能用列级约束添加多字段主键因为一张表只能有一个主键**（不建议用）

2根据主键性质来划分

自然主键

业务主键  如银行卡卡号作为主键，身份证证号作为主键（不建议用）因为业务改变也许主键值也会变化但是与主键性质冲突

#### 主键值自增

```mysql
create table t_student(

id int primary key auto_increment,

username varchar(255) ,

password varchar(255)

);

```

### 外键约束

**相关定义**

**外键约束 ** foreign key

**外键字段** 添加外键约束后的字段

**外键值**  外键字段的每个值

子表

**+-------+--------+-----------+------+------------+---------+---------+--------+**
**| EMPNO （pk)| ENAME  | DEPTNO(fk) |**
**+-------+--------+-----------+------+------------+---------+---------+--------+**
**|  7369 | SMITH  |     20 |**
**|  7499 | ALLEN  |    30 |**
**|  7782 | CLARK  |   10 |**
**+-------+--------+-----------+------+------------+---------+---------+--------+**

父表 

**+--------+------------+----------+**
**| DNO（pk） | DNAME      | LOC      |**
**+--------+------------+----------+**
**|     10 | ACCOUNTING | NEW YORK |**
**|     20 | RESEARCH   | DALLAS   |**
**|     30 | SALES      | CHICAGO  |**
**|     40 | OPERATIONS | BOSTON   |**
**+--------+------------+----------+**

通过实例来理解外键

因为两张表要以deptno来连接故必须这个值的范围不能随意

即**子表的外键值必须由父表中的主键值中选择 （不一定非要主键，唯一约束也可以，但一定要满足唯一性）**

删除数据，表的时候，先弄子表，再弄父表

添加数据，创建表的时候，先弄父表，再弄子表

```mysql
 create table t_dept(

dname varchar(255),

loc varchar(255),

dno int primary key

);

create table  t_emp(

empno int, 

ename varchar(255),

deptno int ,

foreign key(deptno)references t_dept(dno) 

);

```



### 表结构修改 靠工具0 0

## 存储引擎

```mysql
create table t_hh (

id int 

)ENGINE=InnoDB DEFAULT CHARSET=utf8;

```

完整建表方式

如果不加ENGINE=InnoDB DEFAULT CHARSET=utf8;会默认这种方式

### MyISAM存储引擎

优点1使用三个文件管理每个表

格式文件  

数据文件

索引文件

2灵活的auto_increment字段处理

3可被转换为压缩只读表来节省空间

缺点不支持事务

### InnoDB存储引擎

优点支持事务，行级锁，外键等，这种存储引擎数据的安全得到保障。

还支持级联删除和级联更新

缺点无法被压缩，无法转换成只读。提供自动恢复机制

### MEMORY存储引擎

优点 查询速度最快

缺点不支持事务，数据容易丢失，因为所有数据和索引都存储在内存中。

## 事务

### 语句

```mysql
start transaction

rollback

commit

set global transaction isolation level read uncommitted/read committed/repeatable read/serializable

```

### 事务的定义

一个事务是一个完整的业务逻辑单元，不可再分。

比如银行转账，有增就会有减。

事务需要保证操作数据时同时成功，同时失败，故需要事务来保证安全性

因为事务是保证数据的安全性故只有dml语句和数据相关

### 事务的原理

开启事务机制后 执行dml语句并不会直接执行，而是做为历史记录保存，直到事务结束。

不出错的话提交事务，否则回滚事务。

### 事务的特性ACID

A原子性：事务是最小的工作单元，不可再分

C一致性:  同时成功，同时失败

I隔离性：事务A与事务B之间具有隔离

D持久性：持久性说的是最终数据必须持久化到硬盘文件中，才算成功

### 事务隔离性四个级别

一般二档起步0 0

第一级别：读未提交（read uncommitted）

对方事务还没有提交，我们当前事务可以读到对方未提交的事务

存在dirty read现象 该数据不稳定·

解释：只要对方修改，哪怕不提交，我方只要读，每次结果都是不一样的

第二级别： 读已提交（read committed）

对方事务已提交后的数据我方可以读到

读已提交的问题是	不可重复读：

解释：只要对方修改并且提交，我方只要读，每次结果也不一样

第三级别： 可重复读（repeatable read）

我方读到的数据是备份好的每次都相同，在此事务提交前期间对方修改数据提交事务会影响我方数据，

但是由于显示备份数据，可以重复读取。

可重复读的问题是	读到的数据是幻象。

解释：我方不管怎么读取都是已经备份好的数据，但是如果对方进行插入操作并提交的话，我方再进行相同插入操作时就会发生冲突

第四级别：序列化读/串行化读（serializable）

解决所有问题但是效率低需要事务排队

### 事务的传播行为

1、**PROPAGATION_ REQUIRED**: 如果当前没有事务，就创建一个新事务，如果当前存在事务, 
就加入该事务，该设置是最常用的设置。
2、**PROPAGATION_ SUPPORTS**: 支持当前事务，如果当前存在事务，就加入该事务，如果当
前不存在事务，就以非事务执行。
3、**ROPAGATION_ _MANDATORY**: 支持当前事务，如果当前存在事务，就加入该事务，如果
当前不存在事务，就抛出异常。
4、**PROPAGATION_ REQUIRES _NEW**:创建新事务，无论当前存不存在事务，都创建新事务。
5、**PROPAGATION_ _NOT_ SUPPORTED**:以非事务方式执行操作，如果当前存在事务，就把当
前事务挂起。
6、**ROPAGATION_ NEVER**:以非事务方式执行，如果当前存在事务，则抛出异常。
7、**PROPAGATION NESTED**:如果当前存在事务，则在嵌套事务内执行。如果当前没有事务，
则执行与PROPAGATION_ REQUIRED 类似的操作。

spring中 同一个类中A事务调用B事务，而B事务注解不生效的原因

事务采用aop代理实现，而同一个类绕过了代理对象

### 坑

## 索引

### 索引的定义和作用

索引相当于一本书的目录，通过目录可以快速的找到对应的资源，

在数据库方面查询一张表有两张方式

1全表扫描

2索引检索

索引的本质是缩小了扫描的范围

索引虽然可以提高检索效率，但不能随意添加索引，因为有维护成本，如果数据经常修改的话，索引需要重新排序，进行维护

添加索引是为某些字段添加索引

### 索引添加和删除的语法

创建索引对象

create index 索引名称 on 表名 (字段名);

删除索引对象

drop index 索引名称 on 表名 (字段名);

### 索引的分类

单一索引 单一字段

复合索引 多个字段联合添加索引

主键索引  主键上自动添加索引

唯一索引  有unique约束的字段自动添加索引

。。。

### 何时添加索引

数据量庞大，很少出现DML语句，经常出现在where子句中

**主键和unique约束的字段会自动添加索引,效率较高尽量根据主键查询**

### 索引什么时候失效





### ![索引实现原理](F:\资料\网课\动力节点\动力节点mysql\document\document\索引实现原理.jpg)

## 视图

### 视图的定义和作用

站在不同角度去看数据 **同一张表的数据通过不同角度看待**

相当于表中截出了一个窗口 可以通过修改窗口中的数据来修改表

作用是隐藏表的实现细节，保密性很高

### 视图创建和删除的语法

create view 视图名称 as select 语句

drop view 视图名称 ;

## DBA命令

### 导出数据库

mysqldump wtq(数据库名称)>C:\Users\雷神\Desktop\瞎搞\mysql\wtq.sql（路径及名称） -uroot -password

### 导入数据库

create database wtq;

use wtq;

source sql文件路径(拖进来)

## 数据库设计三范式

引入前置知识

**完全函数依赖**

在一张表中，若 X → Y，且对于 X 的任何一个[真子集](https://www.zhihu.com/search?q=真子集&search_source=Entity&hybrid_search_source=Entity&hybrid_search_extra={"sourceType"%3A"answer"%2C"sourceId"%3A29189700})（假如属性组 X 包含超过一个属性的话），X ' → Y 不成立，那么我们称 Y 对于 X **完全函数依赖**，记作 X F→ Y。（那个F应该写在箭头的正上方，没办法打出来……，正确的写法如**图1**）

![img](https://pic3.zhimg.com/50/12513de20079d12b99d946072df7311a_720w.jpg?source=1940ef5c)

例如： 

- 学号 F→ 姓名 
- （学号，课名） F→ 分数  （注：因为同一个的学号对应的分数不确定，同一个课名对应的分数也不确定）

**部分函数依赖**

假如 Y 函数依赖于 X，但同时 Y 并不完全函数依赖于 X，那么我们就称 Y 部分函数依赖于 X，记作 X  P→ Y，如**图2**。

![img](https://pic1.zhimg.com/50/10b52b39b18b8ea9fb17b46babf4d20f_720w.jpg?source=1940ef5c)



例如：

- （学号，[课名](https://www.zhihu.com/search?q=课名&search_source=Entity&hybrid_search_source=Entity&hybrid_search_extra={"sourceType"%3A"answer"%2C"sourceId"%3A29189700})） P→ 姓名 

**传递函数依赖**
假如 Z 函数依赖于 Y，且 Y 函数依赖于 X 

 指出的错误，这里改为：『Y 不包含于 X，且 X 不函数依赖于 Y』这个前提），那么我们就称 Z 传递函数依赖于 X ，记作 X T→ Z，如**图3**。



![img](https://pica.zhimg.com/50/51f8105fbbe92adaa3e343ea2db3bf49_720w.jpg?source=1940ef5c)



图3

**官方定义**

**1NF字段不可分**

**2NF 不存在非主属性对码的部分函数依赖 即存在一部分主属性可以推出自有的相关属性**

**3NF 不存在非主属性对码的传递函数依赖 即存在一部分主属性可推出非主属性，非主属性又可推出另一个非主属性 说明两个非主属性间互相依赖 注意如果传递依赖是 一部分主属性可推出另一个主属性，另一个主属性又可推出非主属性 这样就会不满足二范式**

**bc范式**

**设关系模式*R*<*U*,*F*>∈1NF，若*X* →*Y*且*Y* ⊆ *X*时*X*必含有码，则*R*<*U*,*F*>∈BCNF。即左边一定有候选码**

**通俗理解**

**1NF:字段不可分;** 
**2NF:有主键，非主键字段依赖主键;** 
**3NF:非主键字段不能相互依赖;** 



**解释:** 
**1NF:原子性 字段不可再分,否则就不是关系数据库;** 
**2NF:唯一性 一个表只说明一个事物;** 
**3NF:每列都与主键有直接关系，不存在传递依赖;** 

### 第一范式

任何一张表都应该有主键，并且每个字段原子性不可再分

### 第二范式

建立在第一范式之上要求所有非主键字段完全依赖主键，不能产生部分依赖（不要复合主键）

多对多？三张表，关系表两个外键。

			t_student学生表
			sno(pk)		sname
			-------------------
			1				张三
			2				李四
			3				王五
	
			t_teacher 讲师表
			tno(pk)		tname
			---------------------
			1				王老师
			2				张老师
			3				李老师
	
			t_student_teacher_relation 学生讲师关系表
			id(pk)		sno(fk)		tno(fk)
			----------------------------------
			1				1				3
			2				1				1
			3				2				2
			4				2				3
			5				3				1
			6				3				3
### 第三范式

建立在第二范式之上要求所有非主键字段直接依赖主键，不能产生传递依赖

一对多？两张表，多的表加外键。

			班级t_class
				cno(pk)				cname
	
			---------------------------------------------
	
				1					班级1
				2					班级2
	
			学生t_student
			sno(pk)			sname				classno(fk)
			---------------------------------------------
			101				张1				1
			102				张2				1
			103				张3				2
			104				张4				2
			105				张5				2
在实际的开发中，以满足客户的需求为主，有的时候会拿冗余换执行速度。

### 1对1设计

一对一设计有两种方案：主键共享

```
			t_user_login  用户登录表
			id(pk)		username			password
			--------------------------------------
			1				zs					123
			2				ls					456

			t_user_detail 用户详细信息表
			id(pk+fk)	realname			tel			....
			------------------------------------------------
			1				张三				1111111111
			2				李四				1111415621
```

一对一设计有两种方案：外键唯一。

```
			t_user_login  用户登录表
			id(pk)		username			password
			--------------------------------------
			1				zs					123
			2				ls					456

			t_user_detail 用户详细信息表
			id(pk)	   realname			tel				userid(fk+unique)....
			-----------------------------------------------------------
			1				张三				1111111111		2
			2				李四				1111415621		1
```



## 习题

取得每个部门最高薪水的名称

```mysql
select  

e.ename,t.* 

from  emp e 

join (select deptno, max(sal) as maxsal from emp group by deptno) t 

on e.deptno=t.deptno and e.sal=t.maxsal;
+-------+--------+---------+
| ename | deptno | maxsal  |
+-------+--------+---------+
| BLAKE |     30 | 2850.00 |
| SCOTT |     20 | 3000.00 |
| KING  |     10 | 5000.00 |
| FORD  |     20 | 3000.00 |
+-------+--------+---------+

```

2哪些人的薪水在部门的平均薪水之上

```mysql
select t.*,e.ename,e.sal  from emp e
join (select deptno,avg(e.sal) as avgsal from emp e group by deptno ) t
on e.deptno =t.deptno and e.sal>t.avgsal;
+--------+-------------+-------+---------+
| deptno | avgsal      | ename | sal     |
+--------+-------------+-------+---------+
|     30 | 1566.666667 | ALLEN | 1600.00 |
|     20 | 2175.000000 | JONES | 2975.00 |
|     30 | 1566.666667 | BLAKE | 2850.00 |
|     20 | 2175.000000 | SCOTT | 3000.00 |
|     10 | 2916.666667 | KING  | 5000.00 |
|     20 | 2175.000000 | FORD  | 3000.00 |
+--------+-------------+-------+---------+
```

3取得部门中（所有人的）平均的薪水等级

```mysql
可以连接查询部门及薪资后不要再把这个表当临时表，而是可以直接分组求avg
select e.deptno,avg(s.grade) as avggrade 
from emp e
join salgrade s
on e.sal between s.losal and s.hisal
group by deptno;
+--------+----------+
| deptno | avggrade |
+--------+----------+
|     20 |   2.8000 |
|     30 |   2.5000 |
|     10 |   3.6667 |
+--------+----------+
```

4取得部门平均薪水的薪水等级

```mysql
想要知道部门的平均薪水就必须先分组查询
查询出来的表只能当作临时表和salgrade连接查询
select t.deptno,s.grade
from (select e.deptno,avg(sal) as avgsal from emp e group by deptno) t
join salgrade s
on t.avgsal between s.losal and s.hisal;
+--------+-------+
| deptno | grade |
+--------+-------+
|     20 |     4 |
|     30 |     3 |
|     10 |     4 |
+--------+-------+
```

5不准用组函数（Max），取得最高薪水

```mysql
1select e.ename,e.sal from emp e order by e.sal desc limit 1;
2select max(e.sal) from emp e ;
3select sal from emp where sal not in(select distinct a.sal from emp a join emp b on a.sal<b.sal);
+-------+---------+
| ename | sal     |
+-------+---------+
| KING  | 5000.00 |
+-------+---------+
```

6取得平均薪水最高的部门的部门编号

```mysql
找出平均薪资和部门降序limit
1select t.deptno,t.avgsal from (select e.deptno,avg(sal) as avgsal from emp e group by deptno) t order by avgsal desc limit 1;	
2
select max(t.avgsal) maxsal from (select e.deptno,avg(sal) as avgsal from emp e group by deptno) t;
+-------------+
| maxsal      |
+-------------+
| 2916.666667 |
+-------------+
select e.deptno,avg(e.sal) as avgsal from emp e group by e.deptno; 过滤
+--------+-------------+
| deptno | avgsal      |
+--------+-------------+
|     20 | 2175.000000 |
|     30 | 1566.666667 |
|     10 | 2916.666667 |
+--------+-------------+
select e.deptno,avg(e.sal) as avgsal from emp e group by e.deptno
having avgsal=
(select max(t.avgsal) as maxsal from (select e.deptno,avg(sal) as avgsal from emp e group by deptno) t);
+--------+-------------+
| deptno | avgsal      |
+--------+-------------+
|     10 | 2916.666667 |
+--------+-------------+
```

7取得平均薪水最高的部门的部门名称

```mysql
select d.dname,avg(e.sal) as avgsal from emp e join dept d on d.deptno=e.deptno group by d.dname order by avgsal desc limit 1;
+------------+-------------+
| dname      | avgsal      |.
+------------+-------------+
| ACCOUNTING | 2916.666667 |
+------------+-------------+
```

8求薪水等级的平均值最低的部门的部门名称

```mysql
select t.dname 
from(连接查询找到薪水等级顺势分组得到平均值 作临时表
select e.deptno,d.dname,avg(s.grade) as av
from  emp e 
join dept d
on d.deptno=e.deptno
join salgrade s
on e.sal  between s.losal and s.hisal
group by e.deptno) t
order by av asc limit 0,1;

```

9取得比普通员工(员工代码没有在mgr字段上出现的)的最高薪水还要高的领导人姓名

```mysql
找到mgr员工
select distinct a.mgr,a from emp a where is not null;
找到普通员工最高薪水	
select max(e.sal) as maxsal from emp e where e.empno not in(select distinct a.mgr from emp a where a.mgr is not null);
找到mgr员工大于普通员工最高薪水
```

