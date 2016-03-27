package storage.util;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;
import info.smart_tools.smartactors.core.ListField;

/**
 * Информационные поля файла виртуальной директории
 */
public class FileInfoFields {

    public static final Field<Integer> FILE_SIZE = new Field<>(new FieldName("fileSize"));
    public static final Field<Integer> PARTS_QUANTITY = new Field<>(new FieldName("partsQuantity"));
    public static final Field<Integer> PART_SIZE = new Field<>(new FieldName("partSize"));
    public static final Field<Boolean> ACTIVE = new Field<>(new FieldName("active"));
    public static final Field<String> SERVER_GUID = new Field<>(new FieldName("serverGuid"));
    public static final Field<String> LOGIC_PATH = new Field<>(new FieldName("logicPath"));
    public static final Field<String> PHYSIC_PATH = new Field<>(new FieldName("phyPath"));
    public static final Field<String> ORIGINAL_NAME = new Field<>(new FieldName("originalName"));
    public static final ListField<Integer> PARTS_NUMBERS = new ListField<>(new FieldName("partsNumbers"));
    public static final ListField<String> FILE_PARTS = new ListField<>(new FieldName("fileParts"));

}