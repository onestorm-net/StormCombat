package net.onestorm.plugins.stormcombat.api.user;

import net.onestorm.plugins.stormcombat.api.storage.UserData;

import java.util.UUID;

public interface User {

    UUID getUuid();

    boolean isPunished();

    void setIsPunished(boolean isPunished);

    UserData getUserData();

    void setUserData(UserData data);


}
