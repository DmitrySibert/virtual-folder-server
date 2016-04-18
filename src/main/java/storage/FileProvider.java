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
 *  Отправки файлов клиентам
 */
public class FileProvider extends Actor {

    private ListField<IObject> filesF;
    private Field<String> filePartF;
    private Field<Integer> partNumberF;


    public FileProvider(IObject params) {

        filesF = new ListField<>(new FieldName("files"));
        filePartF = new Field<>(new FieldName("filePart"));
        partNumberF = new Field<>(new FieldName("partNumber"));
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
            FileInfoFields.IS_FOLDER.inject(file, FileInfoFields.IS_FOLDER.from(fileInfo, Boolean.class));
            filesInfoForSend.add(file);
        }

        respondOn(msg, response -> {
            filesF.inject(response, filesInfoForSend);
        });
    }

    @Handler("sendFilePart")
    public void sendFilePart(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject partInfo = FileInfoFields.PART_INFO.from(msg, IObject.class);
        if (partInfo == null) {
            //TODO: Фатальнейшая ошибка
            //TODO: respondOn с ошибкой или эксепшином
        } else {
            String part = FileInfoFields.PART.from(partInfo, String.class);
            respondOn(msg, response -> {
                filePartF.inject(response, part);
            });
        }
    }
}
