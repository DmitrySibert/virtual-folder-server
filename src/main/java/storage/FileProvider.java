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
 *
 */
public class FileProvider extends Actor {

    private ListField<IObject> filesF;
    private Field<String> filePartF;
    private Field<Integer> partNumberF;
    private String fileInfoCollectionName;

    public FileProvider(IObject params) {

        filesF = new ListField<>(new FieldName("files"));
        filePartF = new Field<>(new FieldName("filePart"));
        partNumberF = new Field<>(new FieldName("partNumber"));
        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    @Handler("formFilesSearchQuery")
    public void formFilesSearchQuery(IMessage msg) throws ReadValueException, ChangeValueException {

        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 10000);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
        IObject query = IOC.resolve(IObject.class);
        List<IObject> orConditions = new LinkedList<>();
        for (IObject file : filesF.from(msg, IObject.class)) {
            IObject condition = IOC.resolve(IObject.class);
            DBFields.EQUALS_FIELD.inject(condition, FileInfoFields.SERVER_GUID.from(file, String.class));
            IObject orCondition = IOC.resolve(IObject.class);
            orCondition.setValue(new FieldName("serverGuid"), condition);
            orConditions.add(orCondition);
        }
        DBFields.OR_FIELD.inject(query, orConditions);
        DBFields.QUERY_FIELD.inject(msg, query);
    }

    @Handler("sendFilesInfo")
    public void sendFilesInfo(IMessage msg) throws ReadValueException, ChangeValueException {

        List<IObject> filesInfoForSend = new LinkedList<>();
        List<IObject> filesInfo = DBFields.SEARCH_RESULT_FIELD.from(msg, IObject.class);
        for(IObject fileInfo : filesInfo) {
            IObject file = IOC.resolve(IObject.class);
            FileInfoFields.LOGIC_PATH.inject(file, FileInfoFields.LOGIC_PATH.from(fileInfo, String.class));
            FileInfoFields.ORIGINAL_NAME.inject(file, FileInfoFields.ORIGINAL_NAME.from(fileInfo, String.class));
            FileInfoFields.SERVER_GUID.inject(file, FileInfoFields.SERVER_GUID.from(fileInfo, String.class));
            FileInfoFields.PARTS_QUANTITY.inject(file, FileInfoFields.PARTS_QUANTITY.from(fileInfo, Integer.class));
            FileInfoFields.PART_SIZE.inject(file, FileInfoFields.PART_SIZE.from(fileInfo, Integer.class));
            FileInfoFields.FILE_SIZE.inject(file, FileInfoFields.FILE_SIZE.from(fileInfo, Integer.class));
            filesInfoForSend.add(file);
        }

        respondOn(msg, response -> {
            filesF.inject(response, filesInfoForSend);
        });
    }

    @Handler("fileSearchByGuid")
    public void fileSearchByGuid(IMessage msg) throws ChangeValueException, ReadValueException {

        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 10000);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
        IObject query = IOC.resolve(IObject.class);
        IObject condition = IOC.resolve(IObject.class);
        DBFields.EQUALS_FIELD.inject(condition, FileInfoFields.SERVER_GUID.from(msg, String.class));
        query.setValue(new FieldName("serverGuid"), condition);
        DBFields.QUERY_FIELD.inject(msg, query);
    }

    @Handler("sendFilePart")
    public void sendFilePart(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject fileInfo = DBFields.SEARCH_RESULT_FIELD.from(msg, IObject.class).get(0);
        List<Integer> partsNumbers = FileInfoFields.PARTS_NUMBERS.from(fileInfo, Integer.class);
        Integer trgIndex = null;
        for(Integer number : partsNumbers) {
            if(partNumberF.from(msg, Integer.class).equals(number)) {
                trgIndex = number;
                break;
            }
        }
        if (trgIndex == null) {
            //TODO: Фатальнейшая ошибка
            //TODO: respondOn с ошибкой или эксепшином
        } else {
            String part = FileInfoFields.FILE_PARTS.from(fileInfo, String.class).get(trgIndex);
            respondOn(msg, response -> {
                filePartF.inject(response, part);
            });
        }
    }
}
