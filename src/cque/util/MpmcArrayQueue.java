/**
 * 
 */
package cque.util;


/**
 * @author Xiong 多生产者多消费者的数组队列
 */
@SuppressWarnings({ "restriction", "unused"})
public class MpmcArrayQueue<E> {
	private final int capacity;
	private final int mask;
	private volatile int p001, p002, p003, p004, p005, p006, p007;
	private volatile long head; // next element to be read
	private volatile long p101, p102, p103, p104, p105, p106, p107;
	private volatile long tail; // next element to be written
	private volatile long p201, p202, p203, p204, p205, p206, p207;
	private long cachedHead;
	private volatile long p301, p302, p303, p304, p305, p306, p307;
	private long cachedTail;
	private volatile Object p401, p402, p403, p404, p405, p406, p407;
	private final Object[] array;

	public MpmcArrayQueue(int capacity) {
		// size is a power of 2
		this.capacity = nextPowerOfTwo(capacity);
		this.mask = this.capacity - 1;
		this.array = new Object[this.capacity];
	}

	private static int nextPowerOfTwo(int v) {
		if (v < 0){
			throw new IllegalArgumentException("v < 0 not allowed");
		}
		return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
	}

	public int capacity() {
		return capacity;
	}

	public boolean add(E item) {
		if (item == null){
			throw new IllegalArgumentException("null values not allowed");
		}
		
		final long i = preEnq();
		if (i < 0){
			return false;
		}
		set((int) i & mask, item);
		return true;
	}

	private long preEnq() {
		long t, w;
		do {
			t = tail;
			w = t - capacity; // "wrap point"

			if (cachedHead <= w) {
				cachedHead = head;
				if (cachedHead <= w){
					return -1;
				}
			}
		} while (!compareAndSetTail(t, t + 1));
		return t;
	}

	public E poll() {
		long h;
		E v;
		do {
			h = head;
			if (h >= cachedTail) {
				cachedTail = tail;
				if (h >= cachedTail){
					return null;
				}
			}

			v = get((int) h & mask); // volatile read
		} while (v == null || !compareAndSetHead(h, h + 1));
		cas((int) h & mask, v, null);
		return v;
	}

	public int size() {
		return (int) (tail - head);
	}

	public boolean isEmpty() {
		return tail == head;
	}

	int next(int i) {
		return (i + 1) & mask;
	}

	int prev(int i) {
		return --i & mask;
	}

	static final sun.misc.Unsafe UNSAFE;
	private static final int base;
	private static final int shift;
	private static final long headOffset;
	private static final long tailOffset;

	static {
		try {
			UNSAFE = UnsafeUtils.getUnsafe();
			headOffset = UNSAFE.objectFieldOffset(MpmcArrayQueue.class.getDeclaredField("head"));
			tailOffset = UNSAFE.objectFieldOffset(MpmcArrayQueue.class.getDeclaredField("tail"));

			base = UNSAFE.arrayBaseOffset(Object[].class);
			int scale = UNSAFE.arrayIndexScale(Object[].class);
			if ((scale & (scale - 1)) != 0){
				throw new Error("data type scale not a power of two");
			}
			shift = 31 - Integer.numberOfLeadingZeros(scale);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	/**
	 * CAS tail field. Used only by preEnq.
	 */
	private boolean compareAndSetTail(long expect, long update) {
		return UNSAFE.compareAndSwapLong(this, tailOffset, expect, update);
	}

	private boolean compareAndSetHead(long expect, long update) {
		return UNSAFE.compareAndSwapLong(this, headOffset, expect, update);
	}

	private static long byteOffset(int i) {
		return ((long) i << shift) + base;
	}

	private void set(int i, E value) {
		UNSAFE.putObjectVolatile(array, byteOffset(i), value);
	}

	@SuppressWarnings("unchecked")
	private E get(int i) {
		return (E) UNSAFE.getObjectVolatile(array, byteOffset(i));
	}

	private boolean cas(int i, E expected, E update) {
		return UNSAFE.compareAndSwapObject(array, byteOffset(i), expected, update);
	}
}
