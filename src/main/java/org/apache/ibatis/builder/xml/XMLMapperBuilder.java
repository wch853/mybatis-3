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
package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.*;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * 解析 Mapper 文件
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder extends BaseBuilder {

  private final XPathParser parser;
  private final MapperBuilderAssistant builderAssistant;

  /**
   * 全局配置中的 SQL 片段配置
   */
  private final Map<String, XNode> sqlFragments;

  /**
   * Mapper 文件资源路径
   */
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  /**
   * 解析 Mapper 文件
   */
  public void parse() {
    if (!configuration.isResourceLoaded(resource)) {
      // 解析 mapper 元素
      configurationElement(parser.evalNode("/mapper"));
      // 加入已解析队列
      configuration.addLoadedResource(resource);
      // Mapper 映射文件与对应 namespace 的接口进行绑定
      bindMapperForNamespace();
    }

    // 重新引用配置
    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  /**
   * 解析 mapper 元素
   *
   * @param context
   */
  private void configurationElement(XNode context) {
    try {
      // 获取元素对应的 namespace 名称
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      // 设置 Mapper 文件对应的 namespace 名称
      builderAssistant.setCurrentNamespace(namespace);
      // 解析 cache-ref 元素
      cacheRefElement(context.evalNode("cache-ref"));
      // 解析 cache 元素，会覆盖 cache-ref 配置
      cacheElement(context.evalNode("cache"));
      // 解析 parameterMap 元素（废弃）
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      // 解析 resultMap 元素
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      // 解析 sql 元素
      sqlElement(context.evalNodes("/mapper/sql"));
      // 解析 select|insert|update|delete 元素
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  /**
   * 解析 select|insert|update|delete 元素
   *
   * @param list
   */
  private void buildStatementFromContext(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        // 逐个解析 statement
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  /**
   * 之前没有成功引用缓存配置的缓存解析器，重新解析
   */
  private void parsePendingCacheRefs() {
    // 从全局配置中获取未解析的缓存引用配置
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          // 逐个重新引用缓存配置
          iter.next().resolveCacheRef();
          // 引用成功，删除集合元素
          iter.remove();
        } catch (IncompleteElementException e) {
          // 引用的缓存配置不存在
          // Cache ref is still missing a resource...
        }
      }
    }
  }

  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  /**
   * 解析 cache-ref 元素
   *
   * @param context
   */
  private void cacheRefElement(XNode context) {
    if (context != null) {
      // 当前 namespace - 引用缓存配置的 namespace，在全局配置中进行绑定
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      // 获取缓存配置解析器
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        // 解析获得引用的缓存配置
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        // 指定引用的 namespace 缓存还未加载，暂时放入集合，等待全部 namespace 都加载完成后重新引用
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  /**
   * 解析 cache 元素
   *
   * @param context
   */
  private void cacheElement(XNode context) {
    if (context != null) {
      // 获取缓存类型，默认为 PERPETUAL
      String type = context.getStringAttribute("type", "PERPETUAL");
      // Configuration 构造方法中已为默认的缓存实现注册别名，从别名转换器中获取类对象
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      // 获取失效类型，默认为 LRU
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      // 缓存刷新时间间隔
      Long flushInterval = context.getLongAttribute("flushInterval");
      // 缓存项大小
      Integer size = context.getIntAttribute("size");
      // 是否将序列化成二级制数据
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      // 缓存不命中进入数据库查询时是否加锁（保证同一时刻相同缓存key只有一个线程执行数据库查询任务）
      boolean blocking = context.getBooleanAttribute("blocking", false);
      // 从子元素中加载属性
      Properties props = context.getChildrenAsProperties();
      // 创建缓存配置
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  /**
   * 解析 parameterMap 元素，创建参数表并设置到全局配置中
   *
   * @param list
   */
  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      // 元素 id
      String id = parameterMapNode.getStringAttribute("id");
      // 参数类型
      String type = parameterMapNode.getStringAttribute("type");
      // 加载参数类型类对象
      Class<?> parameterClass = resolveClass(type);
      // 加载 parameter 元素集合
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        // 字段名
        String property = parameterNode.getStringAttribute("property");
        // 字段 java 类型
        String javaType = parameterNode.getStringAttribute("javaType");
        // 字段 jdbc 类型
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        // 对应的 resultMap id
        String resultMap = parameterNode.getStringAttribute("resultMap");
        // 存储过程参数类型
        String mode = parameterNode.getStringAttribute("mode");
        // 字段对应的类型转换器
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        // 数字精度
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        // 转为存储过程参数类型枚举
        ParameterMode modeEnum = resolveParameterMode(mode);
        // java 类型类对象
        Class<?> javaTypeClass = resolveClass(javaType);
        // jdbc 类型类对象
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        // 类型转换器类对象
        Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
        // 创建参数映射对象
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      // 创建参数表并设置到全局配置中
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  /**
   * 解析 resultMap 元素
   *
   * @param list
   * @throws Exception
   */
  private void resultMapElements(List<XNode> list) throws Exception {
    for (XNode resultMapNode : list) {
      try {
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.emptyList(), null);
  }

  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings, Class<?> enclosingType) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    // 获取返回值类型
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    // 加载返回值类对象
    Class<?> typeClass = resolveClass(type);
    if (typeClass == null) {
      // association 和 case 元素没有显式地指定返回值类型
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }
    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<>();
    resultMappings.addAll(additionalResultMappings);
    // 加载子元素
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        // 解析 constructor 元素
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        // 解析 discriminator 元素
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        // 解析 resultMap 元素下的其它元素
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          // id 元素增加标志
          flags.add(ResultFlag.ID);
        }
        // 解析元素映射关系
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    String id = resultMapNode.getStringAttribute("id",
            resultMapNode.getValueBasedIdentifier());
    // extend resultMap id
    String extend = resultMapNode.getStringAttribute("extends");
    // 是否设置自动映射
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    // resultMap 解析器
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      // 解析生成 ResultMap 对象并设置到全局配置中
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      // 异常稍后处理
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  /**
   * 获取嵌套的 association 或 case 元素返回值类型类型
   *
   * @param resultMapNode
   * @param enclosingType
   * @return
   */
  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      // association 元素没有指定 resultMap 属性
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        // 根据反射信息确定字段的类型
        MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
        return metaResultType.getSetterType(property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      // case 元素返回值属性与 resultMap 父元素相同
      return enclosingType;
    }
    return null;
  }

  /**
   * 解析 constructor 元素下的子元素
   *
   * @param resultChild
   * @param resultType
   * @param resultMappings
   * @throws Exception
   */
  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    // 获取子元素
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<>();
      // 标明此元素在 constructor 元素中
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        // 此元素映射 id
        flags.add(ResultFlag.ID);
      }
      // 解析子元素映射关系
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  /**
   * 根据 discriminator 元素生成鉴别器，不同列值对应不同的 resultMap
   */
  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    // 获取需要鉴别的字段的相关信息
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<>();
    // 解析 discriminator 的 case 子元素
    for (XNode caseChild : context.getChildren()) {
      // 解析不同列值对应的不同 resultMap
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  private void sqlElement(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }


  /**
   * 解析 sql 元素，将对应的 sql 片段设置到全局配置中
   */
  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        // 符合当前 databaseId 的 sql fragment，加入到全局配置中
        sqlFragments.put(id, context);
      }
    }
  }

  /**
   * 判断 sql 元素是否满足加载条件
   *
   * @param id
   * @param databaseId
   * @param requiredDatabaseId
   * @return
   */
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      // 如果指定了当前数据源的 databaseId
      if (!requiredDatabaseId.equals(databaseId)) {
        // 被解析 sql 元素的 databaseId 需要符合
        return false;
      }
    } else {
      if (databaseId != null) {
        // 全局未指定 databaseId，不会加载指定了 databaseId 的 sql 元素
        return false;
      }
      // skip this fragment if there is a previous one with a not null databaseId
      if (this.sqlFragments.containsKey(id)) {
        XNode context = this.sqlFragments.get(id);
        if (context.getStringAttribute("databaseId") != null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 解析子元素映射关系
   *
   * @param context
   * @param resultType
   * @param flags
   * @return
   * @throws Exception
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      // constructor 子元素，通过 name 获取参数名
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    // 列名
    String column = context.getStringAttribute("column");
    // java 类型
    String javaType = context.getStringAttribute("javaType");
    // jdbc 类型
    String jdbcType = context.getStringAttribute("jdbcType");
    // 嵌套的 select id
    String nestedSelect = context.getStringAttribute("select");
    // 获取嵌套的 resultMap id
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.emptyList(), resultType));
    // 获取指定的不为空才创建实例的列
    String notNullColumn = context.getStringAttribute("notNullColumn");
    // 列前缀
    String columnPrefix = context.getStringAttribute("columnPrefix");
    // 类型转换器
    String typeHandler = context.getStringAttribute("typeHandler");
    // 集合的多结果集
    String resultSet = context.getStringAttribute("resultSet");
    // 指定外键对应的列名
    String foreignColumn = context.getStringAttribute("foreignColumn");
    // 是否懒加载
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    // 加载返回值类型
    Class<?> javaTypeClass = resolveClass(javaType);
    // 加载类型转换器类型
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    // 加载 jdbc 类型对象
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  /**
   * 处理嵌套的 resultMap，获取 id
   *
   * @param context
   * @param resultMappings
   * @param enclosingType
   * @return
   * @throws Exception
   */
  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings, Class<?> enclosingType) throws Exception {
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
      if (context.getStringAttribute("select") == null) {
        // 如果是 association、collection 或 case 元素并且没有 select 属性
        // collection 元素没有指定 resultMap 或 javaType 属性，需要验证 resultMap 父元素对应的返回值类型是否有对当前集合的赋值入口
        validateCollection(context, enclosingType);
        ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
        return resultMap.getId();
      }
    }
    return null;
  }

  /**
   * collection 元素没有指定 resultMap 或 javaType 属性，需要验证 resultMap 父元素对应的返回值类型是否有对当前集合的赋值入口
   *
   * @param context
   * @param enclosingType
   */
  protected void validateCollection(XNode context, Class<?> enclosingType) {
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
        && context.getStringAttribute("javaType") == null) {
      MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
      String property = context.getStringAttribute("property");
      if (!metaResultType.hasSetter(property)) {
        throw new BuilderException(
          "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }

  /**
   * Mapper 映射文件与对应 namespace 的接口进行绑定
   */
  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) {
        if (!configuration.hasMapper(boundType)) {
          // Spring may not know the real resource name so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          configuration.addLoadedResource("namespace:" + namespace);
          configuration.addMapper(boundType);
        }
      }
    }
  }

}
