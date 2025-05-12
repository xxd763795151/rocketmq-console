package org.apache.rocketmq.console.common;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 14:56:45
 * @description thread safe. based time cache. Not suitable for the big data volume and the high concurrency scenarios because of the use of lock.
 **/
public class ExpireCleanCache<K, V> {

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private TreeMap<TimePair<K>, V> innerCache = new TreeMap<>();
    private Long expireTime;

    public ExpireCleanCache(long expireTime) {
        this.expireTime = expireTime;
        this.innerCache = new TreeMap<>(new Comparator<TimePair<K>>() {
            @Override public int compare(TimePair<K> o1, TimePair<K> o2) {
                if (o1.equals(o2)) {
                    return 0;
                }
                return Long.compare(o1.time, o2.time);
            }
        });
    }

    public boolean contains(K k) {
        cleanExpire();
        try {
            readLock.lock();
            return innerCache.containsKey(new TimePair<>(k));
        } finally {
            readLock.unlock();
        }
    }

    public void put(K k, V v) {
        cleanExpire();
        try {
            writeLock.lock();
            innerCache.put(new TimePair<>(k), v);
        } finally {
            writeLock.unlock();
        }
    }

    public V get(K k) {
        cleanExpire();
        try {
            readLock.lock();
            return innerCache.get(new TimePair<>(k));
        } finally {
            readLock.unlock();
        }
    }

    public V remove(K k) {
        try {
            readLock.lock();
            return innerCache.remove(new TimePair<>(k));
        } finally {
            readLock.unlock();
        }
    }

    private void cleanExpire() {
        Long lastTime = System.currentTimeMillis() - expireTime;
        try {
            writeLock.lock();
            TimePair<K> k;

            while (innerCache.firstEntry() != null && (k = innerCache.firstEntry().getKey()) != null && k.time < lastTime) {
                innerCache.remove(k);
            }
        } finally {
            writeLock.unlock();
        }
    }

    class TimePair<K> {
        K k;
        Long time;

        public TimePair(K k) {
            this.k = k;
            this.time = System.currentTimeMillis();
        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TimePair<?> pair = (TimePair<?>) o;
            return Objects.equals(k, pair.k);
        }

        @Override public int hashCode() {
            return k.hashCode();
        }
    }

//    class InnerCache extends LinkedHashMap<K, TimePair<V>> {
//
//        private long expireTime;
//
//        public InnerCache(long expireTime) {
//            super(100, 0.75f, true);
//            this.expireTime = expireTime;
//        }
//
//        @Override protected boolean removeEldestEntry(Map.Entry<K, TimePair<V>> eldest) {
//            return System.currentTimeMillis() - eldest.getValue().time > expireTime ?
//                super.removeEldestEntry(eldest) : false;
//        }
//    }
}
