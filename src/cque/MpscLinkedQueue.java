/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 非阻塞、非嵌入式多生产者单消费者队列
 * 参考：http://www.boost.org/doc/libs/1_59_0/doc/html/atomic/usage_examples.html#boost_atomic.usage_examples.mp_queue 
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class MpscLinkedQueue<E> {
	private volatile INode head;
	private INode queue;
	private ConcurrentNodePool pool;
	
	/**
	 * 创建一个默认的队列
	 */
	public MpscLinkedQueue(){
		this(new NodeFactory<E>(), 0, Integer.MAX_VALUE);
	}
	
	/**
	 * 使用指定的参数来创建队列
	 * @param nodeFactory 节点工厂，可以为null
	 * @param initPoolSize 池初始大小
	 * @param maxPoolSize 池最大大小，可以小于池初始大小
	 */
	public MpscLinkedQueue(INodeFactory nodeFactory, int initPoolSize, int maxPoolSize){
		this.pool = new ConcurrentNodePool(nodeFactory, initPoolSize, maxPoolSize);
	}
	
	/**
	 * 使用用户指定的节点池来创建队列
	 * @param pool 外部用户创建的节点池
	 */
	public MpscLinkedQueue(ConcurrentNodePool pool){
		this.pool = pool;
	}
	
	/**
	 * 取得当前线程的节点池
	 * @return 节点池
	 */
	public MpscNodePool getLocalPool(){
		return pool.getLocalPool();
	}

	/**
	 * 向队尾插入一个元素
	 * @param e 不能是null
	 */
	public void add(E e){
		MpscNodePool pool = getLocalPool();
		add(pool, e);
	}
	
	/**
	 * 使用由用户之前保存的节点池向队尾插入一个元素
	 * @param pool
	 * @param e
	 */
	public void add(MpscNodePool pool, E e){
		assert e != null;
		Node<E> n = getNode(pool, e);
		INode h = null;
		do{
			h = head;
			n.setNext(h);
		}while (!UNSAFE.compareAndSwapObject(this, headOffset, h, n));
	}
	
	/**
	 * 警告：只能单读线程调用
	 * @return 如果队列空返回null；反之一个有效的元素
	 */
	public E poll(){
		INode n = dequeue();
		if (n == null){
			do{
				n = head;
			}while (n != null && !UNSAFE.compareAndSwapObject(this, headOffset, n, null));
			
			if (n != null && n.getNext() != null){
				// 反转
				INode last = n;
				INode first = null;
				while (last != null){
					INode tmp = last;
					last = last.getNext();
					tmp.setNext(first);
					first = tmp;
				}
				n = first;
				setQueue(n.getNext());
			}
		}
		
		if (n != null){
			return freeNode(n);
		}else{
			return null;
		}
	}
	
	private INode dequeue(){
		INode n = queue;
		if (n != null){
			queue = n.getNext();
			n.setNext(null);
		}
		return n;
	}
	
	private void setQueue(INode n){
		assert queue == null;
		queue = n;
	}
	
	private Node<E> getNode(MpscNodePool pool, E e){
		Node<E> n = this.pool.get(pool);
		n.setItem(e);
		return n;
	}
	
	@SuppressWarnings("unchecked")
	private E freeNode(INode n){
		Node<E> node = (Node<E>) n;
		E e = node.getItem();
		n.dispose();
		return e;
	}

	private static final sun.misc.Unsafe UNSAFE;
	private static final long headOffset;
	static{
		try{
			UNSAFE = Unsafe.get();
			Class k = MpscLinkedQueue.class;
			headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
		}catch (Exception e){
			throw new Error(e);
		}
	}
}
