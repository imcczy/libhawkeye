package com.imcczy.libds.reﬁnement;

import com.imcczy.libds.signature.*;
import com.imcczy.libds.stats.Libstats;
import com.imcczy.libds.utils.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

import static com.imcczy.libds.signature.HashTree.hash;

/**
 * Created by imcczy on 2017/11/23.
 */
public class Reﬁnement {
    private List<Path> libs;
    private HashSet<String> names = new HashSet<>();
    private HashSet<String> hashs = new HashSet<>();

    public Reﬁnement(List<Path> libs) {
        this.libs = libs;
        try (Stream<String> stream = Files.lines(Paths.get("/Users/imcczy/Downloads/cluster2/allnew2-2.csv"))) {
            stream.forEach(s -> {
                String[] tmp = s.split(",");
                names.add(tmp[tmp.length-1]);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (Stream<String> stream = Files.lines(Paths.get("/Users/imcczy/Downloads/gtest/csvs/final-packages.csv"))) {
            stream.forEach(s -> {
                names.add(s.replace("[","").replace("]",""));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (Stream<String> stream = Files.lines(Paths.get("/Users/imcczy/Downloads/gtest/csvs/new_dump5apkhashs.csv"))) {
            stream.forEach(s -> {
                hashs.add(s);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Reﬁnement(){

    }

    public void test(){
        Jedis jedis = new Jedis("127.0.0.1", 16379,6000);
        jedis.select(0);
        Jedis jedis1 = new Jedis("127.0.0.1", 16379,6000);
        jedis1.select(2);

        Set<byte[]> keys = jedis.keys("*".getBytes());
        for (byte[] key:keys){
            boolean flag = false;
            HashSet<String> name = new HashSet<>();
            if (jedis.llen(key)<=3)
                continue;
            List<byte[]> candi = jedis.lrange(key,0,-1);
            for (byte[] bytes:candi){
                if (!jedis1.exists(bytes))
                    continue;
                String tmpname = new String(jedis1.get(bytes));
                String tmp = getname(tmpname);
                name.add(tmp);
                if (name.size()>3){
                    flag = true;
                    break;
                }
            }
            if (flag) {
                jedis.del(key);
            }
        }
        System.out.println("1-----------------");
        jedis.select(3);
        keys = jedis.keys("*".getBytes());
        for (byte[] key:keys){
            boolean flag = false;
            HashSet<String> name = new HashSet<>();
            if (jedis.llen(key)<=2)
                continue;
            List<byte[]> candi = jedis.lrange(key,0,-1);
            for (byte[] bytes:candi){
                if (!jedis1.exists(bytes))
                    continue;
                String tmpname = new String(jedis1.get(bytes));
                String tmp = getname(tmpname);
                name.add(tmp);
                if (name.size()>2){
                    flag = true;
                    break;
                }
            }
            if (flag) {
                jedis.del(key);
            }
        }
        System.out.println("2-----------------");
        keys.clear();

        jedis.select(1);
        keys = jedis.keys("*".getBytes());

        for (byte[] key:keys){
            boolean flag = false;
            HashSet<String> name = new HashSet<>();
            if (jedis.llen(key)<=2)
                continue;
            List<byte[]> candi = jedis.lrange(key,0,-1);
            for (byte[] bytes:candi){
                if (!jedis1.exists(bytes))
                    continue;
                String tmpname = new String(jedis1.get(bytes));
                String tmp = getname(tmpname);
                name.add(tmp);
                if (name.size()>2){
                    flag = true;
                    break;
                }
            }
            if (flag) {
                jedis.del(key);
            }
        }
    }
    private String getname(String tmpname){
        String tmp = null;
        return tmpname;
        /*
        if (tmpname.startsWith("org.apache.")||tmpname.startsWith("com.google.android")){
            tmp =Utils.filter(4,tmpname);
        }else if (tmpname.startsWith("jp.co.")||tmpname.startsWith("uk.co.")||tmpname.startsWith("cn.com.")){
            tmp =Utils.filter(3,tmpname);
        }else {
            tmp =Utils.filter(2,tmpname);
        }

        if ("okio".equals(tmp) || "okhttp3".equals(tmp))
            tmp = "com.squareup";
*/
        /*
        if (tmpname.startsWith("org.apache.")||tmpname.startsWith("com.google.android")){
            tmp =Utils.filter(4,tmpname);
        }else if (tmpname.startsWith("com.google")||tmpname.startsWith("com.facebook")||
                tmpname.startsWith("com.squareup")||tmpname.startsWith("jp.co")||tmpname.startsWith("uk.co")){
            tmp =Utils.filter(3,tmpname);
        }else {
            tmp =Utils.filter(2,tmpname);
        }

        if ("okio".equals(tmp) || "okhttp3".equals(tmp))
            tmp = "com.squareup."+tmp;
         */
        //return tmp;
    }
    public void begin() {

    }

    public void distinct(){


    }

    /*
    public HashMap<byte[], String> calc2(List<String> names) throws NoSuchAlgorithmException {
        HashMap<byte[], String> lib = new HashMap<>();
        LibHandler.NodeComparator comp = new LibHandler.NodeComparator();
        IHash hashFunc = new HashImpl("MD5");
        boolean flag = false;
        for (Integer integer : allLibs.keySet()) {
            flag = false;

            TreeSet<String> sets = allLibs.get(integer);
            List<Set<String>> treeLists = splitTrees(sets);
            if (treeLists == null || treeLists.size() == 0)
                continue;
            for (Set<String> set : treeLists) {
                if (names.contains(set.toString())) {
                    flag = true;
                    break;
                }
            }
            if (!flag)
                continue;
            for (Set<String> set : treeLists) {
                ArrayList<Node> list = new ArrayList<>();
                for (String name : set) {
                    Node node = nodeMap.get(name);
                    if (node != null)
                        list.add(node);
                }
                Collections.sort(list, comp);
                byte[] packageHash = hash(list, hashFunc);
                lib.put(packageHash, set.toString());
            }
        }

        return lib;
    }
    */
    public void split() {
        Set<String> setnew = new HashSet();
        try (Stream<String> stream = Files.lines(Paths.get("/Users/imcczy/Downloads/cluster2/libsnew.csv"))) {
            stream.forEach(line -> {
                setnew.add(line.split(",")[0]);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (Stream<String> stream = Files.lines(Paths.get("/Users/imcczy/Downloads/cluster2/allnew2-2-new.csv"))) {
            Iterator<String> iterator = stream.iterator();
            HashMap<String, String[]> map = new HashMap<>();
            BufferedWriter bw = new BufferedWriter(new FileWriter("/Users/imcczy/Downloads/split31.csv"));
            BufferedWriter delet = new BufferedWriter(new FileWriter("/Users/imcczy/Downloads/delethash31.csv"));

            while (iterator.hasNext()) {
                String[] tmps = iterator.next().split(",");
                if (setnew.contains(tmps[0]))
                    continue;
                List<TreeSet<String>> treeLists = splitTrees(getTree(tmps[tmps.length - 1]));
                boolean flag = false;
                if (treeLists == null || treeLists.size() <= 1)
                    continue;
                for (TreeSet<String> set : treeLists) {
                    if (set.size() <= 1 && !set.first().startsWith("com.google")){
                        flag=false;
                        continue;
                    }
                    String s = set.toString().replace(", ", "/").replace("[","").replace("]","");
                    if (s.startsWith("com.google.common.annotations"))
                        continue;
                    if (!s.startsWith("com.google.common.annotations") &&(
                            s.startsWith("com.google.ads") || s.startsWith("com.google.android"))){
                        flag = true;
                        break;
                    }
                    if (names.contains(s)) {
                        flag = true;
                        //break;
                        if (set.first().startsWith("com.google")){
                            break;
                        }

                    }
                }
                if (flag) {
                    delet.write(tmps[0] + "\n");
                    //System.out.println(tmps[0]);
                    List<String[]> tmp = calc(tmps, treeLists);
                    if (tmp == null)
                        continue;
                    for (String[] strings : tmp) {
                        //ttt.add(strings[4]);
                        if (!map.containsKey(strings[0]))
                            map.put(strings[0], strings);
                    }
                }//else System.out.println(tmps[0]);

            }
            for (String m : map.keySet()) {
                bw.write(String.join(",", map.get(m)) + "\n");
            }

            delet.flush();
            bw.flush();
            delet.close();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeSet<String> getTree(String string) {
        TreeSet<String> treeSet = new TreeSet<>();
        for (String s : string.replace("[", "").replace("]", "").split("/"))
            treeSet.add(s);
        return treeSet;
    }


    //there may be multi trees in a union
    public static List<TreeSet<String>> splitTrees(TreeSet<String> treeSet) {
        List<TreeSet<String>> lists = new ArrayList<>();
        String tmp = treeSet.pollFirst();
        Iterator<String> iterator = treeSet.iterator();
        while (iterator.hasNext()) {
            String nextPName;
            TreeSet<String> tree = new TreeSet<>();
            tree.add(tmp);
            while (iterator.hasNext()) {
                nextPName = iterator.next();
                if (isParent(tree.first(), nextPName)) {
                    tree.add(nextPName);
                    continue;
                }
                if (isParent(tree.last(), nextPName)) {
                    String last = tree.pollLast();
                    lists.add(tree);
                    tree = new TreeSet<>();
                    tree.add(last);
                    tree.add(nextPName);
                    continue;
                }

                tmp = nextPName;
                break;

            }
            lists.add(tree);
            if (!iterator.hasNext()) {
                if (!tree.contains(tmp)) {
                    tree = new TreeSet<>();
                    tree.add(tmp);
                    lists.add(tree);
                }
            }

        }
        //avoid double counting
        //if (lists.size() == 1)
        //    lists = null;
        return lists;
    }

    private static boolean len(String str1, String str2) {
        String[] tem1 = str1.split("\\.");
        String[] tem2 = str2.split("\\.");
        //only cpmsider the first three segments,besides com.google.android.gms and org.apache.
        if (tem1.length >= 4 && tem2.length >= 4 && !str1.startsWith("com.google.android.gms")
                && !str2.startsWith("com.google.android.gms") && !str1.startsWith("org.apache.")
                && !str2.startsWith("org.apache."))
            if (tem1[2].equals(tem2[2]))
                return true;
        return false;

    }

    private static boolean isParent(TreeSet<String> tree, String pname) {
        for (String str : tree) {
            if (pname.startsWith(str + "."))
                return true;
        }
        return false;
    }

    private static boolean isParent(String p, String pname) {

        boolean common = p.startsWith("com.google.common.") && pname.startsWith("com.google.common.");
        if (p.startsWith("com.google.android.gms.") && pname.startsWith("com.google.android.gms."))
            common = common || (p.split("\\.")[4].equals(pname.split("\\.")[4]));

        return pname.startsWith(p + ".") || common || len(p, pname);
        /*
        if (p.equals("org.apache.http.entity.mime"))
            return true;
        boolean common = p.startsWith("org.apache.common.") && pname.startsWith("org.apache.common.");
        return pname.startsWith(p + ".") ||  (!common && len(p, pname));
         */
    }

    //calc and store
    private List<String[]> calc(String[] strs, List<TreeSet<String>> treeLists) throws Exception {
        String p = "/Volumes/ST1000/libsig/signew/libsig/";
        String mi = "/Volumes/ST1000/libsigorig/libsig/";
        String post = ".lib";
        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl("MD5");
        List<String[]> results = new ArrayList<>();

        Libstats libstats = null;
        try {
            Path libpath = Paths.get(p + strs[0] + post);
            if (!Files.exists(libpath)){
                libpath = Paths.get(mi+strs[0]+post);
            }else if (!Files.exists(libpath))
                return null;

            libstats = (Libstats) Utils.disk2Object(libpath);

        } catch (Exception e) {
            System.out.println(strs[0]);
            e.printStackTrace();
        }
        if (libstats == null)
            return null;

        Map<String, Node> nodeMap = libstats.getNodeMap();
        if (nodeMap == null)
            return null;

        for (Set<String> set : treeLists) {

            Libstats newl = new Libstats(new Node(null));
            ArrayList<Node> list = new ArrayList<>();
            for (String name : set) {
                Node node = nodeMap.get(name);
                if (node != null)
                    list.add(node);
            }
            newl.getNode().childs.addAll(list);
            Collections.sort(list, comp);
            byte[] packageHash = hash(list, hashFunc);
            newl.getNode().hash = packageHash;
            String md5 = Utils.bytesToHexlow(packageHash);
            Utils.object2Disk(Paths.get("/Users/imcczy/Downloads/test/" + md5 + ".lib"), newl);

            String[] tmp = {md5, strs[0], strs[1], strs[2], strs[3], ((TreeSet<String>) set).toString().replace(", ", "/")};
            results.add(tmp);
        }
        return results;
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
}
