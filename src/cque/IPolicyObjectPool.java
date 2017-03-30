/**
 * 
 */
package cque;

/**
 * @author Xiong
 *
 */
public interface IPolicyObjectPool<E extends IPooledObject> {
	E borrowObject();
}
