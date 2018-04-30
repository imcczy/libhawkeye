package com.imcczy.libds;

import com.imcczy.libds.graph.ConnectedComponent;
import com.imcczy.libds.signature.*;
import com.imcczy.libds.stats.ApkStats;
import com.imcczy.libds.utils.MutableInterger;
import com.imcczy.libds.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.imcczy.libds.signature.HashTree.hash;

/**
 * Created by imcczy on 2017/6/4.
 */
public class LibHandler {

    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.LibHandler.class);

    private Path libPath;
    private String imcczyh = "imcczyh";
    private ApkStats apkStats = null;
    private ConnectedComponent connectedComponent;
    //map {packag name:pacakge node}
    public HashMap<String, Node> nodeMap = new HashMap<>();

    public LibHandler(Path libPath) {
        this.libPath = libPath;
    }

    public LibHandler(ApkStats apkStats) {
        this.apkStats = apkStats;
    }

    public void init() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

        logger.info(apkStats.pcakageName);

        if (apkStats == null)
            apkStats = (ApkStats) Utils.disk2Object(libPath);
        connectedComponent = new ConnectedComponent(apkStats);
        apkStats.node.childs.forEach(node -> {
            PackageNode p = (PackageNode) node;
            nodeMap.put(p.packageName, node);
            /*
            if (!connectedComponent.connectComponentMap.containsKey(p.packageName))
                System.out.print("sss");
                */
        });
        mergelib();
        // print out lib packgae hierarchy
        //print();
    }

    public void initlog() {

        String logIdentifier = CLI.CmdOptions.logDir.getAbsolutePath() + File.separator;
        logIdentifier += libPath.getFileName().toString().replaceAll("\\.lib", "");
        // set identifier for log
        MDC.put("appPath", logIdentifier);
    }

    //union all packages are not called
    private HashSet<Integer> noneCalleeSet = new HashSet<>();
    //map {union index:union package tree}
    public HashMap<Integer, TreeSet<String>> allLibs = new HashMap<>();
    //
    private HashSet<Integer> singlePackageSet = new HashSet<>();
    private HashMap<Integer, TreeSet<String>> noneCalledLibMap = new HashMap<>();

    private void mergelib() throws NoSuchAlgorithmException {
        mergenone();
        filterSingleSet();
        mergeParent();
        filterNoneCalleeSet();
        mergeSibling();
        allLibs = connectedComponent.ccmap;
        print();
    }

    private void mergenone() {
        Set<String> packageNames = connectedComponent.connectComponentMap.keySet();
        HashSet<String> none = new HashSet<>();
        String parentpackgeName = filter(2, apkStats.pcakageName);
        String mainActivityName = apkStats.mainActivityName != null?filter(2,apkStats.mainActivityName):null;
        for (String name : nodeMap.keySet()) {
            if (name.startsWith(parentpackgeName))
                continue;
            //mainActivity Name is different from package name
            //if (mainActivityName!=null && name.startsWith(mainActivityName))
            //    continue;
            if ("".equals(name))
                continue;
            if (Utils.isTLD(name))
                continue;
            if (!packageNames.contains(name)) {
                //System.out.println("XX-"+name);
                Integer tmp = connectedComponent.connectComponentMap.size();
                connectedComponent.connectComponentMap.put(name,
                        new MutableInterger(tmp));
                connectedComponent.ccmap.put(tmp, new TreeSet<>());
                connectedComponent.ccmap.get(tmp).add(name);
                connectedComponent.calledVertex.remove(name);
            }
        }
    }

    //filter single package set
    private void filterSingleSet() {
        connectedComponent.ccmap.keySet().forEach(integer -> {
            Set<String> strings = connectedComponent.ccmap.get(integer);
            if (strings.size() == 1)
                singlePackageSet.add(integer);
        });
    }

    //filter none callee package set
    private void filterNoneCalleeSet() {
        connectedComponent.ccmap.keySet().forEach(integer -> {
            boolean noneCallee = false;
            TreeSet<String> strings = connectedComponent.ccmap.get(integer);
            for (String s : strings) {
                if (connectedComponent.calledVertex.contains(s)) {
                    noneCallee = true;
                    break;
                }
            }
            if (!noneCallee) {
                noneCalleeSet.add(integer);
                noneCalledLibMap.put(integer, strings);
            }
        });
    }


    //merge package stage 1
    /* i.e.
     *
     * com.a
     * com.a.b
     * com.a.c
     *
     * There is none callee package i.e. com.a.d, merge it with above homogeny compoment
     *
     *
     * com.a.b
     * com.a.c
     * com.a.a
     *
     * merge com.a
     */

    private void mergeParent() {
        Set<String> packageNames = connectedComponent.connectComponentMap.keySet();

        HashSet<String> tmp = new HashSet<>();
        HashMap<String, List<String>> map = new HashMap<>();

        for (Integer integer : singlePackageSet) {
            String name = connectedComponent.ccmap.get(integer).first();
            tmp.add(name);
            map.put(name, new ArrayList<>());
        }

        for (String name : packageNames) {
            if (tmp.contains(name))
                continue;
            String parentName = getParentPName(name);
            if (Utils.isTLD(parentName))
                continue;
            if (tmp.contains(parentName)) {
                map.get(parentName).add(name);
            }

        }
        for (String name : map.keySet()) {
            if (map.get(name).size() == 0) {
                continue;
            }
            Integer i = new Integer(-1);
            int size = 0;
            for (String son : map.get(name)) {
                Integer tmpi = connectedComponent.connectComponentMap.get(son).get();
                if (!connectedComponent.ccmap.containsKey(tmpi))
                    continue;
                int tempsize = connectedComponent.ccmap.get(tmpi).size();
                if (tempsize > size) {
                    i = tmpi;
                    size = tempsize;
                }
            }
            if (i == -1)
                continue;
            connectedComponent.ccmap.get(i).add(name);
            Integer tmps = connectedComponent.connectComponentMap.get(name).get();
            connectedComponent.connectComponentMap.put(name, new MutableInterger(i));
            connectedComponent.ccmap.remove(tmps);
            singlePackageSet.remove(tmps);
        }

        Iterator<Integer> iterator = singlePackageSet.iterator();
        while (iterator.hasNext()) {
            Integer index = iterator.next();
            TreeSet<String> name = connectedComponent.ccmap.get(index);
            String parentName = getParentPName(name.first());
            if (Utils.isTLD(parentName) || imcczyh.equals(parentName))
                continue;
            if (packageNames.contains(parentName)) {

                Integer i = connectedComponent.connectComponentMap.get(parentName).get();
                if (connectedComponent.ccmap.containsKey(i)) {
                    connectedComponent.ccmap.remove(index);
                    connectedComponent.ccmap.get(i).addAll(name);
                    connectedComponent.connectComponentMap.put(name.first(), new MutableInterger(i));
                    iterator.remove();
                }
                continue;
            }
            String grandParentName = getParentPName(parentName);
            if (Utils.isTLD(grandParentName) || imcczyh.equals(grandParentName))
                continue;
            if (packageNames.contains(grandParentName)) {

                Integer i = connectedComponent.connectComponentMap.get(grandParentName).get();
                if (connectedComponent.ccmap.containsKey(i)) {
                    connectedComponent.ccmap.remove(index);
                    iterator.remove();
                    connectedComponent.ccmap.get(i).addAll(name);
                    connectedComponent.connectComponentMap.put(name.first(), new MutableInterger(i));
                }
                continue;
            }
            String greatParentName = getParentPName(grandParentName);
            if (Utils.isTLD(greatParentName) || imcczyh.equals(greatParentName))
                continue;
            if (packageNames.contains(greatParentName)) {

                Integer i = connectedComponent.connectComponentMap.get(greatParentName).get();
                if (connectedComponent.ccmap.containsKey(i)) {
                    iterator.remove();
                    connectedComponent.ccmap.remove(index);
                    connectedComponent.ccmap.get(i).addAll(name);
                    connectedComponent.connectComponentMap.put(name.first(), new MutableInterger(i));
                }
            }
            //parnet's great parent

        }

        logger.debug("step 1 done");
    }

    @Deprecated
    private void stage1() {
        noneCalleeSet.forEach(integer -> {
            TreeSet<String> key = connectedComponent.ccmap.get(integer);
            connectedComponent.ccmap.remove(integer);
            if (key.size() == 1) {
                String parentPackage = getParentPName(key.first());
                if (connectedComponent.connectComponentMap.containsKey(parentPackage)) {
                    int i = connectedComponent.connectComponentMap.get(parentPackage).get();
                    if (connectedComponent.ccmap.containsKey(i))
                        connectedComponent.ccmap.get(i).addAll(key);
                    else if (noneCalledLibMap.containsKey(i))
                        noneCalledLibMap.get(i).addAll(key);
                    else
                        System.out.print("woccccc");

                } else {
                    noneCalledLibMap.put(integer, key);
                }
            } else
                //there more than one package in the union
                noneCalledLibMap.put(integer, key);
        });
    }

    // merge lib stage 2
    /*
     * com.a.a.a
     * com.a.b.c
     * com.a.b.d
     * com.a.b.e
     *
     * merge com.a.b.h
     */


    private void mergeSibling() {
        Iterator<Integer> it = noneCalledLibMap.keySet().iterator();
        Set<String> packageNames = connectedComponent.connectComponentMap.keySet();
        while (it.hasNext()) {
            Integer self = it.next();
            TreeSet<String> none = noneCalledLibMap.get(self);
            String packageName = none.first();
            String parentName = getParentPName(packageName);
            if (Utils.isTLD(parentName) || imcczyh.equals(parentName))
                continue;
            if (packageNames.contains(parentName)) {

                Integer i = connectedComponent.connectComponentMap.get(parentName).get();
                if (connectedComponent.ccmap.containsKey(i)) {
                    connectedComponent.ccmap.remove(self);
                    connectedComponent.ccmap.get(i).addAll(none);
                    it.remove();
                    connectedComponent.connectComponentMap.put(none.first(), new MutableInterger(i));

                }
            }
        }
        it = noneCalledLibMap.keySet().iterator();
        while (it.hasNext()) {
            HashSet<Integer> integers = new HashSet<>();
            Integer self = it.next();
            //if (!singlePackageSet.contains(self))
            //continue;
            TreeSet<String> none = noneCalledLibMap.get(self);
            String packageName = none.first();
            if (!singlePackageSet.contains(self) && packageName.split("\\.").length <= 3 && isComm(none))
                continue;
            String parentName = getParentPName(packageName);
            String grandParentname = getParentPName(parentName);
            if (Utils.isTLD(parentName))
                continue;
            for (String s : packageNames) {
                if (s.equals(packageName))
                    continue;
                if (s.startsWith(parentName + ".") && diff(s, parentName)) {
                    integers.add(connectedComponent.connectComponentMap.get(s).get());
                }
            }

            if (!Utils.isTLD(grandParentname)) {

                for (String s : packageNames) {
                    if (s.equals(packageName))
                        continue;
                    if (s.startsWith(grandParentname + ".") && diff(s, grandParentname)) {
                        integers.add(connectedComponent.connectComponentMap.get(s).get());
                    }
                }
            }

            Integer i = new Integer(-1);
            int size = 0;
            for (Integer integer : integers) {
                if (connectedComponent.ccmap.containsKey(integer)) {
                    int tmp = connectedComponent.ccmap.get(integer).size();
                    if (tmp > size) {
                        size = tmp;
                        i = integer;
                    }
                }
            }
            if (i == -1 || i.equals(self)) {
                continue;
            }

            if (connectedComponent.ccmap.containsKey(i)) {
                TreeSet<String> name = connectedComponent.ccmap.get(i);
                if (!noneCalleeSet.contains(i) && isComm(name))
                    continue;
                connectedComponent.ccmap.get(i).addAll(none);
                it.remove();
                connectedComponent.ccmap.remove(self);
                singlePackageSet.remove(self);
                connectedComponent.connectComponentMap.put(packageName, new MutableInterger(i));
            }
        }

        logger.debug("step 2 done");
    }

    @Deprecated
    private void mergeOthers() {
        HashMap<String, List<Integer>> map = new HashMap<>();
        for (Integer integer : connectedComponent.ccmap.keySet()) {
            String tmp = filter4(connectedComponent.ccmap.get(integer).first());
            if (tmp == null)
                continue;
            if (map.containsKey(tmp)) {
                map.get(tmp).add(integer);
            } else {
                map.put(tmp, new ArrayList<>());
                map.get(tmp).add(integer);
            }
        }

        for (String str : map.keySet()) {
            List<Integer> list = map.get(str);
            int len = list.size();
            if (len <= 1)
                continue;
            for (int i = 1; i < len; i++) {
                connectedComponent.ccmap.get(list.get(0)).addAll(connectedComponent.ccmap.get(list.get(i)));
                connectedComponent.ccmap.remove(list.get(i));
            }
        }
    }


    /*
     * calc lib hash and its tree hash
     */
    public HashMap<byte[], String> calc() throws NoSuchAlgorithmException {
        HashMap<byte[], String> lib = new HashMap<>();
        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl("MD5");
        for (Integer integer : allLibs.keySet()) {
            ArrayList<Node> list = new ArrayList<>();
            TreeSet<String> set = allLibs.get(integer);

            for (String name : set) {
                Node node = nodeMap.get(name);
                //some package have no hash
                if (node != null)
                    list.add(node);
            }
            Collections.sort(list, comp);
            //byte[] treeHash = hashFunc.hash(set.toString());
            byte[] packageHash = hash(list, hashFunc);
            lib.put(packageHash, set.toString());
        }


        return lib;
    }

    public Map<byte[],byte[]> calc2() throws NoSuchAlgorithmException {
        Map<byte[],byte[]> lib = new HashMap<>();

        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl("MD5");
        Set<String> sourceNameTree = new TreeSet<>();
        for (Integer integer : allLibs.keySet()) {
            ArrayList<Node> list = new ArrayList<>();
            TreeSet<String> set = allLibs.get(integer);

            for (String name : set) {
                Node node = nodeMap.get(name);
                //some package have no hash
                if (node != null){
                    list.add(node);
                    node.childs.forEach(c->{
                        sourceNameTree.add(c.toCNode().sourceFileName!=null?c.toCNode().sourceFileName:"imcczy");
                    });
                }
            }
            Collections.sort(list, comp);
            //byte[] treeHash = hashFunc.hash(set.toString());
            byte[] packageHash = hash(list, hashFunc);
            lib.put(packageHash,hashnew(packageHash,sourceNameTree.toString(),hashFunc));
        }

        return lib;
    }

    private boolean diff(String str1, String str2) {
        int diffl = str1.split("\\.").length - str2.split("\\.").length;
        return (diffl > -2) && (diffl < 2);

    }

    private boolean isComm(TreeSet<String> treeSet) {
        if (treeSet.last().startsWith(treeSet.first() + "."))
            return true;
        return false;
    }

    private void print() {
        connectedComponent.ccmap.keySet().forEach(integer -> {
            logger.info("Number of lib" + integer.toString());
            connectedComponent.ccmap.get(integer).forEach(s -> {
                logger.info("\t\t" + s);
            });
            logger.info("\n");
        });
        /*
        logger.info("------------------------------");
        noneCalledLibMap.keySet().forEach(integer -> {
            logger.info("None Called" + "Number of lib" + integer.toString());
            noneCalledLibMap.get(integer).forEach(s -> {
                logger.info("\t\t" + s);
            });
            logger.info("\n");
        });
        */
    }

    private String getParentPName(String name) {
        if (name.contains("."))
            return name.substring(0, name.lastIndexOf("."));
        return imcczyh;
    }

    public class NodeComparator implements Comparator<Node> {
        private Hash.ByteArrayComparator comp;

        public NodeComparator() throws NoSuchAlgorithmException {
            IHash hashFunc = new HashImpl("MD5");
            comp = ((Hash) hashFunc).new ByteArrayComparator();
        }

        @Override
        public int compare(Node n0, Node n1) {
            return comp.compare(n0.hash, n1.hash);
        }
    }

    private String filter(int level, String string) {
        String[] names = string.split("\\.");
        if (names.length <= 2)
            return string;
        else {
            return names[0] + "." + names[1];
        }
    }

    private String filter4(String string) {
        String[] names = string.split("\\.");
        if (names.length < 4)
            return null;
        else if (names.length == 4)
            return string;
        else {
            return names[0] + "." + names[1];
        }
    }
    private byte[] hashnew(byte[] hash,String source,final IHash hashFunc) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(hash);
            outputStream.write(hashFunc.hash(source));
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] arr = outputStream.toByteArray();
        return hashFunc.hash(arr);
    }
}
