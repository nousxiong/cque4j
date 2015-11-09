/**
 * 
 */
package cque;

/**
 * @author Xiong
 * Node的工厂
 */
public class NodeFactory<E> implements INodeFactory {
	@Override
	public INode createInstance(){
		return new Node<E>();
	}
}
