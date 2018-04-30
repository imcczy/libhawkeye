package com.imcczy.libds.utils;

import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by imcczy on 2017/4/14.
 */
public class Edge implements Serializable,Comparable<Edge>{
    private static final long serialVersionUID = 1840429018633793022L;
    private String source;
    private String dest;
    public Edge(String source,String dest){
        this.source = source;
        this.dest = dest;
    }

    public String getD() {
        return dest;
    }

    public String getS() {
        return source;
    }

    public void setS(String s){
        this.source = s;
    }

    public void setD(String d){
        this.source = d;
    }

    public boolean equals(Object obj){
        if (obj instanceof Edge){
            Edge edge = (Edge) obj;
            return source.equals(edge.source) && dest.equals(edge.dest);
        }
        return super.equals(obj);
    }

    public String toString(){
        return source+"->"+dest;
    }

    public int hashCode(){
        Edge edge = (Edge) this;
        return (source+dest).hashCode();
    }

    public int compareTo(Edge e){
        return toString().compareTo(e.toString());
    }

    public String getSubS(int level){
        String[] ss = source.split("\\.");
        String tmp = "";
        for (int i = 0;i < level && i < ss.length; i++)
            tmp = tmp + ss[i] + ".";
        return tmp.substring(0,tmp.lastIndexOf("."));
    }

    public String getSubD(int level){
        String[] ss = dest.split("\\.");
        String tmp = "";
        for (int i = 0;i < level && i < ss.length; i++)
            tmp = tmp + ss[i] + ".";
        return tmp.substring(0,tmp.lastIndexOf("."));
    }
}
