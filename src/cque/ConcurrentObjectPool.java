/**
 * 
 */
package cque;

import cque.util.PoolUtils;

/**
 * @author Xiong
 * 并发对象池（多生产者，多消费者），内部可能有多个池
 */
public class ConcurrentObjectPool<E extends IPooledObject> implements IPolicyObjectPool<E> {
	private MpmcObjectPool<E>[] pools;
	private final int poolSize;
	private final IObjectFactory objectFactory;
	private int polling = 0;
	
	/**
	 * 创建默认的对象池
	 * @param objectFactory 不可为null
	 */
	public ConcurrentObjectPool(IObjectFactory nodeFactory){
		this(nodeFactory, PoolUtils.DEFAULT_POOL_SIZE, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	/**
	 * 根据指定大小参数创建对象池
	 * @param objectFactory 不可为null
	 * @param initSize
	 * @param maxSize
	 */
	@SuppressWarnings("unchecked")
	public ConcurrentObjectPool(IObjectFactory objectFactory, int poolSize, int initSize, int maxSize){
		if (objectFactory == null){
			throw new IllegalArgumentException("null values not allowed");
		}
		
		this.poolSize = poolSize;
		this.objectFactory = objectFactory;
		this.pools = new MpmcObjectPool[poolSize];
		for (int i=0; i<poolSize; ++i){
			IPooledObject[] initObjects = null;
			if (initSize > 0){
				initObjects = new IPooledObject[initSize];
				for (int n=0; n<initSize; ++n){
					initObjects[n] = objectFactory.createInstance();
				}
			}
			this.pools[i] = new MpmcObjectPool<E>(maxSize, initObjects);
		}
	}

	/**
	 * 从池中获取一个可用的对象
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public E borrowObject(){
		int i = 0;
		if (poolSize > 1){
			i = polling;
			if (i >= poolSize){
				i = 0;
			}
		}
		
		MpmcObjectPool<E> pool = pools[i];
		IPooledObject po = pool.borrowObject();
		if (po == null){
			po = objectFactory.createInstance();
			po.onBorrowed(pool);
		}
		
		if (poolSize > 1){
			polling = ++i;
		}
		return (E) po;
	}
}
