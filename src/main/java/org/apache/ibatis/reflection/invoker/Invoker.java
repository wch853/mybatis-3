/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * 用于抽象设置和读取字段值的操作
 *
 * {@link MethodInvoker} 反射执行getter/setter方法
 * {@link GetFieldInvoker} {@link SetFieldInvoker} 反射执行Field对象的get/set方法
 *
 * @author Clinton Begin
 */
public interface Invoker {

  /**
   * 通过反射设置或读取字段值
   *
   * @param target
   * @param args
   * @return
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

  /**
   * 字段类型
   *
   * @return
   */
  Class<?> getType();
}
