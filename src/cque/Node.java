/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 非嵌入式并发队列的节点
 */
public class Node<E> implements INode {
	private E item;
	private INode next;
	private IFreer freer;
	
	/**
	 * 获取保存的元素
	 * @return 可能为null
	 */
	public E getItem(){
		return item;
	}
	
	/**
	 * 设置元素
	 * @param item 可以为null
	 */
	public void setItem(E item){
		this.item = item;
	}
	
	@Override
	public INode getNext(){
		return next;
	}
	
	@Override
	public INode fetchNext(){
		INode n = next;
		next = null;
		return n;
	}
	
	@Override
	public void setNext(INode next){
		this.next = next;
	}

	@Override
	public void onGet(IFreer freer){
		this.freer = freer;
		this.next = null;
	}
	
	@Override
	public void onFree(){
		item = null;
		next = null;
		freer = null;
	}

	@Override
	public void release() {
		if (freer != null){
			freer.free(this);
		}
	}
	
	/**
	 * 反转链表，适用任何元素数量的单链表
	 * @param n 链表头
	 * @return 反转后的链表尾
	 */
	public static INode reverse(INode n){
		INode last = n;
		INode first = null;
		while (last != null){
			INode tmp = last;
			last = last.getNext();
			tmp.setNext(first);
			first = tmp;
		}
		
		return first;
	}
}
