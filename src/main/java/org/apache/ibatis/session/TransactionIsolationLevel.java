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
package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * 事务隔离级别
 *
 * @author Clinton Begin
 */
public enum TransactionIsolationLevel {

  /**
   * 不支持事务
   */
  NONE(Connection.TRANSACTION_NONE),

  /**
   * 读未提交
   * 可以读其它事务未提交的修改，会产生脏读
   */
  READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

  /**
   * 读提交
   * 只会读其它事务提交的修改，但是一个事务中的两次相同查询可能会获取不同结果，即不可重复读
   */
  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

  /**
   * 可重复读
   * 保证在一个事务中多次执行相同查询获取的结果是一样的。但是当一个事务中的两次查询读取某个范围内的记录时，其它事务又在该范围内插入了新的记录，即幻读
   */
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

  /**
   * 可串行化
   * 强制事务串行执行，在读取的每一行数据上加锁
   */
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

  private final int level;

  TransactionIsolationLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }
}
