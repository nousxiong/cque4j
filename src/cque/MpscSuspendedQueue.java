/**
 * 
 */
package cque;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cque.util.ISynchronizer;
import cque.util.NodeFactory;
import cque.util.PoolUtils;
import cque.util.ThreadSynchronizer;
import cque.util.UnsafeUtils;

/**
 * @author Xiong
 *
 */
@SuppressWarnings({ "restriction", "rawtypes"})
public class MpscSuspendedQueue<E> implements Iterable<E> {
	private ISynchronizer sync;
	private ConcurrentObjectPool<Node<E>> cpool;
	private AtomicInteger size = new AtomicInteger(0);
	volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
	volatile Node head;
	volatile Object p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111, p112, p113, p114, p115;
	volatile Node tail;
	
	
	public MpscSuspendedQueue(){
		this(new NodeFactory<E>(), PoolUtils.DEFAULT_POOL_SIZE, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	public MpscSuspendedQueue(ISynchronizer sync){
		this(sync, new NodeFactory<E>(), PoolUtils.DEFAULT_POOL_SIZE, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	public MpscSuspendedQueue(IObjectFactory nodeFactory, int poolSize, int initPoolSize, int maxPoolSize){
		this(new ThreadSynchronizer(), new NodeFactory<E>(), poolSize, initPoolSize, maxPoolSize);
	}
	
	public MpscSuspendedQueue(ISynchronizer sync, IObjectFactory nodeFactory, int poolSize, int initPoolSize, int maxPoolSize){
		if (sync == null){
			throw new IllegalArgumentException("sync null values not allowed");
		}
		
		this.sync = sync;
		this.cpool = new ConcurrentObjectPool<Node<E>>(nodeFactory, poolSize, initPoolSize, maxPoolSize);
	}
	
	/**
	 * 使用用户指定的节点池来创建队列
	 * @param cpool 外部用户创建的节点池
	 */
	public MpscSuspendedQueue(ConcurrentObjectPool<Node<E>> cpool){
		this(new ThreadSynchronizer(), cpool);
	}
	
	public MpscSuspendedQueue(ISynchronizer sync, ConcurrentObjectPool<Node<E>> cpool){
		if (sync == null){
			throw new IllegalArgumentException("sync null values not allowed");
		}
		
		this.sync = sync;
		this.cpool = cpool;
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
	public E poll() {
		final Node<E> n = peekNode();
		if (n == null){
			return null;
		}
		
		final E v = value(n);
		deq(n);
		n.release();
		return v;
	}

	/**
	 * 消费者，从队列中获取一个元素，如果队列空，则阻塞指定的时间等待
	 * @param timeout
	 * @param unit
	 * @return
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
	public E peek() {
		final Node<E> n = peekNode();
		return n != null ? value(n) : null;
	}
	
	/**
	 * 生产者，放入队列一个元素，不会阻塞队列，这个方法永远返回成功
	 * @param e
	 * @return
	 */
	public boolean add(E e){
		if (e == null){
			throw new IllegalArgumentException("null values not allowed");
		}
		
		Node<E> node = cpool.borrowObject();
		node.value = e;
		return enq(node);
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
			E ne = itr.next();
			if (e.equals(ne)){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 消费者，尝试查找并移除指定的元素，使用Object.equals方法比较
	 * @param e
	 * @return
	 */
	public boolean remove(E e){
		if (e == null){
			return false;
		}
		
		QueueIterator<E> itr = iterator();
		while (itr.hasNext()){
			E ne = itr.next();
			if (e.equals(ne)){
				itr.remove();
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 消费者，清空队列
	 */
	public void clear(){
		Node<E> node = null;
		while ((node = peekNode()) != null){
			deq(node);
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
	public boolean isEmpty(){
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
	
	@SuppressWarnings("unchecked")
	Node<E> succ(final Node<E> node) {
		if (node == null) {
			return peekNode();
		}

		if (tail == node) {
			return null;
		}

		AbstractNode succ = null;
		while ((succ = node.next) == null)
			; // wait for next
		return (Node<E>) succ;
	}
	
	private boolean enq(final Node<E> node){
		AbstractNode t = null;
		do{
			t = tail;
			node.prev = t;
		}while (!compareAndSetTail(t, node));
		
		if (t == null){
			head = node;
		}else{
			t.next = node;
		}
		size.incrementAndGet();
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private Node<E> del(Node<E> node) {
		if (isHead(node)) {
			deq(node);
			return null;
		}

		clearValue(node);

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
		return (Node<E>) prev;
	}
	
	class LinkedQueueIterator implements QueueIterator<E> {
		Node<E> n = null;

		@Override
		public boolean hasNext() {
			return succ(n) != null;
		}

		@Override
		public E value() {
			return MpscSuspendedQueue.this.value(n);
		}

		@Override
		public void deq() {
			MpscSuspendedQueue.this.deq(n);
			n.release();
		}

		@Override
		public void reset() {
			if (n != null){
				n.release();
			}
			n = null;
		}

		@Override
		public E next() {
			n = succ(n);
			return value();
		}

		@Override
		public void remove() {
			Node<E> node = n;
			n = del(n);
			node.release();
		}
	}
	
	private E value(Node<E> node){
		return node.value;
	}

	private void deq(final Node<E> node){
		clearValue(node);

		AbstractNode h = node.next;
		if (h == null){
			orderedSetHead(null);
			if (tail == node && compareAndSetTail(node, null)){
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

	@SuppressWarnings("unchecked")
	private Node<E> peekNode(){
		if (tail == null){
			return null;
		}

		for (;;){
			Node h = null;
			if ((h = head) != null){
				return h;
			}
		}
	}

	private boolean isHead(Node<E> node) {
		return node.prev == null;
	}
	

	static final sun.misc.Unsafe UNSAFE;
	private static final long headOffset;
	private static final long tailOffset;
	private static final long nextOffset;
	private static final long prevOffset;
	private static final long valueOffset;

	static{
		try{
			UNSAFE = UnsafeUtils.getUnsafe();
			headOffset = UNSAFE.objectFieldOffset(MpscSyncLinkedQueue.class.getDeclaredField("head"));
			tailOffset = UNSAFE.objectFieldOffset(MpscSyncLinkedQueue.class.getDeclaredField("tail"));
			nextOffset = UNSAFE.objectFieldOffset(AbstractNode.class.getDeclaredField("next"));
			prevOffset = UNSAFE.objectFieldOffset(AbstractNode.class.getDeclaredField("prev"));
			valueOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("value"));
		}catch (Exception ex){
			throw new Error(ex);
		}
	}

	/**
	 * CAS head field. Used only by enq.
	 */
	boolean compareAndSetHead(AbstractNode update) {
		return UNSAFE.compareAndSwapObject(this, headOffset, null, update);
	}

	void orderedSetHead(AbstractNode value) {
		UNSAFE.putOrderedObject(this, headOffset, value);
	}

	void volatileSetHead(AbstractNode value) {
		UNSAFE.putObjectVolatile(this, headOffset, value);
	}

	/**
	 * CAS tail field. Used only by enq.
	 */
	boolean compareAndSetTail(AbstractNode expect, AbstractNode update) {
		return UNSAFE.compareAndSwapObject(this, tailOffset, expect, update);
	}

	/**
	 * CAS next field of a node.
	 */
	static boolean compareAndSetNext(AbstractNode node, AbstractNode expect, AbstractNode update) {
		return UNSAFE.compareAndSwapObject(node, nextOffset, expect, update);
	}

	static void clearNext(AbstractNode node) {
		UNSAFE.putOrderedObject(node, nextOffset, null);
	}

	static void clearPrev(AbstractNode node) {
		UNSAFE.putOrderedObject(node, prevOffset, null);
	}
	
	void clearValue(Node node) {
		UNSAFE.putOrderedObject(node, valueOffset, null);
	}
}
