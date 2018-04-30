package com.imcczy.libds.reﬁnement;

import com.imcczy.libds.match.Identifier;
import com.imcczy.libds.utils.Utils;
import redis.clients.jedis.Jedis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by imcczy on 2018/3/6.
 */
public class Cluster2 {
    Path path = Paths.get("/Users/imcczy/Downloads/cluster2/allnew2-2.csv");
    String fd = "/Users/imcczy/Downloads/cluster2/allnew2-2-new.csv";
    String useless = "/Users/imcczy/Downloads/cluster2/usless.csv";
    private EntryComparator comp = new EntryComparator();

    public void cluster() {
        Set<ByteBuffer> uselessApk = new HashSet<>();
        //map {apk sha256:{libhash:sourcehash}}
        Jedis sourceHash = new Jedis("localhost", 36379);
        //map {libhash:{apk sha256}}
        Jedis libApk = new Jedis("localhost", 16379);
        Jedis apkName = new Jedis("localhost", 26379);
        apkName.select(1);
        try (Stream<String> stream = Files.lines(path)) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fd));
            BufferedWriter bwuse = new BufferedWriter(new FileWriter(useless));
            stream.forEach(line -> {
                Map<ByteBuffer, Integer> newmap = new HashMap<>();
                String[] tmp = line.split(",");
                byte[] libhash = Utils.hexStringToByteArray(tmp[0]);
                if (libApk.exists(libhash)) {
                    Map<ByteBuffer, List<byte[]>> mapapk = new HashMap<>();
                    List<byte[]> apks = libApk.lrange(libhash, 0, -1);
                    for (byte[] apk : apks) {
                        if (uselessApk.contains(apk))
                            continue;
                        Map<byte[], byte[]> map = sourceHash.hgetAll(apk);
                        if (!map.containsKey(libhash))
                            continue;

                        ByteBuffer bk = ByteBuffer.wrap(map.get(libhash));
                        if (!newmap.containsKey(bk))
                            newmap.put(bk, 0);
                        newmap.put(bk, newmap.get(bk) + 1);
                        if (!mapapk.containsKey(bk))
                            mapapk.put(bk, new ArrayList<>());
                        mapapk.get(bk).add(apk);
                    }
                    List<Map.Entry<ByteBuffer, Integer>> list = new ArrayList<>(newmap.entrySet());

                    // 排序
                    Collections.sort(list, comp);
                    if (list.size() > 0) {
                        /*
                        if (list.get(0).getValue() >= 2) {

                            try {
                                //System.out.println(line);
                                bw.write(line + "\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            list.remove(0);
                        }
                        */
                        Map.Entry<ByteBuffer, Integer> e = null;

                        for (Map.Entry<ByteBuffer, Integer> entry : list) {
                            if (entry.getValue()>=2){
                                if (getssize(mapapk.get(entry.getKey()),apkName)>=2){
                                    e=entry;
                                    break;
                                }
                            }
                        }
                        if (e!=null){
                            list.remove(e);
                            try {
                                //System.out.println(line);
                                bw.write(line + "\n");
                            } catch (IOException ee) {
                                ee.printStackTrace();
                            }
                        }
                        for (Map.Entry<ByteBuffer, Integer> entry : list) {
                            mapapk.get(entry.getKey()).forEach(b->{
                                uselessApk.add(ByteBuffer.wrap(b));
                                /*
                                try {
                                    bwuse.write(Utils.bytesToHexlow(b));
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                                */

                            });
                        }
                    }
                }
            });
            bw.flush();
            bw.close();
            bwuse.flush();bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Integer getssize(List<byte[]> list,Jedis apkName){
        Set<String> name = new HashSet<>();
        for (byte[] apk :list){
            if (apkName.exists(apk)){
                name.add(getName(new String(apkName.get(apk))));
            }
        }
        return name.size();
    }
    private String getName(String n){
        String[] tmp = n.split("\\.");
        if (tmp.length<=2)
            return n;
        else
            return tmp[0]+"."+tmp[1];
    }

    public class EntryComparator implements Comparator<Map.Entry<?, Integer>> {
        @Override
        public int compare(Map.Entry<?, Integer> o1,
                           Map.Entry<?, Integer> o2) {
            // TODO Auto-generated method stub
            return o2.getValue() - o1.getValue();
        }
    }
}
