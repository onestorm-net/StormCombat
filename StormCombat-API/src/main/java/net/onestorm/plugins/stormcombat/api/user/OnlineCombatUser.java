package net.onestorm.plugins.stormcombat.api.user;

public interface OnlineCombatUser extends CombatUser {

    String getUsername();

    boolean isInCombat();

    void setInCombat(boolean inCombat);

}
