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
	public void dispose() {
		if (freer != null){
			freer.free(this);
		}
	}
}
