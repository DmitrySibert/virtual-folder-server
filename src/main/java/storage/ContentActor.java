package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.actors.db_accessor.DBFields;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;
import storage.util.FileInfoFields;

import java.util.LinkedList;
import java.util.List;

/**
 * Формирует информацию о содержимом директории
 */
public class ContentActor extends Actor {

    private Field<String> fldrPathF;
    private ListField<IObject> fldrContentF;
    /** Формирование запросов в БД */
    private String fileInfoCollectionName;

    public ContentActor(IObject params) {

        fldrPathF = new Field<>(new FieldName("folderPath"));
        fldrContentF = new ListField<>(new FieldName("folderContent"));
        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    @Handler("formFldrContentQuery")
    public void formFldrContentQuery(IMessage msg) throws ChangeValueException, ReadValueException {

        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 1000000);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
        IObject query = new SMObject();
        IObject condition = new SMObject();
        condition.setValue(new FieldName("$eq"), fldrPathF.from(msg, String.class));
        query.setValue(new FieldName("logicPath"), condition);
        DBFields.QUERY_FIELD.inject(msg, query);
    }

    @Handler("sendFldrContent")
    public void sendFldrContent(IMessage msg) throws ReadValueException, ChangeValueException {

        List<IObject> fldrContentForSend = new LinkedList<>();
        List<IObject> fldrContent = DBFields.SEARCH_RESULT_FIELD.from(msg, IObject.class);
        for(IObject fileInfo : fldrContent) {
            if(FileInfoFields.ACTIVE.from(fileInfo, Boolean.class)) {
                IObject file = IOC.resolve(IObject.class);
                FileInfoFields.SERVER_GUID.inject(file, FileInfoFields.SERVER_GUID.from(fileInfo, String.class));
                FileInfoFields.ORIGINAL_NAME.inject(file, FileInfoFields.ORIGINAL_NAME.from(fileInfo, String.class));
                fldrContentForSend.add(file);
            }
        }

        respondOn(msg, response -> {
            fldrContentF.inject(response, fldrContentForSend);
        });
    }
}
