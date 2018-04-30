package com.imcczy.libds.stats;

import com.imcczy.libds.signature.Hash;
import com.imcczy.libds.signature.Node;
import com.imcczy.libds.signature.PackageNode;

import java.io.Serializable;
import java.util.*;

/**
 * Created by imcczy on 2017/11/20.
 */

public class Libstats implements Serializable{
    private static final long serialVersionUID = -5582977226821156643L;
    private Node node;

    public int classCount;
    public Libstats(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node))
            return false;
        return Hash.equals(((Node) obj).hash, this.node.hash);
    }

    @Override
    public int hashCode() {
        return this.node.hash.hashCode() + this.node.childs.size();
    }

    @Override
    public String toString() {
        return Hash.hash2Str(this.node.hash);
    }

    public TreeSet<String> getTree(){
        TreeSet<String> tree = new TreeSet<>();
        for (Node n:node.childs){
            tree.add(((PackageNode) n).packageName);
        }
        return tree;
    }

    public HashMap<String,Node> getNodeMap(){
        HashMap<String,Node> map = new HashMap<>();
        if (node.childs == null)
            return null;
        for (Node n:node.childs){
            PackageNode tmp = (PackageNode) n;
            map.put(tmp.packageName,n);
        }
        return map;
    }
    public HashSet<Node> getClassNodeSet(){
        HashSet<Node> nodes = new HashSet<>();
        for (Node n:node.childs){
            for (Node c:n.childs){
                classCount++;
                nodes.add(c);
            }
        }
        return nodes;
    }

    public List<Node> getMethodNodeSet(){
        List<Node> nodes = new ArrayList<>();
        for (Node n:node.childs){
            for (Node c:n.childs){
                for (Node m:c.childs)
                    nodes.add(m);
            }
        }
        return nodes;
    }
    public String getName(){
        TreeSet<String> tree = new TreeSet<>();
        for (Node n:node.childs){
            tree.add(((PackageNode) n).packageName);
        }
        return tree.first();
    }
}
