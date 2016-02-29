package storage;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Handle receiving files from remote machines
 */
public class FileReceiver extends Actor {

    /** Данные о файле */
    private Field<String> uuidF;
    private Field<String> serverGuidF;
    private Field<String> fileIdF;
    /** Данные для хранения */
    private String fileInfoCollectionName;
    private Field<String> collectionNameF;
    private ListField<IObject> documentsF;

    /** Данные для кусочной загрузки файла */
    private Field<Integer> filePartSizeF;
    private Field<Integer> filePartsQuantityF;
    private Field<Integer> fileSizeF;
    private Field<Integer> partSizeF;
    private Field<Integer> partNumberF;
    private Field<String> filePartF;
    private ListField<Integer> partsNumbersF;
    private ListField<String> filePartsF;
    private Integer filePartSize;
    private Field<Boolean> statusF;

    /** Формирование запросов в БД */
    private Field<Integer> pageSizeF;
    private Field<Integer> pageNumberF;
    private Field<IObject> queryF;
    private ListField<IObject> searchResultF;


    public FileReceiver(IObject params) {

        uuidF = new Field<>(new FieldName("uuid"));
        serverGuidF = new Field<>(new FieldName("serverGuid"));
        fileIdF = new Field<>(new FieldName("fileId"));
        collectionNameF = new Field<>(new FieldName("collectionName"));
        documentsF = new ListField<>(new FieldName("documents"));
        filePartSizeF = new Field<>(new FieldName("filePartSize"));
        filePartsQuantityF = new Field<>(new FieldName("filePartsQuantity"));
        partSizeF = new Field<>(new FieldName("partSize"));
        fileSizeF = new Field<>(new FieldName("fileSize"));
        partNumberF = new Field<>(new FieldName("partNumber"));
        partsNumbersF = new ListField<>(new FieldName("partsNumbers"));
        filePartsF = new ListField<>(new FieldName("fileParts"));
        pageSizeF = new Field<>(new FieldName("pageSize"));
        pageNumberF = new Field<>(new FieldName("pageNumber"));
        queryF = new Field<>(new FieldName("query"));
        searchResultF = new ListField<>(new FieldName("searchResult"));
        filePartF = new Field<>(new FieldName("filePart"));
        statusF = new Field<>(new FieldName("status"));
        try {
            filePartSize = filePartSizeF.from(params, Integer.class);
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    @Handler("initReceiving")
    public void initReceiving(IMessage msg) throws ChangeValueException {

        String serverGuid = UUID.randomUUID().toString();
        serverGuidF.inject(msg, serverGuid);

        respondOn(msg, response -> {
            fileIdF.inject(response, fileIdF.from(msg, String.class));
            serverGuidF.inject(response, serverGuid);
        });
    }

    @Handler("calcPartsQuantity")
    public void calcPartsQuantity(IMessage msg) throws ReadValueException, ChangeValueException {

        Integer fileSize = fileSizeF.from(msg, Integer.class);
        Integer parts = fileSize % filePartSize == 0 ? fileSize / filePartSize : fileSize % filePartSize + 1;
        filePartsQuantityF.inject(msg, parts);
        respondOn(msg, response -> {
            filePartsQuantityF.inject(response, parts);
            partSizeF.inject(response, filePartSize);
        });
    }

    @Handler("prepareForStorage")
    public void prepareForStorage(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject fileInfo = new SMObject();
        fileIdF.inject(fileInfo, fileIdF.from(msg, String.class));
        serverGuidF.inject(fileInfo, fileIdF.from(msg, String.class));
        filePartsQuantityF.inject(fileInfo, filePartsQuantityF.from(msg, Integer.class));
        partsNumbersF.inject(fileInfo, new LinkedList<>());
        filePartsF.inject(fileInfo, new LinkedList<>());

        collectionNameF.inject(msg, fileInfoCollectionName);
        List<IObject> filesInfo = new LinkedList<>();
        filesInfo.add(fileInfo);
        documentsF.inject(msg, filesInfo);
    }

    @Handler("getFileInfo")
    public void getFileInfo(IMessage msg) throws ChangeValueException, ReadValueException {

        collectionNameF.inject(msg, fileInfoCollectionName);
        pageSizeF.inject(msg, 100);
        pageNumberF.inject(msg, 2);
        IObject query = new SMObject();
        IObject condition = new SMObject();
        condition.setValue(new FieldName("$eq"), serverGuidF.from(msg, String.class));
        query.setValue(new FieldName("serverGuid"), condition);
        queryF.inject(msg, query);
    }

    @Handler("addPart")
    public void addPart(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject fileInfo = searchResultF.from(msg, IObject.class).get(0);
        partsNumbersF.from(fileInfo, Integer.class).add(partNumberF.from(msg, Integer.class));
        filePartsF.from(fileInfo, String.class).add(filePartF.from(msg, String.class));
        collectionNameF.inject(msg, fileInfoCollectionName);
        List<IObject> filesInfo = new LinkedList<>();
        filesInfo.add(fileInfo);
        documentsF.inject(msg, filesInfo);
    }

    @Handler("finishPartAdding")
    public void finishPartAdding(IMessage msg) throws ChangeValueException, ReadValueException {

        respondOn(msg, response-> {
            statusF.inject(response, Boolean.TRUE);
        });
    }
}
