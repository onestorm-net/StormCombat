package net.onestorm.plugins.stormcombat.api.storage;

import java.util.UUID;

public interface UserData {

    UUID getUuid();

    boolean isPunished();

    void setPunished(boolean isPunished);

    void setSaved();

    boolean needsSaving();

    /* for if deleting will be ever a thing
     *
     * void needsDeleting(boolean needsDeleting);
     *
     * boolean needsDeleting();
     *
     * void setDeleted();
     *
     * boolean isDeleted();
     */


}
