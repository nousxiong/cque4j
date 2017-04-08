/**
 * 
 */
package cque.util;

import cque.IObjectFactory;
import cque.IPooledObject;
import cque.Node;

/**
 * @author Xiong
 *
 */
public class NodeFactory<E> implements IObjectFactory {

	@Override
	public IPooledObject createInstance() {
		return new Node<E>();
	}

}
