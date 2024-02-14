package net.onestorm.plugins.stormcombat.api;

import net.onestorm.plugins.stormcombat.api.combat.CombatManager;
import net.onestorm.plugins.stormcombat.api.storage.Storage;
import net.onestorm.plugins.stormcombat.api.user.UserManager;

import java.util.logging.Logger;

public interface CombatPlugin {

    Storage getStorage();

    CombatManager getCombatManager();

    UserManager getUserManager();

    Logger getLogger();

    boolean isDisabling();

    // Configuration getConfiguration() StormLib

    // ActionManager getActionManager() StormLib

    // UsernameManager getUsernameManager() StormLib
    void reload();

}
