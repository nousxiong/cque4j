/**
 * 
 */
package cque;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cque.util.ISynchronizer;
import cque.util.ThreadSynchronizer;
import cque.util.UnsafeUtils;

/**
 * @author Xiong
 *
 */
@SuppressWarnings("restriction")
public class IntrusiveSuspendedQueue<E extends AbstractNode>  implements Iterable<E> {
	private ISynchronizer sync;
	private AtomicInteger size = new AtomicInteger(0);
	volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
	volatile AbstractNode head;
	volatile Object p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111, p112, p113, p114, p115;
	volatile AbstractNode tail;
	
	public IntrusiveSuspendedQueue(){
		this(new ThreadSynchronizer());
	}
	
	public IntrusiveSuspendedQueue(ISynchronizer sync){
		if (sync == null){
			throw new IllegalArgumentException("sync null values not allowed");
		}
		this.sync = sync;
	}
	
	/**
	 * 生产者，放入队列一个元素，如果队列正在阻塞中，唤醒它
	 * @param e
	 */
	public void put(E e){
		add(e);
		if (sync.shouldSignal() /*&& peek() == e*/){
			sync.signal();
		}
	}
	
	/**
	 * 消费者，从队列中取出一个元素，如果队列空，则一直阻塞等待直到有元素或者中断
	 * @return
	 * @throws InterruptedException
	 */
	public E take() throws InterruptedException {
		E e = poll();
		if (e == null){
			sync.register();
			try{
				e = poll();
				while (e == null){
					sync.await();
					e = poll();
				}
			}finally{
				sync.unregister();
			}
		}
		return e;
	}

	/**
	 * 消费者，尝试从队列中获取一个元素，可能返回空，不会阻塞
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E poll() {
		final AbstractNode n = peekNode();
		if (n == null) {
			return null;
		}

		deq(n);
		return (E) n;
	}

	/**
	 * 消费者，从队列中获取一个元素，如果队列空，则阻塞指定的时间等待
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		E e = poll();
		if (e == null){
			long left = unit.toNanos(timeout);
			sync.register();
			try{
				e = poll();
				while (e == null){
					left = sync.awaitNanos(left);
					if (left < 0){
						return null;
					}
					e = poll();
				}
			}finally{
				sync.unregister();
			}
		}
		return e;
	}

	/**
	 * 消费者，返回当前队列的头部，但不移除
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E peek() {
		return (E) peekNode();
	}
	
	/**
	 * 生产者，放入队列一个元素，不会阻塞队列，这个方法永远返回成功
	 * @param e
	 * @return
	 */
	public boolean add(final E e) {
		AbstractNode t = null;
		do {
			t = tail;
			e.prev = t;
		} while (!compareAndSetTail(t, e));
		if (t == null){
			head = e;
		}else{
			t.next = e;
		}
		size.incrementAndGet();
		return true;
	}
	
	/**
	 * 消费者，队列是否包含指定的元素，使用Object.equals方法比较
	 * @param e
	 * @return
	 */
	public boolean contains(E e){
		if (e == null){
			return false;
		}
		
		QueueIterator<E> itr = iterator();
		while (itr.hasNext()){
			AbstractNode n = itr.next();
			if (e.equals(n)){
				return true;
			}
		}
		
		return false;
		
	}
	
	/**
	 * 消费者，尝试查找并移除指定的元素，使用Object.equals方法比较
	 * 如果两个对象不是一个地址，则调用release释放移除的对象
	 * @param e
	 * @return
	 */
	public boolean remove(E e){
		if (e == null){
			return false;
		}
		
		QueueIterator<E> itr = iterator();
		while (itr.hasNext()){
			AbstractNode n = itr.next();
			if (e.equals(n)){
				itr.remove();
				if (n != e){
					n.release();
				}
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 消费者，清空队列
	 */
	public void clear(){
		AbstractNode node = null;
		while ((node = poll()) != null){
			node.release();
		}
	}
	
	/**
	 * 返回当前队列大小
	 * @return
	 */
	public int size() {
//		int n = 0;
//		for (AbstractNode p = tail; p != null; p = p.prev) {
//			n++;
//		}
//		return n;
		return size.get();
	}
	
	/**
	 * 是否队列为空
	 * @return
	 */
	public boolean isEmpty() {
//		return peekNode() == null;
		return size.get() == 0;
	}

	/**
	 * 返回一个迭代器
	 */
	@Override
	public QueueIterator<E> iterator(){
		return new LinkedQueueIterator();
	}
	
	AbstractNode del(AbstractNode node) {
		if (isHead(node)) {
			deq(node);
			return null;
		}

		final AbstractNode prev = node.prev;
		prev.next = null;
		final AbstractNode t = tail;
		if (t != node || !compareAndSetTail(t, node.prev)) {
			// neither head nor tail
			while (node.next == null)
				; // wait for next
			prev.next = node.next;
			node.next.prev = prev;
		}

		clearNext(node);
		clearPrev(node);
		size.decrementAndGet();
		return prev;
	}
	
	AbstractNode succ(final AbstractNode node) {
		if (node == null) {
			return peekNode();
		}

		if (tail == node) {
			return null;
		}

		AbstractNode succ = null;
		while ((succ = node.next) == null)
			; // wait for next
		return succ;
	}
	
	class LinkedQueueIterator implements QueueIterator<E> {
		AbstractNode n = null;

		@Override
		public boolean hasNext() {
			return succ(n) != null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public E value() {
			return (E) n;
		}

		@Override
		public void deq() {
			IntrusiveSuspendedQueue.this.deq(n);
		}

		@Override
		public void reset() {
			n = null;
		}

		@Override
		public E next() {
			n = succ(n);
			return value();
		}

		@Override
		public void remove() {
			n = del(n);
		}
	}

	private void deq(final AbstractNode node) {
		AbstractNode h = node.next;
		if (h == null) {
			orderedSetHead(null);
			if (tail == node && compareAndSetTail(node, null)) {
				node.next = null;
				size.decrementAndGet();
				return;
			}
			while ((h = node.next) == null)
				;
		}
		orderedSetHead(h); // head = h;
		clearPrev(h); // h.prev = null;

		clearNext(node);
		clearPrev(node);
		size.decrementAndGet();
	}

	private AbstractNode peekNode() {
		if (tail == null) {
			return null;
		}

		for (;;) {
			AbstractNode h = null;
			if ((h = head) != null) {
				return h;
			}
		}
	}

	private boolean isHead(AbstractNode node) {
		return node.prev == null;
	}

	static final sun.misc.Unsafe UNSAFE;
	private static final long headOffset;
	private static final long tailOffset;
	private static final long nextOffset;
	private static final long prevOffset;

	static {
		try {
			UNSAFE = UnsafeUtils.getUnsafe();
			headOffset = UNSAFE.objectFieldOffset(IntrusiveSuspendedQueue.class.getDeclaredField("head"));
			tailOffset = UNSAFE.objectFieldOffset(IntrusiveSuspendedQueue.class.getDeclaredField("tail"));
			nextOffset = UNSAFE.objectFieldOffset(AbstractNode.class.getDeclaredField("next"));
			prevOffset = UNSAFE.objectFieldOffset(AbstractNode.class.getDeclaredField("prev"));
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	boolean compareAndSetHead(AbstractNode update) {
		return UNSAFE.compareAndSwapObject(this, headOffset, null, update);
	}

	void orderedSetHead(AbstractNode value) {
		UNSAFE.putOrderedObject(this, headOffset, value);
	}

	void volatileSetHead(AbstractNode value) {
		UNSAFE.putObjectVolatile(this, headOffset, value);
	}

	boolean compareAndSetTail(AbstractNode expect, AbstractNode update) {
		return UNSAFE.compareAndSwapObject(this, tailOffset, expect, update);
	}

	static boolean compareAndSetNext(AbstractNode node, AbstractNode expect, AbstractNode update) {
		return UNSAFE.compareAndSwapObject(node, nextOffset, expect, update);
	}

	static void clearNext(AbstractNode node) {
		UNSAFE.putOrderedObject(node, nextOffset, null);
	}

	static void clearPrev(AbstractNode node) {
		UNSAFE.putOrderedObject(node, prevOffset, null);
	}
}
