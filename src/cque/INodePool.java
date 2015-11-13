/**
 * 
 */
package cque;

/**
 * @author Xiong
 *
 */
public interface INodePool extends IFreer {
	/**
	 * 从池中获取一个可用的对象
	 * @return
	 */
	public <T> T get();
	
	/**
	 * 返回当前池的大小
	 * @return
	 */
	public int size();
	
	/**
	 * 当前池是否为空
	 * @return
	 */
	public boolean isEmpty();
}
