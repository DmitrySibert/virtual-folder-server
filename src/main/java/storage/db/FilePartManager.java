package storage.db;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.actors.db_accessor.DBFields;
import info.smart_tools.smartactors.utils.ioc.IOC;
import storage.util.DbFields;
import storage.util.FileInfoFields;

import java.util.LinkedList;
import java.util.List;

/**
 * Стандартные действия с частями файла
 */
public class FilePartManager extends Actor {

    private String filePartCollectionName;
    private ListField<Integer> documentIdsF;

    public FilePartManager(IObject params) {

        documentIdsF = new ListField<>(new FieldName("documentIds"));
        try {
            filePartCollectionName = new Field<String>(new FieldName("filePartCollectionName")).from(params, String.class);
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    /**
     * Создать объект - часть файла для сохранения в БД
     * @param msg
     * @throws ChangeValueException
     * @throws ReadValueException
     */
    @Handler("createPart")
    public void createPart(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject part = IOC.resolve(IObject.class);
        FileInfoFields.SERVER_GUID.inject(part, FileInfoFields.SERVER_GUID.from(msg, String.class));
        FileInfoFields.PART_NUMBER.inject(part, FileInfoFields.PART_NUMBER.from(msg, Integer.class));
        FileInfoFields.PART.inject(part, FileInfoFields.PART.from(msg, String.class));
        List<IObject> parts = new LinkedList<>();
        parts.add(part);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, filePartCollectionName);
        DBFields.DOCUMENTS_FIELD.inject(msg, parts);
    }

    @Handler("partSearch")
    public void partSearch(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject guidCondition = IOC.resolve(IObject.class);
        DBFields.EQUALS_FIELD.inject(guidCondition, FileInfoFields.SERVER_GUID.from(msg, String.class));
        IObject numberCondition = IOC.resolve(IObject.class);
        numberCondition.setValue(new FieldName("$eq"), FileInfoFields.PART_NUMBER.from(msg, Integer.class));

        IObject andCondition = IOC.resolve(IObject.class);
        andCondition.setValue(new FieldName("serverGuid"), guidCondition);
        andCondition.setValue(new FieldName("partNumber"), numberCondition);

        IObject query = IOC.resolve(IObject.class);
        DBFields.AND_FIELD.inject(query, andCondition);
        DBFields.QUERY_FIELD.inject(msg, query);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, filePartCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 10);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
    }

    @Handler("handleSearching")
    public void handleSearching(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        IObject partInfo = DBFields.SEARCH_RESULT_FIELD.from(msg, IObject.class).get(0);
        FileInfoFields.PART_INFO.inject(msg, partInfo);
        DBFields.SEARCH_RESULT_FIELD.delete(msg);
    }

    @Handler("delete")
    public void delete(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject partInfo = FileInfoFields.PART_INFO.from(msg, IObject.class);
        List<Integer> ids = new LinkedList<>();
        ids.add(DbFields.ID.from(partInfo, Integer.class));
        documentIdsF.inject(msg, ids);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, filePartCollectionName);
    }
}
