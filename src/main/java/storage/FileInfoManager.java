package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.actors.db_accessor.DBFields;
import info.smart_tools.smartactors.utils.ioc.IOC;
import storage.util.FileInfoFields;

import java.util.LinkedList;
import java.util.List;

/**
 * Стандартные действия с информацией по файлу
 */
public class FileInfoManager extends Actor {

    private ListField<IObject> filesF;
    private ListField<String> inConditionF;
    private String fileInfoCollectionName;

    public FileInfoManager(IObject params) {

        filesF = new ListField<>(new FieldName("files"));
        inConditionF = new ListField<>(new FieldName("$in"));
        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    @Handler("fileSearchByGuid")
    public void fileSearchByGuid(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject query = IOC.resolve(IObject.class);
        IObject condition = IOC.resolve(IObject.class);
        DBFields.EQUALS_FIELD.inject(condition, FileInfoFields.SERVER_GUID.from(msg, String.class));
        query.setValue(new FieldName("serverGuid"), condition);
        DBFields.QUERY_FIELD.inject(msg, query);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 10);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
    }

    @Handler("filesSearchByGuid")
    public void filesSearchByGuid(IMessage msg) throws ReadValueException, ChangeValueException {

        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 10000);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
        IObject query = IOC.resolve(IObject.class);
        List<String> serverGuids = new LinkedList<>();
        for (IObject file : filesF.from(msg, IObject.class)) {
            serverGuids.add(FileInfoFields.SERVER_GUID.from(file, String.class));
        }
        IObject inCondition = IOC.resolve(IObject.class);
        inConditionF.inject(inCondition, serverGuids);
        query.setValue(new FieldName("serverGuid"), inCondition);
        DBFields.QUERY_FIELD.inject(msg, query);
    }

    @Handler("handleSearching")
    public void handleSearching(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        IObject fileInfo = DBFields.SEARCH_RESULT_FIELD.from(msg, IObject.class).get(0);
        FileInfoFields.FILE_INFO.inject(msg, fileInfo);
        DBFields.SEARCH_RESULT_FIELD.delete(msg);
    }

    @Handler("prepareForDb")
    public void prepareForDb(IMessage msg) throws ChangeValueException, ReadValueException {

        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        List<IObject> documents = new LinkedList<>();
        documents.add(FileInfoFields.FILE_INFO.from(msg, IObject.class));
        DBFields.DOCUMENTS_FIELD.inject(msg, documents);
    }
}
