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
	private ConcurrentNodePool pool;
	
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
		this(new ConcurrentNodePool(nodeFactory, initPoolSize, maxPoolSize));
	}
	
	/**
	 * 使用用户指定的节点池来创建队列
	 * @param pool 外部用户创建的节点池
	 */
	public LinkedQueue(ConcurrentNodePool pool){
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
	 * @return 如果队列空返回null；反之一个有效的元素
	 */
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
		return freeNode(n);
	}
	
	private Node<E> getNode(INodePool pool, E e){
		Node<E> n = this.pool.get(pool);
		n.setItem(e);
		return n;
	}
	
	private INode fetchNext(INode n){
		INode e = n.getNext();
		n.setNext(null);
		return e;
	}
	
	@SuppressWarnings("unchecked")
	private E freeNode(INode n){
		Node<E> node = (Node<E>) n;
		E e = node.getItem();
		n.release();
		return e;
	}
}
