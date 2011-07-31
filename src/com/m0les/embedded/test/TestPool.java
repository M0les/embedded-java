package com.m0les.embedded.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.Test;

import com.m0les.embedded.Factory;
import com.m0les.embedded.Link;
import com.m0les.embedded.Pool;
import com.m0les.embedded.Pool.PoolExhaustedException;
import com.m0les.embedded.Recycler;

/**
 * A suite of unit and coverage tests for the com.m0les.embedded.Pool class
 * 
 * @author Miles Goodhew
 * @version $Id: TestPool.java,v 1.10 2011-07-24 14:53:46 mgoodhew Exp $
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
public class TestPool {

	/**
	 * Test instantiating-from and exhausting a limited-size resource pool
	 * 
	 * @throws Exception should never occur and will fail test
	 */
	@Test
	public void testInstantiation() throws Exception {
		final int LOOP_COUNT = 5;
		Pool<DummyLink> pool = new Pool<DummyLink>(new DummyLinkFactory(),
				LOOP_COUNT);
		assertEquals(0, pool.getInstanceCount());
		for (int i = 0; i < LOOP_COUNT; i++) {
			assertNotNull(pool.getInstance());
		}
		try {
			pool.getInstance();
			fail("Empty pool did not throw PoolExhaustedException on getInstance()");
		} catch (PoolExhaustedException e) {
			// Intended outcome
		}
		assertEquals(LOOP_COUNT, pool.getInstanceCount());
	}

	/**
	 * Test instantiating-from an unlimited resource pool, returning an instance
	 * and getting it again. Checking all the time that the pool-size is as
	 * expected.
	 * 
	 * @throws Exception should never occur and will fail test
	 */
	@Test
	public void testUnlimitedInstantiation() throws Exception {
		final int LOOP_COUNT = 5;
		Pool<DummyLink> pool = new Pool<DummyLink>(new DummyLinkFactory());
		assertEquals(0, pool.getInstanceCount());
		for (int i = 0; i < LOOP_COUNT; i++) {
			assertNotNull(pool.getInstance());
		}
		DummyLink instance = pool.getInstance();
		assertNotNull(instance);
		pool.returnInstance(instance);
		DummyLink nextInstance = pool.getInstance();
		assertNotNull(nextInstance);
		assertEquals(instance, nextInstance);
		assertEquals(LOOP_COUNT + 1, pool.getInstanceCount());
	}

	/**
	 * Test that the toString() method of a pool works (Presenting the type and pool-size).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testToString() throws Exception {
		Pool<DummyLink> pool = new Pool<DummyLink>(new DummyLinkFactory());
		assertNotNull(pool.getInstance());
		assertEquals("Pool(1)", pool.toString());
	}

	/**
	 * Test-out the discardGarbage() functionality - making sure it frees "available" resources and the instance count is as expected.
	 * @throws Exception
	 */
	@Test
	public void testDiscard() throws Exception{
		Pool<DummyLink> pool = new Pool<DummyLink>( new DummyLinkFactory() );
		assertEquals( 0, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 0, pool.getInstanceCount() );
		DummyLink link = pool.getInstance();
		assertNotNull( link );
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 1, pool.getInstanceCount() );
		pool.returnInstance( link );
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 0, pool.getInstanceCount() );
	}

	/**
	 * Test-out the discardGarbage() functionality - making sure it frees "available" resources and the instance count is as expected.
	 * @throws Exception
	 */
	@Test
	public void testDiscardAll() throws Exception{
		Pool<DummyLink> pool = new Pool<DummyLink>( new DummyLinkFactory() );
		DummyLink link = pool.getInstance();
		assertNotNull( link );
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage( 0 );
		assertEquals( 1, pool.getInstanceCount() );
		pool.returnInstance( link );
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage( 0 );
		assertEquals( 0, pool.getInstanceCount() );
		Field chainField = pool.getClass().getDeclaredField( "chain" );
		chainField.setAccessible( true );
		assertNull( chainField.get( pool ) );
	}

	/**
	 * Test that the no-argument discardGarbage() method behaves as expected, covering all cases.
	 * @see Recycler#discardGarbage()
	 * @throws Exception
	 */
	@Test
	public void testSimpleDiscard() throws Exception{
		Pool<DummyLink> pool = new Pool<DummyLink>( new DummyLinkFactory() );
		assertEquals( 0, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 0, pool.getInstanceCount() );
		DummyLink link = pool.getInstance();
		assertNotNull( link );
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 1, pool.getInstanceCount() );
		pool.returnInstance( link );
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 0, pool.getInstanceCount() );
	}
	
	/**
	 * Test that the 1-argument discardGarbage(int) method behaves as expected, covering all cases.
	 * @see Recycler#discardGarbage(int)
	 * @throws Exception
	 */
	@Test
	public void testComplexDiscard() throws Exception{
		Pool<DummyLink> pool = new Pool<DummyLink>( new DummyLinkFactory() );
		assertEquals( 0, pool.getInstanceCount() );
		pool.discardGarbage(-1);
		assertEquals( 0, pool.getInstanceCount() );
		pool.discardGarbage(0);
		assertEquals( 0, pool.getInstanceCount() );
		pool.discardGarbage(1);
		assertEquals( 0, pool.getInstanceCount() );
		DummyLink link1 = pool.getInstance();
		DummyLink link2 = pool.getInstance();
		assertNotNull( link1 );
		assertNotNull( link2 );
		assertEquals( 2, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 2, pool.getInstanceCount() );
		pool.returnInstance( link1 );
		pool.returnInstance( link2 );
		assertEquals( 2, pool.getInstanceCount() );
		pool.discardGarbage(3);
		assertEquals( 2, pool.getInstanceCount() );
		pool.discardGarbage(2);
		assertEquals( 2, pool.getInstanceCount() );
		pool.discardGarbage(1);
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage(0);
		assertEquals( 0, pool.getInstanceCount() );
		pool.discardGarbage(-1);
		assertEquals( 0, pool.getInstanceCount() );
	}
	
	/**
	 * Factory for a Pool<DummyLink> to use to instantiate new DummyLink instances
	 */
	private static class DummyLinkFactory implements Factory<DummyLink> {
		@Override
		public DummyLink newInstance() {
			return new DummyLink();
		}
	}

	/**
	 * Dummy implementation of the Link<C> abstract class used for unit-tests.
	 */
	private static class DummyLink extends Link<DummyLink> {
	}
}
