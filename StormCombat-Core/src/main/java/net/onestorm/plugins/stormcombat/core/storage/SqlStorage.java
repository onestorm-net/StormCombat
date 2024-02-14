package net.onestorm.plugins.stormcombat.core.storage;

import com.zaxxer.hikari.HikariDataSource;
import net.onestorm.plugins.stormcombat.api.storage.Storage;
import net.onestorm.plugins.stormcombat.api.storage.UserData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SqlStorage implements Storage {

    protected HikariDataSource hikari;
    protected Logger logger;

    protected void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS storm_combat_users (" +
                "`uuid` CHAR(36) NOT NULL, " +
                "`is_punished` BOOLEAN NOT NULL, " +
                "PRIMARY KEY (`uuid`));";

        try (Connection connection = hikari.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException in createTables", e);
        }
    }

    @Override
    public CompletableFuture<Optional<UserData>> getUserData(UUID uuid) {
        CompletableFuture<Optional<UserData>> future = new CompletableFuture<>();

        String query = "SELECT uuid, is_punished FROM storm_combat_users WHERE uuid = ?;";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (Connection connection = hikari.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                preparedStatement.setString(1, uuid.toString());

                ResultSet resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    future.complete(Optional.empty());
                    return; // submit
                }

                UserData data = new UserDataImpl(uuid, resultSet.getBoolean("is_punished"));
                future.complete(Optional.of(data));

            } catch (SQLException e) {
                logger.log(Level.WARNING, "SQLException in getUserData", e);
            } finally {
                executor.shutdown();
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> setUserData(UUID uuid, UserData data) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String query = "INSERT INTO storm_combat_users VALUES (?, ?) ON DUPLICATE KEY UPDATE is_punished = ?;";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (Connection connection = hikari.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                preparedStatement.setString(1, uuid.toString());
                preparedStatement.setBoolean(2, data.isPunished());
                preparedStatement.setBoolean(3, data.isPunished());

                preparedStatement.executeUpdate();

                future.complete(null);

            } catch (SQLException e) {
                logger.log(Level.WARNING, "SQLException in setUserData", e);
            } finally {
                executor.shutdown();
            }
        });

        return future;
    }
}
