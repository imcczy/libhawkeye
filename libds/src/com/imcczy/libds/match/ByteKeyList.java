package com.imcczy.libds.match;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by imcczy on 2017/12/4.
 */
public class ByteKeyList extends ArrayList<ByteBuffer> {

    private static final long serialVersionUID = -806540317066631888L;

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

    public ByteKeyList of(List<byte[]> set){
        set.forEach(bytes -> add(bytes));
        return this;
    }
}
