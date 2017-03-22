/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.AbstractNode;
import cque.IntrusiveQueue;

/**
 * @author Xiong
 *
 */
public class IntrusiveQueueBase {
	class Data extends AbstractNode {
		private int threadId;
		private int id;
		
		public Data(int threadId, int id){
			this.threadId = threadId;
			this.id = id;
		}
		
		public int getThreadId(){
			return threadId;
		}
		
		public int getId(){
			return id;
		}
	}
	
	static final IntrusiveQueue<Data> que = new IntrusiveQueue<Data>();
	@Test
	public void test() {
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("all done, average eclipse: "+eclipse);
	}

	long handleTest(int index){
		final int addSize = 100000;
		
		long bt = System.currentTimeMillis();
		
		for (int i=0; i<addSize; ++i){
			que.add(new Data(1, i));
		}
		
		int pollSize = addSize;
		for (int i=0; i<pollSize; ){
			Data d = que.poll();
			if (d != null){
				int threadId = d.getThreadId();
				assertTrue(threadId == 1);
				int id = d.getId();
				assertTrue(id == i);
				++i;
			}
		}
		assertTrue(que.size() == 0);
		
		long eclipse = System.currentTimeMillis() - bt;
		System.out.println(index + " done.");
		return eclipse;
	}
}
