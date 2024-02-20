package net.onestorm.plugins.stormcombat.api.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager {

    Optional<OnlineCombatUser> getOnlineUser(UUID uuid);

    Optional<OnlineCombatUser> getOnlineUser(String username);

    List<OnlineCombatUser> getOnlineUsers();

    CompletableFuture<Optional<CombatUser>> getUser(UUID uuid);

    CompletableFuture<Optional<CombatUser>> getUser(String username);

    void handleJoin(OnlineCombatUser user);

    void handleQuit(OnlineCombatUser user);

    void close();



}
