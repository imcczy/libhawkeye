package com.imcczy.libds.signature;

import com.imcczy.libds.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by imcczy on 2017/5/15.
 */
public class PackageNode extends Node implements Serializable {
    private static final long serialVersionUID = 3516425637582519635L;
    public String packageName;

    public PackageNode(byte[] hash, String packageName) {
        super(hash);
        this.packageName = packageName;
    }

    //@Override
    /*
    public void debug() {
        logger.info("Debug PackageNode: " + packageName + " (childs: " + childs.size() + ",  " + Hash.hash2Str(hash) + ")");
        for (Node n: this.childs) {
            HashTree.ClassNode cn = (HashTree.ClassNode) n;
            logger.info(Utils.INDENT + "- " + cn.clazzName + "  ::  " + cn.numberOfChilds() + "  ::  " + Hash.hash2Str(cn.hash));
//				cn.debug();
        }
    }
    */


    public List<Node> getClassNodes() {
        return this.childs;
    }

    public List<Node> getMethodNodes() {
        ArrayList<Node> result = new ArrayList<Node>();
        for (Node n : this.childs) {
            result.addAll(((ClassNode) n).getMethodNodes());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageNode))
            return false;

        return Hash.equals(((PackageNode) obj).hash, this.hash);
    }

    @Override
    public String toString() {
        return "PNode(" + packageName + ")";
    }
}