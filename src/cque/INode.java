/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 节点接口
 */
public interface INode {
	/**
	 * 返回下一个节点
	 * @return 可能为null
	 */
	public INode getNext();
	
	/**
	 * 返回下一个节点，之后将其设置为null
	 * @return
	 */
	public INode fetchNext();
	
	/**
	 * 设置下一个节点
	 * @param n 可以为null
	 */
	public void setNext(INode next);
	
	/**
	 * 回调，在从节点池取出来后调用
	 * @param freer 释放者，用于节点自身的dispose
	 */
	public void onGet(IFreer freer);
	
	/**
	 * 回调，在释放回节点池之前调用
	 */
	public void onFree();
	
	/**
	 * 释放自身
	 */
	public void release();
}
