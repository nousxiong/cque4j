/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import cque.IFreer;
import cque.INode;
import cque.IntrusiveMpscQueue;

/**
 * @author Xiong
 * 嵌入式并发队列测试
 */
public class IntrusiveMpscQueueBase {
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
		public void dispose(){
			if (freer != null){
				freer.free(this);
			}
		}
	}
	
	static final IntrusiveMpscQueue<Data> que = new IntrusiveMpscQueue<Data>();
	
	@Test
	public void test(){
		long eclipse = handleTest(0);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i);
			eclipse /= 2;
		}
		System.out.println("all done, average eclipse: "+eclipse);
	}

	long handleTest(int index){
		final int addSize = 100000;
		final int threadNum = Runtime.getRuntime().availableProcessors();
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		List<Integer> producerIds = new ArrayList<Integer>(threadNum);
		
		long bt = System.currentTimeMillis();
		for (int i=0; i<threadNum; ++i){
			final int threadId = i;
			producerIds.add(-1);
			thrs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<addSize; ++i){
						que.put(new Data(threadId, i));
					}
				}
			});
			thrs.get(i).start();
		}
		
		int pollSize = addSize * threadNum;
		for (int i=0; i<pollSize; ){
			Data d = que.poll(1000, TimeUnit.MILLISECONDS);
			if (d != null){
				int threadId = d.getThreadId();
				int id = d.getId();
				int currId = producerIds.get(threadId);
				assertTrue(currId + 1 == id);
				producerIds.set(threadId, id);
				++i;
			}
		}
		
		for (int id : producerIds){
			assertTrue(id == addSize - 1);
		}
		
		for (Thread thr : thrs){
			try{
				thr.join();
			}catch (InterruptedException e){
				System.out.println(e.getMessage());
			}
		}
		
		long eclipse = System.currentTimeMillis() - bt;
		System.out.println(index + " done.");
		return eclipse;
	}
}
