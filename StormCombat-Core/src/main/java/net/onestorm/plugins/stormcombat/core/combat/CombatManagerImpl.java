package net.onestorm.plugins.stormcombat.core.combat;

import net.onestorm.plugins.stormcombat.api.combat.CombatManager;
import net.onestorm.plugins.stormcombat.api.user.OnlineUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CombatManagerImpl implements CombatManager {

    private static final long COMBAT_TAG_TIME_DEFAULT = 15L;
    private final Map<UUID, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final Map<UUID, OnlineUser> inCombatUserMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;

    private long combatTagTime;

    public CombatManagerImpl() {
        executor = Executors.newSingleThreadScheduledExecutor(); // maybe we should use multiple threads?

        reload();
    }

    @Override
    public void setInCombat(OnlineUser user, boolean inCombat) {
        UUID uuid = user.getUuid();

        if (!inCombat) {
            // take player out of combat
            user.setInCombat(false);
            if (inCombatUserMap.remove(uuid) != null) {
                return;
            }
            // remove/cancel task
            ScheduledFuture<?> oldFuture = taskMap.remove(uuid);
            if (oldFuture != null) {
                oldFuture.cancel(true);
            }
            return;
        }

        user.setInCombat(true);

        if (inCombatUserMap.putIfAbsent(uuid, user) == null) {
            // todo run on combat tag actions
        } else {
            // todo run on combat tag timer refresh actions
        }

        ScheduledFuture<?> future = executor.schedule(() -> {
            user.setInCombat(false);
            // todo run on combat leave actions
        }, combatTagTime, TimeUnit.SECONDS);

        // remove/cancel old task
        ScheduledFuture<?> oldFuture = taskMap.remove(uuid);
        if (oldFuture != null) {
            oldFuture.cancel(true);
        }
        // put new task
        taskMap.put(uuid, future);
    }

    @Override
    public List<OnlineUser> getInCombatUsers() {

        // Collections.unmodifiableList(inCombatUserMap.values()); todo
        // maybe just change the return type to map or fully remove this method? will probably only be used internal

        return new ArrayList<>(inCombatUserMap.values());
    }

    @Override
    public void handleJoin(OnlineUser user) {
        if (user.isPunished()) {
            // todo re-join after combat log actions
        }
    }

    @Override
    public void handleQuit(OnlineUser user) {
        // todo run combat log actions
    }

    @Override
    public void handleDeath(OnlineUser user) {
        // todo run on combat leave by death actions
    }

    @Override
    public void reload() {
        inCombatUserMap.forEach((uuid, user) -> {
            user.setInCombat(false); // get all players out of combat, just to be sure
        });

        // todo reload configuration related stuff
        //combatTagTime = plugin.getConfiguration.getLong("combat-tag-time").orElse(COMBAT_TAG_TIME_DEFAULT);
        combatTagTime = COMBAT_TAG_TIME_DEFAULT;
    }

    @Override
    public void close() {
        inCombatUserMap.forEach((uuid, user) -> {
            user.setInCombat(false); // get all players out of combat, just to be sure
        });
        inCombatUserMap.clear();
        executor.shutdown();
    }
}
