/**
 * 
 */
package cque;

import java.util.concurrent.TimeUnit;

import cque.util.ISynchronizer;
import cque.util.ThreadSynchronizer;
import cque.util.UnsafeUtils;

/**
 * @author Xiong
 * 多生产者单消费者侵入式队列
 */
@SuppressWarnings("restriction")
public class IntrusiveSyncLinkedQueue<E extends AbstractNode> implements Iterable<E> {
	private ISynchronizer sync;
	volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
	volatile AbstractNode head;
	volatile Object p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111, p112, p113, p114, p115;
	volatile AbstractNode tail;
	
	public IntrusiveSyncLinkedQueue(){
		sync = new ThreadSynchronizer();
	}
	
	public IntrusiveSyncLinkedQueue(ISynchronizer sync){
		if (sync == null){
			throw new IllegalArgumentException("sync null values not allowed");
		}
		this.sync = sync;
	}
	
	public void put(E e){
		add(e);
		if (sync.shouldSignal() /*&& peek() == e*/){
			sync.signal();
		}
	}
	
	public E take(){
		E e = poll();
		if (e == null){
			sync.register();
			try{
				e = poll();
				while (e == null){
					sync.await();
					e = poll();
				}
			}catch (InterruptedException ie){
				throw new RuntimeException("InterruptedException");
			}finally{
				sync.unregister();
			}
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	public E poll() {
		final AbstractNode n = peekNode();
		if (n == null) {
			return null;
		}

		deq(n);
		return (E) n;
	}

	public E poll(long timeout, TimeUnit unit){
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
			}catch (InterruptedException ie){
				throw new RuntimeException("InterruptedException");
			}finally{
				sync.unregister();
			}
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	public E peek() {
		return (E) peekNode();
	}
	
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
		return true;
	}
	
	public boolean remove(E e){
		if (e == null){
			return false;
		}
		
		QueueIterator<E> itr = iterator();
		while (itr.hasNext()){
			AbstractNode n = itr.next();
			if (e.equals(n)){
				itr.remove();
				n.release();
				return true;
			}
		}
		
		return false;
	}
	
	public void clear(){
		AbstractNode node = null;
		while ((node = poll()) != null){
			node.release();
		}
	}
	
	public int size() {
		int n = 0;
		for (AbstractNode p = tail; p != null; p = p.prev) {
			n++;
		}
		return n;
	}
	
	public boolean isEmpty() {
		return peekNode() == null;
	}

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
			IntrusiveSyncLinkedQueue.this.deq(n);
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
				return;
			}
			while ((h = node.next) == null)
				;
		}
		orderedSetHead(h); // head = h;
		clearPrev(h); // h.prev = null;

		clearNext(node);
		clearPrev(node);
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
			headOffset = UNSAFE.objectFieldOffset(IntrusiveSyncLinkedQueue.class.getDeclaredField("head"));
			tailOffset = UNSAFE.objectFieldOffset(IntrusiveSyncLinkedQueue.class.getDeclaredField("tail"));
			nextOffset = UNSAFE.objectFieldOffset(AbstractNode.class.getDeclaredField("next"));
			prevOffset = UNSAFE.objectFieldOffset(AbstractNode.class.getDeclaredField("prev"));
		} catch (Exception ex) {
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
	static boolean compareAndSetNext(AbstractNode node,
			AbstractNode expect, AbstractNode update) {
		return UNSAFE.compareAndSwapObject(node, nextOffset, expect, update);
	}

	static void clearNext(AbstractNode node) {
		UNSAFE.putOrderedObject(node, nextOffset, null);
	}

	static void clearPrev(AbstractNode node) {
		UNSAFE.putOrderedObject(node, prevOffset, null);
	}
}
