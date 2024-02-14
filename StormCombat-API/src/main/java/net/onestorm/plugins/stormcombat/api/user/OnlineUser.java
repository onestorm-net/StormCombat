package net.onestorm.plugins.stormcombat.api.user;

public interface OnlineUser extends User {

    String getUsername();

    boolean isInCombat();

    void setInCombat(boolean inCombat);

}
