package com.imcczy.libds.signature;

import com.imcczy.libds.utils.Utils;

import java.io.Serializable;
import java.util.List;

/**
 * Created by imcczy on 2017/5/15.
 */
public class ClassNode extends Node implements Serializable {
    private static final long serialVersionUID = 9069055120119162727L;
    public String clazzName;
    public String sourceFileName;
    public int accessFlags;

    public ClassNode(byte[] hash, String clazzName, int flag,String sourceFileName) {
        super(hash);
        this.clazzName = clazzName;
        this.accessFlags = flag;
        this.sourceFileName = sourceFileName;
    }

    public List<Node> getMethodNodes() {
        return this.childs;
    }

    @Override
    public void debug() {
        //logger.info("Debug ClassNode: " + clazzName + "  (childs: " + childs.size() + ",  "  + Hash.hash2Human(hash) + ")");
        for (Node n : this.childs) {
            MethodNode mn = (MethodNode) n;
            //logger.info(Utils.INDENT2 + "- " + mn.signature + "  ::  " + Hash.hash2Str(mn.hash));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClassNode))
            return false;

        return Hash.equals(((ClassNode) obj).hash, this.hash);
    }

    @Override
    public String toString() {
        return "CNode(" + clazzName + ")";
    }
}
