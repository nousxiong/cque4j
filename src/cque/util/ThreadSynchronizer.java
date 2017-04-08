/*
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package cque.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Xiong
 */
@SuppressWarnings("restriction")
public class ThreadSynchronizer implements ISynchronizer {
	private volatile Thread waiter;

	@Override
	public void register() {
		final Thread currentThread = Thread.currentThread();
		if (!casWaiter(null, currentThread)){
			throw new IllegalMonitorStateException("attempt by " + currentThread + " but owned by " + waiter);
		}
	}

	@Override
	public void unregister() {
		if (waiter != Thread.currentThread()){
			throw new IllegalMonitorStateException("attempt by " + Thread.currentThread() + " but owned by " + waiter);
		}
		waiter = null;
	}

	@Override
	public boolean shouldSignal() {
		return waiter != null;
	}

	@Override
	public void signal() {
		final Thread t = waiter;
		if (t != null){
			LockSupport.unpark(t);
		}
	}

	@Override
	public void await() throws InterruptedException {
		LockSupport.park(this);

		if (Thread.interrupted()){
			throw new InterruptedException();
		}
	}

	@Override
	public long awaitNanos(long nanos) throws InterruptedException {
		long left = nanos;
		long deadline = System.nanoTime() + left;
		LockSupport.parkNanos(this, left);
		if (Thread.interrupted()){
			throw new InterruptedException();
		}
		left = deadline - System.nanoTime();
		return left;
	}

	@Override
	public void await(long timeout, TimeUnit unit) throws InterruptedException {
		awaitNanos(TimeUnit.NANOSECONDS.convert(timeout, unit));
	}

	private static final sun.misc.Unsafe UNSAFE;
	private static final long waiterOffset;

	static {
		try {
			UNSAFE = UnsafeUtils.getUnsafe();
			waiterOffset = UNSAFE.objectFieldOffset(ThreadSynchronizer.class.getDeclaredField("waiter"));
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	private boolean casWaiter(Thread expected, Thread update) {
		return UNSAFE.compareAndSwapObject(this, waiterOffset, expected, update);
	}
}
