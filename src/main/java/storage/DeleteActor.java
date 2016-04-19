package storage;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.IMessageMapId;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.routers.MessageBus;
import info.smart_tools.smartactors.utils.ioc.IOC;
import storage.util.FileInfoFields;

/**
 * Актор содержит обработчики, отвечающие за удаление информации о файле из системы
 */
public class DeleteActor extends Actor {

    private Field<Boolean> statusF;
    private Field<Integer> curPartF;

    private IMessageMapId deleteFileMm;
    private IMessageMapId deleteFolderMm;
    private IMessageMapId deletePartMm;
    private IMessageMapId finishFileDeletingMm;

    public DeleteActor(IObject params) {

        statusF = new Field<>(new FieldName("status"));
        curPartF = new Field<>(new FieldName("currentPart"));
        try {
            deleteFileMm = MessageMapId.fromString(
                    new Field<String>(new FieldName("deleteFileMm")).from(params, String.class)
            );
            finishFileDeletingMm = MessageMapId.fromString(
                    new Field<String>(new FieldName("finishFileDeletingMm")).from(params, String.class)
            );
            deleteFolderMm = MessageMapId.fromString(
                    new Field<String>(new FieldName("deleteFolderMm")).from(params, String.class)
            );
            deletePartMm = MessageMapId.fromString(
                    new Field<String>(new FieldName("deletePartMm")).from(params, String.class)
            );
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing DeleteActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    /**
     * Запустить цепочку удаления для файлов
     * @param msg содержит списко FileInfo
     */
    @Handler("initFilesDeleting")
    public void initFilesDeleting(IMessage msg) throws ReadValueException, ChangeValueException {

        for(IObject fileInfo : FileInfoFields.FILES_INFO.from(msg, IObject.class)) {
            IMessage delMsg = IOC.resolve(IMessage.class);
            FileInfoFields.FILE_INFO.inject(delMsg, fileInfo);
            IObject addrF = IOC.resolve(IObject.class);
            if (FileInfoFields.IS_FOLDER.from(fileInfo, Boolean.class)) {
                AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, deleteFolderMm);
            } else {
                AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, deleteFileMm);
            }
            AddressingFields.ADDRESS_FIELD.inject(delMsg, addrF);
            MessageBus.send(delMsg);
        }
    }

    @Handler("successResponse")
    public void successResponse(IMessage msg) {

        respondOn(msg, response -> {
            statusF.inject(response, Boolean.TRUE);
        });
    }

    @Handler("deactivateFile")
    public void deactivateFile(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject fileInfo = FileInfoFields.FILE_INFO.from(msg, IObject.class);
        FileInfoFields.ACTIVE.inject(fileInfo, Boolean.FALSE);
    }

    @Handler("initPartsDeleting")
    public void initPartsDeleting(IMessage msg) throws ChangeValueException, ReadValueException {

        IMessage delMsg = IOC.resolve(IMessage.class);
        FileInfoFields.FILE_INFO.inject(delMsg, FileInfoFields.FILE_INFO.from(msg, IObject.class));
        curPartF.inject(delMsg, 1);
        FileInfoFields.PARTS_QUANTITY.inject(delMsg, FileInfoFields.PARTS_QUANTITY.from(
                FileInfoFields.FILE_INFO.from(msg, IObject.class),
                Integer.class
        ));

        IObject addrF = IOC.resolve(IObject.class);
        AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, deletePartMm);
        AddressingFields.ADDRESS_FIELD.inject(delMsg, addrF);
        MessageBus.send(delMsg);
    }

    @Handler("checkPartsOver")
    public void checkPartsOver(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        curPartF.inject(msg, curPartF.from(msg, Integer.class) + 1);
        if (curPartF.from(msg, Integer.class) > FileInfoFields.PARTS_QUANTITY.from(msg, Integer.class)) {
            IObject addrF = IOC.resolve(IObject.class);
            AddressingFields.MESSAGE_MAP_ID_FIELD.delete(msg);
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, finishFileDeletingMm); //окончательно удалить запись в этой карте сообщений
            AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
        } else {
            IMessage delMsg = IOC.resolve(IMessage.class);
            FileInfoFields.FILE_INFO.inject(delMsg, FileInfoFields.FILE_INFO.from(msg, IObject.class));
            curPartF.inject(delMsg, curPartF.from(msg, Integer.class));
            FileInfoFields.PARTS_QUANTITY.inject(delMsg, FileInfoFields.PARTS_QUANTITY.from(
                    FileInfoFields.FILE_INFO.from(msg, IObject.class),
                    Integer.class
            ));

            IObject addrF = IOC.resolve(IObject.class);
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, deletePartMm);
            AddressingFields.ADDRESS_FIELD.inject(delMsg, addrF);
            MessageBus.send(delMsg);
        }
    }

    @Handler("formPartSearchData")
    public void formPartSearchData(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject fileInfo = FileInfoFields.FILE_INFO.from(msg, IObject.class);
        FileInfoFields.SERVER_GUID.inject(msg, FileInfoFields.SERVER_GUID.from(fileInfo, String.class));
        FileInfoFields.PART_NUMBER.inject(msg, curPartF.from(msg, Integer.class));
    }

    //TODO: возможно, стоит перетащить этот и подобные ему методы в правила преобразования,
    @Handler("setLogicPathFromFileInfo")
    public void setLogicPathFromFileInfo(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject fileInfo = FileInfoFields.FILE_INFO.from(msg, IObject.class);
        FileInfoFields.LOGIC_PATH.inject(msg, FileInfoFields.LOGIC_PATH.from(fileInfo, String.class));
    }
}
