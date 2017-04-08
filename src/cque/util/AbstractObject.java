/**
 * 
 */
package cque.util;

import cque.IPooledObject;
import cque.IRecycler;

/**
 * @author Xiong
 *
 */
public class AbstractObject implements IPooledObject {
	protected IRecycler recycler;
	
	/**
	 * 用户如果需要，可以实现此初始化方法
	 */
	protected void initObject(){
	}
	
	/**
	 * 用户如果需要，可以实现此清理方法
	 */
	protected void resetObject(){
	}

	@Override
	public void onBorrowed(IRecycler recycler) {
		this.recycler = recycler;
		initObject();
	}

	@Override
	public void release() {
		if (recycler != null){
			recycler.returnObject(this);
		}
	}

	@Override
	public void onReturn() {
		resetObject();
		recycler = null;
	}
}
