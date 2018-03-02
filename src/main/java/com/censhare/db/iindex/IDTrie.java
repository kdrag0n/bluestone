/*
*  Copyright (c) 2010 by censhare AG
*  SVN $$Id: IDTrie.java 76168 2012-07-18 06:53:35Z wb $$
*/
package com.censhare.db.iindex;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Abstract trie collection class combining IDSetTrie and IDMapTrie
 * @author Walter Bauer
 * @author Guido von Walter
 */
public abstract class IDTrie {

    // Node layout:
    // node: bitMap, childValue*
    // childValue: childPtr (inner node) or inline bitMap (leaf)
    // childPtr: generation (32 bit) << 32 | index (32 Bit)

    // Note: shift only takes the lowest bits, (1L << (key & 0x3F)) can be written as (1L << key)

    private final static int HEADER_SIZE = 2;
    private final static int FREE_LISTS_SIZE = 64 + 2; // 0..65
    private final static int KNOWN_EMPTY_NODE = 0;
    final static int MIN_SIZE = HEADER_SIZE + 22; // one key

    /** Heap. */
    long[] mem;

    /** Number of entries. */
    int count;

    /** Index to unused space at end of heap. */
    private int freeIdx;

    /** Amount of free memory (replaced nodes). */
    private int freeCount;

    /** Generation. */
    long generation;

    /** Index and generation of current root. */
    long root;

    /** Stack used for updates. */
    long[] nodeIdxs = new long[10+1];
    
    /** Linked lists of free space (if enabled). */
    private int[] freeLists;

    // performance metrics
//    public static int metricExpands;
//    public static int metricCompacts;

    /** Constructor with given size. */
    IDTrie(int size) {
        if (size < MIN_SIZE)
            size = MIN_SIZE;

        mem = new long[size];

        // header
        // mem[0] = 0; // known empty node (bitMap = 0 | pointer to known empty node)
        // mem[1] = 0; // future use

        // free memory
        freeIdx = HEADER_SIZE;

        generation = 0;
        root = KNOWN_EMPTY_NODE; // | (generation << 32)
        count = 0;
    }

    /** Return copy for performing updates increasing generation number. */
    IDTrie(IDTrie bst) {
        mem = bst.mem;
        root = bst.root;
        count = bst.count;
        freeCount = bst.freeCount;
        freeIdx = bst.freeIdx;
        if (freeIdx == 0)
            throw new RuntimeException();
        generation = bst.generation + 1;
        
        // since some other "transaction" (earlier copy) may have failed, the free list may contain nodes pointing to blocks after this freeIdx
        // therefore start with new list to avoid that nodes may be overridden
        if (bst.freeLists != null)
            freeLists = new int[FREE_LISTS_SIZE];
    }
    
    /** 
     * Enable tracking of recycling replaced nodes in current generation. 
     * May improve performance if multiple put/clear are applied on one generation.
     */
    public IDTrie withFreeLists() {
        freeLists = new int[FREE_LISTS_SIZE];
        return this;
    }

    abstract protected IDTrie createInstance(int size);

    /** Contains operation. */
    public boolean get(long key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add key.
     * @return old value (true if set already contained the key)
     */
    public boolean set(long key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get operation.
     * @return mapped value or defaultValue if not found
     */
    public long get(long key, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add key and value.
     */
    public void put(long key, long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add key and value.
     * @return old value or defaultValue if set didn't contain the key
     */
    public long put(long key, long value, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove key.
     * @return true if set contained the key
     */
    public boolean clear(long key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove key.
     * @return old value or defaultValue if set didn't contain the key
     */
    public long clear(long key, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    /** Return first (lowest unsigned) key. */
    abstract public long first();

    /** Return last (highest unsigned) key. */
    abstract public long last();

    /** Create cursor on Trie. */
    protected abstract IDTrieCursor cursor();

    /** Returns number of keys. */
    int size() {
        return count;
    }

    /** Returns used memory size in number of longs. */
    private int sizeUsed() {
        return freeIdx - freeCount;
    }

    /** Returns memory size in number of longs. */
    private int sizeAllocated() {
        return mem.length;
    }

    /**
     * Memory allocation.
     * @param size requested size
     * @param compactAllowed if true, will perform compact instead of expansion which changes indexes of all nodes however
     */
    int allocate(int size, boolean compactAllowed) {
        int currSize = mem.length;
        int sizeUsed = sizeUsed();

        // shrink by 20% if even half of it would be enough
        // (use low threshold to avoid frequent increase/decrease)
        if (compactAllowed && MIN_SIZE + sizeUsed + size < currSize / 2) {
            int newSize = currSize - currSize / 5; // currSize /= 1.25
            compactThis(newSize);
            return -1;
        }

        if (freeLists != null) {
            int free = freeLists[size];
            if (free != 0) {
                // re-link and return head
                freeLists[size] = (int) mem[free];
                freeCount -= size;
                return free;
            }
        }

        // expansion required?
        if (freeIdx + size > mem.length) {
            if (compactAllowed) {
                int newSize;
                if (sizeUsed + size >= currSize / 2 + currSize / 4) {
                    // increase by 25% if less than a quarter of it is free and assure this is enough
                    newSize = currSize + Math.max(currSize / 4, size);
                }
                else {
                    // keep size
                    newSize = currSize;
                }

                // compact
                compactThis(newSize);
                return -1;
            }

            // increase by 25% and assure this is enough
            int newSize = currSize + Math.max(currSize / 4, size);
            mem = Arrays.copyOf(mem, newSize);
//            metricExpands++;
        }

        int idx = freeIdx;
        freeIdx += size;
        return idx;
    }

    /** Memory deallocation. */
    void deallocate(boolean isThisGeneration, int idx, int size) {
        if (idx == 0)
            return; // keep our known empty node

        freeCount += size;

        if (freeLists != null && isThisGeneration) {
            // add to head of free-list
            mem[idx] = freeLists[size];
            freeLists[size] = idx;
        }
    }

    /** Compact this instance. */
    private void compactThis(int newSize) {
        IDTrie bst = compact(newSize);
        this.freeCount = bst.freeCount;
        this.freeIdx = bst.freeIdx;
        this.generation = bst.generation;
        this.root = bst.root;
        this.count = bst.count;
        this.mem = bst.mem;
        this.freeLists = bst.freeLists;
//        metricCompacts++;
    }

    public IDTrie copy() {
        return compact(sizeAllocated());
    }

    /** Return compacted copy. */
    public IDTrie compact() {
        return compact(freeIdx - freeCount);
    }

    /** Return compacted copy with given heap size. */
    private IDTrie compact(int newSize) {
        IDTrie bst = createInstance(newSize);
        int rootIdx = (int) root;
        bst.root = copy(10, rootIdx, bst);
        bst.count = count;
        if (freeLists != null)
            bst.freeLists = new int[FREE_LISTS_SIZE];
        return bst;
    }

    /** Helper for compact. */
    abstract protected int copy(int level, int nodeIdx, IDTrie bst);

        /** Helper for compact. */
        int copy(int level, int endLevel, int nodeIdx, IDTrie bst) {
        // local references for performance
        long[] mem = this.mem;
        long[] mem2 = bst.mem;

        long bitMap = mem[nodeIdx];
        int size = Long.bitCount(bitMap);

        int newNodeIdx = bst.freeIdx;
        bst.freeIdx += size + 1;

        mem2[newNodeIdx] = bitMap;
        int a = newNodeIdx + 1;
        int b = nodeIdx + 1;

        if (level == endLevel) {
            for (int j = 0; j < size; j++)
                mem2[a++] = mem[b++];
        }
        else {
            for (int j = 0; j < size; j++)
                mem2[a++] = copy(level - 1, endLevel, (int) mem[b++], bst);
        }

        return newNodeIdx;
    }

    Iterable<Long> getKeys() {
        final IDTrie.IDTrieCursor cursor = cursor();

        return () -> new Iterator<>() {

            long next = cursor.first();

            @Override
            public boolean hasNext() {
                return next != -1;
            }

            @Override
            public Long next() {
                long r = next;
                next = cursor.next();
                return r;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    /**
     * Cursor to iterate over keys in set.
     */
    public interface IDTrieCursor {

        /** Returns first (lowest) key and (re-)start cursor. */
        public long first();

        /** Returns first (lowest) key that is greater than or equal to given key and (re-)start cursor. */
        public long first(long targetKey);

        /** Returns next key. */
        public long next();

        /** Returns current value of current key. */
        public long getValue();
    }

}
