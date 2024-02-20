package net.onestorm.plugins.stormcombat.api.combat;

import net.onestorm.plugins.stormcombat.api.user.OnlineCombatUser;

import java.util.List;

public interface CombatManager {

    void setInCombat(OnlineCombatUser user, boolean inCombat);

    void handleJoin(OnlineCombatUser user);

    void handleQuit(OnlineCombatUser user);

    void handleDeath(OnlineCombatUser user);

    void reload();

    void close();

}
