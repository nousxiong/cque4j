/**
 * 
 */
package cque;

/**
 * @author Xiong
 * Node<E>的工厂
 */
public class NodeFactory<E> implements IObjectFactory {
	@Override
	public IPooledObject createInstance(){
		return new Node<E>();
	}
}
