/**
 * 
 */
package cque;

/**
 * @author Xiong
 *
 */
public interface INodePool<E extends INode> extends IRecycler {
	
	/**
	 * 从池中获取一个可用的对象
	 * @return
	 */
	E borrowObject();
	
	/**
	 * 返回当前池的大小
	 * @return
	 */
	int size();
	
	/**
	 * 当前池是否为空
	 * @return
	 */
	boolean isEmpty();
}
