/**
 *    Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  private String name;
  private final String indexedName;
  private String index;
  private final String children;

  public PropertyTokenizer(String fullname) {
    // orders[0].items[0].name
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      // name = orders[0]
      name = fullname.substring(0, delim);
      // children = items[0].name
      children = fullname.substring(delim + 1);
    } else {
      name = fullname;
      children = null;
    }
    // orders[0]
    indexedName = name;
    delim = name.indexOf('[');
    if (delim > -1) {
      // 0
      index = name.substring(delim + 1, name.length() - 1);
      // order
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  /**
   * 是否有children表达式继续迭代
   *
   * @return
   */
  @Override
  public boolean hasNext() {
    return children != null;
  }

  /**
   * 分解出的 . 分隔符的 children 表达式可以继续迭代
   * @return
   */
  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
