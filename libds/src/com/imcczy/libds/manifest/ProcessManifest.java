package com.imcczy.libds.manifest;


import javax.swing.text.Style;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.imcczy.libds.manifest.AndroidManifestConstants.*;

/**
 * Created by imcczy on 2017/3/26.
 */

/**
 * <pre>
 * Binary AndroidManifest.xml
 * Magic Number(0x00080003)   4bytes
 * File Size                  4bytes
 * String Chunk
 *   Chunk Type(0x001C0001)   4bytes
 *   Chunk Size               4bytes
 *   String Count             4bytes
 *   Style Count              4bytes
 *   Unknown                  4bytes
 *   String Pool Offset       4bytes
 *   Style Pool Offset        4bytes
 *   String Offsets           4bytes * StringCount
 *   Style Offsets            4bytes * StyleCount
 *   String Pool *
 *     String Length          2bytes
 *     String Content         2bytes * StringLength
 *     \0                     2bytes
 *   Style Pool
 * ResourceId Chunk
 *   Chunk Type(0x00080180)   4bytes
 *   Chunk Size               4bytes
 *   ResourceIds              4bytes * (ChunkSize / 4 - 2)
 * XmlContent Chunk *
 *   Start Namespace Chunk
 *     Chunk Type(0x00100100) 4bytes
 *     Chunk Size             4bytes
 *     Line Number            4bytes
 *     Unknown                4bytes
 *     Prefix                 4bytes
 *     Uri                    4bytes
 *   End Namespace Chunk
 *     Chunk Type(0x00100101) 4bytes
 *     Chunk Size             4bytes
 *     Line Number            4bytes
 *     Unknown                4bytes
 *     Prefix                 4bytes
 *     Uri                    4bytes
 *   Start Tag Chunk
 *     Chunk Type(0x00100102) 4bytes
 *     Chunk Size             4bytes
 *     Line Number            4bytes
 *     Unknown                4bytes
 *     Namespace Uri          4bytes
 *     Name                   4bytes
 *     Flags                  4bytes
 *     Attribute Count        4bytes
 *     Class Attribute        4bytes
 *     Attributes *
 *       Namespace Uri        4bytes
 *       Name                 4bytes
 *       Value                4bytes
 *       Type                 4bytes
 *       Data                 4bytes
 *   End Tag Chunk
 *     Chunk Type(0x00100103) 4bytes
 *     Chunk Size             4bytes
 *     Line Number            4bytes
 *     Unknown                4bytes
 *     Namespace Uri          4bytes
 *     Name                   4bytes
 *   Text Chunk
 *     Chunk Type(0x00100104) 4bytes
 *     Chunk Size             4bytes
 *     Line Number            4bytes
 *     Unknown                4bytes
 *     Name                   4bytes
 *     Unknown                4bytes
 *     Unknown                4bytes
 * </pre>
 */
public class ProcessManifest {

    private String packageName = "";
    private String versionCode = "";
    private String versionName = "";
    private String activityName = "";
    private String tmp = "";
    private boolean isActivity = false;
    private Path apkPath = null;
    private int stringChunkOffset;
    private int stringPoolOffset;
    private int stringOffset;
    private boolean isUTF16 = true;
    private Map<String, String> prefixMapping = new HashMap<>();



/*
    public ProcessManifest(Path path){

        this.apkPath = path;

    }
*/

    public void parse(ByteBuffer buffer) throws IOException {
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Ensure LITTLE_ENDIAN
        buffer.getInt(); // Magic
        buffer.getInt(); // File Size
        String tagName = "";
        String endTagName = "";
        boolean isOver = false;
        while (buffer.hasRemaining()) {
            if (isOver)
                break;
            buffer.mark();
            int chunkType = buffer.getInt(); // Chunk Type
            buffer.reset();
            switch (chunkType) {
                case CHUNK_STRING:
                    parseStringChunk(buffer);
                    break;
                case CHUNK_RESOURCE_ID:
                    parseResourceIdChunk(buffer);
                    break;
                case CHUNK_START_NAMESPACE:
                    parseStartNamespaceChunk(buffer);
                    break;
                case CHUNK_END_NAMESPACE:
                    parseEndNamespaceChunk(buffer);
                    break;
                case CHUNK_START_TAG:
                    tagName = parseStartTagChunk(buffer);
                    if ("activity".equals(tagName)) {
                        isActivity = true;
                        this.activityName = tmp;
                        isOver = parseActivity(buffer);
                    }
                    isActivity = false;
                    //System.out.println(tagName);
                    break;
                case CHUNK_END_TAG:
                    endTagName = parseEndTagChunk(buffer);
                    //System.out.println(endTagName);
                    break;
                case CHUNK_TEXT:
                    parseTextChunk(buffer);
                    break;
                default:
                    assert false;
            }
        }
    }

    private boolean parseActivity(ByteBuffer buffer) throws IOException {
        String tagName = "";
        String endTagName = "";
        boolean isMain = false;
        boolean isLaucher = false;
        boolean reslut = false;
        while (buffer.hasRemaining()) {
            if ("/activity".equals(endTagName)) {
                if (isLaucher && isMain) {
                    reslut = true;
                }
                break;
            }
            buffer.mark();
            int chunkType = buffer.getInt(); // Chunk Type
            buffer.reset();
            switch (chunkType) {
                case CHUNK_STRING:
                    parseStringChunk(buffer);
                    break;
                case CHUNK_RESOURCE_ID:
                    parseResourceIdChunk(buffer);
                    break;
                case CHUNK_START_NAMESPACE:
                    parseStartNamespaceChunk(buffer);
                    break;
                case CHUNK_END_NAMESPACE:
                    parseEndNamespaceChunk(buffer);
                    break;
                case CHUNK_START_TAG:
                    tagName = parseStartTagChunk(buffer);
                    if ("action".equals(tagName)) {
                        if ("android.intent.action.MAIN".equals(tmp)) {
                            isMain = true;
                        }
                    }
                    if ("category".equals(tagName)) {
                        if ("android.intent.category.LAUNCHER".equals(tmp)) {
                            isLaucher = true;
                        }
                    }
                    //System.out.println(tagName);
                    break;
                case CHUNK_END_TAG:
                    endTagName = parseEndTagChunk(buffer);
                    //System.out.println(endTagName);
                    break;
                case CHUNK_TEXT:
                    parseTextChunk(buffer);
                    break;
                default:
                    assert false;
            }
        }
        return reslut;
    }

    private void parseStringChunk(ByteBuffer buffer) {
        this.stringChunkOffset = buffer.position();
        buffer.getInt(); // Chunk Type
        int chunkSize = buffer.getInt(); // Chunk Size
        int stringCount = buffer.getInt(); // String Count
        buffer.getInt(); // Style Count
        buffer.getInt(); // Unknown
        this.stringPoolOffset = buffer.getInt(); // String Pool Offset
        buffer.getInt(); // Style Pool Offset
        this.stringOffset = buffer.position();
        buffer.position(this.stringChunkOffset + this.stringPoolOffset);
        if (buffer.get() == buffer.get()) {
            isUTF16 = false;
        }
        buffer.position(this.stringChunkOffset + chunkSize);
    }

    private void parseResourceIdChunk(ByteBuffer buffer) {
        // Skip
        int chunkOffset = buffer.position();
        buffer.getInt(); // Chunk Type
        int chunkSize = buffer.getInt(); // Chunk Size
        buffer.position(chunkOffset + chunkSize);
    }

    private void parseStartNamespaceChunk(ByteBuffer buffer) {
        buffer.getInt(); // Chunk Type
        buffer.getInt(); // Chunk Size
        buffer.getInt(); // Line Number
        buffer.getInt(); // Unknown
        int prefix = buffer.getInt(); // Prefix
        int uri = buffer.getInt(); // Uri

        prefixMapping.put(getString(uri, buffer), getString(prefix, buffer));
    }

    private void parseEndNamespaceChunk(ByteBuffer buffer) {
        // Chunk Type
        // Chunk Size
        // Line Number
        // Unknown
        // Prefix
        // Uri
        buffer.position(buffer.position() + 24);
    }

    private String parseStartTagChunk(ByteBuffer buffer) {
        buffer.getInt(); // Chunk Type
        buffer.getInt(); // Chunk Size
        buffer.getInt(); // Line Number
        buffer.getInt(); // Unknown
        int namespaceUri = buffer.getInt(); // Namespace Uri
        int name = buffer.getInt(); // Name
        buffer.getInt(); // Flags
        int attributeCount = buffer.getInt(); // Attribute Count
        buffer.getInt(); // Class Attribute
        // Attributes
        for (int i = 0; i < attributeCount; i++) {
            parseAttribute(buffer);
        }
        String uri = getUri(namespaceUri, buffer);
        String localName = getLocalName(name, buffer);
        String qName = getQName(uri, localName);
        return qName;
    }

    private void parseAttribute(ByteBuffer buffer) {
        int namespaceUri = buffer.getInt(); // Namespace Uri
        int name = buffer.getInt(); // Name
        int string = buffer.getInt(); // Value String
        int type = buffer.getInt(); // Type

        String attributeValue;
        if (type == ATTRIBUTE_VALUE_TYPE_STRING) {
            buffer.getInt(); // Data
            attributeValue = getString(string, buffer);
        } else {
            attributeValue = parseAttributeValue(type, buffer);
        }

        String uri = getUri(namespaceUri, buffer);
        String localName = getLocalName(name, buffer);
        String qName = getQName(uri, localName);
        if (qName.equals("package"))
            this.packageName = attributeValue;
        if (qName.equals("android:versionCode"))
            this.versionCode = attributeValue;
        if (qName.equals("android:versionName"))
            this.versionName = attributeValue;
        if (qName.equals("android:name"))
            this.tmp = attributeValue;
    }

    private String getString(int index, ByteBuffer buffer) {
        if (index < 0)
            return null;
        else {
            int mark = buffer.position();
            int stringindex = stringChunkOffset + stringPoolOffset + getStringOffset(index * 4, buffer);
            String string = parseString(stringindex, buffer);
            buffer.position(mark);
            return string;
        }
    }

    private int getStringOffset(int index, ByteBuffer buffer) {
        int mark = buffer.position();
        buffer.position(stringOffset + index);
        int stringOffset = buffer.getInt();
        buffer.position(mark);
        return stringOffset;
    }

    private String getUri(int index, ByteBuffer buffer) {
        String uri = getString(index, buffer);
        return uri == null ? "" : uri;
    }

    private String getLocalName(int index, ByteBuffer buffer) {
        return getString(index, buffer);
    }

    private String getQName(String uri, String localName) {
        if (uri.isEmpty()) {
            return localName;
        }
        return prefixMapping.get(uri) + ":" + localName;
    }

    private String parseAttributeValue(int type, ByteBuffer buffer) {
        switch (type) {
            case ATTRIBUTE_VALUE_TYPE_ID_REF: {
                return String.format("@id/0x%08X", buffer.getInt());
            }
            case ATTRIBUTE_VALUE_TYPE_ATTR_REF: {
                return String.format("?id/0x%08X", buffer.getInt());
            }
            case ATTRIBUTE_VALUE_TYPE_FLOAT: {
                return Float.toString(buffer.getFloat());
            }
            case ATTRIBUTE_VALUE_TYPE_DIMEN: {
                int data = buffer.getInt();
                return Integer.toString(data >> 8) + DIMEN[data & 0xFF];
            }
            case ATTRIBUTE_VALUE_TYPE_FRACTION: {
                return String.format("%.2f%%", buffer.getFloat());
            }
            case ATTRIBUTE_VALUE_TYPE_INT: {
                return Integer.toString(buffer.getInt());
            }
            case ATTRIBUTE_VALUE_TYPE_FLAGS: {
                return String.format("0x%08X", buffer.getInt());
            }
            case ATTRIBUTE_VALUE_TYPE_BOOL: {
                return Boolean.toString(buffer.getInt() != 0);
            }
            case ATTRIBUTE_VALUE_TYPE_COLOR:
            case ATTRIBUTE_VALUE_TYPE_COLOR2: {
                return String.format("#%08X", buffer.getInt());
            }
            default: {
                return String.format("%08X/0x%08X", type, buffer.getInt());
            }
        }
    }

    private String parseEndTagChunk(ByteBuffer buffer) {
        // Chunk Type
        // Chunk Size
        // Line Number
        // Unknown
        // Namespace Uri
        // Name
        buffer.getInt(); // Chunk Type
        buffer.getInt(); // Chunk Size
        buffer.getInt(); // Line Number
        buffer.getInt(); // Unknown
        int namespaceUri = buffer.getInt(); // Namespace Uri
        int name = buffer.getInt(); // Name
        String uri = getUri(namespaceUri, buffer);
        String localName = getLocalName(name, buffer);
        String qName = getQName(uri, localName);
        return "/" + qName;
        //buffer.position(buffer.position()+24);
    }

    private void parseTextChunk(ByteBuffer buffer) {
        buffer.position(buffer.position() + 28);
    }

    private String parseString(int offset, ByteBuffer buffer) {
        //before android studio 3,the manifest encoding is  UTF16 LE
        //after android studio 3.x,the manifest encoding is  UTF-8
        if (isUTF16) {
            buffer.position(offset);
            short stringLength = buffer.getShort(); // String Length
            byte[] string = new byte[stringLength * 2]; // UTF16 LE
            buffer.get(string);
            buffer.getShort(); // 00 00
            return new String(string, StandardCharsets.UTF_16LE);
        }else {
            buffer.position(offset);
            byte b1 = buffer.get();
            byte b2 = buffer.get();
            short stringLength = b1;
            byte[] string = new byte[stringLength]; // UTF16 LE
            buffer.get(string);
            buffer.get(); // 00 00
            return new String(string, StandardCharsets.UTF_8);
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getActivityName() {
        return activityName;
    }
}
