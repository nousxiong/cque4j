/**
 * 
 */
package cque;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Xiong
 * 非阻塞、嵌入式多生产者单消费者队列
 * 参考：http://www.boost.org/doc/libs/1_59_0/doc/html/atomic/usage_examples.html#boost_atomic.usage_examples.mp_queue 
 */
@SuppressWarnings({ "restriction", "rawtypes", "unused"})
public class IntrusiveMpscQueue<E extends INode> {
	private final Lock sync;
	private final Condition cond;
	
	private volatile Object p001, p002, p003, p004, p005, p006, p007;
	private volatile INode head;
	private volatile boolean p101, p102, p103, p104, p105, p106, p107;
	private volatile boolean blocked = false;
	private volatile int p201, p202, p203, p204, p205, p206, p207;
	
	private INode queue;
	private final AtomicInteger size = new AtomicInteger(0);
	
	public IntrusiveMpscQueue(){
		this(new ReentrantLock());
	}
	
	public IntrusiveMpscQueue(Lock lock){
		if (lock == null){
			throw new NullPointerException();
		}
		this.sync = lock;
		this.cond = lock.newCondition();
	}
	
	/**
	 * 向队尾插入一个元素
	 * @param e 不能是null
	 */
	public void add(E e){
		assert e != null;
		INode h = null;
		do{
			h = head;
			e.setNext(h);
		}while (!UNSAFE.compareAndSwapObject(this, headOffset, h, e));
		size.incrementAndGet();
	}
	
	/**
	 * 从队列中移除一个指定的元素，使用Object.equals来比较
	 * 警告：只能单读线程调用
	 * 警告：被移除的元素不会自动调用release，用户要自己保证（通过调用e的release）
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
	 * 警告：只能在单读线程调用
	 * @return 如果队列空返回null；反之一个有效的元素
	 */
	@SuppressWarnings("unchecked")
	public E poll(){
		INode n = dequeue();
		if (n == null){
			do{
				n = head;
			}while (n != null && !UNSAFE.compareAndSwapObject(this, headOffset, n, null));
			
			if (n != null && n.getNext() != null){
				// 反转
				n = Node.reverse(n);
				setQueue(n.fetchNext());
			}
		}
		
		if (n != null){
			size.decrementAndGet();
			return (E) n;
		}else{
			return null;
		}
	}
	
	/**
	 * @param e
	 */
	public void put(E e){
		add(e);
		// 如果发现单读线程在阻塞，唤醒它
		if (isBlocked()){
			sync.lock();
			try{
				if (isBlocked()){
					cond.signal();
				}
			}finally{
				sync.unlock();
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

		sync.lock();
		try{
			block();
			e = poll();
			while (e == null){
				try{
					cond.await();
				}catch (InterruptedException ex){
				}
				
				e = poll();
			}
		}finally{
			sync.unlock();
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
		sync.lock();
		try{
			block();
			e = poll();
			long currTimeout = timeout;
			while (e == null){
				long bt = System.currentTimeMillis();
				try{
					cond.await(currTimeout, TimeUnit.MILLISECONDS);
				}catch (InterruptedException ex){
				}
				long eclipse = System.currentTimeMillis() - bt;
				
				e = poll();
				if (eclipse >= currTimeout){
					break;
				}
				
				currTimeout -= eclipse;
			}
		}finally{
			sync.unlock();
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
	
	private boolean find(INode last, INode i, E e){
		if (e.equals(i)){
			// 找到，从链表中移除
			INode nx = i.fetchNext();
			if (last == null){
				queue = nx;
			}else{
				last.setNext(nx);
			}
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

	private static final sun.misc.Unsafe UNSAFE;
	private static final long headOffset;
	static{
		try{
			UNSAFE = UnsafeUtils.get();
			Class k = IntrusiveMpscQueue.class;
			headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
		}catch (Exception e){
			throw new Error(e);
		}
	}
}
