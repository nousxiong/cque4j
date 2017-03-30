/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 并发节点池（多生产者，多消费者），内部可能有多个池
 */
public class ConcurrentNodePool<E extends INode> {
	private MpmcNodePool<E>[] pools;
	private final int poolSize;
	private final INodeFactory nodeFactory;
	private int polling = 0;
	
	/**
	 * 创建默认的节点池
	 * @param nodeFactory 不可为null
	 */
	public ConcurrentNodePool(INodeFactory nodeFactory){
		this(nodeFactory, PoolUtils.DEFAULT_POOL_SIZE, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	/**
	 * 根据指定大小参数创建节点池
	 * @param nodeFactory 不可为null
	 * @param initSize
	 * @param maxSize
	 */
	@SuppressWarnings("unchecked")
	public ConcurrentNodePool(INodeFactory nodeFactory, int poolSize, int initSize, int maxSize){
		if (nodeFactory == null){
			throw new IllegalArgumentException("null values not allowed");
		}
		
		this.poolSize = poolSize;
		this.nodeFactory = nodeFactory;
		this.pools = new MpmcNodePool[poolSize];
		for (int i=0; i<poolSize; ++i){
			INode[] initNodes = null;
			if (initSize > 0){
				initNodes = new INode[initSize];
				for (int n=0; n<initSize; ++n){
					initNodes[n] = nodeFactory.createInstance();
				}
			}
			this.pools[i] = new MpmcNodePool<E>(maxSize, initNodes);
		}
	}

	/**
	 * 从池中获取一个可用的节点
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public E borrowObject(){
		int i = polling;
		if (i >= poolSize){
			i = 0;
		}
		MpmcNodePool<E> pool = pools[i];
		INode n = pool.borrowObject();
		if (n == null){
			n = nodeFactory.createInstance();
			n.onBorrowed(pool);
		}
		polling = i;
		return (E) n;
	}
}
