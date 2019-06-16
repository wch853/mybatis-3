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
package org.apache.ibatis.datasource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据源创建工厂
 *
 * @author Clinton Begin
 */
public interface DataSourceFactory {

  /**
   * 创建数据源工厂后配置数据源属性
   *
   * @param props
   */
  void setProperties(Properties props);

  /**
   * 获取数据源
   *
   * @return
   */
  DataSource getDataSource();

}
