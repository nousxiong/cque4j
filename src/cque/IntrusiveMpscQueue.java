/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 非阻塞、嵌入式多生产者单消费者队列
 * 参考：http://www.boost.org/doc/libs/1_59_0/doc/html/atomic/usage_examples.html#boost_atomic.usage_examples.mp_queue 
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class IntrusiveMpscQueue<E> {
	private volatile INode head;
	private INode queue;
	
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
