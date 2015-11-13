/**
 * 
 */
package cque;

/**
 * @author Xiong
 * 并发节点池（多生产者，多消费者），使用线程局部存储来实现多消费者（get）
 */
public class ConcurrentNodePool {
	private ThreadLocal<MpscNodePool> local = new ThreadLocal<MpscNodePool>();
	private INodeFactory nodeFactory;
	private int initSize;
	private int maxSize;
	
	/**
	 * 创建默认的节点池
	 * @param nodeFactory 不可为null
	 */
	public ConcurrentNodePool(INodeFactory nodeFactory){
		this(nodeFactory, 0, Integer.MAX_VALUE);
	}
	
	/**
	 * 根据指定大小参数创建节点池
	 * @param nodeFactory 不可为null
	 * @param initSize
	 * @param maxSize
	 */
	public ConcurrentNodePool(INodeFactory nodeFactory, int initSize, int maxSize){
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
	 * 取得当前线程的节点池
	 * @return 节点池
	 */
	public INodePool getLocalPool(){
		MpscNodePool pool = local.get();
		if (pool == null){
			INode[] ns = new INode[initSize];
			for (int i=0; i<initSize; ++i){
				ns[i] = nodeFactory.createInstance();
			}
			pool = new MpscNodePool(ns, maxSize);
			local.set(pool);
		}
		return pool;
	}

	/**
	 * 从池中获取一个可用的节点
	 * @return
	 */
	public <T> T get(){
		INodePool pool = getLocalPool();
		return get(pool);
	}

	/**
	 * 使用由用户之前保存的节点池来get节点
	 * @param pool 用户之前要在当前线程调用getLocalPool来获取当前线程的节点池
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(INodePool pool){
		assert pool == getLocalPool();
		INode n = pool.get();
		if (n == null){
			n = nodeFactory.createInstance();
			n.onGet(pool);
		}
		return (T) n;
	}
}
