/**
 * 
 */
package cque.util;

import java.util.concurrent.TimeUnit;

/**
 * @author Xiong 同步器接口
 */
public interface ISynchronizer {
	void register();
	void unregister();
	void await() throws InterruptedException;
	void await(long timeout, TimeUnit unit) throws InterruptedException;
	long awaitNanos(long nanos) throws InterruptedException;
	boolean shouldSignal();
	void signal();
}
