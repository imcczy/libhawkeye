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

package com.imcczy.libds.profile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import com.ibm.wala.types.TypeReference;
import com.imcczy.libds.pkg.NodeHashTree;
import com.imcczy.libds.pkg.PackageTree;
import com.imcczy.libds.signature.HashTree;
import com.imcczy.libds.signature.HashTree.HashAlgorithm;
import com.imcczy.libds.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public abstract class Profile implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.profile.Profile.class);
    private static final long serialVersionUID = -3211236315615093075L;

    // Package structure of the library|app
    public PackageTree packageTree;

    public HashTree hashTree;

    public NodeHashTree nodeHashTree;

    protected Profile(PackageTree pTree, HashTree hTree, NodeHashTree nTree) {
        this.packageTree = pTree;
        this.hashTree = hTree;
        this.nodeHashTree = nTree;
    }

    public static PackageTree generatePackageTree(IClassHierarchy cha) {
        logger.info("= PackageTree =");
        PackageTree tree = PackageTree.make(cha, true);
        tree.print(true);

        logger.debug("");
        logger.debug("Package names (included classes):");
        Map<String, Integer> pTree = tree.getPackages();
        for (String pkg : pTree.keySet())
            logger.debug(Utils.INDENT + pkg + " (" + pTree.get(pkg) + ")");

        logger.info("");

        return tree;
    }

    /*
     * added by imcczy
     */

    public static PackageTree generatePackageTree(final Collection<IClass> iClasses) {
        logger.info("= PackageTree =");
        PackageTree tree = PackageTree.make(iClasses);
        tree.print(true);

        logger.debug("");
        logger.debug("Package names (included classes):");
        Map<String, Integer> pTree = tree.getPackages();
        for (String pkg : pTree.keySet())
            logger.debug(Utils.INDENT + pkg + " (" + pTree.get(pkg) + ")");

        logger.info("");
        return tree;
    }

    /*
     * Modified by imcczy
     */

    /**
     * Generate hash trees for a certain {@link PackageTree} for all configurations
     *
     * @param cha the {@link IClassHierarchy} instance
     * @return a List of {@link HashTree} for every configuration
     */
    public static HashTree generateHashTrees(final IClassHierarchy cha) {
        final HashAlgorithm algorithm = HashAlgorithm.MD5;
        HashTree hashTree = new HashTree();
//		List<HashTree> hTrees = new ArrayList<HashTree>();
        try {
            boolean filterDups = false;
            boolean publicOnly = false;
            boolean filterInnerClasses = false;
            hashTree.generate(filterDups, publicOnly, filterInnerClasses, algorithm, cha, null,
                    null, null);
        } catch (NoSuchAlgorithmException e) {
            logger.error(Utils.stacktrace2Str(e));
        }

        return hashTree;
    }

    public static HashTree generateHashTrees(final Collection<IClass> iClasses,
                                             Map<TypeReference, ClassHierarchy.Node> map,
                                             IClassLoader iClassLoader) throws NoSuchAlgorithmException {
        final HashAlgorithm algorithm = HashAlgorithm.MD5;
        HashTree hashTree = new HashTree();
        boolean filterDups = false;
        boolean publicOnly = false;
        boolean filterInnerClasses = false;
        hashTree.generate(filterDups, publicOnly, filterInnerClasses, algorithm,
                null, iClasses, map, iClassLoader);
        return hashTree;
    }

    public static NodeHashTree generateNodeHashTrees(final HashTree hashTree) throws NoSuchAlgorithmException {
        logger.info("= NodeHashTree =");
        NodeHashTree nTree = NodeHashTree.make(hashTree);
        nTree.print();
        return nTree;
    }
}
