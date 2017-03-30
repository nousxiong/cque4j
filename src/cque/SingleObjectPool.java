/**
 * 
 */
package cque;

import cque.util.PoolUtils;

/**
 * @author Xiong
 * 单线程节点池
 */
public class SingleObjectPool<E extends IPooledObject> implements IPolicyObjectPool<E> {
	private ObjectPool<E> pool;
	private IObjectFactory objectFactory;
	
	/**
	 * 创建默认的节点池
	 * @param nodeFactory 不可为null
	 */
	public SingleObjectPool(IObjectFactory objectFactory){
		this(objectFactory, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	/**
	 * 根据指定大小参数创建节点池
	 * @param nodeFactory 不可为null
	 * @param initSize
	 * @param maxSize
	 */
	public SingleObjectPool(IObjectFactory objectFactory, int initSize, int maxSize){
		if (objectFactory == null){
			throw new IllegalArgumentException("null values not allowed");
		}
		
		this.objectFactory = objectFactory;
		IPooledObject[] initObjects = null;
		if (initSize > 0){
			initObjects = new IPooledObject[initSize];
			for (int n=0; n<initSize; ++n){
				initObjects[n] = objectFactory.createInstance();
			}
		}
		this.pool = new ObjectPool<E>(maxSize, initObjects);
	}

	/**
	 * 从池中获取一个可用的节点
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public E borrowObject(){
		E e = pool.borrowObject();
		if (e == null){
			IPooledObject po = objectFactory.createInstance();
			po.onBorrowed(pool);
			e = (E) po;
		}
		return e;
	}
}
