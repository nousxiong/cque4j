/**
 * 
 */
package cque;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Xiong
 * 非嵌入式多生产者单消费者队列
 * 参考：http://www.boost.org/doc/libs/1_59_0/doc/html/atomic/usage_examples.html#boost_atomic.usage_examples.mp_queue 
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class MpscLinkedQueue<E> {
	private volatile INode head;
	private INode queue;
	private ConcurrentNodePool<Node<E>> pool;
	private volatile boolean blocked = false;
	private AtomicInteger size = new AtomicInteger(0);
	
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
		this.pool = new ConcurrentNodePool<Node<E>>(nodeFactory, initPoolSize, maxPoolSize);
	}
	
	/**
	 * 使用用户指定的节点池来创建队列
	 * @param pool 外部用户创建的节点池
	 */
	public MpscLinkedQueue(ConcurrentNodePool<Node<E>> pool){
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
		INode h = null;
		do{
			h = head;
			n.setNext(h);
		}while (!UNSAFE.compareAndSwapObject(this, headOffset, h, n));
		size.incrementAndGet();
	}
	
	/**
	 * 从队列中移除一个指定的元素，使用Object.equals来比较
	 * 警告：只能单读线程调用
	 * @param e
	 * @return true移除成功
	 */
	public boolean remove(E e){
		if (e == null){
			return false;
		}
		
		// 首先遍历queue，尝试查找并移除e
		INode last = null;
		for (INode i = queue; i != null; last = i, i = i.getNext()){
			if (find(last, i, e)){
				return true;
			}
		}
		
		// 尝试把head放到last上
		INode n = null;
		do{
			n = head;
		}while (n != null && !UNSAFE.compareAndSwapObject(this, headOffset, n, null));
		
		if (n != null){
			// 反转
			n = Node.reverse(n);
			// 链接到last上
			if (last != null){
				last.setNext(n);
			}else{
				setQueue(n);
			}
			
			// 遍历n，尝试查找并移除e
			for (INode i = n; i != null; last = i, i = i.getNext()){
				if (find(last, i, e)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 清空队列
	 * 警告：只能在单读线程调用
	 * 警告：被清空的元素会自动调用release
	 */
	public void clear(){
		// 首先尝试清空queue
		int clearSize = clearQueue();
		
		// 尝试把head放到queue上
		INode n = null;
		do{
			n = head;
		}while (n != null && !UNSAFE.compareAndSwapObject(this, headOffset, n, null));
		
		if (n != null){
			// 不用反转，直接链接到queue上，然后再清空
			setQueue(n);
			clearSize += clearQueue();
		}
		
		if (clearSize > 0){
			size.addAndGet(-clearSize);
		}
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
				n = Node.reverse(n);
				setQueue(n.getNext());
			}
		}
		
		if (n != null){
			size.decrementAndGet();
			return freeNode(n);
		}else{
			return null;
		}
	}
	
	/**
	 * 向队尾插入一个元素，如果发现单读线程在阻塞，唤醒它
	 * @param e
	 */
	public void put(E e){
		INodePool pool = getLocalPool();
		put(pool, e);
	}
	
	/**
	 * 使用由用户之前保存的节点池向队尾插入一个元素，如果发现单读线程在阻塞，唤醒它
	 * @param pool
	 * @param e
	 */
	public void put(INodePool pool, E e){
		add(pool, e);
		// 如果发现单读线程在阻塞，唤醒它
		if (isBlocked()){
			synchronized (this){
				if (isBlocked()){
					this.notify();
				}
			}
		}
	}
	
	/**
	 * 警告：只能在单读线程调用
	 * @return E or null
	 */
	public E take(){
		assert !isBlocked();
		E e = poll();
		if (e != null){
			return e;
		}

		synchronized (this){
			block();
			e = poll();
			while (e == null){
				try{
					this.wait();
				}catch (InterruptedException ex){
				}
				
				e = poll();
			}
		}

		unblock();
		return e;
	}
	
	/**
	 * 警告：只能在单读线程调用
	 * @param timeout 超时时间（毫秒）
	 * @return
	 */
	public E poll(long timeout){
		return poll(timeout, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 警告：只能在单读线程调用
	 * @param timeout
	 * @param tu
	 * @return
	 */
	public E poll(long timeout, TimeUnit tu){
		assert !isBlocked();
		if (timeout <= 0){
			return poll();
		}
		
		if (timeout == Long.MAX_VALUE){
			return take();
		}
		
		E e = poll();
		if (e != null){
			return e;
		}

		timeout = tu.toMillis(timeout);
		synchronized (this){
			block();
			e = poll();
			long currTimeout = timeout;
			while (e == null){
				long bt = System.currentTimeMillis();
				try{
					this.wait(currTimeout);
				}catch (InterruptedException ex){
				}
				long eclipse = System.currentTimeMillis() - bt;
				
				e = poll();
				if (eclipse >= currTimeout){
					break;
				}
				
				currTimeout -= eclipse;
			}
		}

		unblock();
		return e;
	}
	
	/**
	 * 返回当前队列中的元素数量
	 * @return
	 */
	public int size(){
		return size.get();
	}
	
	/**
	 * 返回队列是否为空
	 * @return
	 */
	public boolean isEmpty(){
		return size() == 0;
	}
	
	@SuppressWarnings("unchecked")
	private boolean find(INode last, INode i, E e){
		if (e.equals(((Node<E>) i).getItem())){
			// 找到，从链表中移除
			INode nx = i.fetchNext();
			if (last == null){
				queue = nx;
			}else{
				last.setNext(nx);
			}
			i.release();
			size.decrementAndGet();
			return true;
		}
		return false;
	}
	
	private boolean isBlocked(){
		return blocked;
	}
	
	private void block(){
		blocked = true;
	}
	
	private void unblock(){
		blocked = false;
	}
	
	private INode dequeue(){
		INode n = queue;
		if (n != null){
			queue = n.fetchNext();
		}
		return n;
	}
	
	private void setQueue(INode n){
		assert queue == null;
		queue = n;
	}
	
	private int clearQueue(){
		int clearSize = 0;
		while (true){
			INode n = dequeue();
			if (n == null){
				break;
			}
			++clearSize;
			n.release();
		}
		return clearSize;
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
