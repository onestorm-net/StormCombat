package net.onestorm.plugins.stormcombat.core.user;

import net.kyori.adventure.text.Component;
import net.onestorm.plugins.stormcombat.api.storage.UserData;
import net.onestorm.plugins.stormcombat.api.user.CombatUser;

import java.util.UUID;

public class UserImpl implements CombatUser {

    private final UUID uuid;
    private UserData data;

    public UserImpl(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean isPunished() {
        if (data == null) {
            return false;
        }
        return data.isPunished();
    }

    @Override
    public void setIsPunished(boolean isPunished) {

    }

    @Override
    public UserData getUserData() {
        return data;
    }

    @Override
    public void setUserData(UserData data) {
        this.data = data;
    }
}
