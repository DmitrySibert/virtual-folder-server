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

    private ListField<IObject> fldrContentF;

    public ContentActor(IObject params) {

        fldrContentF = new ListField<>(new FieldName("folderContent"));
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
