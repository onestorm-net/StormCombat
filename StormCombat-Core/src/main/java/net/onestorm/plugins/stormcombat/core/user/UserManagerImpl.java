package net.onestorm.plugins.stormcombat.core.user;

import net.onestorm.plugins.stormcombat.api.CombatPlugin;
import net.onestorm.plugins.stormcombat.api.storage.UserData;
import net.onestorm.plugins.stormcombat.api.user.OnlineCombatUser;
import net.onestorm.plugins.stormcombat.api.user.CombatUser;
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

    private final Map<UUID, OnlineCombatUser> UuidToUserMap = new ConcurrentHashMap<>();
    private final Map<String, OnlineCombatUser> UsernameToUserMap = new ConcurrentHashMap<>();
    private final CombatPlugin plugin;

    public UserManagerImpl(CombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<OnlineCombatUser> getOnlineUser(UUID uuid) {
        return Optional.ofNullable(UuidToUserMap.get(uuid));
    }

    @Override
    public Optional<OnlineCombatUser> getOnlineUser(String username) {
        return Optional.ofNullable(UsernameToUserMap.get(username.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public List<OnlineCombatUser> getOnlineUsers() {
        return new ArrayList<>(UuidToUserMap.values());
    }

    @Override
    public CompletableFuture<Optional<CombatUser>> getUser(UUID uuid) {
        CombatUser onlineUser = UuidToUserMap.get(uuid);

        if (onlineUser != null) {
            return CompletableFuture.completedFuture(Optional.of(onlineUser));
        }

        CompletableFuture<Optional<CombatUser>> futureUser = new CompletableFuture<>();

        plugin.getStorage().getUserData(uuid).thenAccept(optionalUserData -> {
            if (optionalUserData.isEmpty()) {
                futureUser.complete(Optional.empty());
                return;
            }

            CombatUser user = new UserImpl(uuid);
            user.setUserData(optionalUserData.get());

            futureUser.complete(Optional.of(user));
        });

        return futureUser;
    }

    @Override
    public CompletableFuture<Optional<CombatUser>> getUser(String username) {
        OnlineCombatUser onlineUser = UsernameToUserMap.get(username.toLowerCase(Locale.ENGLISH));

        if (onlineUser != null) {
            return CompletableFuture.completedFuture(Optional.of(onlineUser));
        }

        return CompletableFuture.failedFuture(new UnsupportedOperationException("No implemented username cache yet"));
    }

    @Override
    public void handleJoin(OnlineCombatUser user) {
        UUID uuid = user.getUuid();
        String username = user.getUsername().toLowerCase(Locale.ENGLISH);
        UuidToUserMap.put(uuid, user);
        UsernameToUserMap.put(username, user);

        plugin.getStorage().getUserData(uuid).thenAccept(optionalUserData -> {
            if (optionalUserData.isPresent()) {
                user.setUserData(optionalUserData.get());
            } else {
                UserData data = new UserDataImpl(uuid, false);
                user.setUserData(data);
                plugin.getStorage().setUserData(uuid, data);
            }
            plugin.getCombatManager().handleJoin(user);
        });
    }

    @Override
    public void handleQuit(OnlineCombatUser user) {
        UUID uuid = user.getUuid();
        String username = user.getUsername().toLowerCase(Locale.ENGLISH);
        UserData userData = user.getUserData();

        UsernameToUserMap.remove(username);
        if (UuidToUserMap.remove(uuid) == null || plugin.isDisabling() || userData == null || !userData.needsSaving()) {
            return; // if not in map, don't save
        }

        plugin.getCombatManager().handleQuit(user);

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
