package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;
import storage.util.FileInfoFields;

import java.util.LinkedList;
import java.util.List;

/**
 */
public class ContentActor extends Actor {

    private Field<String> fldrPathF;
    private ListField<IObject> fldrContentF;
    /** Формирование запросов в БД */
    private String fileInfoCollectionName;
    private Field<String> collectionNameF;
    private Field<Integer> pageSizeF;
    private Field<Integer> pageNumberF;
    private Field<IObject> queryF;
    private ListField<IObject> searchResultF;

    public ContentActor(IObject params) {

        fldrPathF = new Field<>(new FieldName("folderPath"));
        fldrContentF = new ListField<>(new FieldName("folderContent"));
        collectionNameF = new Field<>(new FieldName("collectionName"));
        pageSizeF = new Field<>(new FieldName("pageSize"));
        pageNumberF = new Field<>(new FieldName("pageNumber"));
        queryF = new Field<>(new FieldName("query"));
        searchResultF = new ListField<>(new FieldName("searchResult"));
        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    //получить запрос от клиента, тут же сформировать запрос на все файлы с path из message
    @Handler("formFldrContentQuery")
    public void formFldrContentQuery(IMessage msg) throws ChangeValueException, ReadValueException {

        collectionNameF.inject(msg, fileInfoCollectionName);
        pageSizeF.inject(msg, 1000000);
        pageNumberF.inject(msg, 1);
        IObject query = new SMObject();
        IObject condition = new SMObject();
        condition.setValue(new FieldName("$eq"), fldrPathF.from(msg, String.class));
        query.setValue(new FieldName("logicPath"), condition);
        queryF.inject(msg, query);
    }

    @Handler("sendFldrContent")
    public void sendFldrContent(IMessage msg) throws ReadValueException, ChangeValueException {

        List<IObject> fldrContentForSend = new LinkedList<>();
        List<IObject> fldrContent = searchResultF.from(msg, IObject.class);
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
