/**
 * 
 */
package cque.util;

import java.io.Closeable;

import cque.AbstractNode;
import cque.IPooledObject;

/**
 * @author Xiong
 *
 */
public class PoolGuard extends AbstractNode implements Closeable {
	private IPooledObject po = null;

	public <T extends IPooledObject> void protect(IPooledObject po){
		this.po = po;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IPooledObject> T get(){
		return (T) po;
	}
	
	@Override
	public void close() {
		if (po != null){
			po.release();
			po = null;
		}
		release();
	}
}
