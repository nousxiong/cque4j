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
	protected IFreer freer;
	
	/**
	 * 用户如果需要，可以实现此清理方法
	 */
	protected void reset() {
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
	public void onGet(IFreer freer) {
		this.freer = freer;
		this.next = null;
	}
	
	@Override
	public void onFree() {
		reset();
		
		next = null;
		freer = null;
	}
	
	@Override
	public void release() {
		if (freer != null){
			freer.free(this);
		}
	}
}
