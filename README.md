# cque
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
 * remember add vm arg: -ea, for enable assert
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
 * remember add vm arg: -ea, for enable assert
 */
public class Example {
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
