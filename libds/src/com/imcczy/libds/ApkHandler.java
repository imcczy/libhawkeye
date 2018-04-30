package com.imcczy.libds;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.classLoader.DexIClass;
import com.ibm.wala.dalvik.classLoader.DexModuleEntry;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.SeqClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.imcczy.libds.graph.ConnectedComponent;
import com.imcczy.libds.profile.AppProfile;
import com.imcczy.libds.signature.*;
import com.imcczy.libds.stats.ApkStats;
import com.imcczy.libds.stats.Libstats;
import com.imcczy.libds.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.imcczy.libds.signature.HashTree.hash;

/**
 * Created by imcczy on 2017/3/8.
 */
public class ApkHandler {
    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.ApkHandler.class);


    private Path apkFile;
    private IClassHierarchy cha;
    private ApkStats apkStats;
    private Map<TypeReference, ClassHierarchy.Node> map;
    private IClassLoader iClassLoader;
    private Jedis jedis;


    public ApkHandler(Path apkFile) throws IOException {
        this.apkFile = apkFile;
        //Packagename.packageName = new ProcessManifest(apkFile).getPackageName();
        apkStats = new ApkStats(apkFile);
    }

    public ApkHandler(Path apkFile, Map<TypeReference, ClassHierarchy.Node> map, IClassLoader iClassLoader, Jedis jedis) {
        this.apkFile = apkFile;
        this.map = map;
        this.iClassLoader = iClassLoader;
        this.jedis = jedis;
    }


    public void test(Path profilePath, JarFileModule jarFileModule) throws IOException, ClassHierarchyException,
            NoSuchAlgorithmException {

        final AnalysisScope analysisScope = AndroidAnalysisScope.setUpAndroidAnalysisScope(apkFile, null,
                null, jarFileModule);
        cha = SeqClassHierarchyFactory.make(analysisScope);

        //AndroidAnalysisScope.setUpApkScope(apkFile,cha.getScope());
        //cha.addApkClass();
        //getChaStats(cha);
        AppProfile appProfile = AppProfile.create(cha);
        //getChaStats(cha);
        //Utils.object2Disk(Paths.get(profilePath.toString(),apkFile.getFileName().toString()+".lib"),appProfile);
    }

    public void test(Map<TypeReference, ClassHierarchy.Node> map, IClassLoader iClassLoader) throws IOException,
            NoSuchAlgorithmException, ClassNotFoundException {
        //System.out.println(String.format(apkStats.pcakageName, System.nanoTime()));
        long t0 = System.nanoTime();
        AppProfile appProfile = AppProfile.create(DexFileModule.make((apkFile))
                .getEntrysCollection()
                .stream()
                .map(moduleEntry -> new DexIClass(ClassLoaderReference.Application, (DexModuleEntry) moduleEntry))
                .collect(Collectors.toList()), map, iClassLoader);
        apkStats.node = appProfile.hashTree.getRootNode();
        apkStats.dependenceTreeMap = appProfile.hashTree.dependence;
        //LibHandler libHandler = new LibHandler(apkStats);
        //libHandler.init();

        //System.out.println("cc"+TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        Path path = Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + File.separator +
                apkFile.getFileName().toString().replaceAll("\\.apk", ".lib"));

        appProfile = null;
        Utils.object2Disk(path, apkStats);
        logger.info("\nTTTTTTTTT" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        apkStats = null;
    }

    public void init(boolean readLibXML) {
        String logIdentifier = CLI.CmdOptions.logDir.getAbsolutePath() + File.separator;
        logIdentifier += apkFile.getFileName().toString().replaceAll("\\.jar", "")
                .replaceAll("\\.apk", "").replaceAll("\\.aar", "");
        MDC.put("appPath", logIdentifier);
    }

    public boolean storelib(Map<TypeReference, ClassHierarchy.Node> map, IClassLoader iClassLoader,
                            String md5, String[] pcakges) throws IOException,
            NoSuchAlgorithmException, ClassNotFoundException {

        AppProfile appProfile = AppProfile.create(DexFileModule.make((apkFile))
                .getEntrysCollection()
                .stream()
                .map(moduleEntry -> new DexIClass(ClassLoaderReference.Application, (DexModuleEntry) moduleEntry))
                .collect(Collectors.toList()), map, iClassLoader);

        HashMap<String, Node> nodeMap = new HashMap<>();
        appProfile.hashTree.getRootNode().childs.forEach(node -> {
            PackageNode p = (PackageNode) node;
            nodeMap.put(p.packageName, node);
        });
        /*
        ApkStats old =(ApkStats) Utils.disk2Object(apkFile);
        HashMap<String, Node> nodeMap = new HashMap<>();
        old.node.childs.forEach(node -> {
            PackageNode p = (PackageNode) node;
            nodeMap.put(p.packageName, node);
        });
        */
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
        if (ifequal(packageHash, Utils.hexStringToByteArray(md5))) {
            libstats.getNode().hash = packageHash;
        } else {
            return false;
        }
        Utils.object2Disk(Paths.get(CLI.CmdOptions.logDir.getAbsolutePath() + File.separator + md5 + ".lib"), libstats);
        appProfile = null;
        libstats = null;
        return true;
    }


    public static void getChaStats(IClassHierarchy cha) {
        int clCount = 0;
        int innerClCount = 0;
        int publicMethodCount = 0;
        int miscMethodCount = 0;
        HashMap<com.imcczy.libds.utils.AndroidClassType, Integer> clazzTypes = new HashMap<AndroidClassType, Integer>();
        for (AndroidClassType t : AndroidClassType.values())
            clazzTypes.put(t, 0);

        // collect basic cha information
        for (Iterator<IClass> it = cha.iterator(); it.hasNext(); ) {
            IClass clazz = it.next();

            if (WalaUtils.isAppClass(clazz)) {
                AndroidClassType type = WalaUtils.classifyClazz(clazz);
                clazzTypes.put(type, clazzTypes.get(type) + 1);
                logger.trace("App Class: " + WalaUtils.simpleName(clazz) + "  (" + type + ")");

                clCount++;
                if (WalaUtils.isInnerClass(clazz)) innerClCount++;

                for (IMethod im : clazz.getDeclaredMethods()) {
                    if (im.isBridge() || im.isSynthetic()) continue;

                    if (im.isPublic()) {
                        publicMethodCount++;
                    } else {
                        miscMethodCount++;
                    }
                }
            }
        }

        logger.info("");
        logger.info("= ClassHierarchy Stats =");
        logger.info(Utils.INDENT + "# of classes: " + clCount);
        logger.info(Utils.INDENT + "# thereof inner classes: " + innerClCount);
        for (AndroidClassType t : AndroidClassType.values())
            logger.info(Utils.INDENT2 + t + " : " + clazzTypes.get(t));
        logger.info(Utils.INDENT + "# methods: " + (publicMethodCount + miscMethodCount));
        logger.info(Utils.INDENT2 + "# of public methods: " + publicMethodCount);
        logger.info(Utils.INDENT2 + "# of non-public methods: " + miscMethodCount);
        logger.info("");
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

    private boolean ifequal(byte[] b1, byte[] b2) {
        int index = 0;
        while (index < 16) {
            if (b1[index] != b2[index])
                return false;
            index++;
        }
        return true;
    }
}
