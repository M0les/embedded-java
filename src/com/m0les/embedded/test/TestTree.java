package com.m0les.embedded.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.m0les.embedded.Pool.PoolExhaustedException;
import com.m0les.embedded.Tree;
import com.m0les.embedded.Tree.Node;

/**
 * Test and cover the behaviour of the Tree class.
 * 
 * @author Miles Goodhew
 * @version $Id: TestTree.java,v 1.9 2011-07-24 14:53:46 mgoodhew Exp $
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
public class TestTree
{

	@Test
	public void testInstantiate() throws Exception
	{
		assertTrue( null != tree );
		verifyRules();
		assertNull( tree.getFirst() );
		assertNull( tree.getLast() );
	}
	
	@Test(expected=PoolExhaustedException.class)
	public void testLimitedPool() throws PoolExhaustedException
	{
		tree = new Tree<String>( 2 );
		tree.insert( 1, "One" );
		tree.insert( 2, "Two" );
		tree.insert( 3, "Three" );
	}
	
	@Test
	public void testOwnPool() throws Exception {
		Tree.NodePool<String>pool = new Tree.NodePool<String>();
		tree = new Tree<String>( pool );
		assertEquals( 0, pool.getInstanceCount() );
		tree.insert(0, "Zero");
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 1, pool.getInstanceCount() );
		tree.removeAll();
		assertEquals( 1, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 0, pool.getInstanceCount() );
	}
	
	@Test
	public void testNoPoolGarbage() throws Exception {
		final int SIZE = 8;
		Tree.NodePool<String>pool = new Tree.NodePool<String>();
		tree = new Tree<String>( pool );
		DataNode[] nodeSrc = makeDataset( SIZE );
		insertSequence( nodeSrc );
		tree.removeAll();
		assertEquals( SIZE, pool.getInstanceCount() );
		insertSequence( nodeSrc );
		assertEquals( SIZE, pool.getInstanceCount() );
		tree.removeAll();
		assertEquals( SIZE, pool.getInstanceCount() );
		pool.discardGarbage();
		assertEquals( 0, pool.getInstanceCount() );
	}

	@Test
	public void testSomeGarbage() throws Exception {
		final int SIZE = 8;
		final int LEFT = 4;
		Tree.NodePool<String>pool = new Tree.NodePool<String>();
		tree = new Tree<String>( pool );
		DataNode[] nodeSrc = makeDataset( SIZE );
		insertSequence( nodeSrc );
		tree.removeAll();
		assertEquals( SIZE, pool.getInstanceCount() );
		tree.discardGarbage( LEFT );
		assertEquals( LEFT, pool.getInstanceCount() );
	}

	@Test(expected=NullPointerException.class)
	public void testNullPool() throws Exception {
		tree = new Tree<String>(null);
	}

	@Test
	public void testOneTwo() throws PoolExhaustedException
	{
		tree.insert( 1, "One" );
		tree.insert( 2, "Two" );
	}
	
	@Test
	public void testInsert() throws Exception
	{
		tree.insert( 1, "Value" );
		verifyRules();
		Node<String> node = tree.getFirst();
		assertEquals( 1, node.getKey() );
		assertEquals( "Value", node.getValue() );
	}
	
	@Test
	public void testInsertInOrder() throws Exception
	{
		DataNode[] nodes = makeDataset( 3 );
		insertSequence( nodes );
		assertSequence( nodes );
	}

	@Test
	public void testReverseInsert() throws Exception
	{
		DataNode[] assertNodes = makeDataset( 3 );
		DataNode[] insertNodes = new DataNode[assertNodes.length];
		for( int i = 0; i < insertNodes.length; i++ )
		{
			insertNodes[insertNodes.length - 1 - i] = assertNodes[i];
		}
		insertSequence( insertNodes );
		assertSequence( assertNodes );
	}
	
	@Test
	public void testRemoval() throws Exception
	{
		DataNode[] insertNodes = makeDataset( 8 );
		insertSequence( insertNodes );
		DataNode[] assertNodes = new DataNode[insertNodes.length - 1];
		for( int i = 0; i < insertNodes.length; i++ )
		{
			Node<String> node = tree.find( i + 1 );
			Integer key = node.getKey();
			String val = node.getValue();
			tree.remove( node );
			verifyRules();
			for( int j = 0; j < assertNodes.length; j++ )
			{
				assertNodes[j] = insertNodes[j + ( j < i ? 0 : 1 )];
			}
			assertSequence( assertNodes );
			tree.insert( key, val );
			verifyRules();
		}
	}
		
	@Test
	public void testRemoveEach() throws Exception
	{
		DataNode[] insertData = makeDataset( 2 );

		tree.insert( insertData[0].key, insertData[0].value );
		tree.remove( tree.getFirst() );
		assertNull( tree.getFirst() );

		tree.insert( insertData[0].key, insertData[0].value );
		tree.insert( insertData[1].key, insertData[1].value );
		tree.remove( tree.find( insertData[0].key) );
		assertEquals( tree.getFirst().getKey(), insertData[1].key );
		tree.removeAll();
		
		tree.insert( insertData[1].key, insertData[1].value );
		tree.insert( insertData[0].key, insertData[0].value );
		tree.remove( tree.find( insertData[1].key) );
		assertEquals( tree.getFirst().getKey(), insertData[0].key );
}
	
	@Test
	public void testRemoveAll() throws Exception
	{
		tree.insert( 0, "" );
		tree.removeAll();
		assertNull( tree.getFirst() );
	}
	
	@Test
	public void testSplitRemoval() throws Exception
	{
		final int SIZE = 8;
		DataNode[] insertNodes = makeDataset( SIZE );
		insertSequence( insertNodes );
		for( int i = SIZE /2; i >= 1; i-- )
		{
			tree.remove( tree.find( i ) );
			verifyRules();
		}
		Node<String> node = tree.getFirst();
		for( int i = SIZE / 2 + 1; i <= SIZE; i++ )
		{
			assertEquals( i, node.getKey() );
			node = tree.getNext( node );
		}
	}
	
	@Test
	public void testIteration() throws Exception {
		Node<String> node;
		DataNode nodes[] = makeDataset( 3 );
		insertSequence( nodes );
		
		// Forwards
		node = tree.getFirst();
		assertEquals( 1, node.getKey() );
		node = tree.getNext( node );
		assertEquals( 2, node.getKey() );
		node = tree.getNext( node );
		assertEquals( 3, node.getKey() );
		assertEquals( null, tree.getNext( node ) );
		
		// Backwards
		node = tree.getLast();
		assertEquals( 3, node.getKey() );
		node = tree.getPrev( node );
		assertEquals( 2, node.getKey() );
		node = tree.getPrev( node );
		assertEquals( 1, node.getKey() );
		assertEquals( null, tree.getPrev( node ) );
	}
	
	@Test
	public void testToString() throws Exception
	{
		Node<String> node;
		
		tree.insert( 1, "1" );
		tree.insert( 2, "2" );
		String result = tree.toString();
		assertEquals( result, "Tree{{.|1+1|{.|2*2|.}}}" );
		node = tree.getFirst();
		assertEquals( node.toString(), "Node{1+1}" );
		node = tree.getNext( node );;
		assertEquals( node.toString(), "Node{2*2}" );
		result = tree.getFirst().toString();
		assertEquals( result, "Node{1+1}" );
	}
	
	@Test
	public void testFindMiss() throws Exception
	{
		tree.insert( 1, "One" );
		assertNull( tree.find( 2 ) );
	}
	
	@Test
	public void testDeepOrder() throws Exception
	{
		DataNode[] nodeSrc = makeDataset( 8 );
		insertSequence( nodeSrc );
		
		Node<String> head = getNode( tree, HEAD_FIELD );
		assertEquals( head.getKey(), 4 );
		assertTrue( ! isPrime( head ) );
		
		Node<String> upperNode = getNode( head, LESSER_FIELD );
		assertEquals( upperNode.getKey(), 2 );
		assertTrue( isPrime( upperNode ) );
		
		Node<String> midNode = getNode( upperNode, LESSER_FIELD );
		assertEquals( midNode.getKey(), 1 );
		assertTrue( ! isPrime( midNode ) );
		assertNull( getNode( midNode, LESSER_FIELD ) );
		assertNull( getNode( midNode, GREATER_FIELD ) );
		
		midNode = getNode( upperNode, GREATER_FIELD );
		assertEquals( midNode.getKey(), 3 );
		assertTrue( ! isPrime( midNode ) );
		assertNull( getNode( midNode, LESSER_FIELD ) );
		assertNull( getNode( midNode, GREATER_FIELD ) );
		
		upperNode = getNode( head, GREATER_FIELD );
		assertEquals( upperNode.getKey(), 6 );
		assertTrue( isPrime( upperNode ) );
		
		midNode = getNode( upperNode, LESSER_FIELD );
		assertEquals( midNode.getKey(), 5 );
		assertTrue( ! isPrime( midNode ) );
		assertNull( getNode( midNode, LESSER_FIELD ) );
		assertNull( getNode( midNode, GREATER_FIELD ) );

		midNode = getNode( upperNode, GREATER_FIELD );
		assertEquals( midNode.getKey(), 7 );
		assertTrue( ! isPrime( midNode ) );
		assertNull( getNode( midNode, LESSER_FIELD ) );
		
		Node<String> lowerNode = getNode( midNode, GREATER_FIELD );
		assertEquals( lowerNode.getKey(), 8 );
		assertTrue( isPrime( lowerNode ) );
		assertNull( getNode( lowerNode, LESSER_FIELD ) );
		assertNull( getNode( lowerNode, GREATER_FIELD ) );
	}
	
	@Test
	public void testDouble() throws Exception
	{
		DataNode[] data = makeDataset( 1 );
		tree.insert( data[0].key, data[0].value );
		verifyRules();
		tree.insert( data[0].key, data[0].value );
		verifyRules();
		Node<String> res = tree.getFirst();
		tree.remove( res );
		verifyRules();
		try{
			tree.remove( res );
			fail( "Removing the same node twice should have thrown an exception" );
		}catch( NullPointerException e)
		{
			// Expected
		}
		tree.insert( data[0].key, data[0].value );
		verifyRules();
		tree.insert( data[0].key, data[0].value );
		verifyRules();
	}

	@SuppressWarnings("unchecked")
	private static Node<String> getNode( Object obj, String fieldName ) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		Field field = obj.getClass().getDeclaredField( fieldName );
		field.setAccessible( true );
		return (Node<String>)field.get( obj );
	}
	
	private static boolean isPrime( Node<String> node ) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		if( null == node )
		{
			return false; // All leaf (null) nodes are not prime
		}
		Field field = Node.class.getDeclaredField( PRIME_FIELD );
		field.setAccessible( true );
		return ( (Boolean)field.get( node ) ).booleanValue();
	}

	private static DataNode[] makeDataset( int number )
	{
		DataNode result[] = new DataNode[number];
		for( int i = 1; i <= number; i++ )
		{
			result[i - 1] = new DataNode( i, "Value" + i );
		}
		return result;
	}
	
	@Test
	public void testLargeSeq() throws Exception {
		DataNode[] insertNodes = makeDataset(8);
		DataNode[] checkNodes = new DataNode[insertNodes.length - 1];
		for (int i = 0; i < insertNodes.length; i++) {
			insertSequence(insertNodes);
			Node<String> node = tree.find(i + 1);
			tree.remove(node);
			for (int ci = 0; ci < checkNodes.length; ci++) {
				checkNodes[ci] = insertNodes[i <= ci ? ci + 1 : ci];
			}
			assertSequence(checkNodes);
			tree.removeAll();
		}
	}
	
	/**
	 * Test all permutations of adding 9 nodes to a tree. This guarantees coverage of all add/remove cases.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPermutation() throws Exception
	{
		DataNode[] insertNodes = makeDataset( 9 );
		permute( insertNodes );
	};
	
	private void permute( DataNode[] data ) throws Exception
	{
		permute( data, data.length );
	}
	
	private void permute( DataNode[] data, int depth ) throws Exception
	{
		for( int i = 0; i < data.length; i++ )
		{
			DataNode node = data[i];
			if( null == tree.find( node.key ) )
			{
				insertNode( node );
				verifyRules();
				if( 1 == depth )
				{
					assertSequence( data );
				}
				else
				{
					permute( data, depth - 1 );
				}
				tree.remove( tree.find( node.key ) );
				verifyRules();
			}
		}
	}
	
	private void assertSequence( DataNode[] sequence )
	{
		Node<String> node = tree.getFirst();
		for( int i = 0; i < sequence.length; i++ )
		{
			assertEquals( node.getKey(), sequence[i].key );
			assertEquals( node.getValue(), sequence[i].value );
			node = tree.getNext( node );
		}
		assertNull( node );
	}
	
	private void insertSequence( DataNode[] sequence ) throws Exception
	{
		for( DataNode next : sequence )
		{
			insertNode( next );
			verifyRules();
		}
	}
	
	private void insertNode( DataNode node ) throws Exception
	{
		tree.insert( node.key, node.value );
	}
	
	/**
	 * Verifies all rules for a valid "Prime/Non-prime" (A.K.A. "Red/Black") tree
	 * @throws Exception
	 */
	private void verifyRules() throws Exception
	{
		// Rule #1: Nodes are either not prime or prime (implicit in boolean prime member)
		// Rule #2: Head of tree is always not prime
		Node<String> head = getNode( tree, HEAD_FIELD );
		assertTrue( ! isPrime( head ) );
		// Rule #3: All leaves are not prime (Implicit as all leaves are null and all null leaves are not prime)
		// Rule #4: Both children of all prime Nodes are not prime
		checkPrimeChildren( head );
		// Rule #5 Always the same number of non-prime nodes down each path to all leaves
		checkDepth( head );
	}
		
	/**
	 * Checks every child Node in a subtree, asserting that no prime Node has either child Node also
	 * prime. Leaf (null) and non-prime Nodes are not subject to any further testing.
	 * @param node
	 * @throws Exception
	 */
	private void checkPrimeChildren( Node<String> node ) throws Exception
	{
		if( null == node )
		{
			return;
		}
		Node<String> lesser = getNode( node, LESSER_FIELD );
		Node<String> greater = getNode( node, GREATER_FIELD );
		if( isPrime( node ) )
		{
			assertTrue( ! isPrime( lesser ) );
			assertTrue( ! isPrime( greater ) );
		}
		checkPrimeChildren( lesser );
		checkPrimeChildren( greater );
	}
	
	/**
	 * Checks that the "prime node depth" (The number of prime nodes along every path to leaves) is the same for both branches of this
	 * node. This is a recursive method, so the prime-node-depth of this node is returned.
	 * 
	 * @param node The node to test the branch prime-node depth of and return the prime-node-depth for
	 * @return The prime-node-depth of this node
	 * @throws Exception from getNode()
	 */
	private int checkDepth( Node<String> node ) throws Exception
	{
		if( null == node )
		{
			return 1;
		}
		int depthLesser = checkDepth( getNode( node, LESSER_FIELD ) );
		int depthGreater = checkDepth( getNode( node, GREATER_FIELD ) );
		assertEquals( "Child-prime-depth error", depthLesser, depthGreater );
		return depthLesser + ( isPrime( node ) ? 0 : 1 );
	}
	
	@Before
	public void setUp()
	{
		tree = new Tree<String>();
	}

	@After
	public void tearDown()
	{
		tree.removeAll();
		tree.discardGarbage();
		tree = null;
	}

	/**
	 * A container object used for creating and managing a set of "sample" data entries for filling the Tree structure with.
	 */
	private static class DataNode
	{
		public int key;
		public String value;
		
		public DataNode( int key, String value )
		{
			this.key = key;
			this.value = value;
		}
		
		public String toString()
		{
			return "DataNode{" + key + ", \"" + value +"\"}";
		}
	}
	
	private Tree<String> tree;
	private static final String HEAD_FIELD = "head";
	private static final String LESSER_FIELD = "lesser";
	private static final String GREATER_FIELD = "greater";
	private static final String PRIME_FIELD = "prime";
}
