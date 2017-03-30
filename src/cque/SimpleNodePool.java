/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 单线程节点池
 */
public class SimpleNodePool<E extends INode> {
	private NodePool<E> pool;
	private INodeFactory nodeFactory;
	
	/**
	 * 创建默认的节点池
	 * @param nodeFactory 不可为null
	 */
	public SimpleNodePool(INodeFactory nodeFactory){
		this(nodeFactory, 0, Integer.MAX_VALUE);
	}
	
	/**
	 * 根据指定大小参数创建节点池
	 * @param nodeFactory 不可为null
	 * @param initSize
	 * @param maxSize
	 */
	public SimpleNodePool(INodeFactory nodeFactory, int initSize, int maxSize){
		if (nodeFactory == null){
			throw new IllegalArgumentException("null values not allowed");
		}
		
		this.nodeFactory = nodeFactory;
		INode[] initNodes = null;
		if (initSize > 0){
			initNodes = new INode[initSize];
			for (int n=0; n<initSize; ++n){
				initNodes[n] = nodeFactory.createInstance();
			}
		}
		this.pool = new NodePool<E>(initNodes, maxSize);
	}

	/**
	 * 从池中获取一个可用的节点
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E borrowObject(){
		E e = pool.borrowObject();
		if (e == null){
			INode n = nodeFactory.createInstance();
			n.onBorrowed(pool);
			e = (E) n;
		}
		return e;
	}
}
