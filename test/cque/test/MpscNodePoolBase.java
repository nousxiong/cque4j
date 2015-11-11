package cque.test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cque.MpscNodePool;
import cque.Node;

/**
 * 
 */

/**
 * @author Xiong
 * 测试节点池
 */
public class MpscNodePoolBase {
	class Data {
		public int i = 1;
		public String str = "my data";
	}
	private static final MpscNodePool nodeQue = new MpscNodePool();
	private static final int freeSize = 100000;
	
	@Test
	public void test(){
		for (int i=0; i<20; ++i){
			handleTest(i);
		}
		System.out.println("all done.");
	}

	private void handleTest(int index){
		final int threadNum = Runtime.getRuntime().availableProcessors();
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		for (int i=0; i<threadNum; ++i){
			thrs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<freeSize; ++i){
						nodeQue.free(new Node<Data>());
					}
				}
			});
			thrs.get(i).start();
		}
		
		int getSize = freeSize / 2 * threadNum;
		for (int i=0; i<getSize; ){
			Node<Data> n = nodeQue.get();
			if (n != null){
				++i;
			}
		}
		
		for (Thread thr : thrs){
			try{
				thr.join();
			}catch (InterruptedException e){
				System.out.println(e.getMessage());
			}
		}
		
		assertTrue(nodeQue.size() == freeSize * threadNum - getSize);
		assertTrue(!nodeQue.isEmpty());
		
		getSize = freeSize * threadNum - getSize;
		for (int i=0; i<getSize; ){
			if (nodeQue.get() != null){
				++i;
			}
		}
		
		assertTrue(nodeQue.size() == 0);
		assertTrue(nodeQue.isEmpty());
		System.out.println(index+" done.");
	}
}
