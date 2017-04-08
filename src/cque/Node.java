/**
 * 
 */
package cque;

/**
 * @author Xiong
 *
 */
public class Node<E> extends AbstractNode {
	E value;
	
	@Override
	protected void resetNode(){
		value = null;
	}
}
