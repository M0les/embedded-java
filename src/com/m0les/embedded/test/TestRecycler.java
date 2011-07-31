package com.m0les.embedded.test;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.m0les.embedded.Recycler;

/**
 * A suite of coverage tests for the Recycler Interface and its abstract inner implementation class Recycler.Helper
 * 
 * @author Miles Goodhew
 * @version $Id: TestRecycler.java,v 1.2 2011-07-24 14:53:46 mgoodhew Exp $
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
public class TestRecycler {

	/**
	 * Coverage test for the zero-argument discardGarbage() method
	 */
	@Test
	public void testDiscardGarbage() {
		covered = false;
		recycler.discardGarbage();
		assertTrue( covered );
	}

	/**
	 * Coverage test for the zero-argument discardGarbage() method
	 */
	@Test
	public void testDiscardGarbageInt() {
		covered = false;
		recycler.discardGarbage( 0 );
		assertTrue( covered );
	}
	
	/**
	 * Setup a test-harness using an anonymous subclass of Recycler.Helper
	 */
	@Before
	public void setUp(){
		recycler = new Recycler.Helper() {
			@Override
			public void discardGarbage() {
				covered = true;
			}
		};
	}

	private boolean covered;
	private Recycler recycler;
}
