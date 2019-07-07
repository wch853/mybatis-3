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

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析 statement 各个 sql 节点并进行组合
 *
 * @author Clinton Begin
 */
public class XMLScriptBuilder extends BaseBuilder {

  private final XNode context;
  private boolean isDynamic;
  private final Class<?> parameterType;
  private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();

  public XMLScriptBuilder(Configuration configuration, XNode context) {
    this(configuration, context, null);
  }

  public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
    super(configuration);
    this.context = context;
    this.parameterType = parameterType;
    initNodeHandlerMap();
  }


  private void initNodeHandlerMap() {
    nodeHandlerMap.put("trim", new TrimHandler());
    nodeHandlerMap.put("where", new WhereHandler());
    nodeHandlerMap.put("set", new SetHandler());
    nodeHandlerMap.put("foreach", new ForEachHandler());
    nodeHandlerMap.put("if", new IfHandler());
    nodeHandlerMap.put("choose", new ChooseHandler());
    nodeHandlerMap.put("when", new IfHandler());
    nodeHandlerMap.put("otherwise", new OtherwiseHandler());
    nodeHandlerMap.put("bind", new BindHandler());
  }

  /**
   * 解析 statement 各个 sql 节点并进行组合
   */
  public SqlSource parseScriptNode() {
    // 递归解析各 sql 节点
    MixedSqlNode rootSqlNode = parseDynamicTags(context);
    SqlSource sqlSource;
    if (isDynamic) {
      // 动态 sql
      sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    } else {
      // 原始文本 sql
      sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
    }
    return sqlSource;
  }

  /**
   * 处理 statement 各 SQL 组成部分，并进行组合
   */
  protected MixedSqlNode parseDynamicTags(XNode node) {
    // SQL 各组成部分
    List<SqlNode> contents = new ArrayList<>();
    // 遍历子元素
    NodeList children = node.getNode().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      XNode child = node.newXNode(children.item(i));
      if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
        // 解析 sql 文本
        String data = child.getStringBody("");
        TextSqlNode textSqlNode = new TextSqlNode(data);
        if (textSqlNode.isDynamic()) {
          // 判断是否为动态 sql，包含 ${} 类型 token 即为动态 sql
          contents.add(textSqlNode);
          isDynamic = true;
        } else {
          // 静态 sql 元素
          contents.add(new StaticTextSqlNode(data));
        }
      } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) { // issue #628
        // 如果是子元素
        String nodeName = child.getNode().getNodeName();
        // 获取支持的子元素语法处理器
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler == null) {
          throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
        }
        // 根据子元素标签类型使用对应的处理器处理子元素
        handler.handleNode(child, contents);
        // 包含标签元素，认定为动态 SQL
        isDynamic = true;
      }
    }
    return new MixedSqlNode(contents);
  }

  /**
   * 动态 sql 元素处理器抽象接口
   */
  private interface NodeHandler {

    void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
  }

  /**
   * 处理 bind 元素
   */
  private class BindHandler implements NodeHandler {

    public BindHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 占位属性名
      final String name = nodeToHandle.getStringAttribute("name");
      // ongl 表达式
      final String expression = nodeToHandle.getStringAttribute("value");
      // 保存 key 和对应的 ognl 表达式
      final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
      // 放入 sql 节点容器
      targetContents.add(node);
    }
  }

  /**
   * 处理 trim 元素
   */
  private class TrimHandler implements NodeHandler {
    public TrimHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 递归解析 trim 节点
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String prefix = nodeToHandle.getStringAttribute("prefix");
      String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
      String suffix = nodeToHandle.getStringAttribute("suffix");
      String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
      TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
      targetContents.add(trim);
    }
  }

  /**
   * 处理 where 元素
   */
  private class WhereHandler implements NodeHandler {
    public WhereHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
      targetContents.add(where);
    }
  }

  /**
   * 处理 set 元素
   */
  private class SetHandler implements NodeHandler {
    public SetHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 递归解析 sql 节点
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
      targetContents.add(set);
    }
  }

  /**
   * 处理 foreach 节点
   */
  private class ForEachHandler implements NodeHandler {
    public ForEachHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 递归解析 foreach 节点
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      // 遍历的集合名称
      String collection = nodeToHandle.getStringAttribute("collection");
      // 集合项占位名称
      String item = nodeToHandle.getStringAttribute("item");
      // 集合项索引占位名称
      String index = nodeToHandle.getStringAttribute("index");
      // 集合动态语句前缀
      String open = nodeToHandle.getStringAttribute("open");
      // 集合动态语句后缀
      String close = nodeToHandle.getStringAttribute("close");
      // 集合项分隔符
      String separator = nodeToHandle.getStringAttribute("separator");
      ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open, close, separator);
      targetContents.add(forEachSqlNode);
    }
  }

  /**
   * 处理 if、when 元素
   */
  private class IfHandler implements NodeHandler {
    public IfHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 递归处理嵌套的动态元素
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      // 获取 test 表达式
      String test = nodeToHandle.getStringAttribute("test");
      // 保存 sql 节点和 test 表达式
      IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
      targetContents.add(ifSqlNode);
    }
  }

  /**
   * 处理 otherwise 元素
   */
  private class OtherwiseHandler implements NodeHandler {
    public OtherwiseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 递归解析 other 元素
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      targetContents.add(mixedSqlNode);
    }
  }

  /**
   * 处理 choose 元素
   */
  private class ChooseHandler implements NodeHandler {

    public ChooseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // when sql 节点容器
      List<SqlNode> whenSqlNodes = new ArrayList<>();
      // otherwise sql 节点容器
      List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
      // 分别递归解析 when、sql 元素
      handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
      // 获取默认值 sql 节点，确保有且仅有一个生效
      SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
      // when 和 otherwise 节点组合生成 choose 节点
      ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
      targetContents.add(chooseSqlNode);
    }

    private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes, List<SqlNode> defaultSqlNodes) {
      // 遍历子元素
      List<XNode> children = chooseSqlNode.getChildren();
      for (XNode child : children) {
        // 获取元素名
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler instanceof IfHandler) {
          // 处理 when 元素
          handler.handleNode(child, ifSqlNodes);
        } else if (handler instanceof OtherwiseHandler) {
          // 处理 otherwise 元素
          handler.handleNode(child, defaultSqlNodes);
        }
      }
    }

    /**
     * 获取默认值 sql 节点，确保有且仅有一个生效
     *
     * @param defaultSqlNodes
     * @return
     */
    private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
      SqlNode defaultSqlNode = null;
      if (defaultSqlNodes.size() == 1) {
        defaultSqlNode = defaultSqlNodes.get(0);
      } else if (defaultSqlNodes.size() > 1) {
        throw new BuilderException("Too many default (otherwise) elements in choose statement.");
      }
      return defaultSqlNode;
    }
  }

}
