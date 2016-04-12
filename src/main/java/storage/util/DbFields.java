package storage.util;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;
import info.smart_tools.smartactors.core.ListField;

/**
 */
public class DbFields {

    public static final ListField<String> GUIDS = new ListField<>(new FieldName("guids"));
    public static final Field<Integer> ID = new Field<>(new FieldName("id"));
}
