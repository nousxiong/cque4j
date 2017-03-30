/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 节点接口
 */
public interface INode extends IPooledObject {
	/**
	 * 返回下一个节点
	 * @return 可能为null
	 */
	INode getNext();
	
	/**
	 * 返回下一个节点，之后将其设置为null
	 * @return
	 */
	INode fetchNext();
	
	/**
	 * 设置下一个节点
	 * @param n 可以为null
	 */
	void setNext(INode next);
}
