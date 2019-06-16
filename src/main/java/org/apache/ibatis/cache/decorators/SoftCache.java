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
package org.apache.ibatis.cache.decorators;

import org.apache.ibatis.cache.Cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 *
 * 缓存软引用装饰器。使用软引用方式 + 强引用队列的方式维护缓存。
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {

  /**
   * 基于 LinkedList 实现的 LRU 队列
   */
  private final Deque<Object> hardLinksToAvoidGarbageCollection;

  /**
   * 引用队列，用于记录已经被 GC 的 SoftEntry 对象
   */
  private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;

  private final Cache delegate;

  /**
   * LRU 队列大小
   */
  private int numberOfHardLinks;

  public SoftCache(Cache delegate) {
    this.delegate = delegate;
    this.numberOfHardLinks = 256;
    this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
    this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    removeGarbageCollectedItems();
    return delegate.getSize();
  }


  public void setSize(int size) {
    this.numberOfHardLinks = size;
  }

  /**
   * 写入缓存。
   * 不直接写缓存的值，而是写入缓存项对应的软引用
   */
  @Override
  public void putObject(Object key, Object value) {
    removeGarbageCollectedItems();
    // 在 Full GC 时，如果没有一个强引用指向被包装的缓存项或缓存值，并且系统内存不足，缓存项就会被回收，被回收对象进入指定的引用队列。
    delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
  }

  /**
   * 获取缓存。
   * 如果软引用被回收则删除对应的缓存项，如果未回收则加入到有数量限制的 LRU 队列中
   *
   * @param key The key
   * @return
   */
  @Override
  public Object getObject(Object key) {
    Object result = null;
    @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
    SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
    if (softReference != null) {
      result = softReference.get();
      if (result == null) {
        // 软引用已经被回收，删除对应的缓存项
        delegate.removeObject(key);
      } else {
        // 如果未被回收，增将软引用加入到 LRU 队列
        // See #586 (and #335) modifications need more than a read lock
        synchronized (hardLinksToAvoidGarbageCollection) {
          // 将对应的软引用
          hardLinksToAvoidGarbageCollection.addFirst(result);
          if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
            // 超出数量限制，删除最近最久未使用的软引用对象
            hardLinksToAvoidGarbageCollection.removeLast();
          }
        }
      }
    }
    return result;
  }

  @Override
  public Object removeObject(Object key) {
    removeGarbageCollectedItems();
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    synchronized (hardLinksToAvoidGarbageCollection) {
      hardLinksToAvoidGarbageCollection.clear();
    }
    removeGarbageCollectedItems();
    delegate.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 查询已被 GC 的软引用，删除对应的缓存项
   */
  private void removeGarbageCollectedItems() {
    SoftEntry sv;
    while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
      delegate.removeObject(sv.key);
    }
  }

  /**
   * 封装软引用对象
   */
  private static class SoftEntry extends SoftReference<Object> {
    private final Object key;

    SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
      // 声明 value 为软引用对象
      super(value, garbageCollectionQueue);
      // key 为强引用
      this.key = key;
    }
  }

}
