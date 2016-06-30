/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cque.IFreer;
import cque.INode;
import cque.IntrusiveQueue;

/**
 * @author Xiong
 *
 */
public class IntrusiveQueueBase {
	class Data implements INode {
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
		
		/** below code is implements of INode */
		private INode next;
		private IFreer freer;

		@Override
		public INode getNext(){
			return next;
		}

		@Override
		public void setNext(INode next){
			this.next = next;
		}

		@Override
		public void onGet(IFreer freer){
			this.freer = freer;
			this.next = null;
		}

		@Override
		public void onFree(){
			next = null;
			freer = null;
		}

		@Override
		public void release(){
			if (freer != null){
				freer.free(this);
			}
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
		
		long eclipse = System.currentTimeMillis() - bt;
		System.out.println(index + " done.");
		return eclipse;
	}
}
