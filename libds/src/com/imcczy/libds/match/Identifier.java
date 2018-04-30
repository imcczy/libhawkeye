package com.imcczy.libds.match;

import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.classLoader.DexIClass;
import com.ibm.wala.dalvik.classLoader.DexModuleEntry;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.imcczy.libds.graph.ClassCC;
import com.imcczy.libds.reﬁnement.Reﬁnement;
import com.imcczy.libds.CLI;
import com.imcczy.libds.LibHandler;
import com.imcczy.libds.profile.AppProfile;
import com.imcczy.libds.signature.*;
import com.imcczy.libds.stats.ApkStats;
import com.imcczy.libds.stats.Libstats;
import com.imcczy.libds.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import redis.clients.jedis.Jedis;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.imcczy.libds.signature.HashTree.hash;

/**
 * Created by imcczy on 2017/11/25.
 */
public class Identifier {
    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.match.Identifier.class);

    private Path apkFile;
    private ApkStats apkStats;
    private AppProfile appProfile;
    private Jedis sigjedis;
    private Jedis canjedis;
    //{PACKAGE NAME: package node}
    private HashMap<String, Node> nodeMap = new HashMap<>();
    private LibHandler libHandler;
    private EntryComparator comp = new EntryComparator();
    private Set<Node> classNodeSet = new HashSet<>();
    private Set<Node> pkgNodeSet = new HashSet<>();
    private HashSet<String> foundLib = new HashSet<>();
    HashSet<Node> none = new HashSet<>();
    ClassCC classCC;

    public Identifier(Path apk, IClassHierarchy cha) {
        this.apkFile = apk;
        this.canjedis = new Jedis("localhost", 16379);//indexed db
        this.sigjedis = new Jedis("localhost", 6379);//sig db
        creatAppProfile(cha);
        initlog();
    }

    private void creatAppProfile(IClassHierarchy cha) {

        try {
            apkStats = new ApkStats(apkFile);
            appProfile = AppProfile.create(DexFileModule.make((apkFile))
                    .getEntrysCollection()
                    .stream()
                    .map(moduleEntry -> new DexIClass(ClassLoaderReference.Application, (DexModuleEntry) moduleEntry))
                    .collect(Collectors.toList()), cha.getMap(), cha.getLoader(ClassLoaderReference.Primordial));
            String primary = Utils.filter(2, apkStats.pcakageName);
            appProfile.hashTree.getRootNode().childs.forEach(node -> {
                PackageNode packageNode = (PackageNode) node;
                if (!packageNode.packageName.startsWith(primary)) {
                    nodeMap.put(packageNode.packageName, node);
                    node.childs.forEach(c -> classNodeSet.add(c));
                    pkgNodeSet.add(node);
                }
                if (nodeMap.containsKey(Utils.googleinternal))
                    nodeMap.get(Utils.googleinternal).matched = true;
            });
            apkStats.dependenceTreeMap = appProfile.hashTree.dependence;
            apkStats.node = appProfile.hashTree.getRootNode();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void match() throws Exception {
        long start = System.nanoTime();
        // recovering third party libs
        libHandler = new LibHandler(apkStats);
        libHandler.init();
        logger.info("analise end in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        matchUnions();
        logger.info("-------------------------");
        //matchClass();
        logger.info("-------------------------");
        //matchMethod();
        logger.info("match end in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    public void matchUnions() throws Exception {
        for (Integer integer : libHandler.allLibs.keySet()) {
            //if (integer!=18)
            //   continue;
            TreeSet<String> tree = libHandler.allLibs.get(integer);
            //"google gms service" are all union together
            if (tree.first().startsWith(Utils.googleand) || tree.first().startsWith(Utils.googleads))
                try {
                    for (Set<String> t : Reﬁnement.splitTrees(tree)) {
                        identify(t);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(tree.toString());
                }

            else identify(tree);
        }
    }


    private void identify(Set<String> tree) throws NoSuchAlgorithmException {
        //if (fullmatched(tree))
        //    return;
        int classnum = 0;
        HashMap<ByteBuffer, Integer> candidates = new HashMap<>();
        for (String pkg : tree) {
            PackageNode pNode = (PackageNode) nodeMap.get(pkg);
            if (pNode == null || pNode.checked)
                continue;
            canjedis.select(0);
            if (canjedis.exists(pNode.hash)) {
                int[] count = {0};
                //num of indexed method
                pNode.childs.forEach(cnode -> count[0] = count[0] + cnode.childs.size());
                pNode.checked = true;
                classnum = classnum + pNode.childs.size();
                canjedis.lrange(pNode.hash, 0, -1).forEach(bytes -> {
                    ByteBuffer libHashBuffer = ByteBuffer.wrap(bytes);
                    pNode.candilist.add(libHashBuffer);
                    if (candidates.containsKey(libHashBuffer))
                        candidates.put(libHashBuffer, count[0] + candidates.get(libHashBuffer));
                    else candidates.put(libHashBuffer, count[0]);
                });
            } else {
                classnum = classnum + pNode.childs.size();
                for (Node c : pNode.childs) {
                    ClassNode classNode = (ClassNode) c;
                    if (classNode.checked)
                        continue;
                    //filter auto-mate classes
                    if ((classNode.accessFlags & 0x200) != 0 || (classNode.accessFlags & 0x400) != 0)
                        continue;
                    //filter inner classes
                    if (classNode.clazzName.contains("$"))
                        continue;

                    //if (c.childs.size() <= 3)
                    //    continue;
                    canjedis.select(1);
                    if (canjedis.exists(c.hash)) {
                        c.checked = true;
                        getCandidata(c, 1, candidates);
                    } else {
                        //filter inner class

                        for (Node m : c.childs) {
                            if (none.contains(m))
                                continue;
                            MethodNode methodNode = (MethodNode) m;
                            getCandidata(m, 3, candidates);
                        }

                    }
                }

            }
        }

        if (candidates.size() == 0)
            return;
        //List<Set<String>> trees = Reﬁnement.splitTrees(tree);
        List<Map.Entry<ByteBuffer, Integer>> list = new ArrayList<>(candidates.entrySet());

        // 排序
        Collections.sort(list, comp);
        //{lib hash : klib name}
        sigjedis.select(3);
        HashMap<ByteBuffer, String> nameMap = new HashMap<>();
        HashMap<String, List<Map.Entry<ByteBuffer, Integer>>> libmap = new HashMap<>();
        getlibName(libmap, list, nameMap);


        //long times = System.nanoTime();

        List<Map.Entry<ByteBuffer, Integer>> finallist = new ArrayList<>();


        boolean skip = false;
        //if (libmap.containsKey(((TreeSet<String>)tree).first()))
        //    finallist = libmap.get(((TreeSet<String>)tree).first());
        //else
        finallist = getfinallist(libmap);
        if (finallist.size() == 0)
            return;
        String name = nameMap.get(finallist.get(0).getKey());
        for (Map.Entry<ByteBuffer, Integer> entry : finallist) {
            if (skip && name.equals(nameMap.get(entry.getKey())))
                continue;
            name = nameMap.get(entry.getKey());
            if (foundLib.contains(name))
                continue;
            //if (!candidates.containsKey(entry.getKey()))
            //   continue;
            sigjedis.select(2);
            List<ByteKeyList> candidtaclasstree = new ArrayList<>();
            HashMap<ByteKeyList, ByteKeyList> simpleHashMap = new HashMap<>();
            sigjedis.lrange(entry.getKey().array(), 0, -1).forEach(bytes -> {
                sigjedis.select(0);
                ByteKeyList byteKeyList = new ByteKeyList().of(sigjedis.lrange(bytes, 0, -1));
                candidtaclasstree.add(byteKeyList);
                sigjedis.select(1);
                simpleHashMap.put(byteKeyList, new ByteKeyList().of(sigjedis.lrange(bytes, 0, -1)));

            });
            int[] num = {0};
            if (simcalc(tree, candidtaclasstree, num, entry.getKey(), simpleHashMap)) {
                foundLib.add(name);
                skip = true;
                if (num[0] == tree.size()) {
                    logger.info("Fully found lib: " + ((TreeSet<String>) tree).first() + "---->" + nameMap.get(entry.getKey()));
                } else
                    logger.info("found lib: " + ((TreeSet<String>) tree).first() + "---->" + nameMap.get(entry.getKey()));

                //这里还是应该以package计算
                if ((num[0] * 1.0 / (tree.size()) >= 0.9))
                    break;
            }
        }


        //System.out.println(String.format("all task took: %d h",
        //        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - times)));
    }


    private boolean fullmatched(Set<String> tree) throws NoSuchAlgorithmException {
        Libstats libstats = new Libstats(new Node(null));
        for (String name : tree) {
            //some package have no hash
            if (nodeMap.get(name) == null)
                continue;
            libstats.getNode().childs.add(nodeMap.get(name));
        }

        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl("MD5");
        if (libstats.getNode().childs == null || libstats.getNode().childs.size() == 0)
            return false;
        Collections.sort(libstats.getNode().childs, comp);
        byte[] packageHash = hash(libstats.getNode().childs, hashFunc);
        sigjedis.select(3);
        if (sigjedis.exists(packageHash)) {
            logger.info("fully found lib:" + new String(sigjedis.get(packageHash)));
            libstats.getNode().childs.forEach(node -> node.matched = true);
            return true;
        }

        return false;
    }

    private boolean simcalc(Set<String> tree, List<ByteKeyList> classes, int[] num, ByteBuffer libhash, HashMap<ByteKeyList, ByteKeyList> simpleHashMap) {
        HashSet<String> checked = new HashSet<>();
        List<Node> matchedClass = new ArrayList<>();
        HashSet<ByteKeyList> usedInLib = new HashSet<>();
        HashMap<ByteBuffer, Set<ByteKeyList>> classmap = getClassMap(classes);
        float mcount = 0;
        int classmum = 0;
        boolean flag = false;
        for (String str : tree) {
            if (nodeMap.get(str) == null)
                continue;
            Node pNode = nodeMap.get(str);
            if (pNode.matched)
                continue;

            if (pNode.candilist.contains(libhash)) {
                mcount = mcount + pNode.childs.size();
                checked.add(str);
                continue;
            }
            classmum = classmum + pNode.childs.size();
            for (Node c : pNode.childs) {
                if (c.matched)
                    continue;
                if (c.candilist.contains(libhash)) {
                    mcount++;
                    matchedClass.add(c);
                    continue;
                }

                //需要改进
                HashMap<ByteKeyList, Integer> candi = getCandi(c, classmap);
                List<Map.Entry<ByteKeyList, Integer>> list1 = new ArrayList<>(candi.entrySet());
                Collections.sort(list1, comp);
                for (Map.Entry<ByteKeyList, Integer> entry : list1) {
                    float count = 0;
                    if (usedInLib.contains(entry.getKey()))
                        continue;
                    if (c.childs.size() > entry.getKey().size())
                        continue;
                    /*
                    for (Node m : c.childs) {
                        if (entry.getKey().contains(m.hash))
                            count++;
                    }*/
                    for (Node m : c.childs) {
                        MethodNode methodNode = (MethodNode) m;
                        if ((methodNode.accessFlags & 0x1000) != 0 || (methodNode.accessFlags & 0x40) != 0 || (methodNode.accessFlags & 0x400) != 0)
                            continue;
                        if (simpleHashMap.get(entry.getKey()).contains(methodNode.simplehash))
                            count++;
                    }
                    //待定
                    if (count / c.childs.size() >= 0.7) {
                        checked.add(str);
                        matchedClass.add(c);
                        mcount++;
                        usedInLib.add(entry.getKey());
                        break;
                    }
                }
            }
        }


        //TODO 待定
        //if (mcount / classes.size() >= 0.6 ||(mcount / classes.size() >= 0.1 && mcount/classmum>=0.6))
        //if (mcount / classes.size() >= 0.6 ||(mcount / classes.size() >= 0.1/* && mcount/classmum>=0.6*/))
        if ((mcount / classes.size()) >= 0.2) {
            num[0] = checked.size();
            checked.forEach(s -> {
                nodeMap.get(s).matched = true;
                nodeMap.get(s).childs.forEach(node -> node.matched = true);
            });
            //matchedClass.forEach(c -> c.matched = true);
            flag = true;
        }
        return flag;
    }

    public void matchClass() throws Exception {

        HashMap<ByteBuffer, Integer> candidates = getCCandidat(pkgNodeSet);
        if (candidates.size() == 0)
            return;
        identify(candidates);
        //System.out.println();

    }

    public void matchMethod() throws Exception {
        HashMap<ByteBuffer, Integer> candidates = getCCandidat(pkgNodeSet);
        candidates = getMCandidat(pkgNodeSet);
        if (candidates.size() == 0)
            return;
        identify(candidates);

    }

    private void identify(HashMap<ByteBuffer, Integer> candidates) {

        List<Map.Entry<ByteBuffer, Integer>> list = new ArrayList<>(candidates.entrySet());
        HashMap<ByteBuffer, String> nameMap = new HashMap<>();
        HashMap<String, List<Map.Entry<ByteBuffer, Integer>>> libmap = new HashMap<>();
        Collections.sort(list, comp);
        getlibName(libmap, list, nameMap);

        List<Map.Entry<ByteBuffer, Integer>> finallist = getfinallist(libmap);
        if (finallist.size() == 0)
            return;
        String name = nameMap.get(finallist.get(0).getKey());
        boolean skip = false;
        for (Map.Entry<ByteBuffer, Integer> entry : finallist) {
            if (skip && name.endsWith(nameMap.get(entry.getKey())))
                continue;
            name = nameMap.get(entry.getKey());
            if (foundLib.contains(name))
                continue;
            sigjedis.select(2);
            List<ByteKeyList> candidtaclasstree = new ArrayList<>();
            HashMap<ByteKeyList, ByteKeyList> simpleHashMap = new HashMap<>();
            sigjedis.lrange(entry.getKey().array(), 0, -1).forEach(bytes -> {
                sigjedis.select(0);
                ByteKeyList byteKeyList = new ByteKeyList().of(sigjedis.lrange(bytes, 0, -1));
                candidtaclasstree.add(byteKeyList);
                sigjedis.select(1);
                simpleHashMap.put(byteKeyList, new ByteKeyList().of(sigjedis.lrange(bytes, 0, -1)));

            });
            //System.out.print(lib + ":\t");
            if (simcalc(candidtaclasstree, entry.getKey(), simpleHashMap)) {
                skip = true;
                logger.info("found " + nameMap.get(entry.getKey()));
            }
        }


    }


    private boolean simcalc(List<ByteKeyList> classes, ByteBuffer libhash, HashMap<ByteKeyList, ByteKeyList> simpleHashMap) {
        //matched class in lib
        HashSet<ByteKeyList> usedInLib = new HashSet<>();
        HashMap<ByteBuffer, Set<ByteKeyList>> classmap = getClassMap(classes);
        boolean flag = false;
        HashSet<ByteKeyList> used = new HashSet<>();
        List<Node> matchedClass = new ArrayList<>();
        Map<String, List<Node>> tmp = new TreeMap<>();
        float mcount = 0;
        for (Node p : pkgNodeSet) {
            String pname = ((PackageNode) p).packageName;
            if (p.matched)
                continue;
            for (Node c : p.childs) {
                ClassNode classNode = (ClassNode) c;
                if ((classNode.accessFlags & 0x200) != 0 || (classNode.accessFlags & 0x400) != 0)
                    continue;
                if (c.matched)
                    continue;
                //if (c.childs.size() == 1)
                //    continue;
                if (c.candilist.size() > 0) {
                    if (c.candilist.contains(libhash)) {
                        mcount++;
                        matchedClass.add(c);

                        if (tmp.containsKey(pname))
                            tmp.get(pname).add(c);
                        else {
                            tmp.put(pname, new ArrayList<>());
                            tmp.get(pname).add(c);
                        }

                        continue;
                    } else
                        continue;
                }

                HashMap<ByteKeyList, Integer> candi = getCandi(c, classmap);

                List<Map.Entry<ByteKeyList, Integer>> list1 = new ArrayList<>(candi.entrySet());
                Collections.sort(list1, comp);
                for (Map.Entry<ByteKeyList, Integer> entry : list1) {
                    float countsimle = 0;
                    if (usedInLib.contains(entry.getKey()))
                        continue;
                    //if class in app have more methods than class in lib, skip
                    if (c.childs.size() > entry.getKey().size())
                        continue;

                    /*
                    for (Node m : c.childs) {
                        MethodNode methodNode = (MethodNode) m;
                        if ((methodNode.accessFlags&0x1000)!=0||(methodNode.accessFlags&0x40)!=0||(methodNode.accessFlags&0x400)!=0)
                            continue;
                        if (entry.getKey().contains(m.hash))
                           countsimle++;
                    }
                    */
                    for (Node m : c.childs) {
                        MethodNode methodNode = (MethodNode) m;
                        if ((methodNode.accessFlags & 0x1000) != 0 || (methodNode.accessFlags & 0x40) != 0 || (methodNode.accessFlags & 0x400) != 0)
                            continue;
                        if (simpleHashMap.get(entry.getKey()).contains(methodNode.simplehash))
                            countsimle++;
                    }

                    //待定
                    if (countsimle / c.childs.size() > 0.9) {
                        matchedClass.add(c);

                        if (tmp.containsKey(pname))
                            tmp.get(pname).add(c);
                        else {
                            tmp.put(pname, new ArrayList<>());
                            tmp.get(pname).add(c);
                        }

                        mcount++;
                        usedInLib.add(entry.getKey());
                        break;
                    }
                }
            }
        }
        //System.out.println(mcount / classes.size());
        if (mcount / classes.size() > 0.2) {
            matchedClass.forEach(c -> c.matched = true);
            flag = true;
        }
        return flag;
    }


    private List<Map.Entry<ByteBuffer, Integer>> getfinallist(HashMap<String, List<Map.Entry<ByteBuffer, Integer>>> libmap) {
        List<Map.Entry<ByteBuffer, Integer>> list = new ArrayList<>();
        libmap.forEach((s, entries) -> {
            int count = 0;
            //int num = entries.get(0).getValue();
            list.add(entries.get(0));
            for (int i = 1; i < entries.size(); i++) {
                if (count <= 4) {
                    list.add(entries.get(i));
                    count++;
                }
            }
        });
        Collections.sort(list, comp);
        return list;
    }

    /**
     * @param classes element is a list consist of method hash in a class
     * @return {method hash:the list blonged to }
     */
    private HashMap<ByteBuffer, Set<ByteKeyList>> getClassMap(List<ByteKeyList> classes) {
        HashMap<ByteBuffer, Set<ByteKeyList>> classmap = new HashMap<>();
        classes.forEach(bl -> {
            bl.forEach(byteBuffer -> {
                if (classmap.containsKey(byteBuffer)) {
                    classmap.get(byteBuffer).add(bl);
                } else {
                    classmap.put(byteBuffer, new HashSet<>());
                    classmap.get(byteBuffer).add(bl);
                }
            });
        });
        return classmap;
    }

    /**
     * @param c        class node
     * @param classmap {method hash: the method list }
     * @return
     */
    private HashMap<ByteKeyList, Integer> getCandi(Node c, HashMap<ByteBuffer, Set<ByteKeyList>> classmap) {
        HashMap<ByteKeyList, Integer> candi = new HashMap<>();
        for (Node m : c.childs) {
            //if (m.candilist.size() == 0)
            //    continue;
            MethodNode methodNode = (MethodNode) m;
            if ((methodNode.accessFlags & 0x1000) != 0 || (methodNode.accessFlags & 0x40) != 0 || (methodNode.accessFlags & 0x400) != 0)
                continue;
            ByteBuffer key = ByteBuffer.wrap(m.hash);
            if (!classmap.containsKey(key))
                continue;
            for (ByteKeyList l : classmap.get(key)) {
                if (candi.containsKey(l)) {
                    candi.put(l, candi.get(l) + 1);
                } else {
                    candi.put(l, 1);
                }
            }
        }
        return candi;
    }

    private HashMap<ByteBuffer, Integer> getPCandidat(Set<Node> list) {
        canjedis.select(0);
        HashMap<ByteBuffer, Integer> candidates = new HashMap<>();
        for (Node p : list) {
            if (p.checked || p.matched)
                continue;
            getCandidata(p, 0, candidates);
        }
        return candidates;
    }

    private HashMap<ByteBuffer, Integer> getCCandidat(Set<Node> list) {
        canjedis.select(1);

        HashMap<ByteBuffer, Integer> candidates = new HashMap<>();
        for (Node p : list) {
            PackageNode packageNode = (PackageNode) p;
            if (packageNode.matched)
                continue;
            //skip
            for (Node c : p.childs) {
                if (none.contains(c))
                    continue;
                ClassNode classNode = (ClassNode) c;
                //对没有检测到的都再进行一遍检测
                if (c.matched)
                    continue;
                //if ((classNode.accessFlags & 0x200) != 0 || (classNode.accessFlags & 0x400) != 0)
                //continue;
                //filter inner class

                //if (classNode.clazzName.contains("$"))
                //    continue;

                if (c.childs.size() <= 1)
                    continue;
                getCandidata(c, 1, candidates);
            }
        }

        return candidates;
    }

    private HashMap<ByteBuffer, Integer> getMCandidat(Set<Node> list) {
        canjedis.select(3);
        HashMap<ByteBuffer, Integer> candidates = new HashMap<>();
        for (Node p : list) {
            PackageNode packageNode = (PackageNode) p;
            if (packageNode.matched)
                continue;
            for (Node c : p.childs) {
                if (none.contains(c))
                    continue;
                ClassNode classNode = (ClassNode) c;
                if (c.matched)
                    continue;
                if ((classNode.accessFlags & 0x200) != 0 || (classNode.accessFlags & 0x400) != 0)
                    continue;
                //filter inner class
                if (c.childs.size() <= 1)
                    continue;
                for (Node m : c.childs) {
                    if (none.contains(m))
                        continue;
                    MethodNode methodNode = (MethodNode) m;
                    getCandidata(m, 3, candidates);
                }
            }
        }
        return candidates;
    }


    public class EntryComparator implements Comparator<Map.Entry<?, Integer>> {
        @Override
        public int compare(Map.Entry<?, Integer> o1,
                           Map.Entry<?, Integer> o2) {
            // TODO Auto-generated method stub
            return o2.getValue() - o1.getValue();
        }
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

    /**
     * @param node       app class node
     * @param db         the redis db num,1:class,3:method
     * @param candidates key:hash of lib,value:num
     */
    private void getCandidata(Node node, int db, HashMap<ByteBuffer, Integer> candidates) {
        canjedis.select(db);
        if (canjedis.exists(node.hash)) {
            none.add(node);
            if (node instanceof PackageNode)
                node.childs.forEach(c -> c.checked = true);
            node.checked = true;
            canjedis.lrange(node.hash, 0, -1).forEach(bytes -> {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                node.candilist.add(buffer);
                //3 is method indexed db;1 is class
                int count = db == 3 ? 1 : node.childs.size();
                if (candidates.containsKey(buffer))
                    candidates.put(buffer, count + candidates.get(buffer));
                else candidates.put(buffer, count);
            });
        }
    }

    /**
     * @param libmap       key is lib name
     * @param list
     * @param nameMap,{lib hash:lib name}
     */
    private void getlibName(HashMap<String, List<Map.Entry<ByteBuffer, Integer>>> libmap,
                            List<Map.Entry<ByteBuffer, Integer>> list, HashMap<ByteBuffer, String> nameMap) {
        sigjedis.select(3);
        for (Map.Entry<ByteBuffer, Integer> e : list) {
            if (!sigjedis.exists(e.getKey().array()))
                continue;
            String name = new String(sigjedis.get(e.getKey().array()));
            nameMap.put(e.getKey(), name);

            if (!libmap.containsKey(name)) {
                libmap.put(name, new ArrayList<>());
                libmap.get(name).add(e);
            } else {
                libmap.get(name).add(e);
            }
        }
    }

    public void initlog() {

        String logIdentifier = CLI.CmdOptions.logDir.getAbsolutePath() + File.separator;
        logIdentifier += apkFile.getFileName().toString().replaceAll("\\.jar", "")
                .replaceAll("\\.apk", "").replaceAll("\\.aar", "");
        MDC.put("appPath", logIdentifier);
    }

}
