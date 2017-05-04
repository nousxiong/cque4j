/**
 * 
 */
package cque;

import cque.util.ISynchronizer;
import cque.util.ThreadSynchronizer;

/**
 * @author Xiong
 *
 */
public class IntrusiveSuspendedQueue<E extends AbstractNode> extends IntrusiveSyncLinkedQueue<E> {
	
	public IntrusiveSuspendedQueue(){
		this(new ThreadSynchronizer());
	}
	
	public IntrusiveSuspendedQueue(ISynchronizer sync){
		super(sync);
	}
}
