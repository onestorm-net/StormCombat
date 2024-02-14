package net.onestorm.plugins.stormcombat.api.combat;

import net.onestorm.plugins.stormcombat.api.user.OnlineUser;

import java.util.List;

public interface CombatManager {

    void setInCombat(OnlineUser user, boolean inCombat);

    void handleJoin(OnlineUser user);

    void handleQuit(OnlineUser user);

    void handleDeath(OnlineUser user);

    List<OnlineUser> getInCombatUsers();

    void reload();

    void close();

}
