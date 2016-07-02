/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.Node;
import cque.NodePool;

/**
 * @author Xiong
 * 测试单线程节点池
 */
public class NodePoolBase {

	@Test
	public void test() {
		NodePool<Node<Long>> pool = new NodePool<Node<Long>>();
		final int count = 300000;
		for (int i=0; i<count; ++i){
			if (i % 3 == 0){
				pool.free(new Node<Long>());
			}else{
				Node<Long> n = pool.get();
				if (n != null){
					n.release();
				}
			}
		}
		assertTrue(pool.size() == 100000);
	}

}
