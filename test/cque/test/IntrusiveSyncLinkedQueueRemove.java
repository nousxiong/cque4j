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

import cque.AbstractNode;
import cque.ConcurrentObjectPool;
import cque.IObjectFactory;
import cque.IPooledObject;
import cque.IntrusiveSyncLinkedQueue;

/**
 * @author Xiong
 *
 */
public class IntrusiveSyncLinkedQueueRemove {
	static class Data extends AbstractNode {
		private int threadId;
		private int id;
		
		public Data(){
			
		}
		
		public Data(int threadId, int id){
			this.threadId = threadId;
			this.id = id;
		}
		
		public void setThreadId(int threadId) {
			this.threadId = threadId;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getThreadId(){
			return threadId;
		}
		
		public int getId(){
			return id;
		}
		
		@Override
		public boolean equals(Object o){
			if (this == o){
				return true;
			}
			
			if (o == null || getClass() != o.getClass()){
				return false;
			}
			
			Data rhs = (Data) o;
			return 
				this.getThreadId() == rhs.getThreadId() && 
				this.getId() == rhs.getId();
		}
	}
	
	static class DataFactory implements IObjectFactory {
		@Override
		public IPooledObject createInstance() {
			return new Data();
		}
	}
	
	static final int addSize = 10000;
	static final int threadNum = Runtime.getRuntime().availableProcessors();
	static final ConcurrentObjectPool<Data> cpool = 
		new ConcurrentObjectPool<Data>(new DataFactory(), 1, addSize*threadNum, addSize*threadNum);
	static final IntrusiveSyncLinkedQueue<Data> que = new IntrusiveSyncLinkedQueue<Data>();
	static final ConcurrentHashMap<Data, Integer> removes = new ConcurrentHashMap<Data, Integer>();
	
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
		final int multiplier = 71;
		final int removeSize = addSize / multiplier + 1;
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		List<Integer> producerIds = new ArrayList<Integer>(threadNum);
		
		long bt = System.nanoTime();
		for (int i=0; i<threadNum; ++i){
			final int threadId = i;
			producerIds.add(-1);
			thrs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<addSize; ++i){
//						Data dat = new Data(threadId, i);
						Data dat = cpool.borrowObject();
						dat.setId(i);
						dat.setThreadId(threadId);
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
		Data cmp = new Data();
		for (int i=0; i<totalRemoveSize; ){
			Enumeration<Data> enumeration = removes.keys();
			while (enumeration.hasMoreElements()){
				Data dat = enumeration.nextElement();
				cmp.setId(dat.getId());
				cmp.setThreadId(dat.getThreadId());
				if (que.remove(cmp)){
					removes.remove(dat);
					dat.release();
					++i;
				}
			}
		}
		
		int totalSize = addSize * threadNum;
		System.out.println(que.size());
		assertTrue(removes.isEmpty());
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
