package storage.util;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;
import info.smart_tools.smartactors.core.IObject;
import info.smart_tools.smartactors.core.ListField;

/**
 * Информационные поля файла виртуальной директории
 */
public class FileInfoFields {

    public static final Field<IObject> FILE_INFO = new Field<>(new FieldName("fileInfo"));
    public static final Field<IObject> PART_INFO = new Field<>(new FieldName("partInfo"));
    public static final Field<Integer> FILE_SIZE = new Field<>(new FieldName("fileSize"));
    public static final Field<Integer> PARTS_QUANTITY = new Field<>(new FieldName("partsQuantity"));
    public static final Field<Integer> PART_SIZE = new Field<>(new FieldName("partSize"));
    public static final Field<Boolean> ACTIVE = new Field<>(new FieldName("active"));
    public static final Field<String> SERVER_GUID = new Field<>(new FieldName("serverGuid"));
    public static final Field<String> LOGIC_PATH = new Field<>(new FieldName("logicPath"));
    public static final Field<String> PHYSIC_PATH = new Field<>(new FieldName("phyPath"));
    public static final Field<String> ORIGINAL_NAME = new Field<>(new FieldName("originalName"));
    public static final Field<String> PART = new Field<>(new FieldName("part"));
    public static final Field<Integer> PART_NUMBER = new Field<>(new FieldName("partNumber"));
    public static final Field<Boolean> IS_FOLDER = new Field<>(new FieldName("isFolder"));
}
