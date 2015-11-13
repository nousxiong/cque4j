/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 单线程节点池
 */
public class NodePool implements INodePool {
	private INode head;
	private int size = 0;
	private final int maxSize;
	
	/**
	 * 创建默认的节点池
	 */
	public NodePool(){
		this(null, Integer.MAX_VALUE);
	}
	
	/**
	 * 根据指定大小参数创建节点池
	 * @param initNodes
	 * @param maxSize
	 */
	public NodePool(INode[] initNodes, int maxSize){
		this.maxSize = maxSize;
		if (initNodes != null){
			for (INode n : initNodes){
				n.setNext(head);
				head = n;
			}
			size = initNodes.length;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(){
		INode n = head;
		if (n != null){
			head = n.getNext();
			n.setNext(null);
			--size;
			n.onGet(this);
			return (T) n;
		}else{
			return null;
		}
	}
	
	@Override
	public void free(INode n) {
		if (n == null){
			return;
		}

		n.onFree();
		if (size >= maxSize){
			return;
		}
		
		n.setNext(head);
		head = n;
		++size;
	}
	
	@Override
	public int size(){
		return size;
	}
	
	@Override
	public boolean isEmpty(){
		return size() == 0;
	}

}
