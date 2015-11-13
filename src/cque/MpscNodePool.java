/**
 * 
 */
package cque;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Xiong
 * 非阻塞多生产者单消费者节点池
 * 参考：http://www.boost.org/doc/libs/1_59_0/doc/html/atomic/usage_examples.html#boost_atomic.usage_examples.mp_queue 
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class MpscNodePool implements INodePool {
	private volatile INode head;
	private AtomicInteger size = new AtomicInteger(0);
	private final int maxSize;
	private INode cache;
	
	/**
	 * 创建默认的节点池
	 */
	public MpscNodePool(){
		this(null, Integer.MAX_VALUE);
	}
	
	/**
	 * 根据指定大小参数创建节点池
	 * @param initNodes 初始池，用户需要创建一个INode数组，可以为null
	 * @param maxSize 池最大大小，可以小于池初始大小
	 */
	public MpscNodePool(INode[] initNodes, int maxSize){
		this.maxSize = maxSize;
		if (initNodes != null){
			for (INode n : initNodes){
				n.setNext(head);
				head = n;
			}
			size.set(initNodes.length);
		}
	}
	
	/**
	 * 从池中尝试获取一个节点
	 * @return 如果池为空返回null
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(){
		INode n = getCache();
		if (n == null){
			do{
				n = head;
			}while (n != null && !UNSAFE.compareAndSwapObject(this, headOffset, n, null));

			if (n != null && n.getNext() != null){
				setCache(n.getNext());
			}
		}

		if (n != null){
			size.decrementAndGet();
			n.onGet(this);
			return (T) n;
		}else{
			return null;
		}
	}
	
	@Override
	public void free(INode n){
		if (n == null){
			return;
		}

		n.onFree();
		if (size.get() >= maxSize){
			return;
		}
		
		INode h = null;
		do{
			h = head;
			n.setNext(h);
		}while (!UNSAFE.compareAndSwapObject(this, headOffset, h, n));
		size.incrementAndGet();
	}
	
	public int size(){
		return size.get();
	}
	
	public boolean isEmpty(){
		return size() == 0;
	}
	
	private INode getCache(){
		INode n = cache;
		if (n != null){
			cache = n.getNext();
			n.setNext(null);
		}
		return n;
	}
	
	private void setCache(INode n){
		assert cache == null;
		cache = n;
	}
	
	private static final sun.misc.Unsafe UNSAFE;
	private static final long headOffset;
	static{
		try{
			UNSAFE = Unsafe.get();
			Class k = MpscNodePool.class;
			headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
		}catch (Exception e){
			throw new Error(e);
		}
	}
}
