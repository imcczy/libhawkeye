package com.imcczy.libds.manifest;

/**
 * Created by imcczy on 2017/4/9.
 */
public class AndroidManifestConstants {
    public static final int MAGIC = 0x00080003;

    public static final int CHUNK_STRING = 0x001C0001;
    public static final int CHUNK_RESOURCE_ID = 0x00080180;
    public static final int CHUNK_START_NAMESPACE = 0x00100100;
    public static final int CHUNK_END_NAMESPACE = 0x00100101;
    public static final int CHUNK_START_TAG = 0x00100102;
    public static final int CHUNK_END_TAG = 0x00100103;
    public static final int CHUNK_TEXT = 0x00100104;

    public static final int ATTRIBUTE_VALUE_TYPE_ID_REF = 0x01000008;
    public static final int ATTRIBUTE_VALUE_TYPE_ATTR_REF = 0x02000008;
    public static final int ATTRIBUTE_VALUE_TYPE_STRING = 0x03000008;
    public static final int ATTRIBUTE_VALUE_TYPE_FLOAT = 0x04000008;
    public static final int ATTRIBUTE_VALUE_TYPE_DIMEN = 0x05000008;
    public static final int ATTRIBUTE_VALUE_TYPE_FRACTION = 0x06000008;
    public static final int ATTRIBUTE_VALUE_TYPE_INT = 0x10000008;
    public static final int ATTRIBUTE_VALUE_TYPE_FLAGS = 0x11000008;
    public static final int ATTRIBUTE_VALUE_TYPE_BOOL = 0x12000008;
    public static final int ATTRIBUTE_VALUE_TYPE_COLOR = 0x1C000008;
    public static final int ATTRIBUTE_VALUE_TYPE_COLOR2 = 0x1D000008;

    public static final String[] DIMEN = { "px", "dp", "sp", "pt", "in", "mm" };
}
