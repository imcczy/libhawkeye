package com.imcczy.libds;

import com.imcczy.libds.signature.HashImpl;
import com.imcczy.libds.signature.IHash;
import com.imcczy.libds.stats.ApkStats;
import com.imcczy.libds.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by imcczy on 2017/11/3.
 */
public class LibCluster {
    private static final Logger logger = LoggerFactory.getLogger(com.imcczy.libds.LibCluster.class);
    private List<Path> list;

    public LibCluster(List<Path> list) {
        this.list = list;
    }

    public void start() throws Exception {
        //map {libhash:count}
        Jedis hashCount = new Jedis("localhost", 6379);
        //map {libhash:{apk sha256}}
        Jedis libApk = new Jedis("localhost", 16379);
        //map {libhash:{treehash:count}}
        Jedis libtrees = new Jedis("localhost", 16379);
        //map {treehash:tree string}
        Jedis treehash = new Jedis("localhost", 26379);
        //1:map {apkhash:apk package name}
        //2:map {apkhash:mainActivityName}
        //Jedis mainActivityName = new Jedis("localhost", 26379);

        libtrees.select(1);
        libApk.select(0);
        //mainActivityName.select(2);


        Pipeline pipehashCount = hashCount.pipelined();
        Pipeline pipelibApk = libApk.pipelined();
        Pipeline pipetreehash = treehash.pipelined();
        Pipeline pipelibtrees = libtrees.pipelined();

        IHash hashFunc = new HashImpl("MD5");

        first(list, pipelibApk, pipehashCount, pipetreehash, pipelibtrees, hashFunc);
        //second(list,hashFunc);
    }

    public void start2() throws Exception{
        IHash hashFunc = new HashImpl("MD5");
        second(list,hashFunc);
    }


    private void first(List<Path> list, Pipeline pipelibApk, Pipeline pipehashCount
            , Pipeline pipetreehash, Pipeline pipelibtrees, IHash hashFunc) {
        list.parallelStream().forEach(path -> {


            //libHandler.initlog();

            try {
                ApkStats apkStatsnew = (ApkStats) Utils.disk2Object(path);
                LibHandler libHandler = new LibHandler(apkStatsnew);
                libHandler.init();
                HashMap<byte[], String> lib = libHandler.calc();
                byte[] apkhash = Utils.hexStringToByteArray(path.getFileName().toString().split("\\.")[0]);
                synchronized (this) {
                    lib.forEach((bytes, tree) -> {
                        byte[] tHash = hashFunc.hash(tree);

                        pipelibApk.rpush(bytes, apkhash);
                        pipehashCount.incr(bytes);
                        pipetreehash.set(tHash, tree.getBytes());
                        pipelibtrees.hincrBy(bytes, tHash, 1);
                    });

                    pipehashCount.sync();
                    pipelibApk.sync();
                    pipetreehash.sync();
                    pipelibtrees.sync();
                    //if (apkStatsnew.mainActivityName!=null)
                    //    mainActivityName.set(apkhash,apkStatsnew.mainActivityName.getBytes());
                }
            } catch (Exception e) {
                System.out.println(path.getFileName().toString());
                e.printStackTrace();
            }
        });
    }

    public void second(List<Path> list, IHash hashFunc) throws IOException {
        Jedis jedis = new Jedis("localhost", 36379);
        Pipeline pipeline = jedis.pipelined();
        list.parallelStream().forEach(path -> {

            try {
                ApkStats apkStatsnew = (ApkStats) Utils.disk2Object(path);
                LibHandler libHandler = new LibHandler(apkStatsnew);
                libHandler.init();
                Map<byte[],byte[]> lib = libHandler.calc2();
                byte[] apkhash = Utils.hexStringToByteArray(path.getFileName().toString().split("\\.")[0]);
                synchronized (this) {
                    lib.entrySet().forEach(entry -> {
                        pipeline.hset(apkhash,entry.getKey(),entry.getValue());
                    });
                    pipeline.sync();
                }
            }catch (Exception e){
                System.out.println(path.getFileName().toString());
                e.printStackTrace();
            }

        });
    }
}

