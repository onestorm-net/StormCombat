package net.onestorm.plugins.stormcombat.core.combat;

import net.onestorm.library.action.Action;
import net.onestorm.library.configuration.Configuration;
import net.onestorm.library.configuration.Section;
import net.onestorm.plugins.stormcombat.api.CombatPlugin;
import net.onestorm.plugins.stormcombat.api.combat.CombatManager;
import net.onestorm.plugins.stormcombat.api.user.OnlineCombatUser;

import java.util.ArrayList;
import java.util.LinkedList;
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
    private final Map<UUID, OnlineCombatUser> inCombatUserMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private final CombatPlugin plugin;

    // config
    private long combatTagTime;
    private List<Action> combatTagActions = new LinkedList<>();
    private List<Action> initialCombatTagActions = new LinkedList<>();
    private List<Action> refreshCombatTagActions = new LinkedList<>();
    private List<Action> leaveCombatActions = new LinkedList<>();
    private List<Action> leaveCombatByTimeActions = new LinkedList<>();
    private List<Action> leaveCombatByDeathActions = new LinkedList<>();
    private List<Action> rejoinActions = new LinkedList<>();
    private List<Action> quitActions = new LinkedList<>();


    public CombatManagerImpl(CombatPlugin plugin) {
        this.plugin = plugin;

        executor = Executors.newSingleThreadScheduledExecutor(); // maybe we should use multiple threads?

        reload();
    }

    @Override
    public void setInCombat(OnlineCombatUser user, boolean inCombat) {
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

        combatTagActions.forEach(action -> {
            action.execute(user);
        });

        if (inCombatUserMap.putIfAbsent(uuid, user) == null) {
            initialCombatTagActions.forEach(action -> {
                action.execute(user);
            });
        } else {
            refreshCombatTagActions.forEach(action -> {
                action.execute(user);
            });
        }

        ScheduledFuture<?> future = executor.schedule(() -> {
            user.setInCombat(false);
            leaveCombatActions.forEach(action -> {
                action.execute(user);
            });
            leaveCombatByTimeActions.forEach(action -> {
                action.execute(user);
            });
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
    public void handleJoin(OnlineCombatUser user) {
        if (user.isPunished()) {
            rejoinActions.forEach(action -> {
                action.execute(user);
            });
        }
    }

    @Override
    public void handleQuit(OnlineCombatUser user) {
        UUID uuid = user.getUuid();

        if (user.isInCombat()) {
            user.setIsPunished(true);

            quitActions.forEach(action -> {
                action.execute(user);
            });
        }

        user.setInCombat(false);
        if (inCombatUserMap.remove(uuid) != null) {
            return;
        }
        // remove/cancel task
        ScheduledFuture<?> oldFuture = taskMap.remove(uuid);
        if (oldFuture != null) {
            oldFuture.cancel(true);
        }
    }

    @Override
    public void handleDeath(OnlineCombatUser user) {
        leaveCombatActions.forEach(action -> {
            action.execute(user);
        });
        leaveCombatByDeathActions.forEach(action -> {
            action.execute(user);
        });
    }

    @Override
    public void reload() {
        inCombatUserMap.forEach((uuid, user) -> {
            user.setInCombat(false); // get all players out of combat, just to be sure
        });

        Configuration configuration = plugin.getConfiguration();

        combatTagTime = configuration.getLong("combat-tag-time").orElse(COMBAT_TAG_TIME_DEFAULT);

        configuration.getSection("combat-tag-actions").ifPresentOrElse(section -> {
            combatTagActions = plugin.getActionManager().getActions(section);
        }, () -> combatTagActions = new LinkedList<>());

        configuration.getSection("initial-combat-tag-actions").ifPresentOrElse(section -> {
            initialCombatTagActions = plugin.getActionManager().getActions(section);
        }, () -> initialCombatTagActions = new LinkedList<>());

        configuration.getSection("refresh-combat-tag-actions").ifPresentOrElse(section -> {
            refreshCombatTagActions = plugin.getActionManager().getActions(section);
        }, () -> refreshCombatTagActions = new LinkedList<>());

        configuration.getSection("leave-combat-actions").ifPresentOrElse(section -> {
            leaveCombatActions = plugin.getActionManager().getActions(section);
        }, () -> leaveCombatActions = new LinkedList<>());

        configuration.getSection("leave-combat-by-time-actions").ifPresentOrElse(section -> {
            leaveCombatByTimeActions = plugin.getActionManager().getActions(section);
        }, () -> leaveCombatByTimeActions = new LinkedList<>());

        configuration.getSection("leave-combat-by-death-actions").ifPresentOrElse(section -> {
            leaveCombatByDeathActions = plugin.getActionManager().getActions(section);
        }, () -> leaveCombatByDeathActions = new LinkedList<>());

        configuration.getSection("rejoin-after-combat-log-actions").ifPresentOrElse(section -> {
            rejoinActions = plugin.getActionManager().getActions(section);
        }, () -> rejoinActions = new LinkedList<>());

        configuration.getSection("quit-while-in-combat-actions").ifPresentOrElse(section -> {
            quitActions = plugin.getActionManager().getActions(section);
        }, () -> quitActions = new LinkedList<>());

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
