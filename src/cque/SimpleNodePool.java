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
	private int initSize;
	private int maxSize;
	
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
		assert nodeFactory != null;
		this.nodeFactory = nodeFactory;
		this.initSize = initSize;
		this.maxSize = maxSize;
	}
	
	/**
	 * @param initSize
	 */
	public void setInitSize(int initSize){
		this.initSize = initSize;
	}
	
	/**
	 * @param maxSize
	 */
	public void setMaxSize(int maxSize){
		this.maxSize = maxSize;
	}

	/**
	 * 从池中获取一个可用的节点
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E get(){
		NodePool<E> pool = getPool();
		E e = pool.get();
		if (e == null){
			INode n = nodeFactory.createInstance();
			n.onGet(pool);
			e = (E) n;
		}
		return e;
	}
	
	/**
	 * 获取节点池
	 * @return
	 */
	public NodePool<E> getPool(){
		if (pool == null){
			INode[] ns = new INode[initSize];
			for (int i=0; i<initSize; ++i){
				ns[i] = nodeFactory.createInstance();
			}
			pool = new NodePool<E>(ns, maxSize);
		}
		return pool;
	}
}
