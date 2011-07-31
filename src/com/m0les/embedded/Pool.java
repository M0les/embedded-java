package com.m0les.embedded;

/**
 * A pool for managing the allocation and reuse of "temporary use" resource
 * objects. The Pool is instantiated with a factory for making new instances of
 * C under management and an optional limit to the number of instances that may
 * exist. When client code calls the getInstance() method the Pool first looks
 * at the head of its chain of recycled instances to see if there's an instance
 * to be reused, otherwise it will need to instantiate a new object. Then, if a
 * set limit's been reached, this class throws a PoolExhaustedException,
 * otherwise it calls the factory to make a new instance and returns that.
 * 
 * @author Miles Goodhew
 * @version $Id: Pool.java,v 1.11 2011-07-24 14:53:46 mgoodhew Exp $
 * 
 * @param <C> The class of objects managed by this pool (The value-type of the pool)
 */

/* LICENSE (2-clause BSD):
 * Copyright (c) 2011, Miles "M0les" Goodhew
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Pool<C extends Link<C>> implements Recycler{
	/**
	 * Exception thrown when an attempt is made to call getInstance() from a
	 * limited-instance pool that is exhausted.
	 */
	@SuppressWarnings("serial")
	public static final class PoolExhaustedException extends Exception {
	}

	/**
	 * Static exception instance to be reused upon pool exhaustion.
	 */
	public static final PoolExhaustedException POOL_EXHAUSTED = new PoolExhaustedException();

	/**
	 * Create a limited pool of C instances. When the limit of instances is
	 * reached and a caller tries to get another instance from the pool, they
	 * will receive a PoolExhaustedException instead.
	 * 
	 * @param factory
	 *            The factory to create new C instances when the pool is empty
	 * @param limit
	 *            The maximum number of C instantiations that will be made
	 *            through the factory.
	 */
	public Pool(Factory<C> factory, int limit) {
		this.factory = factory;
		this.limit = limit;
	}

	/**
	 * Create a virtually unlimited pool of C instances. Callers can always get
	 * a new or recycled instance from getInstance(), within the overriding
	 * limits of the VM itself.
	 * 
	 * @param factory
	 *            The factory to create new C instances when the pool is empty
	 */
	public Pool(Factory<C> factory) {
		this.factory = factory;
		limit = 0;
	}

	/**
	 * Return a C instance to the pool. It's assumed that the caller has already
	 * freed any resources in C and nullified any external references. The
	 * caller should nolonger hold a reference to the returned instance after
	 * this call.
	 * 
	 * @param instance
	 */
	public synchronized void returnInstance(C instance) {
		instance.next = chain;
		chain = instance;
	}

	/**
	 * Get a new or recycled C instance.
	 * 
	 * @return The instance to be used (never null)
	 * @throws PoolExhaustedException
	 *             If a limited pool is already empty before this call.
	 */
	public synchronized C getInstance() throws PoolExhaustedException {
		if (null != chain) {
			C instance = chain;
			chain = instance.next;
			return instance;
		}
		switch (limit) {
		default:
			if (limit <= instanceCount) {
				throw POOL_EXHAUSTED;
			}
		case 0:
			instanceCount++;
			return factory.newInstance();
		}
	}

	/**
	 * Get the number of C instances managed by this Pool instance. This
	 * includes all allocated and pooled instances.
	 * 
	 * @return The number of instances in the pool.
	 */
	public int getInstanceCount() {
		return instanceCount;
	}

	/**
	 * Unlink all reusable C instances in the pool, so the garbage-collector can
	 * free them up. Currently allocated instances are unaffected and still
	 * count-towards the instanceCount of this instance.
	 * 
	 * @Note This "forgets" about C instances, so it produces garbage. This needs
	 *       to be used carefully in embedded environments where
	 *       garbage-collection can cause significant delays. This does have its
	 *       uses in such a situation though E.g. cleanup after initialisation,
	 *       but before main operations. In many situations the caller might
	 *       also want to explicitly invoke the garbage-collector soon after
	 *       this method terminates in order to get the "pain" out of the way at
	 *       an opportune "non-realtime" moment.

	 * @see Recycler#discardGarbage()
	 */
	@Override
	public synchronized void discardGarbage() {
		while( null != chain ){
			final C next = chain.next;
			chain.next = null;
			chain = next;
			instanceCount--;
		}
	}

	/**
	 * @see Recycler#discardGarbage()
	 */
	@Override
	public synchronized void discardGarbage(int maxRemaining) {
		if (0 < maxRemaining) {
			C current = chain;
			while (null != current && 0 < --maxRemaining) {
				current = current.next;
			}
			while (null != current) {
				final C next = current.next;
				if (null != next) {
					current.next = null;
					instanceCount--;
				}
				current = next;
			}
		} else {
			discardGarbage();
		}
	}

	/**
	 * Present a human-readable representation of this instance, showing its
	 * type and number of managed-instances.
	 * 
	 * @note This composes strings, which produces garbage. Its use in embedded
	 *       environments is discourages, its main use is for examining variable
	 *       in a debugger.
	 */
	@Override
	public String toString() {
		return "Pool(" + instanceCount + ")";
	}

	private final Factory<C> factory;
	private final int limit;
	private int instanceCount = 0;
	private C chain = null;
}
