package com.imcczy.libds.utils;

import java.io.Serializable;

/**
 * Created by imcczy on 2017/4/14.
 */
public class MutableInterger implements Serializable{
    private static final long serialVersionUID = 4362501402930667374L;
    private int val;

    public MutableInterger(int val){
        this.val = val;
    }

    public int get(){
        return val;
    }

    public void set(int val){
        this.val = val;
    }
}
