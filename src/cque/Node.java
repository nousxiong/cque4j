/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 非嵌入式并发队列的节点
 */
public class Node<E> extends AbstractNode {
	private E item;
	
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
	protected void reset(){
		item = null;
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
