/**
 * 
 */
package cque.util;

/**
 * @author Xiong
 * 参考：http://tutorials.jenkov.com/java-performance/ring-buffer.html
 */
public class ArrayQueue<E> {
	private E[] items = null;
	private final int capacity;
	private int position = 0;
	private int size = 0;

	@SuppressWarnings("unchecked")
	public ArrayQueue(int capacity){
		this.capacity = capacity;
		this.items = (E[]) new Object[capacity];
	}

	public int capacity(){
		return capacity;
	}

	public int size(){
		return size;
	}
	
	public boolean isEmpty(){
		return size() == 0;
	}

	/**
	 * 像队尾压入一个元素，返回true表示成功，false表示失败，已满
	 * @param item
	 * @return
	 */
	public boolean add(E item){
		if (size < capacity){
			if (position >= capacity){
				position = 0;
			}
			items[position] = item;
			position++;
			size++;
			return true;
		}
		return false;
	}

	public E poll(){
		if (size == 0){
			return null;
		}
		
		int h = head();
		E nextObj = items[h];
		size--;
		return nextObj;
	}
	
	private int head(){
		int ns = position - size;
		if (ns < 0) {
			ns += capacity;
		}
		return ns;
	}
}
