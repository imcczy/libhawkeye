package com.imcczy.libds.match;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by imcczy on 2017/12/4.
 */
public class ByteKeyHashSet extends HashSet<ByteBuffer> {
    private static final long serialVersionUID = -2491167519293300679L;

    public boolean add(byte[] key) {
        return super.add(ByteBuffer.wrap(key));
    }

    public boolean add(String key) {
        return super.add(ByteBuffer.wrap(key.getBytes()));
    }

    public boolean remove(byte[] key) {
        return super.remove(ByteBuffer.wrap(key));
    }

    public boolean remove(String key) {
        return super.remove(ByteBuffer.wrap(key.getBytes()));
    }

    public boolean contains(byte[] key) {
        return super.contains(ByteBuffer.wrap(key));
    }

    public boolean contains(ByteBuffer key){
        return super.contains(key);
    }

    public boolean contains(String key) {
        return super.contains(ByteBuffer.wrap(key.getBytes()));
    }

    public ByteKeyHashSet of(Set<byte[]> set){
        set.forEach(bytes -> add(bytes));
        return this;
    }
}
