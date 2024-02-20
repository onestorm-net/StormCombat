package net.onestorm.plugins.stormcombat.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.onestorm.library.configuration.Configuration;
import net.onestorm.library.configuration.Section;
import net.onestorm.plugins.stormcombat.api.CombatPlugin;

import java.util.Optional;

public class MySqlStorage extends SqlStorage {

    // todo replace with configuration
    private final String host = "";
    private final String port = "";
    private final String name = "";
    private final String username = "";
    private final String password = "";

    public MySqlStorage(CombatPlugin plugin) {

        Configuration configuration = plugin.getConfiguration();

        String host = configuration.getString("database.host").orElse("127.0.0.1");
        String port = configuration.getString("database.port").orElse("3306");
        String name = configuration.getString("database.name").orElse("storm");
        String username = configuration.getString("database.username").orElse("root");
        String password = configuration.getString("database.password").orElse("");

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
