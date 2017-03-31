/**
 * 
 */
package cque;

/**
 * @author Xiong
 *
 */
public class AbstractNode implements INode {
	protected INode next;
	protected IRecycler recycler;

	/**
	 * 用户如果需要，可以实现此初始化方法
	 */
	protected void init(){
	}
	
	/**
	 * 用户如果需要，可以实现此清理方法
	 */
	protected void reset(){
	}
	
	@Override
	public INode getNext() {
		return next;
	}
	
	@Override
	public INode fetchNext() {
		INode n = next;
		next = null;
		return n;
	}
	
	@Override
	public void setNext(INode next) {
		this.next = next;
	}
	
	@Override
	public void onBorrowed(IRecycler recycler) {
		this.recycler = recycler;
		this.next = null;
		init();
	}
	
	@Override
	public void onReturn() {
		reset();
		next = null;
		recycler = null;
	}
	
	@Override
	public void release() {
		if (recycler != null){
			recycler.returnObject(this);
		}
	}
}
