package net.onestorm.plugins.stormcombat.core.user;

import net.onestorm.plugins.stormcombat.api.CombatPlugin;
import net.onestorm.plugins.stormcombat.api.storage.UserData;
import net.onestorm.plugins.stormcombat.api.user.OnlineUser;
import net.onestorm.plugins.stormcombat.api.user.User;
import net.onestorm.plugins.stormcombat.api.user.UserManager;
import net.onestorm.plugins.stormcombat.core.storage.UserDataImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UserManagerImpl implements UserManager {

    private final Map<UUID, OnlineUser> UuidToUserMap = new ConcurrentHashMap<>();
    private final Map<String, OnlineUser> UsernameToUserMap = new ConcurrentHashMap<>();
    private final CombatPlugin plugin;

    public UserManagerImpl(CombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<OnlineUser> getOnlineUser(UUID uuid) {
        return Optional.ofNullable(UuidToUserMap.get(uuid));
    }

    @Override
    public Optional<OnlineUser> getOnlineUser(String username) {
        return Optional.ofNullable(UsernameToUserMap.get(username.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public List<OnlineUser> getOnlineUsers() {
        return new ArrayList<>(UuidToUserMap.values());
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(UUID uuid) {
        User onlineUser = UuidToUserMap.get(uuid);

        if (onlineUser != null) {
            return CompletableFuture.completedFuture(Optional.of(onlineUser));
        }

        CompletableFuture<Optional<User>> futureUser = new CompletableFuture<>();

        plugin.getStorage().getUserData(uuid).thenAccept(optionalUserData -> {
            if (optionalUserData.isEmpty()) {
                futureUser.complete(Optional.empty());
                return;
            }

            User user = new UserImpl(uuid);
            user.setUserData(optionalUserData.get());

            futureUser.complete(Optional.of(user));
        });

        return futureUser;
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(String username) {
        OnlineUser onlineUser = UsernameToUserMap.get(username.toLowerCase(Locale.ENGLISH));

        if (onlineUser != null) {
            return CompletableFuture.completedFuture(Optional.of(onlineUser));
        }

        return CompletableFuture.failedFuture(new UnsupportedOperationException("No implemented username cache yet"));
    }

    @Override
    public void handleJoin(OnlineUser user) {
        UUID uuid = user.getUuid();
        String username = user.getUsername().toLowerCase(Locale.ENGLISH);
        UuidToUserMap.put(uuid, user);
        UsernameToUserMap.put(username, user);

        plugin.getStorage().getUserData(uuid).thenAccept(optionalUserData -> {
            if (optionalUserData.isPresent()) {
                user.setUserData(optionalUserData.get());
                return;
            }

            UserData data = new UserDataImpl(uuid, false);
            user.setUserData(data);
            plugin.getStorage().setUserData(uuid, data);
        });
    }

    @Override
    public void handleQuit(OnlineUser user) {
        UUID uuid = user.getUuid();
        String username = user.getUsername().toLowerCase(Locale.ENGLISH);
        UserData userData = user.getUserData();

        UsernameToUserMap.remove(username);
        if (UuidToUserMap.remove(uuid) == null || plugin.isDisabling() || userData == null || !userData.needsSaving()) {
            return; // if not in map, don't save
        }

        plugin.getStorage()
                .setUserData(uuid, userData)
                .thenAccept(unused -> userData.setSaved());
    }

    @Override
    public void close() {
        UuidToUserMap.forEach((uuid, user) -> {
            UserData userData = user.getUserData();

            if (userData == null || !userData.needsSaving()) {
                return;
            }

            plugin.getStorage()
                    .setUserData(uuid, userData)
                    .thenAccept(unused -> userData.setSaved());
        });

        UuidToUserMap.clear();
        UsernameToUserMap.clear();
    }
}
