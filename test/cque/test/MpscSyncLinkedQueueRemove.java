/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import cque.MpscSyncLinkedQueue;

/**
 * @author Xiong
 *
 */
public class MpscSyncLinkedQueueRemove {
	static class Data {
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
	
	static final MpscSyncLinkedQueue<Data> que = new MpscSyncLinkedQueue<Data>();
	
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
		final int addSize = 10000;
		final int multiplier = 71;
		final int removeSize = addSize / multiplier + 1;
		final int threadNum = Runtime.getRuntime().availableProcessors();
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		List<Integer> producerIds = new ArrayList<Integer>(threadNum);
		final ConcurrentHashMap<Data, Integer> removes = new ConcurrentHashMap<Data, Integer>();
		
		long bt = System.nanoTime();
		for (int i=0; i<threadNum; ++i){
			final int threadId = i;
			producerIds.add(-1);
			thrs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<addSize; ++i){
						Data dat = new Data(threadId, i);
						que.add(dat);
						if (i % multiplier == 0){
							removes.put(dat, i);
						}
					}
				}
			});
			thrs.get(i).start();
		}
		
		final int totalRemoveSize = removeSize * threadNum;
		for (int i=0; i<totalRemoveSize; ){
			Enumeration<Data> enumeration = removes.keys();
			while (enumeration.hasMoreElements()){
				Data dat = enumeration.nextElement();
				if (que.remove(dat)){
					removes.remove(dat);
					++i;
				}
			}
		}
		
		int totalSize = addSize * threadNum;
		System.out.println(que.size());
		assertTrue(que.size() == totalSize - totalRemoveSize);
		
		que.clear();
		assertTrue(que.size() == 0 && que.isEmpty());
		
		for (Thread thr : thrs){
			try{
				thr.join();
			}catch (InterruptedException e){
				System.out.println(e.getMessage());
			}
		}
		
		long eclipse = System.nanoTime() - bt;
		System.out.println(index + " done.");
		return TimeUnit.NANOSECONDS.toMillis(eclipse);
	}

}
