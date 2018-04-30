package com.imcczy.libds;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.classLoader.DexIClass;
import com.ibm.wala.dalvik.classLoader.DexModuleEntry;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.SeqClassHierarchyFactory;
import com.ibm.wala.model.java.lang.reflect.Array;
import com.ibm.wala.types.ClassLoaderReference;
import com.imcczy.libds.match.Identifier;
import com.imcczy.libds.profile.AppProfile;
import com.imcczy.libds.signature.*;
import com.imcczy.libds.stats.ApkStats;
import com.imcczy.libds.stats.Libstats;
import com.imcczy.libds.utils.Utils;
import com.imcczy.libds.utils.WalaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.imcczy.libds.signature.HashTree.hash;


/**
 * Created by imcczy on 2017/11/25.
 */
public class LibFactory {
    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.LibFactory.class);
    private List<Path> targetFiles;
    private IClassHierarchy cha;

    public LibFactory(List<Path> files) {
        this.targetFiles = files;
        try {
            this.cha = SeqClassHierarchyFactory.make(AndroidAnalysisScope.setUpAndroidAnalysisScope(null,
                    null, CLI.CmdOptions.pathToAndroidJar));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void parse() throws IOException, ClassHierarchyException {
        HashSet<String> all = new HashSet<>();
        try (Stream<Path> stream = Files.list(CLI.CmdOptions.profilesDir)) {
            stream.forEach(path -> {
                all.add(path.getFileName().toString().split("\\.")[0]);
            });
        }
        if (CLI.CmdOptions.runMode.equals(CLI.RunMode.SERIAL))
            targetFiles.forEach(path -> {
                long start = System.nanoTime();
                String key = path.getFileName().toString().split("\\.")[0];
                try {
                    ApkHandler apkHandler = new ApkHandler(path);
                    apkHandler.init(false);
                    apkHandler.test(cha.getMap(), cha.getLoader(ClassLoaderReference.Primordial));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                logger.info("\nTTTTTTTTT" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

            });
        else if (CLI.CmdOptions.runMode.equals(CLI.RunMode.PARALLEL)) {
            targetFiles.parallelStream().forEach(path -> {
                String key = path.getFileName().toString().split("\\.")[0];
                if (all.contains(key))
                    return;
                try {

                    ApkHandler apkHandler = new ApkHandler(path);
                    apkHandler.init(false);
                    apkHandler.test(cha.getMap(), cha.getLoader(ClassLoaderReference.Primordial));
                    apkHandler = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public void store() throws IOException, ClassHierarchyException {
        HashMap<String, String> libs = new HashMap<>();
        HashMap<String, String> apks = new HashMap<>();
        Jedis jedis = new Jedis("localhost", 16379, 6000);
        Jedis jedis1 = new Jedis("localhost", 16379, 6000);
        jedis.select(0);
        jedis1.select(10);
        try (Stream<String> stream = Files.lines(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + "/libs.csv"))) {
            stream.forEach(s -> {
                String[] tmp = s.split(",");
                if (tmp.length == 3) {
                    apks.put(tmp[0], tmp[1]);
                } else apks.put(tmp[0], tmp[0]);
                libs.put(tmp[0], tmp[tmp.length - 1]);
                tmp = null;
            });
        }
        HashSet<String> map = new HashSet<>();
        try (Stream<Path> stream = Files.list(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath()))) {
            stream.filter(path -> path.toString().endsWith("lib")).forEach(path -> {
                map.add(path.getFileName().toString().split("\\.")[0]);
            });
        }
        try (Stream<String> stream = Files.lines(Paths.get("/home/nipc/nohup.out"))) {
            stream.forEach(s -> {
                map.add(s);
            });
        }
        map.addAll(jedis1.keys("*"));

        libs.keySet().parallelStream().forEach(md5 -> {
            if (map.contains(md5))
                return;
            byte[] md5bytes = Utils.hexStringToByteArray(apks.get(md5));
            long len;
            synchronized (jedis) {
                len = jedis.llen(md5bytes);
            }
            String sha251 = null;
            try {

                boolean flag = false;
                for (long index = 0; index < len; index++) {
                    //System.out.println(md5+index);
                    synchronized (jedis) {
                        sha251 = Utils.bytesToHex(jedis.lindex(md5bytes, index));
                    }
                    //if (new ApkHandler(Paths.get(CLI.CmdOptions.profilesDir+ File.separator +sha251+".lib"))
                    if (new ApkHandler((Utils.getpath(sha251)))
                            .storelib(cha.getMap(), cha.getLoader(ClassLoaderReference.Primordial), md5, libs.get(md5)
                                    .split("/"))) {
                        flag = true;
                        break;
                    }
                }
                if (!flag)
                    System.out.println(md5);
                else {
                    synchronized (jedis1) {
                        jedis1.set(md5, sha251);
                    }
                }
            } catch (Exception e) {
                System.out.println(md5 + ":\t" + sha251);

                e.printStackTrace();
            }

        });
    }

    public void storenew() throws IOException, ClassHierarchyException {
        HashMap<String, String> libs = new HashMap<>();
        HashMap<String, List<String>> apks = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + "/libs.csv"))) {
            stream.forEach(s -> {
                String[] tmp = s.split(",");
                libs.put(tmp[0], tmp[tmp.length - 1]);
            });
        }
        try (Stream<String> stream = Files.lines(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + "/apks.csv"))) {
            stream.forEach(s -> {
                String[] tmp = s.split("---");
                ArrayList<String> list = new ArrayList<>();
                Arrays.stream(tmp[tmp.length - 1].split(",")).forEach(lib -> list.add(lib));
                apks.put(tmp[0], list);
            });
        }

        apks.keySet().parallelStream().forEach(sha251 -> {
            try {
                Path apkp = null;
                if (CLI.CmdOptions.logDir.getName().contains("mi")) {
                    apkp = Utils.getpathmi(sha251);
                } else {
                    apkp = Utils.getpath(sha251);
                }

                AppProfile appProfile = AppProfile.create(DexFileModule.make((apkp))
                        .getEntrysCollection()
                        .stream()
                        .map(moduleEntry -> new DexIClass(ClassLoaderReference.Application, (DexModuleEntry) moduleEntry))
                        .collect(Collectors.toList()), cha.getMap(), cha.getLoader(ClassLoaderReference.Primordial));

                HashMap<String, Node> nodeMap = new HashMap<>();
                appProfile.hashTree.getRootNode().childs.forEach(node -> {
                    PackageNode p = (PackageNode) node;
                    nodeMap.put(p.packageName, node);
                });
                for (String lib : apks.get(sha251)) {
                    if (!libcal(nodeMap, libs.get(lib).split("/"))) {
                        System.out.println(lib);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    public void addsourcename() throws IOException{
        HashSet<String> set = new HashSet<>();
        try (Stream<Path> stream = Files.list(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath()))) {
            stream.filter(path -> path.toString().endsWith("lib")).forEach(path -> {
                set.add(path.getFileName().toString().split("\\.")[0]);
            });
        }
        targetFiles.parallelStream().forEach(path -> {
            try {
                String sha251 = path.getFileName().toString().split("\\.")[0];
                if (set.contains(sha251)){
                    return;
                }
                Path apkPath = Utils.getpath(sha251);
                ApkStats apkStats = new ApkStats(apkPath);
                Map<String, IClass> map = new HashMap<>();
                DexFileModule.make((apkPath))
                        .getEntrysCollection()
                        .stream()
                        .map(moduleEntry -> new DexIClass(ClassLoaderReference.Application, (DexModuleEntry) moduleEntry))
                        .collect(Collectors.toList()).forEach(dexIClass -> {
                    map.put(WalaUtils.getFullClassName(dexIClass), dexIClass);
                });
                ApkStats apkStatsold = (ApkStats) Utils.disk2Object(path);
                apkStatsold.mainActivityName = apkStats.mainActivityName;
                apkStatsold.node.childs.forEach(pN -> {
                    pN.childs.forEach(cN -> {
                        String cname = pN.toPNode().packageName + "." + cN.toCNode().clazzName;
                        if (map.containsKey(cname)) {
                            cN.toCNode().sourceFileName = map.get(cname).getClassSourceFileName();
                        }
                    });
                });

                Path pathnew = Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + File.separator +
                       sha251+".lib");

                Utils.object2Disk(pathnew, apkStatsold);

            }catch (Exception e){
                e.printStackTrace();
                System.out.println(path.toString());
            }

        });
    }

    private boolean libcal(HashMap<String, Node> nodeMap, String[] pcakges) throws NoSuchAlgorithmException, IOException {
        Libstats libstats = new Libstats(new Node(null));
        for (String name : pcakges) {
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
        libstats.getNode().hash = packageHash;
        Utils.object2Disk(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + File.separator + Utils.bytesToHexlow(packageHash) + ".lib"), libstats);
        //appProfile = null;
        libstats = null;
        return true;
    }

    public void match() {
        if (CLI.CmdOptions.runMode.equals(CLI.RunMode.SERIAL)) {

            targetFiles.forEach(path -> {
                Identifier identifier = new Identifier(path, cha);
                try {
                    identifier.match();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

        } else if (CLI.CmdOptions.runMode.equals(CLI.RunMode.PARALLEL)) {
            targetFiles.parallelStream().forEach(path -> {
                Identifier identifier = new Identifier(path, cha);
                try {
                    identifier.match();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
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
}

