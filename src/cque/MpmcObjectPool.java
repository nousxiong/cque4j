/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 多生产者多消费者对象池
 */
public class MpmcObjectPool<E extends IPooledObject> implements IObjectPool<E> {
	private MpmcArrayQueue<E> que;
	
	public MpmcObjectPool(int capacity){
		this(capacity, null);
	}
	
	@SuppressWarnings("unchecked")
	public MpmcObjectPool(int capacity, IPooledObject[] initObjects){
		if (initObjects != null && capacity < initObjects.length){
			throw new IllegalArgumentException("capacity < initObjects.length not allowed");
		}

		this.que = new MpmcArrayQueue<E>(capacity);
		for (IPooledObject po : initObjects){
			this.que.add((E) po);
		}
	}

	@Override
	public E borrowObject() {
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
	public int size() {
		return que.size();
	}

	@Override
	public boolean isEmpty() {
		return que.isEmpty();
	}

}
