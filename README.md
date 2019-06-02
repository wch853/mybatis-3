# MyBatis 源码阅读
base on mybatis-3.5.1
参考书籍：徐郡明.MyBatis技术内幕[M].电子工业出版社,2017.

## 反射模块
- `org.apache.ibatis.reflection.Reflector`：缓存类的字段名和getter/setter方法的元信息，使得反射时有更好的性能。
- `org.apache.ibatis.reflection.invoker.Invoker:`：用于抽象设置和读取字段值的操作。
- `org.apache.ibatis.reflection.TypeParameterResolver`：针对Java-Type体系的多种实现，解析指定类中的字段、方法返回值或方法参数的类型。
- `org.apache.ibatis.reflection.DefaultReflectorFactory`：默认的Reflector创建工厂。
- `org.apache.ibatis.reflection.factory.ObjectFactory`：MyBatis对象创建工厂，其默认实现DefaultObjectFactory通过构造器反射创建对象。
- `org.apache.ibatis.reflection.property`：property工具包，针对映射文件表达式进行解析和Java对象的反射赋值。
- `org.apache.ibatis.reflection.MetaClass`：依赖PropertyTokenizer和Reflector查找表达式是否可以匹配Java对象中的字段，以及对应字段是否有getter/setter方法。
- `org.apache.ibatis.reflection.MetaObject`：对原始对象进行封装，将对象操作委托给ObjectWrapper处理。
- `org.apache.ibatis.reflection.wrapper.ObjectWrapper`：对象包装类，封装对象的读取和赋值等操作。



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
