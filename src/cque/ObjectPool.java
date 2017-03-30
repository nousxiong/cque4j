/**
 * 
 */
package cque;

import cque.util.ArrayQueue;

/**
 * @author Xiong
 * 单线程对象池
 */
public class ObjectPool<E extends IPooledObject> implements IObjectPool<E> {
	private ArrayQueue<E> que;
	
	/**
	 * 创建默认的节点池
	 */
	public ObjectPool(int capacity){
		this(capacity, null);
	}
	
	/**
	 * 根据指定大小参数创建对象池
	 * @param capacity
	 * @param initObjects
	 */
	@SuppressWarnings("unchecked")
	public ObjectPool(int capacity, IPooledObject[] initObjects){
		if (initObjects != null && capacity < initObjects.length){
			throw new IllegalArgumentException("capacity < initObjects.length not allowed");
		}

		this.que = new ArrayQueue<E>(capacity);
		if (initObjects != null){
			for (IPooledObject po : initObjects){
				this.que.add((E) po);
			}
		}
	}
	
	@Override
	public E borrowObject(){
		E po = que.poll();
		if (po != null){
			po.onBorrowed(this);
		}
		return po;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void returnObject(IPooledObject po) {
		if (po == null){
			return;
		}
		
		po.onReturn();
		que.add((E) po);
	}
	
	@Override
	public int size(){
		return que.size();
	}
	
	@Override
	public boolean isEmpty(){
		return que.isEmpty();
	}

}
