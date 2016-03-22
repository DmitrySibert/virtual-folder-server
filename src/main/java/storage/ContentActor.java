package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;

import java.util.LinkedList;
import java.util.List;

/**
 */
public class ContentActor extends Actor {

    private Field<String> fldrPathF;
    private ListField<IObject> fldrContentF;
    /** данные о файле */
    private Field<String> serverGuidF;
    private Field<String> originalNameF;
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
        serverGuidF = new Field<>(new FieldName("serverGuid"));
        originalNameF = new Field<>(new FieldName("originalName"));
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
        pageSizeF.inject(msg, 1000);
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
            IObject file = IOC.resolve(IObject.class);
            serverGuidF.inject(file, serverGuidF.from(fileInfo, String.class));
            originalNameF.inject(file, originalNameF.from(fileInfo, String.class));
            fldrContentForSend.add(file);
        }

        respondOn(msg, response -> {
            fldrContentF.inject(response, fldrContentForSend);
        });
    }
}
