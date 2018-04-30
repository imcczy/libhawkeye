package com.imcczy.libds.utils;

/**
 * Created by imcczy on 2017/6/3.
 */
public class Clear implements Runnable{
    @Override
    public void run(){
        while (true){
            try {

                Thread.sleep(30000);
                System.gc();
                System.out.println("gccccccccccccccccccc");
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
