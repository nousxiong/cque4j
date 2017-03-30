/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 可池化的对象接口
 */
public interface IPooledObject {
	/**
	 * 回调，在从池取出来后调用
	 * @param recycler 回收者，用于对象的回收
	 */
	void onBorrowed(IRecycler recycler);
	
	/**
	 * 释放自身
	 */
	void release();
	
	/**
	 * 回调，在释放回池之前调用
	 */
	void onReturn();
}
