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
package org.apache.ibatis.parsing;

/**
 * 搜索指定格式 token 并进行解析
 *
 * @author Clinton Begin
 */
public class GenericTokenParser {

  /**
   * token 前缀
   */
  private final String openToken;

  /**
   * token 后缀
   */
  private final String closeToken;

  /**
   * token 处理器，定义 token 以何种方式被解析
   */
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * 解析 token 文本
   *
   * @param text
   * @return
   */
  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token 搜索 token 前缀
    int start = text.indexOf(openToken);
    if (start == -1) {
      // 没有 token 前缀，返回原文本
      return text;
    }
    char[] src = text.toCharArray();
    // 当前解析偏移量
    int offset = 0;
    // 已解析文本
    final StringBuilder builder = new StringBuilder();
    // 当前占位符内的表达式
    StringBuilder expression = null;
    while (start > -1) {
      if (start > 0 && src[start - 1] == '\\') {
        // 如果待解析属性前缀被转义，则去掉转义字符，加入已解析文本
        // this open token is escaped. remove the backslash and continue.
        builder.append(src, offset, start - offset - 1).append(openToken);
        // 更新解析偏移量
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        // 前缀前面的部分加入已解析文本
        builder.append(src, offset, start - offset);
        // 更新解析偏移量
        offset = start + openToken.length();
        // 获取对应的后缀索引
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if (end > offset && src[end - 1] == '\\') {
            // 后缀被转义，加入已解析文本
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            // 寻找下一个后缀
            end = text.indexOf(closeToken, offset);
          } else {
            // 找到后缀，获取占位符内的表达式
            expression.append(src, offset, end - offset);
            offset = end + closeToken.length();
            break;
          }
        }
        if (end == -1) {
          // 找不到后缀，前缀之后的部分全部加入已解析文本
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          // 能够找到后缀，追加 token 处理器处理后的文本
          builder.append(handler.handleToken(expression.toString()));
          // 更新解析偏移量
          offset = end + closeToken.length();
        }
      }
      // 寻找下一个前缀，重复解析表达式
      start = text.indexOf(openToken, offset);
    }
    if (offset < src.length) {
      // 将最后的部分加入已解析文本
      builder.append(src, offset, src.length - offset);
    }
    // 返回解析后的文本
    return builder.toString();
  }
}
