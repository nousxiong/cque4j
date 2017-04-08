/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import cque.MpscSyncLinkedQueue;
import cque.util.NodeFactory;

/**
 * @author Xiong
 *
 */
public class MpscSyncLinkedQueueBase {
	static class Data {
		private int threadId;
		private int id;
		
		public int getThreadId(){
			return threadId;
		}
		
		public int getId(){
			return id;
		}

		public void setThreadId(int threadId) {
			this.threadId = threadId;
		}

		public void setId(int id) {
			this.id = id;
		}
	}
	
	static final int addSize = 100000;
	static final int threadNum = Runtime.getRuntime().availableProcessors();
	static final MpscSyncLinkedQueue<Data> que = 
		new MpscSyncLinkedQueue<Data>(
			new NodeFactory<Data>(), 1, addSize*threadNum, addSize*threadNum
		);
	static final List<List<Data>> dataList = new ArrayList<List<Data>>(threadNum);
	static {
		for (int i=0; i<threadNum; ++i){
			List<Data> dats = new ArrayList<Data>(addSize);
			for (int j=0; j<addSize; ++j){
				dats.add(new Data());
			}
			dataList.add(dats);
		}
	}
	
	@Test
	public void test(){
		long eclipse = handleTest(0, false);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i, false);
			eclipse /= 2;
		}
		System.out.println("all done, average eclipse: "+eclipse);
		
		eclipse = handleTest(0, true);
		for (int i=1; i<10; ++i){
			eclipse += handleTest(i, true);
			eclipse /= 2;
		}
		System.out.println("check data all done, average eclipse: "+eclipse);
	}

	long handleTest(int index, final boolean checkData){
//		final int threadNum = 10;
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		List<Integer> producerIds = new ArrayList<Integer>(threadNum);
		
		long bt = System.nanoTime();
		for (int i=0; i<threadNum; ++i){
			final int tid = i;
			producerIds.add(-1);
			thrs.add(new Thread() {
				@Override
				public void run(){
					List<Data> dats = dataList.get(tid);
					for (int i=0; i<addSize; ++i){
						Data dat = dats.get(i);
						if (checkData){
							dat.setThreadId(tid);
							dat.setId(i);
						}

//						que.add(dat);
						que.put(dat);
						if (i % 1000 == 0){
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			});
			thrs.get(i).start();
		}
		
		int pollSize = addSize * threadNum;
		for (int i=0; i<pollSize; ){
//			Data d = que.poll();
//			Data d = que.take();
			Data d = que.poll(3000, TimeUnit.MILLISECONDS);
			if (d != null){
				if (checkData){
					int tid = d.getThreadId();
					int id = d.getId();
					int currId = producerIds.get(tid);
					assertTrue(currId + 1 == id);
					producerIds.set(tid, id);
				}
				++i;
			}
		}
		
		if (checkData){
			for (int id : producerIds){
				assertTrue(id == addSize - 1);
			}
		}
		assertTrue(que.isEmpty());
		
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
