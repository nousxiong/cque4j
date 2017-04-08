/**
 * 
 */
package cque;

import cque.util.AbstractObject;

/**
 * @author Xiong
 * 
 */
public class AbstractNode extends AbstractObject {
	volatile AbstractNode next;
	volatile AbstractNode prev;
	
	/**
	 * 用户如果需要，可以实现此初始化方法
	 */
	protected void initNode(){
	}
	
	/**
	 * 用户如果需要，可以实现此重置清理方法
	 */
	protected void resetNode(){
	}
	
	@Override
	protected final void initObject(){
		this.next = null;
		this.prev = null;
		initNode();
	}
	
	@Override
	protected final void resetObject(){
		resetNode();
		this.next = null;
		this.prev = null;
	}
}
