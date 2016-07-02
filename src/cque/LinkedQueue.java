/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 非嵌入式单线程队列
 */
public class LinkedQueue<E> {
	private INode head;
	private INode tail;
	private ConcurrentNodePool<Node<E>> pool;
	private int size = 0;
	
	/**
	 * 创建一个默认的队列
	 */
	public LinkedQueue(){
		this(new NodeFactory<E>(), 0, Integer.MAX_VALUE);
	}
	
	/**
	 * 使用指定的参数来创建队列
	 * @param nodeFactory 节点工厂，可以为null
	 * @param initPoolSize 池初始大小
	 * @param maxPoolSize 池最大大小，可以小于池初始大小
	 */
	public LinkedQueue(INodeFactory nodeFactory, int initPoolSize, int maxPoolSize){
		this(new ConcurrentNodePool<Node<E>>(nodeFactory, initPoolSize, maxPoolSize));
	}
	
	/**
	 * 使用用户指定的节点池来创建队列
	 * @param pool 外部用户创建的节点池
	 */
	public LinkedQueue(ConcurrentNodePool<Node<E>> pool){
		this.pool = pool;
	}
	
	/**
	 * 取得当前线程的节点池
	 * @return 节点池
	 */
	public INodePool getLocalPool(){
		return pool.getLocalPool();
	}

	/**
	 * 向队尾插入一个元素
	 * @param e 不能是null
	 */
	public void add(E e){
		INodePool pool = getLocalPool();
		add(pool, e);
	}
	
	/**
	 * 使用由用户之前保存的节点池向队尾插入一个元素
	 * @param pool
	 * @param e
	 */
	public void add(INodePool pool, E e){
		assert e != null;
		Node<E> n = getNode(pool, e);
		
		++size;
		if (tail == null){
			assert head == null;
			head = n;
			tail = n;
			return;
		}
		tail.setNext(n);
		tail = n;
	}
	
	/**
	 * 从队列中移除一个指定的元素，使用Object.equals来比较
	 * @param e
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean remove(E e){
		if (e == null || head == null){
			return false;
		}
		
		// 遍历，尝试查找并移除e
		for (INode i = head, last = null; i != null; last = i, i = i.getNext()){
			if (e.equals(((Node<E>) i).getItem())){
				// 找到，从链表中移除
				if (last == null){
					if (head == tail){
						tail = i.getNext();
					}
					head = i.getNext();
				}else{
					last.setNext(i.getNext());
					if (i == tail){
						tail = last;
					}
				}
				i.release();
				--size;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return 如果队列空返回null；反之一个有效的元素
	 */
	public E poll(){
		INode n = pollNode();
		if (n != null){
			return freeNode(n);
		}else{
			return null;
		}
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
	
	/**
	 * 清空队列
	 */
	public void clear(){
		while (true){
			INode n = pollNode();
			if (n == null){
				break;
			}
			n.release();
		}
	}
	
	private INode pollNode(){
		if (head == null){
			return null;
		}
		
		INode n = head;
		head = n.fetchNext();
		if (head == null){
			assert n == tail;
			tail = null;
		}
		--size;
		return n;
	}
	
	private Node<E> getNode(INodePool pool, E e){
		Node<E> n = this.pool.get(pool);
		n.setItem(e);
		return n;
	}
	
	@SuppressWarnings("unchecked")
	private E freeNode(INode n){
		Node<E> node = (Node<E>) n;
		E e = node.getItem();
		n.release();
		return e;
	}
}
