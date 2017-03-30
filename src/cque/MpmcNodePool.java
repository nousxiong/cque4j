/**
 * 
 */
package cque;

/**
 * @author Xiong
 *
 */
public class MpmcNodePool<E extends INode> implements INodePool<E> {
	private MpmcArrayQueue<E> que;
	
	public MpmcNodePool(int capacity){
		this(capacity, null);
	}
	
	@SuppressWarnings("unchecked")
	public MpmcNodePool(int capacity, INode[] initNodes){
		if (initNodes != null && capacity < initNodes.length){
			throw new IllegalArgumentException("capacity < initNodes.length not allowed");
		}

		this.que = new MpmcArrayQueue<E>(capacity);
		for (INode node : initNodes){
			this.que.add((E) node);
		}
	}

	@Override
	public E borrowObject() {
		E n = que.poll();
		if (n != null){
			n.onBorrowed(this);
		}
		return n;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void returnObject(IPooledObject n) {
		if (n == null){
			return;
		}
		
		n.onReturn();
		que.add((E) n);
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
