package net.onestorm.plugins.stormcombat.api.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager {

    Optional<OnlineUser> getOnlineUser(UUID uuid);

    Optional<OnlineUser> getOnlineUser(String username);

    List<OnlineUser> getOnlineUsers();

    CompletableFuture<Optional<User>> getUser(UUID uuid);

    CompletableFuture<Optional<User>> getUser(String username);

    void handleJoin(OnlineUser user);

    void handleQuit(OnlineUser user);

    void close();



}
