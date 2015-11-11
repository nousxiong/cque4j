/**
 * 
 */
package cque;

import java.util.concurrent.TimeUnit;

/**
 * @author Xiong
 * 非阻塞、嵌入式多生产者单消费者队列
 * 参考：http://www.boost.org/doc/libs/1_59_0/doc/html/atomic/usage_examples.html#boost_atomic.usage_examples.mp_queue 
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class IntrusiveMpscQueue<E> {
	private volatile INode head;
	private INode queue;
	private volatile boolean blocked = false;
	private Object condVar = new Object();
	
	/**
	 * 向队尾插入一个元素
	 * @param e 不能是null
	 */
	public void add(INode e){
		assert e != null;
		INode h = null;
		do{
			h = head;
			e.setNext(h);
		}while (!UNSAFE.compareAndSwapObject(this, headOffset, h, e));
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
			return (E) n;
		}else{
			return null;
		}
	}
	
	/**
	 * @param e
	 */
	public void put(INode e){
		add(e);
		// 如果发现单读线程在阻塞，唤醒它
		if (isBlocked()){
			synchronized (condVar){
				if (isBlocked()){
					condVar.notify();
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

		synchronized (condVar){
			block();
			e = poll();
			while (e == null){
				try{
					condVar.wait();
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
		synchronized (condVar){
			block();
			e = poll();
			long currTimeout = timeout;
			while (e == null){
				long bt = System.currentTimeMillis();
				try{
					condVar.wait(currTimeout);
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
			queue = n.getNext();
			n.setNext(null);
		}
		return n;
	}
	
	private void setQueue(INode n){
		assert queue == null;
		queue = n;
	}

	private static final sun.misc.Unsafe UNSAFE;
	private static final long headOffset;
	static{
		try{
			UNSAFE = Unsafe.get();
			Class k = IntrusiveMpscQueue.class;
			headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
		}catch (Exception e){
			throw new Error(e);
		}
	}
}
