/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.annotations;

import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import java.lang.annotation.*;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespace {

  /**
   * 缓存基础实现
   *
   * @return
   */
  Class<? extends org.apache.ibatis.cache.Cache> implementation() default PerpetualCache.class;

  /**
   * 缓存失效装饰器
   *
   * @return
   */
  Class<? extends org.apache.ibatis.cache.Cache> eviction() default LruCache.class;

  /**
   * 缓存刷新间隔
   *
   * @return
   */
  long flushInterval() default 0;

  /**
   * 缓存大小
   *
   * @return
   */
  int size() default 1024;

  /**
   * 是否序列化缓存
   *
   * @return
   */
  boolean readWrite() default true;

  /**
   * 未命中访问数据库是否加锁
   *
   * @return
   */
  boolean blocking() default false;

  /**
   * 自定义配置
   *
   * Property values for a implementation object.
   * @since 3.4.2
   */
  Property[] properties() default {};

}
