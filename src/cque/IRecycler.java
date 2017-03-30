/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 回收器
 */
public interface IRecycler {
	void returnObject(IPooledObject po);
}
