/**
 * 
 */
package cque;

import java.util.Iterator;

/**
 * @author Xiong
 *
 */
public interface QueueIterator<E> extends Iterator<E> {
	E value();

	void deq();

	void reset(); 

}
