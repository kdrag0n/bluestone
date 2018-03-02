/*
 * Copyright (c) by censhare AG
 * SVN $Id: IDSetTrie.java 75644 2012-07-02 13:24:25Z wb $
 */
package com.censhare.db.iindex;

import java.util.Arrays;

/**
 * Implementation of concurrent set of long integers using bit-group trie.
 * Supports multiple concurrent readers and one writer.
 * @author Walter Bauer
 */
public class IDSetTrie extends IDTrie {

    /** Constructor. */
    public IDSetTrie() {
        this(MIN_SIZE);
    }

    /** Constructor with given size. */
    public IDSetTrie(int size) {
        super(size);
    }

    /** Return copy for performing updates increasing generation. */
    public IDSetTrie(IDSetTrie bst) {
        super(bst);
    }

    @Override
    protected IDTrie createInstance(int size) {
        return new IDSetTrie(size);
    }

    /** Contains operation. */
    @Override
    final public boolean get(long key) {
        long[] mem = this.mem; // local reference for performance
        long childValue = root;

        // inner nodes
        for (int i = 10; i > 0; i--) {
            int nodeIdx = (int) childValue;
            long bitPos = 1L << (key >>> (i * 6));
            long bitMap = mem[nodeIdx];
            if ((bitMap & bitPos) == 0) 
                return false;
            int childIdx = Long.bitCount(bitMap & (bitPos - 1));
            childValue = mem[nodeIdx + 1 + childIdx];
        }

        // leaf node
        long bitPosLeaf = 1L << key;
        return (childValue & bitPosLeaf) != 0;
    }

    /**
     * Add key.
     * @return old value (true if set already contained the key)
     */
    @Override
    final public boolean set(long key) {

        retry: for (;;) {
            
            // first step: search for key (as in get) but note childPtr walking down trie, exit if found

            Arrays.fill(nodeIdxs, 0);

            long childValue = root;

            // inner nodes
            for (int i = 10; i > 0; i--) {
                nodeIdxs[i] = childValue;
                int nodeIdx = (int) childValue;
                long bitPos = 1L << (key >>> (i * 6));
                long bitMap = mem[nodeIdx];
                if ((bitMap & bitPos) == 0) {
                    childValue = 0;
                    break; // doesn't contain key
                }
                int childIdx = Long.bitCount(bitMap & (bitPos - 1));
                childValue = mem[nodeIdx + 1 + childIdx];
            }

            // leaf node
            long bitPosLeaf = 1L << key;
            if ((childValue & bitPosLeaf) != 0)
                return true;  // contains key

            // update value
            childValue = childValue | bitPosLeaf;

            // second step: since not found, update or add childValue walking up trie again

            for (int i = 1; i <= 10; i++) {
                long parentValue = nodeIdxs[i];
                int nodeIdx = (int) parentValue;
                long nodeGen = (parentValue >>> 32);
                long bitMap = mem[nodeIdx];
                long bitPos = 1L << (key >>> (i * 6));
                int childIdx = Long.bitCount(bitMap & (bitPos - 1));

                int newNodeIdx;
                int size = Long.bitCount(bitMap);
                if ((bitMap & bitPos) == 0) {
                    // insert
                    // sizeof(bitMap) + oldSize + 1
                    // compact only if leaf (otherwise deallocate has been called already and we cannot change node indexes any longer)
                    newNodeIdx = allocate(size + 2, i == 1); 
                    if (newNodeIdx < 0)
                        continue retry;
                    mem[newNodeIdx] = bitMap | bitPos;
                    
                    // copy with child inserted
                    int a = newNodeIdx + 1;
                    int b = nodeIdx + 1;
                    for (int j = 0; j < childIdx; j++)
                        mem[a++] = mem[b++];
                    mem[a++] = childValue;
                    for (int j = childIdx; j < size; j++)
                        mem[a++] = mem[b++];
                }
                else {
                    // update childValue

                    if (nodeGen == generation) {
                        // optimization: if updated in this generation can override
                        mem[nodeIdx + 1 + childIdx] = childValue;
                        count++; // finally increment count now that we will exit
                        return false; // did not contain the key
                    }

                    // copy and update childValue
                    // sizeof(bitMap) + oldSize
                    // compact only if leaf (otherwise deallocate has been called already and we cannot change node indexes any longer)
                    newNodeIdx = allocate(size + 1, i == 1); 
                    if (newNodeIdx < 0)
                        continue retry;
                    int a = newNodeIdx;
                    int b = nodeIdx;
                    for (int j = 0; j < size + 1; j++)
                        mem[a++] = mem[b++];
                    mem[newNodeIdx + 1 + childIdx] = childValue;
                }

                childValue = newNodeIdx | (generation << 32);
                deallocate(nodeGen == generation, nodeIdx, size + 1);
            }

            root = childValue;

            count++; // finally increment count now that we will exit
            return false; // did not contain the key

        }
    }

    /**
     * Remove key.
     * @return old value (true if set contained the key)
     */
    @Override
    final public boolean clear(long key) {

        retry: for (;;) {

            // first step: search for key (as in get) but note childPtr walking down trie, exit if not found

            Arrays.fill(nodeIdxs, 0);

            long childValue = root;

            // inner nodes
            for (int i = 10; i > 0; i--) {
                nodeIdxs[i] = childValue;
                int nodeIdx = (int) childValue;
                long bitPos = 1L << (key >>> (i * 6));
                long bitMap = mem[nodeIdx];
                if ((bitMap & bitPos) == 0) 
                    return false; // doesn't contain key
                int childIdx = Long.bitCount(bitMap & (bitPos - 1));
                childValue = mem[nodeIdx + 1 + childIdx];
            }

            // leaf node
            long bitPosLeaf = 1L << key;
            if ((childValue & bitPosLeaf) == 0)
                return false;  // did not contain the key

            // second step: since found, delete walking up trie again, either delete child in list or remove whole node

            // flag to indicate empty node
            boolean remove = false;

            childValue = childValue & ~bitPosLeaf;
            if (childValue == 0)
                remove = true;

            // update childValue in parent node or remove child if empty
            for (int i = 1; i <= 10; i++) {
                long parentValue = nodeIdxs[i];
                int nodeIdx = (int) parentValue;
                long nodeGen = (parentValue >>> 32);
                
                long bitMap = mem[nodeIdx];
                long bitPos = 1L << (key >>> (i * 6));
                int childIdx = Long.bitCount(bitMap & (bitPos - 1));

                int newNodeIdx = 0;
                int size = Long.bitCount(bitMap);
                if (remove) {
                    // remove child
                    if (size > 1) {
                        // still has more children, remove child from list
                        // sizeof(bitMap) + oldSize - 1
                        // compact only if leaf (otherwise deallocate has been called already and we cannot change node indexes any longer)
                        newNodeIdx = allocate(size, i == 1); 
                        if (newNodeIdx < 0)
                            continue retry;
                        mem[newNodeIdx] = bitMap & ~bitPos;

                        // copy with child removed
                        int a = newNodeIdx + 1;
                        int b = nodeIdx + 1;
                        for (int j = 0; j < childIdx; j++)
                            mem[a++] = mem[b++];
                        b++;
                        for (int j = childIdx + 1; j < size; j++)
                            mem[a++] = mem[b++];
                        remove = false;
                    }
                    // else
                    //   last child, keep remove flag to remove this node
                }
                else {
                    // update childValue

                    if (nodeGen == generation) {
                        // optimization: if updated in this generation can override
                        mem[nodeIdx + 1 + childIdx] = childValue;
                        count--;
                        return true; // did contain the key
                    }

                    // copy and update childValue
                    // sizeof(bitMap) + oldSize
                    // compact only if leaf (otherwise deallocate has been called already and we cannot change node indexes any longer)
                    newNodeIdx = allocate(size + 1, i == 1);
                    if (newNodeIdx < 0)
                        continue retry;
                    int a = newNodeIdx;
                    int b = nodeIdx;
                    for (int j = 0; j < size + 1; j++)
                        mem[a++] = mem[b++];
                    mem[newNodeIdx + 1 + childIdx] = childValue;
                }

                childValue = newNodeIdx | (generation << 32);

                // if allocated in this generation can safely remove it again
                deallocate(nodeGen == generation, nodeIdx, size + 1);
            }

            if (remove)
                root = (generation << 32);
            else
                root = childValue;

            count--;
            return true; // did contain the key
        }

    }

    /** Return first (lowest unsigned) key. */
    @Override
    public long first() {
        if (count == 0)
            throw new IllegalArgumentException("empty set");
        
        long childValue = root;
        long value = 0;

        // inner nodes
        for (int i = 10; i > 0; i--) {
            int nodeIdx = (int) childValue;
            long bitMap = mem[nodeIdx];
            int first = Long.numberOfTrailingZeros(bitMap);
            value = (value << 6) | first;
            childValue = mem[nodeIdx + 1]; // take first child
        }

        long first = Long.numberOfTrailingZeros(childValue);
        value = (value << 6) | first;
        return value;
    }

    /** Return last (highest unsigned) key. */
    @Override
    public long last() {
        if (count == 0)
            throw new IllegalArgumentException("empty set");

        long childValue = root;
        long value = 0;

        // inner nodes
        for (int i = 10; i > 0; i--) {
            int nodeIdx = (int) childValue;
            long bitMap = mem[nodeIdx];
            int last = 63 - Long.numberOfLeadingZeros(bitMap);
            value = (value << 6) | last;
            long bitPos = 1L << last;
            int childIdx = Long.bitCount(bitMap & (bitPos - 1));                    
            childValue = mem[nodeIdx + 1 + childIdx]; // take last child
        }

        long last = 63 - Long.numberOfLeadingZeros(childValue);
        value = (value << 6) | last;
        return value;
    }

    /** Helper for compact. */
    @Override
    protected int copy(int level, int nodeIdx, IDTrie bst) {
        return copy(level, 1, nodeIdx, bst);
    }

    @Override
    public IDTrieCursor cursor() {
        return new IDSetTrieCursor(this);
    }

    /**
     * Cursor to iterate over keys in set.
     */
    public static class IDSetTrieCursor implements IDTrieCursor {

        long endKey;
        IDSetTrie bst;
        long[] nodeIdxs = new long[10+1];
        long key;

        /** 
         * Constructor with -1 as end of list key.
         * Does <code>IDSetTrieCursor(bst, -1)</code>.
         */
        IDSetTrieCursor(IDSetTrie bst) {
            this(bst, -1);
        }

        /** Constructor. */
        IDSetTrieCursor(IDSetTrie bst, long endKey) {
            this.bst = bst;
            this.endKey = endKey;
        }

        /** Returns first (lowest) key and (re-)start cursor. */
        @Override
        public long first() {
            if (bst.size() == 0) {
                key = endKey;
                return key;
            }

            long childValue = bst.root;
            key = 0;

            // inner nodes
            for (int i = 10; i > 0; i--) {
                nodeIdxs[i] = childValue;
                int nodeIdx = (int) childValue;
                long bitMap = bst.mem[nodeIdx];
                int first = Long.numberOfTrailingZeros(bitMap);
                key = (key << 6) | first;
                childValue = bst.mem[nodeIdx + 1]; // take first child
            }

            nodeIdxs[0] = childValue; // note: store value, not index
            long next = Long.numberOfTrailingZeros(childValue);
            key = (key << 6) | next;
            return key;
        }
        
        /** Returns first (lowest) key that is greater than or equal to given key and (re-)start cursor. */
        @Override
        public long first(long targetKey) {
            if (bst.size() == 0) {
                key = endKey;
                return key;
            }

            key = 0;                        
            int i = 10;
            nodeIdxs[i] = bst.root;
            long[] mem = bst.mem; // local reference for performance
            for (;;) {
                long childValue = nodeIdxs[i];
                if (i > 0) {
                  int nodeIdx = (int) childValue;
                  int group = (int) (targetKey >>> (i * 6)) & 0x3F;
                  long bitPos = 1L << group;
                  long bitMap = mem[nodeIdx];
                  if ((bitMap & bitPos) != 0) {
                      // match
                      key = (key << 6) | group;
                      int childIdx = Long.bitCount(bitMap & (bitPos - 1));                    
                      i--;
                      nodeIdxs[i] = mem[nodeIdx + 1 + childIdx];
                  }
                  else {
                      // no match, take next higher
                      int first = Long.numberOfTrailingZeros(bitMap & ~(bitPos - 1));
                      if (first >= 64) {
                          if (i == 10) {
                              key = endKey;
                              return key;
                          }
                          int x = (i + 1) * 6;
                          targetKey = ((targetKey >>> x) + 1) << x; // increment at parent group level and clear lower groups

                          // find level to continue (carry position) 
                          // a slower solution is simply setting: value = 0; i = 10;
                          int j = Long.numberOfTrailingZeros(targetKey) / 6;
                          key = key >>> ((j - i) * 6);
                          i = j;
                      }
                      else {
                          key = (key << 6) | first;
                          int childIdx = Long.bitCount(bitMap & (bitPos - 1));         
                          i--;
                          nodeIdxs[i] = mem[nodeIdx + 1 + childIdx];
                          targetKey = 0; // we are already greater, take first of remaining groups
                      }
                  }
                }
                else {
                    long bitPosLeaf = 1L << targetKey;
                    long next = Long.numberOfTrailingZeros(childValue & ~(bitPosLeaf - 1));
                    if (next >= 64) {
                        int x = (i + 1) * 6;
                        targetKey = ((targetKey >>> x) + 1) << x;
                        
                        // find level to continue (carry position) 
                        // a slower solution is simply setting: value = 0; i = 10;
                        int j = Long.numberOfTrailingZeros(targetKey) / 6;
                        key = key >>> ((j - i) * 6);
                        i = j;
                    }
                    else {
                        key = (key << 6) | next;
                        return key;
                    }
                }
            }
            
        }
        
        /** Returns next key. */
        @Override
        public long next() {
            if (key == endKey)
                return key;
            
            // get next in leaf
            long childValue = nodeIdxs[0];
            int group = (int) key & 0x3F;
            if (group < 0x3F) {
                long bitMask = 0xFFFFFFFFFFFFFFFEL << group; // 0x..FF << (group + 1)
                long next = Long.numberOfTrailingZeros(childValue & bitMask);
                if (next < 64) {
                    key = (key & ~0x3FL) | next;
                    return key;
                }
            }
            key = key >>> 6;

            long[] mem = bst.mem; // local reference for performance

            // get next in inner nodes
            for (int i = 1; i <= 10; i++) {
                long parentValue = nodeIdxs[i];
                int nodeIdx = (int) parentValue;
                long bitMap = mem[nodeIdx];
                group = (int) key & 0x3F;
                if (group < 0x3F) {
                    long bitMask = 0xFFFFFFFFFFFFFFFEL << group; // 0x..FF << (group + 1)
                    long next = Long.numberOfTrailingZeros(bitMap & bitMask);
                    if (next < 64) {
                        key = (key & ~0x3FL) | next;
                        int childIdx = Long.bitCount(bitMap & ~bitMask);
                        childValue = mem[nodeIdx + 1 + childIdx];

                        // starting at next level, find first
                        for (int j = i-1; j > 0; j--) {
                            nodeIdxs[j] = childValue;
                            nodeIdx = (int) childValue;
                            bitMap = mem[nodeIdx];
                            long first = Long.numberOfTrailingZeros(bitMap);
                            key = (key << 6) | first;
                            childValue = mem[nodeIdx + 1]; // take first child
                        }

                        nodeIdxs[0] = childValue; // note: store value, not index
                        long first = Long.numberOfTrailingZeros(childValue);
                        key = (key << 6) | first;
                        return key;
                    }
                }
                key = key >>> 6;
            }
            key = endKey;
            return key;
        }

        @Override
        public long getValue() {
            throw new UnsupportedOperationException();
        }
    }
    
}
