/**
 * 
 */
package cque.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cque.LinkedQueue;

/**
 * @author Xiong
 *
 */
public class LinkedQueueRemove {
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
	
	static final LinkedQueue<Data> que = new LinkedQueue<Data>();
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
		List<Data> removes = new ArrayList<Data>(removeSize);
		
		long bt = System.currentTimeMillis();
		
		for (int i=0; i<addSize; ++i){
			Data dat = new Data(1, i);
			que.add(dat);
			if (i % multiplier == 0){
				removes.add(dat);
			}
		}
		
		for (Data dat : removes){
			assertTrue(que.remove(dat));
		}
		System.out.println(que.size());
		assertTrue(que.size() == addSize - removeSize);
		
		que.clear();
		assertTrue(que.size() == 0);
		
		long eclipse = System.currentTimeMillis() - bt;
		System.out.println(index + " done.");
		return eclipse;
	}
}
