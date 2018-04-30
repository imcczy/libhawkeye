package com.imcczy.libds.utils;

import java.io.Serializable;

/**
 * Created by imcczy on 2017/4/26.
 */
public class Dependence implements Serializable{
    private static final long serialVersionUID = 4557863444787342029L;
    private boolean isInherite;
    private boolean isAnnotation;
    private MutableInterger callMethodCount;
    private  MutableInterger getFiledCount;

    public Dependence(){
        this.isInherite = false;
        this.callMethodCount = new MutableInterger(0);
        this.getFiledCount = new MutableInterger(0);
    }


    public MutableInterger getCallMethodCount() {
        return callMethodCount;
    }

    public void setCallMethodCount(int count) {
        callMethodCount.set(count);
    }

    public MutableInterger getGetFiledCount() {
        return getFiledCount;
    }

    public void setGetFiledCount(int count) {
        getFiledCount.set(count);
    }

    public boolean isInherite() {
        return isInherite;
    }

    public void setInherite(boolean inherite) {
        isInherite = inherite;
    }

    public int get(){
        return callMethodCount.get()*10+getFiledCount.get();
    }

    public boolean isAnnotation() {
        return isAnnotation;
    }

    public void setAnnotation(boolean annotation) {
        isAnnotation = annotation;
    }
}
