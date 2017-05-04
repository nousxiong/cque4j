/**
 * 
 */
package cque;

import cque.util.ISynchronizer;
import cque.util.NodeFactory;
import cque.util.PoolUtils;
import cque.util.ThreadSynchronizer;

/**
 * @author Xiong
 *
 */
public class MpscSuspendedQueue<E> extends MpscSyncLinkedQueue<E> {
	
	public MpscSuspendedQueue(){
		this(new NodeFactory<E>(), PoolUtils.DEFAULT_POOL_SIZE, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	public MpscSuspendedQueue(ISynchronizer sync){
		this(sync, new NodeFactory<E>(), PoolUtils.DEFAULT_POOL_SIZE, PoolUtils.DEFAULT_INIT_SIZE, PoolUtils.DEFAULT_MAX_SIZE);
	}
	
	public MpscSuspendedQueue(IObjectFactory nodeFactory, int poolSize, int initPoolSize, int maxPoolSize){
		this(new ThreadSynchronizer(), new NodeFactory<E>(), poolSize, initPoolSize, maxPoolSize);
	}
	
	public MpscSuspendedQueue(ISynchronizer sync, IObjectFactory nodeFactory, int poolSize, int initPoolSize, int maxPoolSize){
		super(sync, nodeFactory, poolSize, initPoolSize, maxPoolSize);
	}
	
	/**
	 * 使用用户指定的节点池来创建队列
	 * @param cpool 外部用户创建的节点池
	 */
	public MpscSuspendedQueue(ConcurrentObjectPool<Node<E>> cpool){
		this(new ThreadSynchronizer(), cpool);
	}
	
	public MpscSuspendedQueue(ISynchronizer sync, ConcurrentObjectPool<Node<E>> cpool){
		super(sync, cpool);
	}
}
