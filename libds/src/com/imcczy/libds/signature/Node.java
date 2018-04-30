package com.imcczy.libds.signature;

import com.imcczy.libds.utils.Utils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by imcczy on 2017/5/15.
 */
public class Node implements Serializable {

    private static final long serialVersionUID = -1923676267200797695L;
    public byte[] hash;
    public List<Node> childs;
    public boolean matched = false;
    public boolean checked = false;
    public List<ByteBuffer> candilist = new ArrayList<>();

    public Node(byte[] hash) {
        this.hash = hash;
        this.childs = new ArrayList<Node>();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node))
            return false;
        return Hash.equals(((Node) obj).hash, this.hash);
    }

    @Override
    public int hashCode() {
        int result = 1;
        //return hash.hashCode() + childs.size();
        for (byte b : hash) {
            result = 31 * result + b;
        }
        return result + childs.size();
    }

    @Override
    public String toString() {
        return Hash.hash2Str(this.hash);
    }

    public PackageNode toPNode() {
        return (PackageNode) this;
    }
    public ClassNode toCNode() {
        return (ClassNode) this;
    }
    public MethodNode toMNode() {
        return (MethodNode) this;
    }
    public int numberOfChilds() {
        return this.childs.size();
    }

    public void debug() {
    }

    public String getStats() {
        StringBuilder sb = new StringBuilder();
        int pNodes = 0;
        int cNodes = 0;
        int mNodes = 0;

        LinkedList<Node> worklist = new LinkedList<Node>();
        worklist.add(this);
        Node curNode;

        while (!worklist.isEmpty()) {
            curNode = worklist.poll();
            worklist.addAll(curNode.childs);

            for (Node n : curNode.childs) {
                if (n instanceof PackageNode)
                    pNodes++;
                else if (n instanceof ClassNode)
                    cNodes++;
                else if (n instanceof MethodNode)
                    mNodes++;
            }
        }

        sb.append("Node stats:\n");
        sb.append(Utils.INDENT + "- contains " + mNodes + " method hashes.\n");
        sb.append(Utils.INDENT + "- contains " + cNodes + " clazz hashes.\n");
        sb.append(Utils.INDENT + "- contains " + pNodes + " package hashes.");

        return sb.toString();
    }

    public boolean isLeaf() {
        return childs.isEmpty();
    }
}
