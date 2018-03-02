/*
 * Copyright (c) by censhare AG
 * SVN $Id: IDMapTrie.java 75644 2012-07-02 13:24:25Z wb $
 */
package com.censhare.db.iindex;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of concurrent map of long integers using bit-group trie.
 * Supports multiple concurrent readers and one writer.
 * @author Walter Bauer
 */
public class IDMapTrie extends IDTrie {


    /** Constructor. */
    public IDMapTrie() {
        this(MIN_SIZE);
    }

    /** Constructor with given size. */
    private IDMapTrie(int size) {
        super(size);
    }

    /** Return copy for performing updates. */
    public IDMapTrie(IDMapTrie bst) {
        super(bst);
    }

    @Override
    protected IDTrie createInstance(int size) {
        return new IDMapTrie(size);
    }

    /**
     * Contains operation.
     * @return mapped value or defaultValue if not found 
     */
    @Override
    final public long get(long key, long defaultValue) {
        long[] mem = this.mem; // local reference for performance
        long childValue = root;

        // inner nodes
        for (int i = 10; i >= 0; i--) {
            int nodeIdx = (int) childValue;
            long bitPos = 1L << (key >>> (i * 6));
            long bitMap = mem[nodeIdx];
            if ((bitMap & bitPos) == 0) 
                return defaultValue;
            int childIdx = Long.bitCount(bitMap & (bitPos - 1));
            childValue = mem[nodeIdx + 1 + childIdx];
        }

        return childValue;
    }

    /**
     * Add key and value.
     */
    @Override
    final public void put(long key, long value) {
        put(key, value, -1);
    }

    /**
     * Add key and value.
     * @return old value or defaultValue if set didn't contain the key
     */
    @Override
    final public long put(long key, long value, long defaultValue) {

        retry: for (;;) {

            // first step: search for key (as in get) but note childPtr walking down trie, exit if found

            Arrays.fill(nodeIdxs, 0);

            long childValue = root;
            long oldValue = defaultValue;
            boolean newKey = false;

            // inner nodes
            for (int i = 10; i >= 0; i--) {
                nodeIdxs[i] = childValue;
                int nodeIdx = (int) childValue;
                long bitPos = 1L << (key >>> (i * 6));
                long bitMap = mem[nodeIdx];
                if ((bitMap & bitPos) == 0) {
                    newKey = true; // doesn't contain key, remember to increment count on exit
                    break;
                }
                int childIdx = Long.bitCount(bitMap & (bitPos - 1));
                childValue = mem[nodeIdx + 1 + childIdx];

                // already set
                if (i == 0) {
                    oldValue = childValue;
                    if (oldValue == value)
                        return oldValue;
                }
            }

            // update value
            childValue = value;

            // second step: since not found, update or add childValue walking up trie again

            for (int i = 0; i <= 10; i++) {
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
                    newNodeIdx = allocate(size + 2, i == 0); 
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
                        if (newKey)
                            count++;
                        return oldValue;
                    }

                    // copy and update childValue
                    // sizeof(bitMap) + oldSize
                    // compact only if leaf (otherwise deallocate has been called already and we cannot change node indexes any longer)
                    newNodeIdx = allocate(size + 1, i == 0); 
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

            if (newKey)
                count++;
            return oldValue;

        }
    }

    /**
     * Remove key.
     */
    @Override
    final public boolean clear(long key) {
        return -1 != clear(key, -1);
    }

    /**
     * Remove key.
     * @return old value or defaultValue if set didn't contain the key
     */
    @Override
    final public long clear(long key, long defaultValue) {

        retry: for (;;) {

            // first step: search for key (as in get) but note childPtr walking down trie, exit if not found

            Arrays.fill(nodeIdxs, 0);

            long childValue = root;

            // inner nodes
            for (int i = 10; i >= 0; i--) {
                nodeIdxs[i] = childValue;
                int nodeIdx = (int) childValue;
                long bitPos = 1L << (key >>> (i * 6));
                long bitMap = mem[nodeIdx];
                if ((bitMap & bitPos) == 0) 
                    return defaultValue; // doesn't contain key
                int childIdx = Long.bitCount(bitMap & (bitPos - 1));
                childValue = mem[nodeIdx + 1 + childIdx];
            }

            long oldValue = childValue;

            // second step: since found, delete walking up trie again, either delete child in list or remove whole node

            // flag to indicate empty node
            boolean remove = true;

            // update childValue in parent node or remove child if empty
            for (int i = 0; i <= 10; i++) {
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
                        newNodeIdx = allocate(size, i == 0); 
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
                        return oldValue; // did contain the key
                    }

                    // copy and update childValue
                    // sizeof(bitMap) + oldSize
                    // compact only if leaf (otherwise deallocate has been called already and we cannot change node indexes any longer)
                    newNodeIdx = allocate(size + 1, i == 0);
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
            return oldValue; // did contain the key
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
        for (int i = 10; i >= 0; i--) {
            int nodeIdx = (int) childValue;
            long bitMap = mem[nodeIdx];
            int first = Long.numberOfTrailingZeros(bitMap);
            value = (value << 6) | first;
            childValue = mem[nodeIdx + 1]; // take first child
        }

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
        for (int i = 10; i >= 0; i--) {
            int nodeIdx = (int) childValue;
            long bitMap = mem[nodeIdx];
            int last = 63 - Long.numberOfLeadingZeros(bitMap);
            value = (value << 6) | last;
            long bitPos = 1L << last;
            int childIdx = Long.bitCount(bitMap & (bitPos - 1));                    
            childValue = mem[nodeIdx + 1 + childIdx]; // take last child
        }

        return value;
    }

    /** Helper for compact. */
    @Override
    protected int copy(int level, int nodeIdx, IDTrie bst) {
        return copy(level, 0, nodeIdx, bst);
    }

    @Override
    protected IDTrieCursor cursor() {
        return new IDMapTrieCursor(this);
    }

    /**
     * Cursor to iterate over keys in set.
     */
    public static class IDMapTrieCursor implements IDTrieCursor {

        long endKey;
        IDMapTrie bst;
        long[] nodeIdxs = new long[10+1];
        long key;
        long value;

        /** 
         * Constructor with -1 as end of list key.
         * Does <code>IDMapTrieCursor(bst, -1)</code>.
         */
        IDMapTrieCursor(IDMapTrie bst) {
            this(bst, -1);
        }

        /** Constructor. */
        IDMapTrieCursor(IDMapTrie bst, long endKey) {
            this.bst = bst;
            this.endKey = endKey;
        }
        
        /** Returns current value of current key. */
        @Override
        public long getValue() {
            return value;
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
            for (int i = 10; i >= 0; i--) {
                nodeIdxs[i] = childValue;
                int nodeIdx = (int) childValue;
                long bitMap = bst.mem[nodeIdx];
                int first = Long.numberOfTrailingZeros(bitMap);
                key = (key << 6) | first;
                childValue = bst.mem[nodeIdx + 1]; // take first child
            }
            value = childValue;
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
                int nodeIdx = (int) childValue;
                int group = (int) (targetKey >>> (i * 6)) & 0x3F;
                long bitPos = 1L << group;
                long bitMap = mem[nodeIdx];
                if ((bitMap & bitPos) != 0) {
                    // match
                    key = (key << 6) | group;
                    int childIdx = Long.bitCount(bitMap & (bitPos - 1));
                    if (i == 0) {
                        value = mem[nodeIdx + 1 + childIdx];
                        return key;
                    }
                    else {
                        i--;
                        nodeIdxs[i] = mem[nodeIdx + 1 + childIdx];
                    }
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
                        if (i == 0) {
                            value = mem[nodeIdx + 1 + childIdx];
                            return key;
                        }
                        i--;
                        nodeIdxs[i] = mem[nodeIdx + 1 + childIdx];
                        targetKey = 0; // we are already greater, take first of remaining groups
                    }
                }
            }

        }

        /** Returns next key. */
        @Override
        public long next() {
            if (key == endKey)
                return key;

            long[] mem = bst.mem; // local reference for performance

            // get next in inner nodes
            for (int i = 0; i <= 10; i++) {
                long parentValue = nodeIdxs[i];
                int nodeIdx = (int) parentValue;
                long bitMap = mem[nodeIdx];
                int group = (int) key & 0x3F;
                if (group < 0x3F) {
                    long bitMask = 0xFFFFFFFFFFFFFFFEL << group; // 0x..FF << (group + 1)
                    long next = Long.numberOfTrailingZeros(bitMap & bitMask);
                    if (next < 64) {
                        key = (key & ~0x3FL) | next;
                        int childIdx = Long.bitCount(bitMap & ~bitMask);
                        long childValue = mem[nodeIdx + 1 + childIdx];

                        // starting at next level, find first
                        for (int j = i-1; j >= 0; j--) {
                            nodeIdxs[j] = childValue;
                            nodeIdx = (int) childValue;
                            bitMap = mem[nodeIdx];
                            long first = Long.numberOfTrailingZeros(bitMap);
                            key = (key << 6) | first;
                            childValue = mem[nodeIdx + 1]; // take first child
                        }
                        value = childValue;
                        return key;
                    }
                }
                key = (key >>> 6);
            }
            key = endKey;
            return key;
        }
    }

    /** Collections implementation. uses a live view of the Map. */
    public Map<Long, Long> getMap() {
        return new Map<>() {
            @Override
            public int size() {
                return IDMapTrie.this.size();
            }

            @Override
            public boolean isEmpty() {
                return IDMapTrie.this.size() > 0;
            }

            @Override
            public boolean containsKey(Object key) {
                long l = IDMapTrie.this.get((Long) key, -1L);
                return l != -1;
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long get(Object key) {
                long l = IDMapTrie.this.get((Long) key, -1L);
                return l == -1 ? null : l;
            }

            @Override
            public Long put(Long key, Long value) {
                long l = IDMapTrie.this.put(key, value, -1L);
                return l == -1 ? null : l;
            }

            @Override
            public Long remove(Object key) {
                long l = IDMapTrie.this.clear((Long) key, -1L);
                return l == -1 ? null : l;
            }

            @Override
            public void putAll(Map<? extends Long, ? extends Long> m) {
                for (Map.Entry<? extends Long, ? extends Long> e : m.entrySet())
                    put(e.getKey(), e.getValue());
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Long> keySet() {
                return new Set<>() {
                    @Override
                    public int size() {
                        return IDMapTrie.this.size();
                    }

                    @Override
                    public boolean isEmpty() {
                        return IDMapTrie.this.size() > 0;
                    }

                    @Override
                    public boolean contains(Object key) {
                        long l = IDMapTrie.this.get((Long) key, -1L);
                        return l != -1;
                    }

                    @Override
                    public Iterator<Long> iterator() {
                        return IDMapTrie.this.getKeys().iterator();
                    }

                    @Override
                    public Object[] toArray() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public <T> T[] toArray(T[] a) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean add(Long aLong) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean remove(Object o) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean containsAll(Collection<?> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean addAll(Collection<? extends Long> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean retainAll(Collection<?> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean removeAll(Collection<?> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void clear() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public Collection<Long> values() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Entry<Long, Long>> entrySet() {
                return new Set<>() {
                    @Override
                    public int size() {
                        return IDMapTrie.this.size();
                    }

                    @Override
                    public boolean isEmpty() {
                        return IDMapTrie.this.size() > 0;
                    }

                    @Override
                    public boolean contains(Object o) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Iterator<Entry<Long, Long>> iterator() {
                        return new Iterator<>() {

                            IDTrie.IDTrieCursor tcursor = IDMapTrie.this.cursor();
                            long next = tcursor.first();

                            @Override
                            public boolean hasNext() {
                                return next != -1;
                            }

                            @Override
                            public Entry<Long, Long> next() {
                                Map.Entry<Long, Long> e = new Map.Entry<>() {
                                    long k = next;
                                    long v = tcursor.getValue();

                                    @Override
                                    public Long getKey() {
                                        return k;
                                    }

                                    @Override
                                    public Long getValue() {
                                        return v;
                                    }

                                    @Override
                                    public Long setValue(Long value) {
                                        throw new UnsupportedOperationException();
                                    }
                                };
                                next = tcursor.next();
                                return e;
                            }

                            @Override
                            public void remove() {
                            }
                        };
                    }

                    @Override
                    public Object[] toArray() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public <T> T[] toArray(T[] a) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean add(Entry<Long, Long> longLongEntry) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean remove(Object o) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean containsAll(Collection<?> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean addAll(Collection<? extends Entry<Long, Long>> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean retainAll(Collection<?> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean removeAll(Collection<?> c) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void clear() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
