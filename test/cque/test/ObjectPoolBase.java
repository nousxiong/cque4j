/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.ObjectPool;
import cque.Node;

/**
 * @author Xiong
 * 测试单线程节点池
 */
public class ObjectPoolBase {

	@Test
	public void test() {
		ObjectPool<Node<Long>> pool = new ObjectPool<Node<Long>>(300000);
		final int count = 300000;
		for (int i=0; i<count; ++i){
			if (i % 3 == 0){
				pool.returnObject(new Node<Long>());
			}else{
				Node<Long> n = pool.borrowObject();
				if (n != null){
					n.release();
				}
			}
		}
		assertTrue(pool.size() == 100000);
	}

}
