package com.imcczy.libds.graph;

import com.imcczy.libds.CLI;
import com.imcczy.libds.pkg.PackageUtils;
import com.imcczy.libds.signature.Hash;
import com.imcczy.libds.stats.ApkStats;
import com.imcczy.libds.utils.Edge;
import com.imcczy.libds.utils.MutableInterger;
import com.imcczy.libds.utils.Utils;
import com.imcczy.libds.utils.WalaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by imcczy on 2017/4/27.
 */
/*
 * There is none app vertex in Edges
 */
public class ConnectedComponent implements Serializable{
    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.graph.ConnectedComponent.class);
    private static final long serialVersionUID = 2154734335275663529L;
    //all vertex come from all edges
    private HashSet<String> allVertex = new HashSet<>();
    //all third lib's vertex(package)
    private HashSet<String> thirdLibVertex = new HashSet<>();
    //all vertex that is called
    public TreeSet<String> calledVertex = new TreeSet<>();
    // edge between third lib packages
    private HashSet<Edge> thirdLibEdge = new HashSet<>();

    public TreeSet<Edge> calledEdge = new TreeSet<>();

    //weakly connected union{index:union}
    public HashMap<Integer,TreeSet<String>> ccmap = new HashMap<>();
    //package's index
    public HashMap<String,MutableInterger> connectComponentMap = new HashMap<>();
    public ConnectedComponent(ApkStats apkStats){
        int i = 0,ii= 0;
        for (Edge edge: apkStats.dependenceTreeMap.keySet()){
            allVertex.add(edge.getS());
            allVertex.add(edge.getD());
            logger.debug(edge.toString()+","+ i++ +","+apkStats.dependenceTreeMap.get(edge).get());
        }
        allVertex.remove("com");
        allVertex.remove("net");
        allVertex.remove("io");
        allVertex.remove("me");
        //logger.debug("---------------------------\n");
        /*
        HashSet<Node> test = new HashSet<>();
        apkStats.node.childs.forEach(node -> {
            if (!allVertex.contains(((PackageNode)node).packageName))
                test.add(node);
        });
        */

        String ParentpackgeName = filter(2,apkStats.pcakageName);
        String mainActivityName = apkStats.mainActivityName != null?filter(2,apkStats.mainActivityName):null;
        for (Edge edge: apkStats.dependenceTreeMap.keySet()) {
            // filter caller
            if (edge.getS().startsWith(ParentpackgeName)) {
                calledVertex.add(edge.getD());
                if ( !edge.getSubS(2).equals(edge.getSubD(2)))
                    calledEdge.add(edge);
                continue;
            }

            // filter callee
            if (edge.getD().startsWith(ParentpackgeName))
                continue;

            // mainActivityName is diffierent from packageName
            /*
            if (mainActivityName != null && !mainActivityName.equals(ParentpackgeName)){
                if (edge.getS().startsWith(mainActivityName)) {
                    calledVertex.add(edge.getD());
                    if ( !edge.getSubS(2).equals(edge.getSubD(2)))
                        calledEdge.add(edge);
                    continue;
                }

                // filter callee
                if (edge.getD().startsWith(mainActivityName))
                    continue;
            }*/

            // filter root nodes
            if ("".equals(edge.getS()) || Utils.isTLD(edge.getS())){
                calledVertex.add(edge.getD());
                continue;
            }
            if ("".equals(edge.getD()) || Utils.isTLD(edge.getD())){
                continue;
            }
            //if the first level package name is same,continue
            //e.g: okhttps and okio
            if (!edge.getSubS(1).equals(edge.getSubD(1))){
                calledVertex.add(edge.getD());
                calledEdge.add(edge);
                continue;
            }
            //e.g: com.facebook and com.github
            if (!allVertex.contains(edge.getSubS(1)) && Utils.isTLD(edge.getSubS(1)) &&
                    !edge.getSubS(2).equals(edge.getSubD(2))){
                calledVertex.add(edge.getD());
                calledEdge.add(edge);
                continue;
            }
            //logger.info(edge.toString()+","+ii+","+(apkStats.dependenceTreeMap.get(edge).get()));
            //logger.info(edge.toString() + "[label=" + apkStats.callListMap.get(edge).get() + "]");
            ii++;
            // the rests are identified third lib packages and internal edge
            thirdLibVertex.add(edge.getS());
            thirdLibVertex.add(edge.getD());
            thirdLibEdge.add(edge);
        }
        thirdLibVertex.forEach(s -> connectComponentMap.put(s,new MutableInterger(connectComponentMap.size())));
        thirdLibEdge.forEach(edge -> connect(edge));
        connectComponentMap.forEach((s, mutableInterger) -> ccmap.put(mutableInterger.get(),new TreeSet<String >()));
        connectComponentMap.forEach((s, mutableInterger) -> ccmap.get(mutableInterger.get()).add(s));
    }

    // quick-find alg
    // todo quick-union alg ?
    private void connect(Edge edge){
        int sID = connectComponentMap.get(edge.getS()).get();
        int dID = connectComponentMap.get(edge.getD()).get();
        if (sID == dID) return;
        connectComponentMap.forEach((s, mutableInterger) -> {
            if (mutableInterger.get()==dID) mutableInterger.set(sID);
        });
    }
    private String filter(int level,String string){
        String[] names = string.split("\\.");
        if (names.length <= 2)
            return string;
        else{
            return names[0] + "." + names[1];
        }
    }
}
