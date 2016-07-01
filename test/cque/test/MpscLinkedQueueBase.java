/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import cque.MpscLinkedQueue;

/**
 * @author Xiong
 *
 */
public class MpscLinkedQueueBase {
	class Data {
		int threadId;
		int id;
		
		public Data(int threadId, int id) {
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
	
	static final MpscLinkedQueue<Data> que = new MpscLinkedQueue<Data>();
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
		assertTrue(que.size() == 0);
		
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
