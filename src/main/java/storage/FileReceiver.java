package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.actors.db_accessor.DBFields;
import info.smart_tools.smartactors.core.impl.SMObject;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Handle receiving files from remote machines
 */
public class FileReceiver extends Actor {

    /** Данные о файле */
    private Field<String> serverGuidF;
    private Field<String> fileIdF;
    private Field<String> originalNameF;
    private Field<String> logicPathF;
    private Field<Boolean> activeF;
    /** Данные для хранения */
    private String fileInfoCollectionName;
    /** Данные для кусочной загрузки файла */
    private Field<Integer> filePartSizeF;
    private Field<Integer> partsQuantityF;
    private Field<Integer> fileSizeF;
    private Field<Integer> partSizeF;
    private Field<Integer> partNumberF;
    private Field<String> filePartF;
    private ListField<Integer> partsNumbersF;
    private ListField<String> filePartsF;
    private Integer filePartSize;
    private Field<Boolean> statusF;


    public FileReceiver(IObject params) {

        serverGuidF = new Field<>(new FieldName("serverGuid"));
        fileIdF = new Field<>(new FieldName("fileId"));
        originalNameF = new Field<>(new FieldName("originalName"));
        logicPathF = new Field<>(new FieldName("logicPath"));
        activeF = new Field<>(new FieldName("active"));
        filePartSizeF = new Field<>(new FieldName("filePartSize"));
        partsQuantityF = new Field<>(new FieldName("partsQuantity"));
        partSizeF = new Field<>(new FieldName("partSize"));
        fileSizeF = new Field<>(new FieldName("fileSize"));
        partNumberF = new Field<>(new FieldName("partNumber"));
        partsNumbersF = new ListField<>(new FieldName("partsNumbers"));
        filePartsF = new ListField<>(new FieldName("fileParts"));
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
        Integer parts = fileSize % filePartSize == 0 ? fileSize / filePartSize : fileSize / filePartSize + 1;
        partsQuantityF.inject(msg, parts);
        respondOn(msg, response -> {
            partsQuantityF.inject(response, parts);
            partSizeF.inject(response, filePartSize);
        });
    }

    @Handler("prepareForStorage")
    public void prepareForStorage(IMessage msg) throws ChangeValueException, ReadValueException {

        String fileId = fileIdF.from(msg, String.class);
        Integer lastSplitIndex = fileId.lastIndexOf("\\");
        IObject fileInfo = new SMObject();
        originalNameF.inject(fileInfo, fileId.substring(lastSplitIndex + 1));
        logicPathF.inject(fileInfo, fileId.substring(0, lastSplitIndex));
        fileIdF.inject(fileInfo, fileIdF.from(msg, String.class));
        serverGuidF.inject(fileInfo, serverGuidF.from(msg, String.class));
        partsQuantityF.inject(fileInfo, partsQuantityF.from(msg, Integer.class));
        partsNumbersF.inject(fileInfo, new LinkedList<>());
        filePartsF.inject(fileInfo, new LinkedList<>());
        filePartSizeF.inject(fileInfo, filePartSize);
        activeF.inject(fileInfo, Boolean.FALSE);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        List<IObject> filesInfo = new LinkedList<>();
        filesInfo.add(fileInfo);
        DBFields.DOCUMENTS_FIELD.inject(msg, filesInfo);
    }

    @Handler("getFileInfo")
    public void getFileInfo(IMessage msg) throws ChangeValueException, ReadValueException {

        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        DBFields.PAGE_SIZE_FIELD.inject(msg, 100);
        DBFields.PAGE_NUMBER_FIELD.inject(msg, 1);
        IObject query = new SMObject();
        IObject condition = new SMObject();
        condition.setValue(new FieldName("$eq"), serverGuidF.from(msg, String.class));
        query.setValue(new FieldName("serverGuid"), condition);
        DBFields.QUERY_FIELD.inject(msg, query);
    }

    @Handler("addPart")
    public void addPart(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject fileInfo = DBFields.SEARCH_RESULT_FIELD.from(msg, IObject.class).get(0);
        List<Integer> partsNumbers = partsNumbersF.from(fileInfo, Integer.class);
        partsNumbers.add(partNumberF.from(msg, Integer.class));
        partsNumbersF.inject(fileInfo, partsNumbers);
        List<String> fileParts = filePartsF.from(fileInfo, String.class);
        fileParts.add(filePartF.from(msg, String.class));
        filePartsF.inject(fileInfo,fileParts);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        List<IObject> filesInfo = new LinkedList<>();
        filesInfo.add(fileInfo);
        if(partsQuantityF.from(fileInfo, Integer.class).equals(fileParts.size())) {
            activeF.inject(fileInfo, Boolean.TRUE);
        }
        DBFields.DOCUMENTS_FIELD.inject(msg, filesInfo);
    }

    @Handler("finishPartAdding")
    public void finishPartAdding(IMessage msg) throws ChangeValueException, ReadValueException {

        respondOn(msg, response-> {
            statusF.inject(response, Boolean.TRUE);
        });
    }
}
