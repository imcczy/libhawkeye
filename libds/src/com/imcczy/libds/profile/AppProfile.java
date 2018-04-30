/*
 * Copyright (c) imcczy
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
import com.imcczy.libds.signature.HashTree;
import com.imcczy.libds.pkg.PackageTree;
import com.imcczy.libds.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class AppProfile extends Profile implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.profile.AppProfile.class);
    private static final long serialVersionUID = -3672939629964418240L;

    public AppProfile(PackageTree pTree, HashTree hTree, NodeHashTree nTree) {
        super(pTree, hTree, nTree);
    }

    public static AppProfile create(IClassHierarchy cha) throws NoSuchAlgorithmException {
        long startTime = System.currentTimeMillis();

        // generate app package tree
        PackageTree ptree = Profile.generatePackageTree(cha);
        logger.info("- generated app package tree (in " + Utils.millisecondsToFormattedTime(System.currentTimeMillis() - startTime) + ")");
        logger.info("");

        // generate app hash trees
        startTime = System.currentTimeMillis();
        HashTree hTree = Profile.generateHashTrees(cha);
        logger.info("- generated app hash trees (in " + Utils.millisecondsToFormattedTime(System.currentTimeMillis() - startTime) + ")");
        logger.info("");

        // generate app node hash trees
        startTime = System.currentTimeMillis();
        NodeHashTree nTree = Profile.generateNodeHashTrees(hTree);
        logger.info("- generated app node hash trees (in " + Utils.millisecondsToFormattedTime(System.currentTimeMillis() - startTime) + ")");
        logger.info("");
        AppProfile appProfile = new AppProfile(null, hTree, nTree);

        return appProfile;
    }


    public static AppProfile create(Collection<IClass> iClasses, Map<TypeReference, ClassHierarchy.Node> map, IClassLoader iClassLoader) throws NoSuchAlgorithmException {
        //long startTime = System.currentTimeMillis();
        //startTime = System.currentTimeMillis();
        //PackageTree ptree = Profile.generatePackageTree(iClasses);
        HashTree hTree = Profile.generateHashTrees(iClasses, map, iClassLoader);
        //logger.info("- generated app hash trees (in " + Utils.millisecondsToFormattedTime(System.currentTimeMillis() - startTime) + ")");
        //logger.info("");

        // generate app node hash trees
        //startTime = System.currentTimeMillis();

        AppProfile appProfile = new AppProfile(null, hTree, null);

        return appProfile;
    }
}
