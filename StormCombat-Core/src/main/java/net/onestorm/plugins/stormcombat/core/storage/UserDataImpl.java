package net.onestorm.plugins.stormcombat.core.storage;

import net.onestorm.plugins.stormcombat.api.storage.UserData;

import java.util.UUID;

public class UserDataImpl implements UserData {

    private final UUID uuid;
    private boolean isPunished;
    private boolean needsSaving = false;

    public UserDataImpl(UUID uuid) {
        this.uuid = uuid;
        this.isPunished = false;
    }

    public UserDataImpl(UUID uuid, boolean isPunished) {
        this.uuid = uuid;
        this.isPunished = isPunished;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean isPunished() {
        return isPunished;
    }

    @Override
    public void setPunished(boolean isPunished) {
        if (this.isPunished == isPunished) {
            return;
        }
        this.isPunished = isPunished;
        needsSaving = true;
    }

    @Override
    public void setSaved() {
        needsSaving = false;
    }

    @Override
    public boolean needsSaving() {
        return needsSaving;
    }


}
