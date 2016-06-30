/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 嵌入式单线程队列
 */
public class IntrusiveQueue<E> {
	private INode head;
	private INode tail;
	
	/**
	 * 使用由用户之前保存的节点池向队尾插入一个元素
	 * @param pool
	 * @param e
	 */
	public void add(INode e){
		assert e != null;
		
		if (tail == null){
			assert head == null;
			head = e;
			tail = e;
		}
		tail.setNext(e);
		tail = e;
	}
	
	/**
	 * @return 如果队列空返回null；反之一个有效的元素
	 */
	@SuppressWarnings("unchecked")
	public E poll(){
		if (head == null){
			return null;
		}
		
		INode n = head;
		head = fetchNext(n);
		if (head == null){
			assert n == tail;
			tail = null;
		}
		return (E) n;
	}
	
	private INode fetchNext(INode n){
		INode e = n.getNext();
		n.setNext(null);
		return e;
	}
}
