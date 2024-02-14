package net.onestorm.plugins.stormcombat.api.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    CompletableFuture<Optional<UserData>> getUserData(UUID uuid);

    CompletableFuture<Void> setUserData(UUID uuid, UserData data);

    void close();

}
