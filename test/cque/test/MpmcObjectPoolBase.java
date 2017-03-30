/**
 * 
 */
package cque.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cque.AbstractNode;
import cque.MpmcObjectPool;

/**
 * @author Xiong
 *
 */
public class MpmcObjectPoolBase {
	static class Data extends AbstractNode {
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

	private static final int ADD_CNT = 100000;

	@Test
	public void test() throws InterruptedException {
		for (int i=0; i<5; ++i){
			handleTest(i);
		}
		System.out.println("all done.");
	}
	
	private void handleTest(int index) throws InterruptedException{
		final int threadNum = Runtime.getRuntime().availableProcessors();
		final MpmcObjectPool<Data> pool = new MpmcObjectPool<Data>(ADD_CNT * threadNum);
		List<Thread> ps = new ArrayList<Thread>(threadNum);
		List<Thread> cs = new ArrayList<Thread>(threadNum);
		
		for (int i=0; i<threadNum; ++i){
			final int tid = i;
			ps.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<ADD_CNT; ++i){
						pool.returnObject(new Data(tid, i));
					}
				}
			});
			ps.get(i).start();
		}
		
		Thread.sleep(1);
		
		for (int i=0; i<threadNum; ++i){
			cs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<ADD_CNT;){
						if (pool.borrowObject() != null){
							++i;
						}
					}
				}
			});
		}
		
		for (Thread thr : cs){
			thr.start();
		}
		
		for (Thread thr : ps){
			thr.join();
		}
		
		for (Thread thr : cs){
			thr.join();
		}
		
		assertTrue(pool.isEmpty());
		System.out.println(index+" done.");
	}

}
