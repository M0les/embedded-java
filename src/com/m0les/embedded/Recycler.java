package com.m0les.embedded;

/**
 * A class that internally manages unused Object references that would otherwise become garbage immediately.
 * An implementing class should reuse unused instances wherever possible and at an appropriate time, the discardGarbage()
 * method can be called to cast-off any pooled unused objects as actual garbage.
 * 
 * @author Miles Goodhew
 * @version $Id: Recycler.java,v 1.3 2011-07-24 14:53:46 mgoodhew Exp $
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
 * 
 * Rationale:
 * This interface is for classes that house one or more pools of reusable Objects to be able to "emit garbage" at a time that's
 * appropriate and convenient rather than immediately a client's done with a particular Object instance.
 * 
 * Places where this might be appropriate are immediately after a period of high concurrently-used instances (e.g. After reading some
 * configuration file that needed a lot of instances to handle) and when entering a low-activity period of the VM's life (e.g.
 * returning to an event-handling loop).
 * 
 * Probably most informative example of this is the @see Pool class.
 */

public interface Recycler {
	/**
	 * Immediately dispose of all unused object references within this instance.
	 * @note This method will NOT invoke the garbage-collector explicitly. If that behaviour is required, the caller should
	 * invoke it "manually" upon return (They may need to call several Recycler-implementers before then anyway).
	 */
	public void discardGarbage();
	
	/**
	 * Immediately dispose of as much of the pool(s) of reusable objects as possible, leaving at most maxRemaining instances
	 * available for re-allocation. This is intended to allow a "floor" of recyclable instances available at all times to prevent
	 * multiple phases of redundant allocation and deletion throughout the lifetime of a VM.
	 * @note This is an optional operation and classes that don't have a specific behaviour they want for this method, should
	 * defer to the no-argument {@link #discardGarbage()} method. Sub-classing the {@link Helper} abstract class is one of the
	 * simplest ways to achieve this (either as immediate superclass or an anonymous inner-class instance).
	 * 
	 * @param maxRemaining The maximum number of "free" instances to leave "undiscarded" and available for reuse. As indicated above,
	 * this is only a maximum, so fewer (or no) instances will remain available when this method returns.
	 */
	public void discardGarbage( int maxRemaining );
	
	/**
	 * A Helper implementation of this interface. This directs the {@link Recycler#discardGarbage(int)} method to simply call
	 * {@link #discardGarbage()}. Subclasses still need to implement {@link #discardGarbage()}
	 */
	public abstract class Helper implements Recycler
	{
		/**
		 * Redirect to zero-argument {@link #discardGarbage()}
		 * @see Recycler#discardGarbage(int)
		 */
		public void discardGarbage( int maxRemaining )
		{
			discardGarbage();
		}
	}
}
