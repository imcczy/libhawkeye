package com.imcczy.libds.signature;

import java.io.Serializable;

/**
 * Created by imcczy on 2017/5/15.
 */
public class MethodNode extends Node implements Serializable {
    private static final long serialVersionUID = -4450765162076318885L;
    public String signature;
    public int accessFlags;
    public byte[] simplehash;

    public MethodNode(byte[] hash, byte[] simplehash, String signature, int flag) {
        super(hash);
        this.simplehash = simplehash;
        this.signature = signature;
        this.accessFlags = flag;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodNode))
            return false;

        return Hash.equals(((MethodNode) obj).hash, this.hash);
    }

    @Override
    public String toString() {
        return "MNode(" + signature + ")";
    }
}
