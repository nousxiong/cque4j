/**
 * 
 */
package cque.util;

import cque.IObjectFactory;
import cque.IPooledObject;

/**
 * @author Xiong
 *
 */
public class PoolGuardFactory implements IObjectFactory {
	@Override
	public IPooledObject createInstance() {
		return new PoolGuard();
	}
}
