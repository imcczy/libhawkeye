package com.imcczy.libds.pkg;

import com.imcczy.libds.signature.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by imcczy on 2017/3/23.
 */
public class NodeHashTree implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.pkg.NodeHashTree.class);
    private static final long serialVersionUID = 3033625761110675664L;
    private Node rootNode;

    public class Node implements Serializable {
        private static final long serialVersionUID = 3586632688610205125L;
        private String name;
        private String namePath;
        private List<Node> childs;
        private byte[] hash;
        private byte[] nodeHash;
        private Node parent;

        public Node getParent() {
            return this.parent;
        }

        public String getName() {
            return this.name;
        }

        public Collection<Node> getChilds() {
            return childs;
        }

        public Node(String name, Node node) {
            this.name = name;
            this.childs = new ArrayList<Node>();
            this.hash = null;
            this.nodeHash = null;
            this.parent = node;
            this.namePath = getNamePath(getNamePath(this));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node))
                return false;
            return ((Node) obj).name.equals(this.name);
        }

        public void print() {
            print("", true);
        }

        private void print(String prefix, boolean isTail) {
            logger.info(prefix + (isTail ? "└── " : "├── ") + name + " (" + Hash.hash2Str(nodeHash) + ")");

            for (int i = 0; i < childs.size(); i++) {
                childs.get(i).print(prefix + (isTail ? "    " : "│   "), i == childs.size() - 1);
            }
        }

        public boolean hasClasses() {
            return !(hash == null);
        }

        public boolean isLeaf() {
            return childs.isEmpty();
        }
    }

    public TreeSet<String> getSubTreeRoot() {
        return getSubTreeRoot(rootNode);
    }

    public TreeSet<String> getSubTreeRoot(Node n) {
        TreeSet<String> res = new TreeSet<>();

        if (!(n.parent == null) && n.hasClasses() && !n.parent.hasClasses())
            res.add(n.namePath);
        if (!n.isLeaf()) {
            for (Node c : n.childs) {
                res.addAll(getSubTreeRoot(c));
            }
        }
        return res;
    }

    public void print() {

        logger.info("Root Package: " + rootNode.name);
        //rootNode.print();
    }

    private String getNamePath(String name) {
        if (name.length() >= 1)
            return name.substring(0, name.length() - 1);
        else
            return name;
    }

    private String getNamePath(Node node) {
        if (node.parent == null)
            return "";
        else {
            return getNamePath(node.getParent()) + (node.getName()) + (".");
        }
    }

    private NodeHashTree() {
        this.rootNode = new Node("root", null);
    }

    public static NodeHashTree make(HashTree hashTree) throws NoSuchAlgorithmException {
        NodeHashTree nodehashTree = new NodeHashTree();
        Collection<com.imcczy.libds.signature.Node> packageNodes = hashTree.getPackageNodes();
        for (com.imcczy.libds.signature.Node node : packageNodes) {
            com.imcczy.libds.signature.PackageNode pNode = (com.imcczy.libds.signature.PackageNode) node;
            nodehashTree.update(pNode.packageName, pNode.hash);
        }

        //nodehashTree.updateHashTree();
        //nodehashTree.updateCallList();
        return nodehashTree;
    }

    private void updateHashTree() {
        calHash(rootNode);
    }
    /*
    private void updateCallList(){
        updateCall(rootNode);
    }

    private void updateCall(Node node){
        if (node.childs.size() ==0)
            return;
        node.childs.forEach(n -> updateCall(n));
        for (Node c: node.childs){
            node.callist.addAll(c.callist);
        }
    }
    */

    private void calHash(Node node) {
        try {
            node.childs.forEach(n -> calHash(n));
            calNodeHash(node);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    private void calNodeHash(Node node) throws NoSuchAlgorithmException {
        if (node.childs.size() == 0 && node.hash != null) {
            node.nodeHash = node.hash;
            return;
        }

        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl(HashAlgorithm.MD5.toString());
        node.nodeHash = hash(node.childs.stream().sorted(comp).collect(Collectors.toList()), hashFunc);
        if (node.hash != null) {
            node.nodeHash = hash(node, hashFunc);
        }
    }

    public class NodeComparator implements Comparator<Node> {
        private Hash.ByteArrayComparator comp;

        public NodeComparator() throws NoSuchAlgorithmException {
            IHash hashFunc = new HashImpl(HashAlgorithm.MD5.toString());
            comp = ((Hash) hashFunc).new ByteArrayComparator();
        }

        @Override
        public int compare(Node n0, Node n1) {
            return comp.compare(n0.nodeHash, n1.nodeHash);
        }
    }


    public enum HashAlgorithm {
        MD5("MD5"), SHA1("SHA-1"), SHA256("SHA-256");

        private String value;

        HashAlgorithm(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    ;

    /**
     * Generic hash function that takes a list of hashes, concatenates and hashes them
     *
     * @param nodes    a collection of input hashes
     * @param hashFunc a hash function
     * @return a hash
     */
    public static byte[] hash(Collection<Node> nodes, final IHash hashFunc) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            for (Node node : nodes)
                outputStream.write(node.nodeHash);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] arr = outputStream.toByteArray();
        return hashFunc.hash(arr);
    }

    public static byte[] hash(Node node, final IHash hashFunc) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(node.hash);
            outputStream.write(node.nodeHash);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] arr = outputStream.toByteArray();
        return hashFunc.hash(arr);
    }

    private boolean update(String packageName, byte[] hash) {
        Node curNode = rootNode;
        List<String> packageStruct = PackageUtils.parsePackage(packageName, false);
        for (int i = 0; i < packageStruct.size(); i++) {
            Node n = matchChilds(curNode, packageStruct.get(i));

            if (n != null) {
                curNode = n;
                if (curNode.namePath.equals(packageName))
                    curNode.hash = hash;
            } else {
                Node newNode = new Node(packageStruct.get(i), curNode);
                if (newNode.namePath.equals(packageName)) {
                    newNode.hash = hash;
                }
                curNode.childs.add(newNode);
                curNode = newNode;
            }

        }


        return true;
    }

    private Node matchChilds(Node n, String str) {
        for (Node node : n.childs) {
            if (node.name.equals(str))
                return node;
        }
        return null;
    }
}
