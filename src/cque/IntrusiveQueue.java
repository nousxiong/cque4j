/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 嵌入式单线程队列
 */
public class IntrusiveQueue<E extends AbstractNode> {
	private AbstractNode head;
	private AbstractNode tail;
	private int size = 0;
	
	/**
	 * 使用由用户之前保存的节点池向队尾插入一个元素
	 * @param pool
	 * @param e
	 */
	public void add(E e){
		assert e != null;
		
		++size;
		if (tail == null){
			assert head == null;
			head = e;
			tail = e;
			return;
		}
		tail.next = e;
		tail = e;
	}
	
	/**
	 * 从队列中移除一个指定的元素，使用Object.equals来比较
	 * 警告：被移除的元素不会自动调用release，用户要自己保证（通过调用e的release）
	 * @param e
	 * @return
	 */
	public boolean remove(E e){
		if (e == null || head == null){
			return false;
		}
		
		// 遍历，尝试查找并移除e
		for (AbstractNode i = head, last = null; i != null; last = i, i = i.next){
			if (e.equals(i)){
				// 找到，从链表中移除
				if (last == null){
					if (head == tail){
						tail = i.next;
					}
					head = i.next;
				}else{
					last.next = i.next;
					if (i == tail){
						tail = last;
					}
				}
				i.next = null;
				--size;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 清空队列
	 */
	public void clear(){
		while (true){
			AbstractNode n = pollNode();
			if (n == null){
				break;
			}
			n.release();
		}
	}
	
	/**
	 * @return 如果队列空返回null；反之一个有效的元素
	 */
	@SuppressWarnings("unchecked")
	public E poll(){
		return (E) pollNode();
	}
	
	@SuppressWarnings("unchecked")
	public E peek(){
		return (E) head;
	}
	
	@SuppressWarnings("unchecked")
	public E next(E prev){
		if (prev != null){
			return (E) prev.next;
		}
		return null;
	}
	
	/**
	 * 返回当前队列中的元素数量
	 * @return
	 */
	public int size(){
		return size;
	}
	
	/**
	 * 是否队列为空
	 * @return
	 */
	public boolean isEmpty(){
		return size() == 0;
	}
	
	private AbstractNode pollNode(){
		if (head == null){
			return null;
		}
		
		AbstractNode n = head;
		head = n.next;
		n.next = null;
		if (head == null){
			assert n == tail;
			tail = null;
		}
		--size;
		return n;
	}
}
