# LibHawkeye
LibHawkeye is a fast and precise analysis tool to detect third-party libraries in Android based on [WALA](https://github.com/wala/WALA) and [LibSout](https://github.com/reddr/LibScout).

## basic usage

```
usage: libds --opmode [PARSE|CLUSTER|MATCH] <options>
 -a,--android <file>             path to android.jar
 -d,--log-dir <directory>        path to store the log
 -o,--opmode <value>             mode of operation, one of
                                 [PARSE|CLUSTER|MATCH]
 -p,--profiles-dir <directory>   path to store the app profile
 -r,--runmode <value>            the run mode default serial(or parallel)
```

-----------------

## Getting Started for library matching

1.  Download third-party library database form [Dropbox](https://www.dropbox.com/sh/z1vvpuiqd8ynlqm/AABqMT8wgMF1Sp802YbuKccAa?dl=0)
2.  Require Redis >= 4.0 and run redis-server with redis/*.conf
3.  java -jar libds.jar


## Example 

   java -jar libds.jar --opmode match -a path-to-android.jar -p path-to-profiles -d path-to-log -r serial path-to-apk


