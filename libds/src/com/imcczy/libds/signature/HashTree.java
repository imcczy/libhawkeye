/*
 * Copyright (c) 2015-2016  Erik Derr [derr@cs.uni-saarland.de]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.imcczy.libds.signature;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.dex.instructions.Constant;
import com.ibm.wala.dalvik.dex.instructions.GetField;
import com.ibm.wala.dalvik.dex.instructions.Instruction;
import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.imcczy.libds.CLI;
import com.imcczy.libds.IFilter;
import com.imcczy.libds.signature.Hash.ByteArrayComparator;
import com.imcczy.libds.pkg.PackageTree;
import com.imcczy.libds.pkg.PackageUtils;
//import de.infsec.tpl.profile.ProfileMatch.MatchLevel;
import com.imcczy.libds.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * The main data structure for (library/app) profiles
 *
 * @author ederr
 */
public class HashTree implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.signature.HashTree.class);
    private static final long serialVersionUID = -4905503470725030446L;

    private Config config = new Config();

    public enum HTREE_BUILD_VERBOSENESS {
        MINIMAL /* Root and Package hashes/names only */,
        NORMAL  /* Root/Package/Class hashes including package names (DEFAULT) */,
        DEBUG   /* Root/Package/Class hashes including package/class names */,
        TRACE   /* Root/Package/Class/Method hashes including all names/signatures */
    }

    /**
     * Build config for HashTree
     */
    public class Config implements Serializable {

        private static final long serialVersionUID = -6847481132127810995L;
        // if true, filters duplicate method hashes, i.e. methods that have the same fuzzy descriptor
        // this introduces some kind of fuzziness as we abstract from the concrete number of certain descriptor
        public boolean filterDups = false;

        // if true, only public methods are considered during hashing
        // this introduces some fuzziness however better abstracts from internal changes as it better matches the public
        // interfaces used by the developer (e.g. different library versions with the same interface)
        public boolean publicOnly = false;

        // if true, inner classes are not considered during hashing
        public boolean filterInnerClasses = false;

        public boolean filterAndroidClasses = true;

        // the hash algorithm used for hashing
        public HashAlgorithm hashAlgorithm = HashAlgorithm.MD5;

        public HTREE_BUILD_VERBOSENESS buildVerboseness = HTREE_BUILD_VERBOSENESS.TRACE;

        public IClassLoader iClassLoader = null;
        public Map<TypeReference, ClassHierarchy.Node> map = null;


        public Config() {
        }

        public Config(boolean filterDups, boolean publicOnly, boolean filterInnerClasses) {
            this.filterDups = filterDups;
            this.publicOnly = publicOnly;
            this.filterInnerClasses = filterInnerClasses;
        }


        public Config(boolean filterDups, boolean publicOnly, boolean filterInnerClasses, HashAlgorithm hashAlgorithm) {
            this(filterDups, publicOnly, filterInnerClasses);
            this.hashAlgorithm = hashAlgorithm;
        }

        public Config(boolean filterDups, boolean publicOnly, boolean filterInnerClasses, HashAlgorithm hashAlgorithm,
                      Map<TypeReference, ClassHierarchy.Node> map, IClassLoader iClassLoader) {
            this(filterDups, publicOnly, filterInnerClasses);
            this.hashAlgorithm = hashAlgorithm;
            this.map = map;
            this.iClassLoader = iClassLoader;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Config))
                return false;
            Config c = (Config) obj;

            return c.filterDups == this.filterDups &&
                    c.publicOnly == this.publicOnly &&
                    c.filterInnerClasses == this.filterInnerClasses &&
                    c.hashAlgorithm.equals(this.hashAlgorithm);
        }

        @Override
        public int hashCode() {
            return 10000 * (this.filterDups ? 1 : 0) + 1000 * (this.publicOnly ? 1 : 0) + 100 * (this.filterInnerClasses ? 1 : 0) + hashAlgorithm.value.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[Config]");
            sb.append("filterDups? " + this.filterDups);
            sb.append(" | publicOnly? " + this.publicOnly);
            sb.append(" | filterInnerClasses: " + this.filterInnerClasses);
            sb.append(" | hash-algo: " + this.hashAlgorithm);
            return sb.toString();
        }
    }

    private Node rootNode;

    //public TreeMap<Edge,MutableInterger> callListMap= new TreeMap<>();
    public TreeMap<Edge, Dependence> dependence = new TreeMap<>();
    //public Set<Edge> classDependence = new HashSet<>();

    public class NodeComparator implements Comparator<Node> {
        private ByteArrayComparator comp;

        public NodeComparator() throws NoSuchAlgorithmException {
            IHash hashFunc = new HashImpl(config.hashAlgorithm.toString());
            comp = ((Hash) hashFunc).new ByteArrayComparator();
        }

        @Override
        public int compare(Node n0, Node n1) {
            return comp.compare(n0.hash, n1.hash);
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

    public static IFilter<IMethod> publicOnlyFilter = new IFilter<IMethod>() {
        @Override
        public Collection<IMethod> filter(Collection<IMethod> input) {
            List<IMethod> result = new ArrayList<IMethod>();
            for (IMethod m : input)
                if (m.isPublic())
                    result.add(m);

            return result;
        }

    };


    public HashTree() {
    }


    /*
     * Setter methods
     */
    public void setFilterDups(final boolean filterDups) {
        config.filterDups = filterDups;
    }

    public void setPublicOnly(final boolean publicOnly) {
        config.publicOnly = publicOnly;
    }

    public void setFilterInnerClasses(final boolean filterInnerClasses) {
        config.filterInnerClasses = filterInnerClasses;
    }

    public void setHashAlgorithm(final HashAlgorithm algorithm) {
        config.hashAlgorithm = algorithm;
    }

    public void setBuildVerboseness(final HTREE_BUILD_VERBOSENESS v) {
        config.buildVerboseness = v;
    }

    public boolean hasDefaultConfig() {
        return !this.config.filterDups && !this.config.filterInnerClasses && !this.config.publicOnly;
    }


    /*
     * Getter methods
     */
    public Node getRootNode() {
        return this.rootNode;
    }

    public byte[] getRootHash() {
        return this.rootNode.hash;
    }

    public Config getConfig() {
        return this.config;
    }

    public Collection<Node> getPackageNodes() {
        return this.getRootNode().childs;
    }

    public int getNumberOfPackages() {
        return rootNode.numberOfChilds();
    }

    public int getNumberOfClasses() {
        int cCount = 0;
        for (Node pNode : rootNode.childs)
            cCount += getNumberOfClasses((PackageNode) pNode);
        return cCount;
    }

    public int getNumberOfClasses(PackageNode pNode) {
        return pNode.numberOfChilds();
    }

    public int getNumberOfMethods(PackageNode pNode) {
        int mCount = 0;
        for (Node cNode : pNode.childs)
            mCount += cNode.numberOfChilds();
        return mCount;
    }


    public int getNumberOfMethods() {
        int mCount = 0;
        for (Node pNode : rootNode.childs)
            mCount += getNumberOfMethods((PackageNode) pNode);
        return mCount;
    }

    public List<String> getAllMethodSignatures() {
        List<String> signatures = new ArrayList<String>();
        for (Node pNode : rootNode.childs) {
            for (Node cNode : pNode.childs) {
                for (Node mNode : cNode.childs) {
                    signatures.add(((MethodNode) mNode).signature);
                }
            }
        }
        Collections.sort(signatures);
        return signatures;
    }

    /*
     * Modified by imcczy
     *
     */
 /*
 *  public int getNumberOfHashesByLevel(MatchLevel lvl) {
		switch(lvl) {
			case CLASS:
				return getNumberOfClasses();
			case METHOD:
				return getNumberOfMethods();
			case PACKAGE:
				return getNumberOfPackages();
		}
		return -1;
	}
*/
    public void printConfig() {
        logger.info(config.toString());
    }

    public boolean matchesConfig(HashTree hTree) {
        return this.config.equals(hTree.getConfig());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HashTree))
            return false;

        HashTree ht = (HashTree) obj;
        if (!ht.config.equals(this.config))
            return false;

        return Hash.equals(this.getRootHash(), ht.getRootHash());
    }


    public void generate(boolean filterDups, boolean publicOnly, boolean filterInnerClasses, HashAlgorithm algorithm,
                         IClassHierarchy cha, Collection<IClass> iClasses, Map<TypeReference, ClassHierarchy.Node> map,
                         IClassLoader iClassLoader) throws NoSuchAlgorithmException {
        this.config = new Config(filterDups, publicOnly, filterInnerClasses, algorithm, map, iClassLoader);
        generate(cha, iClasses);
    }


    /**
     * Generates a HashTree for every class loaded via application classLoader
     *
     * @throws NoSuchAlgorithmException
     */
    public void generate(IClassHierarchy cha, Collection<IClass> iClasses) throws NoSuchAlgorithmException {
        //logger.debug("Generate library hash tree..");
        if (logger.isDebugEnabled())
            printConfig();

        IHash hashFunc = new HashImpl(config.hashAlgorithm.toString());
        NodeComparator comp = new NodeComparator();
        IFilter<IMethod> methodFilter = config.publicOnly ? publicOnlyFilter : null;

        int classHashCount = 0;
        int methodHashCount = 0;

        // create map package name -> list of clazzNodes
        HashMap<String, Collection<ClassNode>> packageMap = new HashMap<String, Collection<ClassNode>>();

        Collection<IClass> classes = new ArrayList<>();
        if (cha != null && iClasses == null)
            classes = cha.getIClass();
        else if (cha == null && iClasses != null)
            classes = iClasses;
        long t0 = System.nanoTime();
        for (IClass clazz : classes) {
//			if (config.publicOnly && !clazz.isPublic())
//				continue;

            if (WalaUtils.isAppClass(clazz)) {
                //if (WalaUtils.isUsefullClass(clazz)){

                //if (WalaUtils.isApkClass(clazz))
                //continue;
                if (config.filterInnerClasses && WalaUtils.isInnerClass(clazz)) {
                    t0 = 0;
                    continue;
                }
                if (config.filterAndroidClasses && WalaUtils.isAndroidClass(clazz)) {
                    continue;
                }
                //HashSet<String> call = new HashSet<>();

                String supPackage = PackageUtils.getPackageName(
                        Utils.convertToFullClassName(clazz.getSuperClassName())
                );
                String supPackageclass = Utils.convertToFullClassName(clazz.getSuperClassName());

                Collection<MethodNode> methodNodes = config.filterDups ?
                        new TreeSet<MethodNode>(comp) : new ArrayList<MethodNode>();
                //Collection<?> in = clazz.getAllImplementedInterfaces();
                Collection<IMethod> methods = clazz.getDeclaredMethods();
                Collection<Annotation> annotations = clazz.getAnnotations();
                String pckgName = PackageUtils.getPackageName(clazz);
                //String classname = WalaUtils.simpleName(clazz);
                //classDependence.add(new Edge(classname,supPackageclass));
                /*
				annotations.forEach(annotation -> {
				    Edge e = new Edge(pckgName,PackageUtils.getPackageName(Utils.convertToFullClassName(
				            annotation.getType().getName().toString()
                    )));
                    //Edge eclass = new Edge(classname,Utils.convertToFullClassName(annotation.getType().getName().toString()));
                    Dependence d = new Dependence();
                    d.setAnnotation(true);
                    dependence.put(e,d);
                    //classDependence.add(eclass);
                });
                */
                /*
				for (Annotation annotation:annotations){
				    annotation.getNamedArguments().get("value");
                    Edge e = new Edge(pckgName,PackageUtils.getPackageName(Utils.convertToFullClassName(
                            annotation.getType().getName().toString()
                    )));
                    //Edge eclass = new Edge(classname,Utils.convertToFullClassName(annotation.getType().getName().toString()));
                    Dependence d = new Dependence();
                    d.setAnnotation(true);
                    dependence.put(e,d);
                }*/
                if (methodFilter != null)
                    methods = methodFilter.filter(methods);

                Edge e = new Edge(pckgName, supPackage);
                if (dependence.containsKey(e))
                    dependence.get(e).setInherite(true);
                else {
                    Dependence d = new Dependence();
                    d.setInherite(true);
                    dependence.put(e, d);
                }
                for (IMethod m : methods) {
                    // normalize java|dex bytecode by skipping compiler-generated methods
                    if (m.isBridge() || m.isSynthetic()) {
                        continue;
                    }
                    for (Annotation annotation : m.getAnnotations()) {
                        if (!"Ldalvik/annotation/Throws".equals(annotation.getType().getName().toString()))
                            continue;
                        AnnotationsReader.ElementValue arrayElementValue = annotation.getNamedArguments().get("value");
                        Arrays.stream(((AnnotationsReader.ArrayElementValue) arrayElementValue).vals).forEach(elementValue -> {
                            Edge ee = new Edge(pckgName, PackageUtils.getPackageName(Utils.convertToFullClassName(
                                    elementValue.toString()
                            )));
                            //Edge eclass = new Edge(classname,Utils.convertToFullClassName(annotation.getType().getName().toString()));
                            Dependence d = new Dependence();
                            d.setAnnotation(true);
                            dependence.put(e, d);
                        });

                    }

                    Instruction[] instructions = m.getDexInstructions();
                    StringBuilder methodSig = new StringBuilder();
                    StringBuilder methodSimpleSig = new StringBuilder();
                    TreeSet<String> test = new TreeSet<>();
                    if (instructions != null) {
                        for (Instruction instruction : m.getDexInstructions()) {
                            methodSig.append(instruction.getOpcodeName());

                            if (instruction instanceof Invoke) {
                                String className = Utils.convertToFullClassName(((Invoke) instruction).clazzName);
                                String p = PackageUtils.getPackageName(className);
                                Edge edge = new Edge(pckgName, p);
                                //classDependence.add(new Edge(classname,className));
                                if (dependence.containsKey(edge)) {
                                    MutableInterger count = dependence.get(edge).getCallMethodCount();
                                    count.set(count.get() + 1);
                                } else {
                                    Dependence d = new Dependence();
                                    d.setCallMethodCount(1);
                                    dependence.put(edge, d);
                                }
                                StringBuilder invokesb = getFuzzyDescriptor(((Invoke) instruction).describerList);
                                methodSig.append(invokesb);
                                test.add(invokesb.toString());
                                //methodSimpleSig.append(invokesb);
                            }

                            if (instruction instanceof GetField) {
                                String p = PackageUtils.getPackageName(
                                        Utils.convertToFullClassName(((GetField) instruction).clazzName)
                                );
                                //call.add(p);
                                //classDependence.add(new Edge(classname,Utils.convertToFullClassName(((GetField) instruction).clazzName)));
                                Edge edge = new Edge(pckgName, p);
                                if (dependence.containsKey(edge)) {
                                    MutableInterger count = dependence.get(edge).getGetFiledCount();
                                    count.set(count.get() + 1);
                                } else {
                                    Dependence d = new Dependence();
                                    d.setGetFiledCount(1);
                                    dependence.put(edge, d);
                                }
                                methodSig.append("[");
                                String type = ((GetField) instruction).fieldType;
                                TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, type);
                                IClass ct = null;
                                if (t != null) {
                                    ct = IClassHierarchy.lookupClassRecursive(t, config.map, config.iClassLoader);
                                    /*
                                    if (t.getClassLoader().equals(ClassLoaderReference.Application))
                                        ct = IClassHierarchy.lookupClassRecursive(t,config.map,config.iClassLoader);
                                    if (t.getClassLoader().equals(ClassLoaderReference.Primordial)){
                                        methodSig.append(type);
                                        continue;
                                    }
                                     */
                                    methodSig.append(ct == null ? "X" : type);
                                }
                                methodSig.append("]");
                            }
/*
                            if (instruction instanceof Constant.ClassConstant){
                                TypeReference t =((Constant.ClassConstant) instruction).value;
                                String p = PackageUtils.getPackageName(
                                        Utils.convertToFullClassName(t.getName().toString())
                                );
                                Edge edge = new Edge(pckgName,p);
                                if (dependence.containsKey(edge)){
                                    MutableInterger count = dependence.get(edge).getGetFiledCount();
                                    count.set(count.get()+1);
                                }else {
                                    Dependence d = new Dependence();
                                    d.setGetFiledCount(1);
                                    dependence.put(edge,d);
                                }
                                methodSig.append("{");
                                IClass ct = null;
                                if (t.getClassLoader().equals(ClassLoaderReference.Application))
                                    ct = IClassHierarchy.lookupClassRecursive(t,config.map,config.iClassLoader);
                                if (t.getClassLoader().equals(ClassLoaderReference.Primordial)){
                                    methodSig.append(t.getName().toString());
                                    continue;
                                }
                                methodSig.append("X");
                                methodSig.append("}");
                            }
*/
                        }
                    }
                    instructions = null;

                    //byte[] hash = hashFunc.hash(getFuzzyDescriptor(m));
                    //fillter init() and cinit()
                    if (!m.isClinit() && !m.isInit()) {
                        StringBuilder namesb = getFuzzyDescriptor(m);
                        byte[] hash = hashFunc.hash(methodSig.append(namesb).toString());
                        methodNodes.add(new MethodNode(hash, hashFunc.hash(methodSimpleSig.append(namesb).append(test.toString()).toString())
                                , null, m.getAccessFlags()));//m.getSignature()
                    }
                    methodSig.setLength(0);
                    methodSig = null;
                    methods = null;
                }

                // normalization (if we have no methods, either because there are none or due to our filter properties, skip this class)
                if (methodNodes.isEmpty()) {
                    //logger.trace(Utils.INDENT + ">> No methods found for clazz: " + WalaUtils.simpleName(clazz) + "  [SKIP]");
                    continue;
                }
                //if (!config.filterDups)
                //	Collections.sort((List<MethodNode>) methodNodes, comp);  // sort but do not filter dups

                methodHashCount += methodNodes.size();
                classHashCount++;
                byte[] clazzHash = hash(methodNodes.stream().sorted(comp).collect(Collectors.toList()), hashFunc);
				/*
				String classIdentifier = config.buildVerboseness == HTREE_BUILD_VERBOSENESS.DEBUG ||
                        config.buildVerboseness == HTREE_BUILD_VERBOSENESS.TRACE? WalaUtils.simpleName(clazz) : "";*/
                ClassNode clazzNode = new ClassNode(clazzHash, WalaUtils.getClassName(clazz),
                        clazz.getModifiers(),clazz.getSourceFileName());

                // only store method hashes if configured (space vs accuracy)
                clazzNode.childs = config.buildVerboseness == HTREE_BUILD_VERBOSENESS.TRACE ?
                        new ArrayList<Node>(methodNodes) : new ArrayList<Node>();


                if (!packageMap.containsKey(pckgName)) {
                    packageMap.put(pckgName, config.filterDups ? new TreeSet<ClassNode>(comp) : new ArrayList<ClassNode>());
                }
                packageMap.get(pckgName).add(clazzNode);

            }

        }
        long t1 = System.nanoTime();
        long millis = TimeUnit.NANOSECONDS.toMillis(t1 - t0);
        //System.out.println("get hash: "+millis);


        /*
         * clear system api call
         */
        Iterator<Edge> it = dependence.keySet().iterator();
        while (it.hasNext()) {
            if (!packageMap.containsKey(it.next().getD()))
                it.remove();
        }
        /*
        it = classDependence.iterator();
        while (it.hasNext()){
            if (!packageMap.containsKey(PackageUtils.getPackageName(it.next().getD())))
                it.remove();
        }*/

        Collection<PackageNode> packageNodes = config.filterDups ? new TreeSet<PackageNode>(comp) : new ArrayList<PackageNode>();
        for (String pckgName : new TreeSet<String>(packageMap.keySet())) {
            if (!config.filterDups)
                Collections.sort((List<ClassNode>) packageMap.get(pckgName), comp);  // sort but do not filter dups

            byte[] packageHash = hash(packageMap.get(pckgName), hashFunc);
            PackageNode n = new PackageNode(packageHash, pckgName);
            if (!config.buildVerboseness.equals(HTREE_BUILD_VERBOSENESS.MINIMAL)) // do not add class nodes in min verboseness
                n.childs.addAll(packageMap.get(pckgName));
            packageNodes.add(n);
        }
        //TreeSet<String> treeSet = new TreeSet<String>(packageMap.keySet());

        //logger.debug(Utils.INDENT + "- generated " + methodHashCount   + " method hashes.");
        //logger.debug(Utils.INDENT + "- generated " + classHashCount    + " clazz hashes.");
        //logger.debug(Utils.INDENT + "- generated " + packageNodes.size() + " package hashes.");


        // generate library hash
        if (!config.filterDups)
            Collections.sort((List<PackageNode>) packageNodes, comp);  // sort but do not filter dups

        byte[] libraryHash = hash(packageNodes, hashFunc);
        rootNode = new Node(libraryHash);
        rootNode.childs.addAll(packageNodes);
        iClasses = null;
        classes = null;
        config.iClassLoader = null;


        //logger.debug(Utils.INDENT + "=> Library Hash: " + Hash.hash2Str(libraryHash));
    }


    public Node getSubTreeByPackage(PackageTree ptree) throws NoSuchAlgorithmException {
        String rootPackage = ptree.getRootPackage();

        // Since we have a flattened tree (in terms of package nodes, we collect all package nodes that
        // equal or start with the rootPackage, then create and return a new rootnode with the collected package nodes
        // as child
        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl(config.hashAlgorithm.toString());
        Collection<PackageNode> childs = config.filterDups ? new TreeSet<PackageNode>(comp) : new ArrayList<PackageNode>();
        for (Node n : rootNode.childs) {
            PackageNode pn = (PackageNode) n;
            if (pn.packageName.startsWith(rootPackage))
                childs.add(pn);
        }

        if (!config.filterDups)
            Collections.sort((List<PackageNode>) childs, comp);  // sort but do not filter dups

        // generate new root node
        if (!childs.isEmpty()) {
            Node rootNode = new Node(hash(childs, hashFunc));
            rootNode.childs.addAll(childs);
            return rootNode;
        } else
            return null;  // no matching node found
    }


    public Node generateRootNode(Collection<PackageNode> pnodes) throws NoSuchAlgorithmException {

        // Since we have a flattened tree (in terms of package nodes, we collect all package nodes that
        // equal or start with the rootPackage, then create and return a new rootnode with the collected package nodes
        // as child
        NodeComparator comp = new NodeComparator();
        IHash hashFunc = new HashImpl(config.hashAlgorithm.toString());
        Collection<PackageNode> childs = config.filterDups ? new TreeSet<PackageNode>(comp) : new ArrayList<PackageNode>();
        childs.addAll(pnodes);

        if (!config.filterDups)
            Collections.sort((List<PackageNode>) childs, comp);  // sort but do not filter dups

        // generate new root node
        if (childs.isEmpty()) {
            logger.warn("[generateRootNode] no childs - return empy rootNode");
        }

        Node rootNode = new Node(hash(childs, hashFunc));
        rootNode.childs.addAll(childs);
        return rootNode;
    }


    /**
     * Generic hash function that takes a list of hashes, concatenates and hashes them
     *
     * @param nodes    a collection of input hashes
     * @param hashFunc a hash function
     * @return a hash
     */
    public static byte[] hash(Collection<? extends Node> nodes, final IHash hashFunc) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            for (Node node : nodes)
                outputStream.write(node.hash);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] arr = outputStream.toByteArray();
        return hashFunc.hash(arr);
    }


    /**
     * A {@link Descriptor} only describes input arg types + return type, e.g.
     * The Descriptor of AdVideoView.onError(Landroid/media/MediaPlayer;II)Z  is (Landroid/media/MediaPlayerII)Z
     * In order to produce a fuzzy (robust against identifier-renaming) descriptor we replace each custom type by a fixed
     * replacement, e.g. we receive a descriptor like (XII)Z
     * Note: library dependencies, i.e. lib A depends on lib B are not a problem. If we analyze lib A without loading lib B,
     * any type of lib B will be loaded with the Application classloader but will _not_ be in the classhierarchy.
     *
     * @param m {@link Descriptor} retrieved from a {@link IMethod}
     * @return a fuzzy descriptor
     */
    public StringBuilder getFuzzyDescriptor(IMethod m) {
        final String customTypeReplacement = "X";

        logger.trace("[getFuzzyDescriptor]");
        logger.trace("-  signature: " + m.getSignature());
        logger.trace("- descriptor: " + m.getDescriptor().toString());

        StringBuilder sb = new StringBuilder("(");
        int j = m.getNumberOfParameters();
        for (int i = (m.isStatic() ? 0 : 1); i < j; i++) {
            boolean isAppClazz = false;

            if (m.getParameterType(i).getClassLoader().equals(ClassLoaderReference.Application)) {
                TypeReference typeReference = m.getParameterType(i);
                //IClass ct = m.getClassHierarchy().lookupClass(m.getParameterType(i));
                IClass ct = IClassHierarchy.lookupClassRecursive(typeReference, config.map, config.iClassLoader);
                //isAppClazz = ct == null || WalaUtils.isAppClass(ct);
                isAppClazz = ct == null;
                sb.append(isAppClazz ? customTypeReplacement : m.getParameterType(i).getName().toString());
            } else
                sb.append(m.getParameterType(i).getName().toString());

            //logger.trace(LogConfig.INDENT + "- param ref: " + m.getParameterType(i).getName().toString() + (isAppClazz? "  -> " + customTypeReplacement : ""));
        }
        //logger.trace("");
        sb.append(")");
        if (m.getReturnType().getClassLoader().equals(ClassLoaderReference.Application)) {
            //IClass ct = m.getClassHierarchy().lookupClass(m.getReturnType());
            IClass ct = IClassHierarchy.lookupClassRecursive(m.getReturnType(), config.map, config.iClassLoader);
            //sb.append(ct == null || WalaUtils.isAppClass(ct)? customTypeReplacement : m.getReturnType().getName().toString());
            sb.append(ct == null ? customTypeReplacement : m.getReturnType().getName().toString());
        } else {
            sb.append(m.getReturnType().getName().toString());
        }

        logger.trace("-> new type: " + sb.toString());
        return sb;
    }

    /*
    public static String getFuzzyDescriptor(IMethod m) {
        final String customTypeReplacement = "X";

        logger.trace("[getFuzzyDescriptor]");
        logger.trace("-  signature: " + m.getSignature());
        logger.trace("- descriptor: " + m.getDescriptor().toString());

        StringBuilder sb = new StringBuilder("(");
        int j = m.getNumberOfParameters();
        for (int i = (m.isStatic()? 0 : 1) ; i < j; i++) {
            boolean isAppClazz = false;

            if (m.getParameterType(i).getClassLoader().equals(ClassLoaderReference.Application)) {
                TypeReference typeReference = m.getParameterType(i);
                IClass ct = m.getClassHierarchy().lookupClass(m.getParameterType(i));
                isAppClazz = ct == null || WalaUtils.isAppClass(ct);
                sb.append(isAppClazz? customTypeReplacement : m.getParameterType(i).getName().toString());
            } else
                sb.append(m.getParameterType(i).getName().toString());

            //logger.trace(LogConfig.INDENT + "- param ref: " + m.getParameterType(i).getName().toString() + (isAppClazz? "  -> " + customTypeReplacement : ""));
        }
        //logger.trace("");
        sb.append(")");
        if (m.getReturnType().getClassLoader().equals(ClassLoaderReference.Application)) {
            IClass ct = m.getClassHierarchy().lookupClass(m.getReturnType());
            sb.append(ct == null || WalaUtils.isAppClass(ct)? customTypeReplacement : m.getReturnType().getName().toString());
        } else
            sb.append(m.getReturnType().getName().toString());

        logger.trace("-> new type: " + sb.toString());
        return sb.toString();
    }
*/
    public StringBuilder getFuzzyDescriptor(LinkedList<String> linkedList) {
        final String customTypeReplacement = "X";
        StringBuilder stringBuilder = new StringBuilder("(");
        for (String type : linkedList) {
            TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, type);
            IClass ct = null;
            if (t != null) {
                getsig(t, stringBuilder, type);
            }

        }
        stringBuilder.append(")");
        return stringBuilder;
    }

    public StringBuilder getFuzzyDescriptor(Invoke invoke) {
        final String customTypeReplacement = "X";
        StringBuilder stringBuilder = new StringBuilder();
        TypeReference tc = TypeReference.findOrCreate(ClassLoaderReference.Application, invoke.clazzName);
        getsig(tc, stringBuilder, invoke.clazzName);
        stringBuilder.append("(");
        for (String type : invoke.describerList) {
            TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, type);
            IClass ct = null;
            if (t != null) {
                getsig(t, stringBuilder, type);
            }

        }
        stringBuilder.append(")");
        return stringBuilder;
    }

    private void getsig(TypeReference t, StringBuilder stringBuilder, String type) {
        final String customTypeReplacement = "X";
        IClass ct = null;
        if (t.getClassLoader().equals(ClassLoaderReference.Application))
            ct = IClassHierarchy.lookupClassRecursive(t, config.map, config.iClassLoader);
        if (t.getClassLoader().equals(ClassLoaderReference.Primordial)) {
            stringBuilder.append(type);
            return;
        }
        stringBuilder.append(ct == null ? customTypeReplacement : type);
    }

}
