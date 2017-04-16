/**
 * 
 */
package cque.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cque.AbstractNode;
import cque.ConcurrentObjectPool;
import cque.IObjectFactory;
import cque.IPooledObject;

/**
 * @author Xiong
 *
 */
public class ConcurrentObjectPoolBase {
	static class Data extends AbstractNode {
		private int threadId;
		private int id;
		
		public Data(){
			
		}
		
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
		final int threadNum = Runtime.getRuntime().availableProcessors() * 2;
		final ConcurrentObjectPool<Data> cpool = 
			new ConcurrentObjectPool<Data>(
				new IObjectFactory() {
					@Override
					public IPooledObject createInstance() {
						return new Data();
					}
				}, 
				threadNum, 0, ADD_CNT
			);
		List<Thread> ps = new ArrayList<Thread>(threadNum);
		
		for (int i=0; i<threadNum; ++i){
			final int idx = i;
			ps.add(new Thread() {
				@Override
				public void run(){
					List<Data> dataList = new ArrayList<Data>(ADD_CNT);
					for (int i=0; i<ADD_CNT; ++i){
						if (i % 10 == idx){
							for (Data dat : dataList){
								dat.release();
							}
							dataList.clear();
						}else{
							dataList.add(cpool.borrowObject());
						}
					}
				}
			});
			ps.get(i).start();
		}
		
		Thread.sleep(1);
		
		for (Thread thr : ps){
			thr.join();
		}
		
		System.out.println(index+" done.");
	}
}
