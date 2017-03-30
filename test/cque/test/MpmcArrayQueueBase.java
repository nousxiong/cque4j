/**
 * 
 */
package cque.test;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import cque.MpmcArrayQueue;
import static org.junit.Assert.*;

/**
 * @author Xiong
 *
 */
public class MpmcArrayQueueBase {
	class Data {
		public int i = 1;
		public String str = "my data";
	}
	
	private static final int ADD_CNT = 100000;

	@Test
	public void test() throws InterruptedException {
		for (int i=0; i<20; ++i){
			handleTest(i);
		}
		System.out.println("all done.");
	}
	
	private void handleTest(int index) throws InterruptedException{
		final int threadNum = Runtime.getRuntime().availableProcessors();
		final MpmcArrayQueue<Data> arrque = new MpmcArrayQueue<Data>(ADD_CNT * threadNum);
		List<Thread> ps = new ArrayList<Thread>(threadNum);
		List<Thread> cs = new ArrayList<Thread>(threadNum);
		
		for (int i=0; i<threadNum; ++i){
			ps.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<ADD_CNT; ++i){
						arrque.add(new Data());
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
						if (arrque.poll() != null){
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
		
		assertTrue(arrque.isEmpty());
		System.out.println(index+" done.");
	}
}
