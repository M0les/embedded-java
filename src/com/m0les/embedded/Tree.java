package com.m0les.embedded;

import com.m0les.embedded.Pool.PoolExhaustedException;

/**
 * <h2>An efficient, low-garbage, integer-keyed map intended for embedded
 * applications.</h2>
 * 
 * <h3>Description</h3>
 * <p>This is built on a Bayer red-black tree design with some modifications. One
 * change is the use of in-lining of insert/remove cases into a single method
 * call each. Also, the meaningless and arbitrary concept of "colouring" has
 * been replaced with a boolean property of nodes called "prime" (equivalent to
 * "red" in the classic design). Whenever the classic design has a choice of
 * equivalent operations (e.g. Selecting a min/max descendant node to swap
 * values with when removing a node with two non-null children), this
 * implementation just hardcodes one option (typically "left") for expediency.</p>
 * 
 * <h3>Typical usage</h3>
 * <p>Initially a Tree can be instantiated in one of three ways. The first and simplest method is the no-argument constructor that
 * instantiates a stand-alone Tree with no limit on its size.</p>
 * <p>The second constructor takes an integer pool-size parameter, which is the maximum number of nodes (key/value pairs) this Tree
 * can hold.</p>
 * <p>The third constructor takes a Pool parameter of the same generic-type as this instance. This constructor
 * is for the complex cases where a common pool of data objects is used for many different trees. This is useful when data objects
 * are moved and copied between Trees.</p>
 * 
 * @author Miles Goodhew
 * @version $Id: Tree.java,v 1.16 2011-07-24 14:53:46 mgoodhew Exp $
 * @param <V> The subclass of Objects stored within the tree (the value-type of
 *            the tree)
 * 
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
public class Tree<V> implements Recycler {
	/**
	 * Create a new Tree with its own Node Pool that has no arbitrary size-limits.
	 * The pool can grow indefinitely until environment-restrictions come into play
	 * (memory size, etc.)
	 */
	public Tree() {
		pool = new NodePool<V>();
	}

	/**
	 * Create a Tree with its own finite-sized Pool of recyclable Node instances. This
	 * essentially limits the number of Nodes the Tree instance can contain.
	 * 
	 * @param poolSize
	 *            The maximum number of Node instances this Tree can hold.
	 */
	public Tree(int poolSize) {
		pool = new NodePool<V>( poolSize );
	}

	/**
	 * Create a Tree with an externally-provided Pool of Nodes.
	 * 
	 * @param pool The pool of Nodes to fill tree with (Must not be null).
	 */
	public Tree(NodePool<V> pool) throws NullPointerException {
		if( null == pool ){
			throw NULL_POOL;
		}
		this.pool = pool;
	}

	/**
	 * The complete Node-insertion operation, including all cases of the
	 * procedure.
	 * 
	 * @note This method has a high McCabe cyclomatic complexity. This is a
	 *       necessary downside to the "high efficiency" nature of this
	 *       implementation. Using the fully monolithic implementation saves on
	 *       the overhead of stack-frame management.
	 * 
	 * @param key
	 *            The key to refer to the new value by
	 * @param value
	 *            The instance referred-to by key inside this Tree instance.
	 * @throws PoolExhaustedException
	 *             if this Tree was constructed with a finite-sized pool of
	 *             recyclable Node instances and the pool has been exhausted.
	 */
	public synchronized void insert(int key, V value) throws PoolExhaustedException {
		Node<V> node = getNode(key, value);
		if (null == head) {
			// Case 1
			node.parent = null;
			node.prime = false;
			head = node;
		} else {
			Node<V> parent = head;
			while (true) {
				if (parent.key > node.key) {
					if (null == parent.lesser) {
						node.parent = parent;
						parent.lesser = node;
						break;
					} else {
						parent = parent.lesser;
					}
				} else {
					if (null == parent.greater) {
						node.parent = parent;
						parent.greater = node;
						break;
					} else {
						parent = parent.greater;
					}
				}
			}

			// colouring
			while (true) {
				if (null == parent) {
					node.prime = false;
					return;
				}
				if (!parent.prime) // Case 2
				{
					return;
				}

				// Case 3
				final Node<V> grandparent = parent.parent;
				final Node<V> uncle;
				if (grandparent.lesser == parent) {
					uncle = grandparent.greater;
				} else {
					uncle = grandparent.lesser;
				}
				if (parent.prime && isPrime(uncle)) {
					parent.prime = false;
					uncle.prime = false;
					grandparent.prime = true;
					parent = grandparent.parent;
					// NOTE: ( uncle != null ) == ( grandparent != null )
					node = grandparent;
				} else // Case 4
				{
					if (parent.greater == node && grandparent.lesser == parent) {
						rotate(parent, DIR_LEFT);
						node = parent;
						parent = node.parent;
					} else if (parent.lesser == node
							&& grandparent.greater == parent) {
						rotate(parent, DIR_RIGHT);
						node = parent;
						parent = node.parent;
					}
					// Case 5
					if (parent == grandparent.lesser) {
						rotate(grandparent, DIR_RIGHT);
					} else {
						rotate(grandparent, DIR_LEFT);
					}
					grandparent.prime = true; // grandparent cannot be null as
										      // that would mean !parent.prime,
											  // caught in case 2.
					parent.prime = false;
					return;
				}
			}
		}
	}

	/**
	 * The complete Node-removal operation, including all cases of the
	 * procedure.
	 * 
	 * @note This method has a high McCabe cyclomatic complexity. This is a
	 *       necessary downside to the "high efficiency" nature of this
	 *       implementation. Using the fully monolithic implementation saves on
	 *       the overhead of stack-frame management.
	 * 
	 * @param node
	 *            The Node to remove from this Tree
	 */
	public synchronized void remove(Node<V> node) {
		/* If the Node has two non-null children, then swap values with the
		 * greatest element in its lesser-branch and remove that former
		 * "greatest lesser" node (which must have < 2 non-null children).
		 */
		if (null != node.greater && null != node.lesser) {
			Node<V> newNode = node.lesser.findGreatest();
			node.key = newNode.key;
			node.value = newNode.value;
			node = newNode;
		}
		// Node now has 0 or 1 non-null children.
		Node<V> parent = node.parent;
		Node<V> child = null != node.lesser ? node.lesser : node.greater;
		if (node == head) {
			head = child;
			if (null != head) {
				head.parent = null;
				head.prime = false;
			}
			discard(node);
			return;
		}
		if (parent.lesser == node) {
			parent.lesser = child;
		} else {
			parent.greater = child;
		}
		if (null != child) {
			child.parent = parent;
		}
		if (!node.prime) {
			discard(node);
			if (isPrime(child)) {
				child.prime = false;
			} else {
				node = child;
				// Delete case 1
				while (null != parent) {
					boolean left = parent.lesser == node;
					// Delete case 2
					Node<V> sibling;
					if (left) {
						sibling = parent.greater;
					} else {
						sibling = parent.lesser;
					}
					if (isPrime(sibling)) {
						parent.prime = true;
						sibling.prime = false;
						if (left) {
							rotate(parent, DIR_LEFT);
							sibling = parent.greater;
						} else {
							rotate(parent, DIR_RIGHT);
							sibling = parent.lesser;
						}
					}
					// Delete case 3
					if (!(isPrime(sibling.lesser) || isPrime(sibling.greater))) {
						sibling.prime = true;
						// Delete case 4
						if (parent.prime) {
							parent.prime = false;
							return;
						} else {
							node = parent;
							parent = node.parent;
						}
					} else {
						// Delete case 5
						if (left && isPrime(sibling.lesser)) {
							sibling.prime = true;
							sibling.lesser.prime = false;
							rotate(sibling, DIR_RIGHT);
							sibling = parent.greater;
						} else if (!left && isPrime(sibling.greater)) {
							sibling.prime = true;
							sibling.greater.prime = false;
							rotate(sibling, DIR_LEFT);
							sibling = parent.lesser;
						}
						// Delete case 6
						sibling.prime = parent.prime;
						parent.prime = false;
						if (left) {
							if (sibling.greater != null) {
								sibling.greater.prime = false;
							}
							rotate(parent, DIR_LEFT);
						} else {
							if (sibling.lesser != null) {
								sibling.lesser.prime = false;
							}
							rotate(parent, DIR_RIGHT);
						}
						return;
					}
				}
			}
		}
	}

	/**
	 * Get the first value (lowest key) in the Tree.
	 * 
	 * @return The lowest-keyed Node in this Tree
	 */
	public synchronized Node<V> getFirst() {
		if (null == head) {
			return null;
		}
		return head.findLeast();
	}

	/**
	 * Get the last value (highest key) in the Tree.
	 * 
	 * @return The highest-keyed Node in this Tree
	 */
	public synchronized Node<V> getLast() {
		if (null == head) {
			return null;
		}
		return head.findGreatest();
	}

	/**
	 * Remove every Node in this Tree and recycle them. After this call, the
	 * tree shall be empty and ready to add new entries.
	 */
	public synchronized void removeAll() {
		removeNodes(head);
		head = null;
	}

	/**
	 * Find a Node stored within this Tree, referenced by its integer key.
	 * 
	 * @param key
	 *            The key of the Node to look for
	 * @return The Node with the specified key or null if no such key exists in
	 *         the Tree
	 */
	public synchronized Node<V> find(final int key) {
		Node<V> current = head;
		while (null != current) {
			if (key < current.key) {
				current = current.lesser;
			} else if (key > current.key) {
				current = current.greater;
			} else {
				return current;
			}
		}
		return null;
	}

	/**
	 * Generate a human-readable representation of this Tree instance.
	 * 
	 * @note This method composes Strings and thus generates garbage, use in
	 *       embedded applications should be avoided generally, barring
	 *       exceptional situations.
	 * @see #nodeString
	 */
	@Override
	public synchronized String toString() {
		return "Tree{" + nodeString(head) + "}";
	}

	/**
	 * Find the Node with the next-lowest key related to this Node.
	 * 
	 * @param node
	 *            The Node to find the Node with the next lowest key for
	 * @return The Node with the next lowest key below the nominated Node or
	 *         null if there is no lower-keyed Node.
	 */
	public synchronized Node<V> getPrev(Node<V> node) {
		Node<V> candidate = node.lesser;
		if (null != candidate) {
			return candidate.findGreatest();
		}
		while (null != (candidate = node.parent)) {
			if (node == candidate.greater) {
				return candidate;
			}
			node = candidate;
		}
		return null;
	}

	/**
	 * Find the Node with the next-highest key related to this Node.
	 * 
	 * @param node
	 *            The Node to find the Node with the next highest key for
	 * @return The Node with the next highest key above the nominated Node or
	 *         null if there is no higher-keyed Node.
	 */
	public synchronized Node<V> getNext(Node<V> node) {
		Node<V> candidate = node.greater;
		if (null != candidate) {
			return candidate.findLeast();
		}
		while (null != (candidate = node.parent)) {
			if (node == candidate.lesser) {
				return candidate;
			}
			node = candidate;
		}
		return null;
	}

	/**
	 * @see Recycler
	 */
	@Override
	public void discardGarbage() {
		pool.discardGarbage();
	}

	/**
	 * @see Recycler
	 */
	@Override
	public void discardGarbage(int maxRemaining) {
		pool.discardGarbage(maxRemaining);
	}
	
	/**
	 * This is a node within a tree. Nodes have a key element so they can be
	 * located and have a value element that they carry for the user of the
	 * class. The key and value may be different instances and classes, but it's
	 * quite possible and legal that they are the same instance,
	 * 
	 * @param <K>
	 *            The key-class for node elements
	 * @param <V>
	 *            The value-class for node elements
	 */
	public static class Node<V> {
		private int key;
		private V value;
		private boolean prime; // => "RED"?
		private Node<V> parent = null;
		private Node<V> lesser = null;
		private Node<V> greater = null;
		private NodeLink link = new NodeLink();

		/**
		 * A Link to another node
		 * 
		 * @see Link
		 */
		private class NodeLink extends Link<NodeLink> {
			/**
			 * @see Link
			 */
			public Node<V> getNode() {
				return Node.this;
			}
		}

		/**
		 * Get the key for this node
		 * 
		 * @return The key element of this node
		 */
		public int getKey() {
			return key;
		}

		/**
		 * Get the value of this node
		 * 
		 * @return The value element of this node
		 */
		public V getValue() {
			return value;
		}

		/**
		 * Generate a human-readable representation of this Node instance.
		 * 
		 * @note This method composes Strings and thus generates garbage, use in
		 *       embedded applications should be avoided generally, barring
		 *       exceptional situations.
		 * @see #contentString()
		 */
		@Override
		public String toString() {
			return "Node{" + contentString() + "}";
		}

		/**
		 * Helper method to pretty-format the key/prime/value components of a
		 * Node
		 * 
		 * @note This method composes Strings and thus generates garbage, use in
		 *       embedded applications should be avoided generally, barring
		 *       exceptional situations.
		 * 
		 * @return The String representing the core properties of this Node
		 * @see #toString()
		 */
		private String contentString() {
			return key + (prime ? "*" : "+") + value;
		}

		/**
		 * Find the Node with the greatest key from this Node downward. Usually
		 * this will be the last Node found when iterating down the greater
		 * child branch or this Node if the branch is null.
		 * 
		 * @return The Node with the greatest key from this Node downward.
		 */
		private Node<V> findGreatest() {
			Node<V> node = this;
			Node<V> next = greater;
			while (null != next) {
				node = next;
				next = node.greater;
			}
			return node;
		}

		/**
		 * Find the Node with the lowest key from this Node downward. Usually
		 * this will be the
		 * 
		 * @return The Node with the lowest key from this Node downward. last
		 *         Node found when iterating down the lesser child branch or
		 *         this Node if the branch is null.
		 */
		private Node<V> findLeast() {
			Node<V> node = this;
			Node<V> next = lesser;
			while (null != next) {
				node = next;
				next = node.lesser;
			}
			return node;
		}
	}

	/**
	 * Perform a tree-rotation about a nominated node in a nominated direction.
	 * 
	 * @param node
	 *            The Node about which to rotate.
	 * @param left
	 *            One of DIR_LEFT (For a rotate-left) or DIR_RIGHT (For a
	 *            rotate-right).
	 */
	private void rotate(Node<V> node, boolean left) {
		Node<V> parent = node.parent;
		Node<V> newParent;
		Node<V> newChild;
		if (left) {
			newParent = node.greater;
			newChild = node.greater = newParent.lesser;
			newParent.lesser = node;
		} else // right
		{
			newParent = node.lesser;
			newChild = node.lesser = newParent.greater;
			newParent.greater = node;
		}
		node.parent = newParent;
		newParent.parent = parent;
		if (null != newChild) {
			newChild.parent = node;
		}
		if (null == parent) {
			head = newParent;
		} else {
			if (parent.lesser == node) {
				parent.lesser = newParent;
			} else {
				parent.greater = newParent;
			}
		}
	}

	/**
	 * A simple test to see if a Node is prime or not. Note that null parameters
	 * are allowed and considered "non-prime"
	 * 
	 * @param node
	 *            The Node to test for prime-status
	 * @return True if node is not-null and prime, false otherwise
	 */
	private final boolean isPrime(Node<V> node) {
		return null != node && node.prime;
	}

	/**
	 * Create or recycle a Node to store a new key/value pair in.
	 * 
	 * @param key
	 *            The key for the Node to return
	 * @param value
	 *            The value for the Node to return
	 * @return A newly-allocated or recently-recycled Node object
	 * @throws PoolExhaustedException
	 *             If this Tree has a limited resource pool that has been
	 *             exhausted.
	 */
	private Node<V> getNode(int key, V value) throws PoolExhaustedException {
		Node<V> node = pool.getInstance().getNode();
		node.key = key;
		node.value = value;
		node.parent = node.lesser = node.greater = null;
		node.prime = true;
		return node;
	}

	/**
	 * Helper method to remove all nodes from the nominated Node downwards.
	 * 
	 * @param node
	 *            The node to delete (along with all its children).
	 * @see #discard(Node)
	 * @see #removeAll()
	 */
	private void removeNodes(Node<V> node) {
		if (null != node) {
			removeNodes(node.lesser);
			removeNodes(node.greater);
			discard(node);
		}
	}

	/**
	 * Clear the contents of a Node (Especially Object references) and pool it
	 * for reuse.
	 * 
	 * @param node
	 *            The node to clear the data-members of.
	 * @see #removeAll()
	 * @see #removeNodes()
	 */
	private void discard(Node<V> node) {
		node.value = null;
		node.lesser = null;
		node.greater = null;
		node.parent = null;
		pool.returnInstance(node.link);
	}

	/**
	 * Helper method to pretty-format the structure and nodes of a Tree
	 * 
	 * @note This method composes Strings and thus generates garbage, use in
	 *       embedded applications should be avoided generally, barring
	 *       exceptional situations.
	 * 
	 * @return The String representing the core values of this Tree
	 * @see #toString()
	 */
	private String nodeString(Node<V> node) {
		if (null == node) {
			return ".";
		}
		String result = "{";
		result += nodeString(node.lesser);
		result += "|" + node.contentString() + "|";
		result += nodeString(node.greater);
		return result + "}";
	}

	/**
	 * A Pool of Nodes for Trees to use. Nodes are instantiated and presented as their inner
	 * NodeLink members, so that the Pool can manage them.
	 * 
	 * @see Factory
	 * @see Pool
	 * @author mgoodhew
	 */
	public static class NodePool<V> extends Pool<Node<V>.NodeLink>
	{
		public NodePool() {
			super(new NodeFactory<V>());
		}

		public NodePool(int poolSize) {
			super(new NodeFactory<V>(), poolSize);
		}

		private static class NodeFactory<V> implements Factory<Node<V>.NodeLink> {
			/**
			 * @see Factory
			 */
			@Override
			public Node<V>.NodeLink newInstance() {
				return new Node<V>().link;
			}
		}
	}
	
	private static final boolean DIR_LEFT = true;
	private static final boolean DIR_RIGHT = !DIR_LEFT;
	private static final NullPointerException NULL_POOL = new NullPointerException( "Null pool argument" );
	
	private Node<V> head = null;
	private final NodePool<V> pool;
}
