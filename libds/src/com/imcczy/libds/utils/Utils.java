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

package com.imcczy.libds.utils;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Some random utility functions
 *
 * @author Erik Derr
 */
public class Utils {

    // indent for various kinds of messages
    public static final String INDENT = "    ";
    public static final String INDENT2 = INDENT + INDENT;
    public static final String[] INDENTATION;
    public static final String[] g = {
            "06AE3BDC4C1F0C99EC1EEED96636DFF9D3D38CC43CE29BD5645C320ECDF5B1AE",
            "0D600B7B9CD81F3C6D0723D49CA458854B88D247E0AF4D8FBDBC60724DCFBD7C",
            "141274F401DA07F8CF5911E1A616C5DE867DB056EE58D775F7841261B40A4900",
            "1AC0B4AE73E23BD2F310337EB5BBB8E30D3CC24ABA2D03A84D901930BC15DDA4",
            "21744A8B41C82FBB15AF4D451BA6171107CB754BCCF9CCC326CFDAD952126AD9",
            "28294E7A229B34FB2D6072E8B8FD9322D63ACC85738CEC1DE8201774A4FD2DE8",
            "2EDE839A4E86667D57B1A9AF4E135C10B3B51BDBAE100B699078768102CE5FB6",
            "358BFFDCD778EB0C440823B13CA737ED6122E0DBEBE03927DB80C10AB7A6EAC1",
            "3C3AA166A0D3FCA1A52159A2723E6A8805E0445F454A30ACE00A73A28D10CB14",
            "42F60DA7AF77BE5F4A1F2124D845AAA6F5A27B68FB0DC847F9A2E910EFA22FDE",
            "49A9AF7571527B52B612D12192D5BACFDCA1AB8A5E928A87CCA4698859329936",
            "505CAB6874249A6EFDB6A3F316972B3085782520089DACC93EC4FCDEC8C19AD8",
            "570E1CC55969BBD5676E6089E876974FE2B8099B5BE4E17850BFDBBB095F2AE4",
            "5DBFEDA8E7BDA01A74A4F15B901847D5A44D4464915B9320F5A142233DF86E6D",
            "646C5BA9EF606D9F5C61F031CB4E9977AC5405DF5BB9C007DF74A4DC56729117",
            "6B19FDB1BFAA5E30728EB098CBAC46E3E6CB5975747738E369142DB807B249CF",
            "71C6542786814D743916771599A61B141A614B46F693752D135091149FB691AD",
            "786F49051C72F11615055A98E5D7264770B856C9060DA844C7F4FD28424F0024",
            "7F24639E11F50D201158B92C197628F644D3AC118C10329F7D13D3EAB40F0BF8",
            "85CE4B09E94DA3EDCBA1CAFE8371799BAA6B2303F54D9BEE1F95441BD1CAF920",
            "8C7A75B6A022520A7DD7CB6D3363185A519D1BB11A25DAD7523190209DF7E4D9",
            "932D30DE86BCCAC7490FD60C4E3BBC3F4D3C3D2D62E235CE845F5D9C97C15A04",
            "99D9EC55CAF125D40668763452009F989811DF3E614073AFAC57BD8578462041",
            "A08FA74D6B9E3C000A858E334B07DBFCBAFB655B506CC1A70CE5AD7A5E308BFF",
            "A74250626E327532D423A81D3C5D92488AE6FA440D7EF09BDFB6506BCD8A9AD2",
            "ADF2FBE86E7DC85214CF596A0F1B3035656E798AA6E6A3E048A63C7660DA4322",
            "B4A13B89DB92C06A8283EC5A2C73266399CE4AE36B2AC01183FCC007A6BB4BF2",
            "BB57F5C710BEF8FCC0C046AE477861C893B46D1D6A62269EC5417A42B60DB943",
            "C20B1157F9112BD60E1C437BD000DB9039176A6402C85BCBC9AA5CCA12F2E20B",
            "C8C14A84A1562B232A14830E3F1C1692147D30C5FDE3935B62FD451ECFCBD9D6",
            "CF7B241FA4138E574D276F0348BB32337EEBDD6F32386148FA4C0BB854541C0E",
            "D69A95D3B66BFDAB40662430CBE8EF356237BAD9E3284DA57FEAEB851A50A63A",
            "DD4F881B5D8FECFFC8AA356ACF9F647C6C6762313181B8F68FE84C3FA365F0C5",
            "E4002CA07317E4F90D89559F67D71131BC2E408BAA0DD45D89E5F610CF7AE271",
            "EAB243B71220A623A265CF0F1CBE650AA466C309F84FFE5346FD86E28638B7A9",
            "F167578A9D2FAB241EE8E28C8B8A5B002B8FAB9C041FC23BB7E25BA3B2AD1933",
            "F814280633ABB9C3A648D1F368C7DB5B7BF96E694202E59C4C6BEB7E5EF26AD7",
            "FEC3BFDBEDE40D90C77E9F29DD62CFA5578E088A98C769CCEB597420BD016FEF",
            "FFFFF87AB6F0F50A49621DD18C93DBA0ED31E83E07A23C0DB5649DCC8DDF7358"
    };
    public static String googleinternal = "com.google.android.gms.internal";
    public static String googleand = "com.google.and";
    public static String googleads = "com.google.ads";
    public static final String[] domain = {"com","edu","gov","int","mil","net",
            "org","biz","info","pro","name","museum","coop","aero","xxx","idv",
            "ac","ad","ae","af","ag","ai","al","am","an","ao","aq","ar","as","at",
            "au","aw","az","ba","bb","bd","be","bf","bg","bh","bi","bj","bm","bn",
            "bo","br","bs","bt","bv","bw","by","bz","ca","cc","cd","cf","cg","ch",
            "ci","ck","cl","cm","cn","co","cr","cu","cv","cx","cy","cz","de","dj",
            "dk","dm","do","dz","ec","ee","eg","eh","er","es","et","eu","fi","fj",
            "fk","fm","fo","fr","ga","gd","ge","gf","gg","gh","gi","gl","gm","gn",
            "gp","gq","gr","gs","gt","gu","gw","gy","hk","hm","hn","hr","ht","hu",
            "id","ie","il","im","in","io","iq","ir","is","it","je","jm","jo","jp",
            "ke","kg","kh","ki","km","kn","kp","kr","kw","ky","kz","la","lb","lc",
            "li","lk","lr","ls","lt","lu","lv","ly","ma","mc","md","mg","mh","mk",
            "ml","mm","mn","mo","mp","mq","mr","ms","mt","mu","mv","mw","mx","my",
            "mz","na","nc","ne","nf","ng","ni","nl","no","np","nr","nu","nz","om",
            "pa","pe","pf","pg","ph","pk","pl","pm","pn","pr","ps","pt","pw","py",
            "qa","re","ro","ru","rw","sa","sb","sc","sd","se","sg","sh","si","sj",
            "sk","sl","sm","sn","so","sr","st","sv","sy","sz","tc","td","tf","tg",
            "th","tj","tk","tl","tm","tn","to","tp","tr","tt","tv","tw","tz","ua",
            "ug","uk","um","us","uy","uz","va","vc","ve","vg","vi","vn","vu","wf",
            "ws","ye","yt","yu","yr","za","zm","zw","me","edu","ti"};

    private static final Set<String> domainSet = new HashSet<>(Arrays.asList(domain));

    public enum LOGTYPE {NONE, CONSOLE, FILE}

    ;

    static {
        INDENTATION = new String[11];
        String curIndent = "";
        for (int i = 0; i < 11; i++) {
            INDENTATION[i] = curIndent;
            curIndent += INDENT;
        }
    }

    public static String indent() {
        return indent(1);
    }

    public static String indent(int indentLevel) {
        indentLevel = Math.min(indentLevel, 10);
        indentLevel = Math.max(0, indentLevel);
        return INDENTATION[indentLevel];
    }


    /**
     * Converts class name in dex bytecode notation to fully-qualified class name
     *
     * @param className in dex bytcode notation, e.g. "Lcom/ebay/motors/garage/myvehicles/GarageInsertActivity;"
     * @return className fully-qualified class name, e.g. "com.ebay.motors.garage.myvehicles.GarageInsertActivity"
     */
    public static String convertToFullClassName(String className) {
        if (className.startsWith("L")) className = className.substring(1);
        if (className.endsWith(";")) className = className.substring(0, className.length() - 1);

        return className.replaceAll("/", "\\.");
    }


    /**
     * Converts fully-qualified class name to class name in dex bytecode notation
     *
     * @param className fully-qualified class name, e.g. "com.motors.myvehicles.GarageInsertActivity"
     * @return class name in broken dex bytcode notation (trailing ";" is missing), e.g. "Lcom/motors/myvehicles/GarageInsertActivity"
     * @deprecated once this classname notation mess in the dex frontend is fixed
     */
    public static String convertToBrokenDexBytecodeNotation(String className) {
        if (className == null) return null;
        return className.startsWith("L") ? className : "L" + className.replaceAll("\\.", "/");
    }


    public static String convertToDexBytecodeNotation(String typeName) {
        if (typeName.isEmpty()) return typeName;

        // check if type is array
        int dimension = 0;
        while (typeName.endsWith("[]")) {
            typeName = typeName.substring(0, typeName.length() - 2);
            dimension++;
        }

        if (TOVARTYPES.containsKey(typeName))
            typeName = TOVARTYPES.get(typeName).toString();
        else
            typeName = "L" + typeName.replaceAll("\\.", "/") + ";";

        for (int i = 0; i < dimension; i++)
            typeName = "[" + typeName;

        return typeName;
    }


    /**
     * Checks whether a given method is a framework method
     *
     * @param methodSignature
     * @return true if it's a framework method, false otherwise
     */
    // TODO spaghetti code, to be rewritten
    public static boolean isFrameworkCall(String methodSignature) {
        if (methodSignature.startsWith("java.") ||    // java packages
                methodSignature.startsWith("Ljava/") ||    // java packages

                methodSignature.startsWith("javax.") ||    // javax packages
                methodSignature.startsWith("Ljavax/") ||    // javax packages

                methodSignature.startsWith("junit.") ||    // junit package
                methodSignature.startsWith("Ljunit/") ||    // junit package

                methodSignature.startsWith("android.") ||    // android package
                methodSignature.startsWith("Landroid/") ||    // android package

                methodSignature.startsWith("dalvik.") ||    // dalvik package
                methodSignature.startsWith("Ldalvik/") ||    // dalvik package

                methodSignature.startsWith("org.apache.") ||    // org.apache.* package
                methodSignature.startsWith("Lorg/apache/") ||    // org.apache.* package

                methodSignature.startsWith("org.json.") ||    // org.json.* package
                methodSignature.startsWith("Lorg/json/") ||    // org.json.* package

                methodSignature.startsWith("org.w3c.dom.") ||    // W3C Java bindings for the Document Object Model
                methodSignature.startsWith("Lorg/w3c/dom/") ||    // W3C Java bindings for the Document Object Model

                methodSignature.startsWith("org.xml.sax.") ||    // core SAX APIs
                methodSignature.startsWith("Lorg/xml/sax/") ||    // core SAX APIs

                methodSignature.startsWith("org.xmlpull.v1.") ||    // XML Pull Parser
                methodSignature.startsWith("Lorg/xmlpull/v1/") ||     // XML Pull Parser

                methodSignature.startsWith("sun.") ||    // sun
                methodSignature.startsWith("Lsun/") ||    // sun

                methodSignature.startsWith("com.sun.") ||    // sun
                methodSignature.startsWith("Lcom/sun/") ||    // sun

                methodSignature.startsWith("libcore.io.") ||    //  libcore.io
                methodSignature.startsWith("Llibcore/io/") ||    // libcore.io

                methodSignature.startsWith("Lorg/omg/"))

            return true;
        else
            return false;
    }


    /**
     * Returns the full class name of a method signature
     *
     * @param methodSignature in notation "java.lang.StringBuilder.append(Ljava/lang/String;)"
     * @return the extracted class substring
     */
    public static String getFullClassName(String methodSignature) {
        int endIdx = methodSignature.indexOf("(");
        if (endIdx == -1) return methodSignature;

        String result = methodSignature.substring(0, endIdx); // strip args and return type
        return result.substring(0, result.lastIndexOf("."));
    }


    /**
     * Vartypes used in Dex bytecode and their mnemonics
     */
    public static final HashMap<Character, String> VARTYPES = new HashMap<Character, String>() {
        private static final long serialVersionUID = 1L;

        {
            put('V', "void");    // can only be used for return types
            put('Z', "boolean");
            put('B', "byte");
            put('S', "short");
            put('C', "char");
            put('I', "int");
            put('J', "long");    // 64 bits
            put('F', "float");
            put('D', "double");  // 64 bits
        }
    };

    /**
     * Mnemonics to dex bytecode vartypes
     */
    public static final HashMap<String, Character> TOVARTYPES = new HashMap<String, Character>() {
        private static final long serialVersionUID = 1L;

        {
            put("void", 'V');    // can only be used for return types
            put("boolean", 'Z');
            put("byte", 'B');
            put("short", 'S');
            put("char", 'C');
            put("int", 'I');
            put("long", 'J');    // 64 bits
            put("float", 'F');
            put("double", 'D');  // 64 bits
        }
    };


    public static boolean isPrimitiveType(String type) {
        return TOVARTYPES.containsKey(type) || VARTYPES.containsKey(type.charAt(0));
    }

    public static boolean isArrayType(String type) {
        return type.startsWith("[");
    }

    public static boolean isParameterRegister(String register) {
        return register.matches("^p\\d{1,4}$");
    }

    public static boolean isNormalRegister(String register) {
        return register.matches("^v\\d{1,5}$");
    }


    /**
     * Parses the method argument header of a dex method signature
     *
     * @param signature     method signature in dex notation
     * @param humanReadable if true it converts ther dex vartypes to human readable types
     * @return an array of (human readable) argument types
     * @deprecated use IMethodReference directly instead of parsing ourselves (arguments are already parsed in CallInstructions)
     */
    @Deprecated
    public static List<String> parseMethodArguments(String signature, boolean humanReadable) {
        ArrayList<String> result = new ArrayList<String>();

        // Parse arguments
        String args = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));
        Boolean parsingObject = false;
        String currentStr = "";
        for (char c : args.toCharArray()) {
            currentStr += c;

            if (c == 'L') { // start of class object
                parsingObject = true;
            } else if (VARTYPES.containsKey(c) && !parsingObject) {  // found var type
                result.add(humanReadable ? VARTYPES.get(c) : currentStr);
                currentStr = "";
            } else if (c == ';') {  // end of class object
                parsingObject = false;
                result.add(humanReadable ? currentStr.substring(1, currentStr.length() - 1).replaceAll("/", ".") : currentStr);
                currentStr = "";
            }
        }

        return result;
    }


    /**
     * Strips leading and trailing quotes of strings
     *
     * @param str input string
     * @return dequoted string
     */
    public static String dequote(String str) {
        if (isConstant(str)) {
            str = str.replaceFirst("\"", "");
            return str.substring(0, str.length() - 1);
        }
        return str;
    }


    /**
     * Returns a quoted String (double quote)·
     *
     * @param str input string
     * @return quoted input string
     */
    public static String quote(String str) {
        return "\"" + str + "\"";
    }

    public static String singleQuote(String str) {
        return "\'" + str + "\'";
    }

    public static String escapeQuotes(String str) {
        return str.replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'");
    }

    /**
     * Checks whether a given value is a constant.
     * Here a constant is a quoted value.
     *
     * @param val
     * @return true if val is a constant, false otherwise
     */
    public static boolean isConstant(String val) {
        return val.startsWith("\"") && val.endsWith("\"");
    }


    public static String millisecondsToFormattedTime(long milliseconds) {
        final String SEP = ", ";
        int millis = (int) milliseconds % 1000;
        int seconds = (int) (milliseconds / 1000) % 60;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        StringBuilder sb = new StringBuilder();
        sb.append(hours > 0 ? hours + " hours" + SEP : "");
        sb.append(minutes > 0 ? minutes + " min" + SEP : "");
        sb.append(seconds > 0 ? seconds + " sec" + SEP : "");
        sb.append(millis >= 0 ? millis + " ms" : "");
        return sb.toString();
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    public static float computePercentage(int first, int second) {
        if (second == 0)
            return Float.NaN;
        else
            return (float) Math.round((Float.intBitsToFloat(first) / Float.intBitsToFloat(second)) * 100 * 100) / 100;
    }

    public static double computePercentage(long first, long second) {
        if (second == 0)
            return Double.NaN;
        else
            return (double) Math.round((Double.longBitsToDouble(first) / Double.longBitsToDouble(second)) * 100 * 100) / 100;
    }

    public static float round(float number, int digits) {
        return Math.round(number * ((float) Math.pow(10, digits))) / ((float) Math.pow(10, digits));
    }

    public static double round(double number, int digits) {
        return Math.round(number * Math.pow(10, digits)) / Math.pow(10, digits);
    }


    /**
     * List join function, which creates a single string of the items of the
     * array separated by _sep. If "spaceFirst" is set to true the first two elements
     * are joined by a space character.
     *
     * @param list       the input list
     * @param _sep
     * @param spaceFirst
     * @returns a single assembled string
     */
    public static <T> String join(List<T> list, String _sep, Boolean spaceFirst) {
        String sep = "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).toString().isEmpty())
                result.append(sep + list.get(i).toString());
            sep = (i == 0 && spaceFirst) ? " " : _sep;
        }
        return result.toString();
    }

    public static <T> String join(List<T> list, String _sep) {
        return join(list, _sep, false);
    }

    public static <T> String join(List<T> list) {
        return join(list, "");
    }


    /**
     * Recursively searches a given directory for certain file types
     *
     * @param dir        the directory file
     * @param extensions an array of file extensions without leading dot
     * @return a list of {@link File}s
     */
    public static List<File> collectFiles(File dir, String[] extensions) {
        ArrayList<File> files = new ArrayList<File>();

        // gather all input files
        if (dir.isDirectory()) {
            try {
                // Finds files within a root directory and optionally its·
                // subdirectories which match an array of extensions.
                // This method will returns matched file as java.io.File
                boolean recursive = true;

                Collection<File> foundApks = FileUtils.listFiles(dir, extensions, recursive);

                for (Iterator<File> iterator = foundApks.iterator(); iterator.hasNext(); ) {
                    files.add(iterator.next());
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        return files;
    }

    /*
     * added by imcczy
     */
    public static List<Path> collectFiles(Path path, String postfix) throws IOException {
        return Files.list(path).filter(apk -> apk.toString().endsWith(postfix)).collect(Collectors.toList());
    }

    public interface IPredicate<T> {
        boolean apply(T type);
    }

    public static <T> Collection<T> filter(Collection<T> target, IPredicate<T> predicate) {
        Collection<T> result = new ArrayList<T>();
        for (T element : target) {
            if (predicate.apply(element)) {
                result.add(element);
            }
        }
        return result;
    }

    public static <T> Collection<T> filter(T[] target, IPredicate<T> predicate) {
        return filter(Arrays.asList(target), predicate);
    }


    /**
     * Serialize an Java Object to disk
     *
     * @param targetFile the {@link File} to store the object
     * @param obj        a serializable {@link Object}
     * @return true if no exception was thrown, false otherwise
     * @throws IOException
     */
    public static boolean object2Disk(final File targetFile, final Object obj) {
        File basePath = new File(targetFile.getPath().substring(0, targetFile.getPath().lastIndexOf(File.separator)));
        if (!basePath.exists()) basePath.mkdirs();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(targetFile))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            //logger.warn(Utils.stacktrace2Str(e));
            return false;
        }

        return true;
    }

    public static boolean object2Disk(final Path targetFile, final Object obj) throws IOException {
        //File basePath = new File(targetFile.getPath().substring(0, targetFile.getPath().lastIndexOf(File.separator)));
        //if (!basePath.exists()) basePath.mkdirs();

        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(targetFile))) {
            oos.writeObject(obj);
        }

        return true;
    }

    public static Object disk2Object(final Path path) throws ClassNotFoundException, IOException {
        Object obj = null;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            obj = in.readObject();
        }
        return obj;
    }

    /**
     * Deserialize a Java Object from disk
     *
     * @param file the {@link File} to read the object from
     * @return the deserialized {@link Object}
     * @throws ClassNotFoundException
     */
    public static Object disk2Object(File file) throws ClassNotFoundException {
        Object obj = null;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            obj = in.readObject();
        } catch (IOException e) {
            // logger.warn(Utils.stacktrace2Str(e));
        }

        return obj;
    }

    public static String stacktrace2Str(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString(); // stack trace as a string
    }

    public static <T extends Serializable> T clone(T obj) {
        T cloneObj = null;
        try {
            //写入字节流
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream obs = new ObjectOutputStream(out);
            obs.writeObject(obj);
            obs.close();

            //分配内存，写入原始对象，生成新对象
            ByteArrayInputStream ios = new ByteArrayInputStream(out.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(ios);
            //返回生成的新对象
            cloneObj = (T) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cloneObj;
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        try {
            byte[] buf = new byte[1024 * 8];
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                int len;
                while ((len = in.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                return bos.toByteArray();
            }
        } finally {
            in.close();
        }
    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getSub(String string, int level) {
        String[] ss = string.split("\\.");
        String tmp = "";
        for (int i = 0; i < level && i < ss.length; i++)
            tmp = tmp + ss[i] + ".";
        return tmp.substring(0, tmp.lastIndexOf("."));
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString().toUpperCase();
    }

    public static String bytesToHexlow(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

	public static boolean isTLD(String parentName) {
		return domainSet.contains(parentName);
		/*
		||parentName.equals("com")
				|| parentName.equals("org")
				|| parentName.equals("io")
				|| parentName.equals("net")
				|| parentName.equals("me")
				|| parentName.equals("uk")
				|| parentName.equals("ca")
				|| parentName.equals("de")
				|| parentName.equals("jp")
				|| parentName.equals("fr")
				|| parentName.equals("au")
				|| parentName.equals("us")
				|| parentName.equals("it")
				|| parentName.equals("nl")
		 */
	}

    public static Path getpath(String sha251) {
        int index = 0;
        for (; index < 39; index++) {
            if (sha251.compareTo(g[index]) <= 0)
                if (index <= 18)
                    return Paths.get("/media/nipc/F/googleplay" + index + "/" + sha251 + ".apk");
                else return Paths.get("/media/nipc/H/googleplay" + index + "/" + sha251 + ".apk");
        }
        return null;
    }

    public static Path getPathI(String sha251) {
        int index = 0;
        for (; index < 39; index++) {
            if (sha251.compareTo(g[index]) <= 0)

                return Paths.get("/media/nipc/I/googleplay" + index + "/" + sha251 + ".apk");

        }
        return null;
    }

    public static Path getpathmi(String sha251) {
        return Paths.get("/media/nipc/I/mi/" + sha251 + ".apk");
    }

    public static String filter(int level, String string) {
        String[] names = string.split("\\.");
        if (names.length <= 2)
            return string;
        else {
            return names[0] + "." + names[1];
        }
    }
}
