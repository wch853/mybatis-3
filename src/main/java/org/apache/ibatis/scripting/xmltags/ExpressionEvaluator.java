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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.BuilderException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ognl 表达式计算工具
 *
 * @author Clinton Begin
 */
public class ExpressionEvaluator {

  /**
   * 计算 ognl 表达式 true / false
   *
   * @param expression
   * @param parameterObject
   * @return
   */
  public boolean evaluateBoolean(String expression, Object parameterObject) {
    // 根据 ognl 表达式和参数计算值
    Object value = OgnlCache.getValue(expression, parameterObject);
    // true / false
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    // 不为 0
    if (value instanceof Number) {
      return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
    }
    // 不为 null
    return value != null;
  }

  /**
   * 计算获得一个可迭代的对象
   *
   * @param expression
   * @param parameterObject
   * @return
   */
  public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value == null) {
      throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
    }
    if (value instanceof Iterable) {
      // 已实现 Iterable 接口
      return (Iterable<?>) value;
    }
    if (value.getClass().isArray()) {
      // 数组转集合
      // the array may be primitive, so Arrays.asList() may throw
      // a ClassCastException (issue 209).  Do the work manually
      // Curse primitives! :) (JGB)
      int size = Array.getLength(value);
      List<Object> answer = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        Object o = Array.get(value, i);
        answer.add(o);
      }
      return answer;
    }
    if (value instanceof Map) {
      // Map 获取 entry
      return ((Map) value).entrySet();
    }
    throw new BuilderException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
  }

}
