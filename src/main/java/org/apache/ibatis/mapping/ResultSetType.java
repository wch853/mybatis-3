/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.mapping;

import java.sql.ResultSet;

/**
 * 结果集类型
 *
 * @author Clinton Begin
 */
public enum ResultSetType {
  /**
   * behavior with same as unset (driver dependent).
   * 默认结果集，与未设置时效果一样
   *
   * @since 3.5.0
   */
  DEFAULT(-1),

  /**
   * 只可向前滚动的结果集
   */
  FORWARD_ONLY(ResultSet.TYPE_FORWARD_ONLY),

  /**
   * 可以前后滚动的结果集，对修改不敏感
   */
  SCROLL_INSENSITIVE(ResultSet.TYPE_SCROLL_INSENSITIVE),

  /**
   * 可以前后滚动的结果集，对修改敏感
   */
  SCROLL_SENSITIVE(ResultSet.TYPE_SCROLL_SENSITIVE);

  private final int value;

  ResultSetType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
