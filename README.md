# cque4j
Wait-free multi-producer queue (Intrusive and non-Intrusive) with node pool (for reducing gc pressure)

Usage
--------

Just import to eclipse.

Note
--------

**cque using sun.misc.Unsafe**, so you need ignore your ide's error, for eclipse: 
Preferences->Java->Compiler->Errors/Warnings->Deprecated and restricted API->Forbidden reference->Warning

Example (Non-Intrusive)
--------

```java
import java.util.ArrayList;
import java.util.List;

import cque.MpscLinkedQueue;

/**
 * @author Xiong
 */
public class Example {
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
	
	public void test(){
		final int addSize = 100000;
		final int threadNum = Runtime.getRuntime().availableProcessors();
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		List<Integer> producerIds = new ArrayList<Integer>(threadNum);

		for (int i=0; i<threadNum; ++i){
			final int threadId = i;
			producerIds.add(-1);
			thrs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<addSize; ++i){
						que.add(new Data(threadId, i));
					}
				}
			});
			thrs.get(i).start();
		}
		
		int pollSize = addSize * threadNum;
		for (int i=0; i<pollSize; ){
			Data d = que.poll();
			if (d != null){
				int threadId = d.getThreadId();
				int id = d.getId();
				int currId = producerIds.get(threadId);
				assert(currId + 1 == id);
				producerIds.set(threadId, id);
				++i;
			}
		}
		
		for (int id : producerIds){
			assert(id == addSize - 1);
		}
		
		for (Thread thr : thrs){
			try{
				thr.join();
			}catch (InterruptedException e){
				System.out.println(e.getMessage());
			}
		}
		
		System.out.println("done.");
	}
}
```

Example (Intrusive)
--------

```java
import java.util.ArrayList;
import java.util.List;

import cque.IFreer;
import cque.INode;
import cque.IntrusiveMpscQueue;

/**
 * @author Xiong
 */
public class Example {
	class Data extends AbstractNode {
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
	
	static final IntrusiveMpscQueue<Data> que = new IntrusiveMpscQueue<Data>();

	public long test(){
		final int addSize = 100000;
		final int threadNum = Runtime.getRuntime().availableProcessors();
		List<Thread> thrs = new ArrayList<Thread>(threadNum);
		List<Integer> producerIds = new ArrayList<Integer>(threadNum);
		
		for (int i=0; i<threadNum; ++i){
			final int threadId = i;
			producerIds.add(-1);
			thrs.add(new Thread() {
				@Override
				public void run(){
					for (int i=0; i<addSize; ++i){
						que.add(new Data(threadId, i));
					}
				}
			});
			thrs.get(i).start();
		}
		
		int pollSize = addSize * threadNum;
		for (int i=0; i<pollSize; ){
			Data d = que.poll();
			if (d != null){
				int threadId = d.getThreadId();
				int id = d.getId();
				int currId = producerIds.get(threadId);
				assert(currId + 1 == id);
				producerIds.set(threadId, id);
				++i;
			}
		}
		
		for (int id : producerIds){
			assert(id == addSize - 1);
		}
		
		for (Thread thr : thrs){
			try{
				thr.join();
			}catch (InterruptedException e){
				System.out.println(e.getMessage());
			}
		}
		
		System.out.println("done.");
	}
}
```
