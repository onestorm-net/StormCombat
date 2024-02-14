package net.onestorm.plugins.stormcombat.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.onestorm.plugins.stormcombat.api.CombatPlugin;

public class MySqlStorage extends SqlStorage {

    // todo replace with configuration
    private final String host = "";
    private final String port = "";
    private final String name = "";
    private final String username = "";
    private final String password = "";

    public MySqlStorage(CombatPlugin plugin) {

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8",
                host, port, name));
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikari = new HikariDataSource(hikariConfig);

        createTables();
    }

    @Override
    public void close() {
        hikari.close();
    }
}
