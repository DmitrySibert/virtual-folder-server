package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.actors.db_accessor.DBFields;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.IMessageMapId;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;
import storage.util.FileInfoFields;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Обработка получения файла с клиента
 */
public class FileReceiver extends Actor {

    private Field<String> fileIdF;
    private String fileInfoCollectionName;
    private IMessageMapId updateFileInfoMmId;
    private Integer filePartSize;
    private Field<Boolean> statusF;

    public FileReceiver(IObject params) {

        fileIdF = new Field<>(new FieldName("fileId"));
        statusF = new Field<>(new FieldName("status"));
        try {
            filePartSize = new Field<Integer>(new FieldName("filePartSize")).from(params, Integer.class);
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
            updateFileInfoMmId = MessageMapId.fromString(
                    new Field<String>(new FieldName("updateFileInfoMm")).from(params, String.class)
            );
        } catch (ChangeValueException | ReadValueException e) {
            String err = "An error occurred while instancing class";
            System.out.println(err);
            throw new RuntimeException(err);
        }
    }

    /**
     * Сформировать серверный идентификатор файла
     * @throws ChangeValueException
     */
    @Handler("initReceiving")
    public void initReceiving(IMessage msg) throws ChangeValueException {

        String serverGuid = UUID.randomUUID().toString();
        FileInfoFields.SERVER_GUID.inject(msg, serverGuid);

        respondOn(msg, response -> {
            fileIdF.inject(response, fileIdF.from(msg, String.class));
            FileInfoFields.SERVER_GUID.inject(response, serverGuid);
        });
    }

    @Handler("calcPartsQuantity")
    public void calcPartsQuantity(IMessage msg) throws ReadValueException, ChangeValueException {

        Integer fileSize = FileInfoFields.FILE_SIZE.from(msg, Integer.class);
        Integer parts = fileSize % filePartSize == 0 ? fileSize / filePartSize : fileSize / filePartSize + 1;
        FileInfoFields.PARTS_QUANTITY.inject(msg, parts);
        respondOn(msg, response -> {
            FileInfoFields.PARTS_QUANTITY.inject(response, parts);
            FileInfoFields.PART_SIZE.inject(response, filePartSize);
        });
    }

    /**
     * Сформировать запись о новом файле для БД
     * @throws ChangeValueException
     * @throws ReadValueException
     */
    @Handler("prepareFileForStorage")
    public void prepareFileForStorage(IMessage msg) throws ChangeValueException, ReadValueException {

        String fileId = fileIdF.from(msg, String.class);
        Integer lastSplitIndex = fileId.lastIndexOf("\\");
        IObject fileInfo = IOC.resolve(IObject.class);
        FileInfoFields.ORIGINAL_NAME.inject(fileInfo, fileId.substring(lastSplitIndex + 1));
        FileInfoFields.LOGIC_PATH.inject(fileInfo, fileId.substring(0, lastSplitIndex));
        fileIdF.inject(fileInfo, fileIdF.from(msg, String.class));
        FileInfoFields.SERVER_GUID.inject(fileInfo, FileInfoFields.SERVER_GUID.from(msg, String.class));
        FileInfoFields.PARTS_QUANTITY.inject(fileInfo, FileInfoFields.PARTS_QUANTITY.from(msg, Integer.class));
        FileInfoFields.PART_SIZE.inject(fileInfo, filePartSize);
        FileInfoFields.FILE_SIZE.inject(fileInfo, FileInfoFields.FILE_SIZE.from(msg, Integer.class));
        FileInfoFields.ACTIVE.inject(fileInfo, Boolean.FALSE);
        FileInfoFields.IS_FOLDER.inject(fileInfo, Boolean.FALSE);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        List<IObject> filesInfo = new LinkedList<>();
        filesInfo.add(fileInfo);
        DBFields.DOCUMENTS_FIELD.inject(msg, filesInfo);
    }

    /**
     * Сформировать запись о новой директории для БД
     * @throws ChangeValueException
     * @throws ReadValueException
     */
    @Handler("prepareFolderForStorage")
    public void prepareFolderForStorage(IMessage msg) throws ChangeValueException, ReadValueException {

        String fileId = fileIdF.from(msg, String.class);
        Integer lastSplitIndex = fileId.lastIndexOf("\\");
        IObject fileInfo = IOC.resolve(IObject.class);
        FileInfoFields.ORIGINAL_NAME.inject(fileInfo, fileId.substring(lastSplitIndex + 1));
        FileInfoFields.LOGIC_PATH.inject(fileInfo, fileId.substring(0, lastSplitIndex));
        fileIdF.inject(fileInfo, fileIdF.from(msg, String.class));
        FileInfoFields.SERVER_GUID.inject(fileInfo, FileInfoFields.SERVER_GUID.from(msg, String.class));
        FileInfoFields.ACTIVE.inject(fileInfo, Boolean.FALSE);
        FileInfoFields.IS_FOLDER.inject(fileInfo, Boolean.TRUE);
        DBFields.COLLECTION_NAME_FIELD.inject(msg, fileInfoCollectionName);
        List<IObject> filesInfo = new LinkedList<>();
        filesInfo.add(fileInfo);
        DBFields.DOCUMENTS_FIELD.inject(msg, filesInfo);
    }

    /**
     * Проверить, все ли файлы пришли. Если все, то добавить изменение статуса в цепочку
     * @throws DeleteValueException
     * @throws ChangeValueException
     * @throws ReadValueException
     */
    @Handler("checkComplete")
    public void checkComplete(IMessage msg) throws DeleteValueException, ChangeValueException, ReadValueException {

        IObject fileInfo = FileInfoFields.FILE_INFO.from(msg, IObject.class);
        if (FileInfoFields.PARTS_QUANTITY.from(fileInfo, Integer.class).equals(
                FileInfoFields.PART_NUMBER.from(msg, Integer.class))
        ) {
            FileInfoFields.ACTIVE.inject(fileInfo, Boolean.TRUE);
            AddressingFields.MESSAGE_MAP_FIELD.delete(msg);
            IObject addrF = IOC.resolve(IObject.class);
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, updateFileInfoMmId);
            AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
        }
    }

    /**
     * Отправить положительный результат клиенту
     * @throws ChangeValueException
     * @throws ReadValueException
     */
    @Handler("finishPartAdding")
    public void finishPartAdding(IMessage msg) throws ChangeValueException, ReadValueException {

        respondOn(msg, response-> {
            statusF.inject(response, Boolean.TRUE);
        });
    }
}
