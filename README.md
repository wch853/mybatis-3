# MyBatis 源码分析

Based on mybatis-3.5.1

相关源码分析请见 [MyBatis 源码分析](https://wch853.github.io/posts/mybatis/MyBatis%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%80%EF%BC%89%EF%BC%9AMyBatis%E7%AE%80%E4%BB%8B%E5%92%8C%E6%95%B4%E4%BD%93%E6%9E%B6%E6%9E%84.html#mybatis-%E7%AE%80%E4%BB%8B)

参考书籍：徐郡明.MyBatis技术内幕[M].电子工业出版社,2017.

## 基础支持层
### 反射模块
- `org.apache.ibatis.reflection.Reflector`：缓存类的字段名和getter/setter方法的元信息，使得反射时有更好的性能。
- `org.apache.ibatis.reflection.invoker.Invoker:`：用于抽象设置和读取字段值的操作。
- `org.apache.ibatis.reflection.TypeParameterResolver`：针对Java-Type体系的多种实现，解析指定类中的字段、方法返回值或方法参数的类型。
- `org.apache.ibatis.reflection.ReflectorFactory`：反射信息创建工厂抽象接口。
- `org.apache.ibatis.reflection.DefaultReflectorFactory`：默认的反射信息创建工厂。
- `org.apache.ibatis.reflection.factory.ObjectFactory`：MyBatis对象创建工厂，其默认实现DefaultObjectFactory通过构造器反射创建对象。
- `org.apache.ibatis.reflection.property`：property工具包，针对映射文件表达式进行解析和Java对象的反射赋值。
- `org.apache.ibatis.reflection.MetaClass`：依赖PropertyTokenizer和Reflector查找表达式是否可以匹配Java对象中的字段，以及对应字段是否有getter/setter方法。
- `org.apache.ibatis.reflection.MetaObject`：对原始对象进行封装，将对象操作委托给ObjectWrapper处理。
- `org.apache.ibatis.reflection.ywrapper.ObjectWrapper`：对象包装类，封装对象的读取和赋值等操作。

### 类型转换
- `org.apache.ibatis.type.TypeHandler`：类型转换器接口，抽象 `JDBC` 类型和 `Java` 类型互转逻辑。
- `org.apache.ibatis.type.BaseTypeHandler`：`TypeHandler` 的抽象实现，针对null和异常处理做了封装，具体逻辑仍由相应的类型转换器实现。
- `org.apache.ibatis.type.TypeHandlerRegistry`：`TypeHandler` 注册类，维护 `JavaType`、`JdbcType` 和 `TypeHandler` 关系。

### 别名注册
- `org.apache.ibatis.type.TypeAliasRegistry`：别名注册类。注册常用类型的别名，并提供多种注册别名的方式。

### 日志配置
- `org.apache.ibatis.logging.Log`：`MyBatis` 日志适配接口，支持 `trace`、`debug`、`warn`、`error` 四种级别。
- `org.apache.ibatis.logging.LogFactory`：`MyBatis` 日志工厂，负责适配第三方日志实现。
- `org.apache.ibatis.logging.jdbc`：`SQL` 执行日志工具包，针对执行 `Connection`、`PrepareStatement`、`Statement`、`ResultSet` 类中的相关方法，提供日志记录工具。

### 资源加载
- `org.apache.ibatis.io.Resources`：`MyBatis` 封装的资源加载工具类。
- `org.apache.ibatis.io.ClassLoaderWrapper`：资源加载底层实现。组合多种 `ClassLoader` 按顺序尝试加载资源。
- `org.apache.ibatis.io.ResolverUtil`：按条件加载指定包下的类。

### 数据源实现
- `org.apache.ibatis.datasource.DataSourceFactory`：数据源创建工厂接口。
- `org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory`：非池化数据源工厂。
- `org.apache.ibatis.datasource.pooled.PooledDataSourceFactory`：池化数据源工厂。
- `org.apache.ibatis.datasource.unpooled.UnpooledDataSource`：非池化数据源。
- `org.apache.ibatis.datasource.pooled.PooledDataSource`：池化数据源。
- `org.apache.ibatis.datasource.pooled.PooledConnection`：池化连接。
- `org.apache.ibatis.datasource.pooled.PoolState`：连接池状态。

### 事务实现
- `org.apache.ibatis.transaction.Transaction`：事务抽象接口
- `org.apache.ibatis.session.TransactionIsolationLevel`：事务隔离级别。
- `org.apache.ibatis.transaction.TransactionFactory`：事务创建工厂抽象接口。
- `org.apache.ibatis.transaction.jdbc.JdbcTransaction`：封装 `JDBC` 数据库事务操作。
- `org.apache.ibatis.transaction.managed.ManagedTransaction`：数据库事务操作依赖外部管理。

### 缓存实现
- `org.apache.ibatis.cache.Cache`：缓存抽象接口。
- `org.apache.ibatis.cache.impl.PerpetualCache`：使用 `HashMap` 作为缓存实现容器的 `Cache` 基本实现。
- `org.apache.ibatis.cache.decorators.BlockingCache`：缓存阻塞装饰器。保证相同 `key` 同一时刻只有一个线程执行数据库操作，其它线程在缓存层阻塞。
- `org.apache.ibatis.cache.decorators.FifoCache`：缓存先进先出装饰器。按写缓存顺序维护缓存 `key` 队列，缓存项超出指定大小，删除最先入队的缓存。
- `org.apache.ibatis.cache.decorators.LruCache`：缓存最近最久未使用装饰器。基于 `LinkedHashMap` 维护了 `key` 的 `LRU` 顺序。
- `org.apache.ibatis.cache.decorators.LoggingCache`：缓存日志装饰器。查询缓存时记录查询日志并统计命中率。
- `org.apache.ibatis.cache.decorators.ScheduledCache`：缓存定时清理装饰器。
- `org.apache.ibatis.cache.decorators.SerializedCache`：缓存序列化装饰器。
- `org.apache.ibatis.cache.decorators.SynchronizedCache`：缓存同步装饰器。在缓存操作方法上使用 `synchronized` 关键字同步。
- `org.apache.ibatis.cache.decorators.TransactionalCache`：事务缓存装饰器。在事务提交后再将缓存写入，如果发生回滚则不写入。
- `org.apache.ibatis.cache.decorators.SoftCache`：缓存软引用装饰器。使用软引用 + 强引用队列的方式维护缓存。
- `org.apache.ibatis.cache.decorators.WeakCache`：缓存弱引用装饰器。使用弱引用 + 强引用队列的方式维护缓存。

### Binding 模块
- `org.apache.ibatis.binding.MapperRegistry`： `Mapper` 接口注册类，管理 `Mapper` 接口类型和其代理创建工厂的映射。
- `org.apache.ibatis.binding.MapperProxyFactory`：`Mapper` 接口代理创建工厂。
- `org.apache.ibatis.binding.MapperProxy`：`Mapper` 接口方法代理逻辑，封装 `SqlSession` 相关操作。
- `org.apache.ibatis.binding.MapperMethod`：封装 `Mapper` 接口对应的方法和 `SQL` 执行信息。

## 核心处理层
### 配置解析
- `org.apache.ibatis.builder.BaseBuilder`：为 `MyBatis` 初始化过程提供一系列工具方法。如别名转换、类型转换、类加载等。
- `org.apache.ibatis.builder.xml.XMLConfigBuilder`：`XML` 配置解析入口。
- `org.apache.ibatis.session.Configuration`：`MyBatis` 全局配置，包括运行行为、类型容器、别名容器、注册 `Mapper`、注册 `statement` 等。
- `org.apache.ibatis.mapping.VendorDatabaseIdProvider`：根据数据源获取对应的厂商信息。
- `org.apache.ibatis.builder.annotation.MapperAnnotationBuilder`：解析 `Mapper` 接口。
- `org.apache.ibatis.builder.xml.XMLMapperBuilder`：解析 `Mapper` 文件。
- `org.apache.ibatis.builder.MapperBuilderAssistant`：`Mapper` 文件解析工具。生成元素对象并设置到全局配置中。
- `org.apache.ibatis.builder.CacheRefResolver`：缓存引用配置解析器，应用其它命名空间缓存配置到当前命名空间下。
- `org.apache.ibatis.builder.IncompleteElementException`：当前映射文件引用了其它命名空间下的配置，而该配置还未加载到全局配置中时会抛出此异常。
- `org.apache.ibatis.mapping.ResultMapping`：返回值字段映射关系对象。
- `org.apache.ibatis.builder.ResultMapResolver`：`ResultMap` 解析器。
- `org.apache.ibatis.mapping.ResultMap`：返回值映射对象。
- `org.apache.ibatis.builder.xml.XMLStatementBuilder`：解析 `Mapper` 文件中的 `select|insert|update|delete` 元素。
- `org.apache.ibatis.parsing.GenericTokenParser.GenericTokenParser`：搜索指定格式 `token` 并进行解析。
- `org.apache.ibatis.parsing.TokenHandler`：`token` 处理器抽象接口。定义 `token` 以何种方式被解析。
- `org.apache.ibatis.parsing.PropertyParser`：`${}` 类型 `token` 解析器。
- `org.apache.ibatis.session.Configuration.StrictMap`：封装 `HashMap`，对键值存取有严格要求。
- `org.apache.ibatis.builder.xml.XMLIncludeTransformer`：`include` 元素解析器。
- `org.apache.ibatis.mapping.SqlSource`：`sql` 生成抽象接口。根据传入参数生成有效 `sql` 语句和参数绑定对象。
- `org.apache.ibatis.scripting.xmltags.XMLScriptBuilder`：解析 `statement` 各个 `sql` 节点并进行组合。
- `org.apache.ibatis.scripting.xmltags.SqlNode`：`sql` 节点抽象接口。用于判断当前 `sql` 节点是否可以加入到生效的 sql 语句中。
- `org.apache.ibatis.scripting.xmltags.DynamicContext`：动态 `sql` 上下文。用于保存绑定参数和生效 `sql` 节点。
- `org.apache.ibatis.scripting.xmltags.OgnlCache`：`ognl` 缓存工具，缓存表达式编译结果。
- `org.apache.ibatis.scripting.xmltags.ExpressionEvaluator`：`ognl` 表达式计算工具。
- `org.apache.ibatis.scripting.xmltags.MixedSqlNode`：`sql` 节点组合对象。
- `org.apache.ibatis.scripting.xmltags.StaticTextSqlNode`：静态 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.TextSqlNode`：`${}` 类型 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.IfSqlNode`：`if` 元素 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.TrimSqlNode`：`trim` 元素 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.WhereSqlNode`：`where` 元素 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.SetSqlNode`：`set` 元素 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.ForEachSqlNode`：`foreach` 元素 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.ChooseSqlNode`：`choose` 元素 `sql` 节点对象。
- `org.apache.ibatis.scripting.xmltags.VarDeclSqlNode`：`bind` 元素 `sql` 节点对象。
- `org.apache.ibatis.mapping.MappedStatement`：`statement` 解析对象。
- `org.apache.ibatis.mapping.BoundSql`：可执行 `sql` 和参数绑定对象。
- `org.apache.ibatis.scripting.xmltags.DynamicSqlSource`：根据参数动态生成有效 `sql` 和绑定参数。
- `org.apache.ibatis.builder.SqlSourceBuilder`：解析 `#{}` 类型 `token` 并绑定参数对象。


=====================================

MyBatis SQL Mapper Framework for Java
=====================================

[![Build Status](https://travis-ci.org/mybatis/mybatis-3.svg?branch=master)](https://travis-ci.org/mybatis/mybatis-3)
[![Coverage Status](https://coveralls.io/repos/mybatis/mybatis-3/badge.svg?branch=master&service=github)](https://coveralls.io/github/mybatis/mybatis-3?branch=master)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/org.mybatis/mybatis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.mybatis/mybatis)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Stack Overflow](http://img.shields.io/:stack%20overflow-mybatis-brightgreen.svg)](http://stackoverflow.com/questions/tagged/mybatis)
[![Project Stats](https://www.openhub.net/p/mybatis/widgets/project_thin_badge.gif)](https://www.openhub.net/p/mybatis)

![mybatis](http://mybatis.github.io/images/mybatis-logo.png)

The MyBatis SQL mapper framework makes it easier to use a relational database with object-oriented applications.
MyBatis couples objects with stored procedures or SQL statements using a XML descriptor or annotations.
Simplicity is the biggest advantage of the MyBatis data mapper over object relational mapping tools.

Essentials
----------

* [See the docs](http://mybatis.github.io/mybatis-3)
* [Download Latest](https://github.com/mybatis/mybatis-3/releases)
* [Download Snapshot](https://oss.sonatype.org/content/repositories/snapshots/org/mybatis/mybatis/)
