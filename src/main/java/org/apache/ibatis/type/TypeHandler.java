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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型转换器接口
 *
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

  /**
   * 为PreparedStatement对象设置参数
   *
   * @param ps SQL 预编译对象
   * @param i 参数索引
   * @param parameter 参数值
   * @param jdbcType 参数 JDBC类型
   * @throws SQLException
   */
  void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

  /**
   * 根据列名从结果集中取值
   *
   * @param rs 结果集
   * @param columnName 列名
   * @return
   * @throws SQLException
   */
  T getResult(ResultSet rs, String columnName) throws SQLException;

  /**
   * 根据索引从结果集中取值
   * @param rs 结果集
   * @param columnIndex 索引
   * @return
   * @throws SQLException
   */
  T getResult(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * 根据索引从存储过程函数中取值
   *
   * @param cs 存储过程对象
   * @param columnIndex 索引
   * @return
   * @throws SQLException
   */
  T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
