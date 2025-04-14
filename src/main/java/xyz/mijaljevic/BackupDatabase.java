/**
 * Copyright (C) 2025 Karlo Mijaljević
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.mijaljevic;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the H2 database used for backup indexing. Only contains
 * operations tied to the {@link BackupFile} entity and nothing else.
 */
final class BackupDatabase {
    /**
     * Environment name where the database pathname is stored.
     */
    private static final String DATABASE_ENVIRONMENT_NAME = "BACKUP_DB";

    /**
     * The default H2 database name to use in case there is no user provided
     * database pathname.
     */
    private static final String DEFAULT_DATABASE_NAME = "./backup.db";

    /**
     * H2 database base JDBC url parameters.
     */
    private static final String DATABASE_BASE_JDBC_URL = "jdbc:h2:";

    /**
     * Default database username.
     */
    private static final String DATABASE_USERNAME = "backup";

    /**
     * Default database password.
     */
    private static final String DATABASE_PASSWORD = "backup";

    /**
     * Checks if the {@link BackupFile} entity table exists. If it does not it
     * creates it.
     */
    private static final String BACKUP_FILE_TABLE_SQL = """
                                                        CREATE TABLE IF NOT EXISTS backup_files (
                                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                            name VARCHAR(256) NOT NULL,
                                                            hash VARCHAR(256) NOT NULL,
                                                            path VARCHAR(1024) UNIQUE NOT NULL,
                                                            type VARCHAR(256) NOT NULL,
                                                            noted TIMESTAMP NOT NULL
                                                        );
                                                        """;

    /**
     * SQL INSERT statement for the {@link BackupFile} entity.
     */
    private static final String BACKUP_FILE_INSERT_SQL = """
                                                         INSERT INTO backup_files
                                                         (name, hash, path, type, noted) VALUES
                                                         (?, ?, ?, ?, ?);
                                                         """;

    /**
     * Selects the {@link BackupFile} entity which matches the user provided ID.
     */
    private static final String BACKUP_FILE_SELECT_SQL = "SELECT * FROM backup_files WHERE id = ?;";

    /**
     * SQL DELETE statement for the {@link BackupFile} entity.
     */
    private static final String BACKUP_FILE_DELETE_SQL = """
                                                         DELETE FROM backup_files
                                                         WHERE id = ?;
                                                         """;

    /**
     * Paging done by ID where the client provides the latest read ID.
     */
    private static final String BACKUP_FILE_PAGE_SQL = """
                                                       SELECT * FROM backup_files
                                                       WHERE id > ?
                                                       ORDER BY id
                                                       LIMIT ?;
                                                       """;

    /**
     * Database {@link Connection} used to execute statements.
     */
    private final Connection connection;

    /**
     * Constructor for the backup database class.
     *
     * @throws SQLException in case it fails to create a {@link Connection}
     *                      instance.
     */
    public BackupDatabase() throws SQLException {
        this.connection = DriverManager.getConnection(
                generateJdbcUrl(null),
                DATABASE_USERNAME,
                DATABASE_PASSWORD
        );

        configureTable();
    }

    /**
     * Constructor for the backup database class.
     *
     * @param name     Name of the database
     * @param username Database username
     * @param password Database password
     * @throws SQLException in case it fails to create a {@link Connection}
     *                      instance.
     */
    public BackupDatabase(String name, String username, String password) throws SQLException {
        this.connection = DriverManager.getConnection(
                generateJdbcUrl(name),
                username == null || username.isEmpty() ? DATABASE_USERNAME : username,
                password == null || password.isEmpty() ? DATABASE_PASSWORD : password
        );

        configureTable();
    }

    /**
     * Closes the database connection. After this the instance owning the
     * connection should be thrashed.
     *
     * @throws SQLException in case it fails to close the connection.
     */
    public void close() throws SQLException {
        this.connection.close();
    }

    /**
     * Saves the {@link BackupFile} entity into the database.
     *
     * @param backupFile The backup file to save to the database
     * @return Returns the new {@link BackupFile} with the autogenerated ID
     * @throws SQLException in case it fails to save the entity to the database
     */
    public BackupFile save(BackupFile backupFile) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                BACKUP_FILE_INSERT_SQL,
                Statement.RETURN_GENERATED_KEYS
        );

        statement.setString(1, backupFile.getName());
        statement.setString(2, backupFile.getHash());
        statement.setString(3, backupFile.getPath());
        statement.setString(4, backupFile.getType());
        statement.setTimestamp(5, Timestamp.valueOf(backupFile.getNoted()));

        statement.executeUpdate();

        ResultSet generatedKeys = statement.getGeneratedKeys();

        long id = -1;
        if (generatedKeys.next()) {
            id = generatedKeys.getLong(1);
        }

        statement.close();

        return read(id);
    }

    /**
     * Reads a {@link BackupFile} with the provided ID. If no such entity is
     * found in the database then null is returned.
     *
     * @param id A {@link Long} ID
     * @return {@link BackupFile} with the provided ID or null
     * @throws SQLException in case it failed to read from the database.
     */
    public BackupFile read(Long id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(BACKUP_FILE_SELECT_SQL);

        statement.setLong(1, id);

        ResultSet result = statement.executeQuery();

        if (!result.next()) {
            return null;
        }

        BackupFile backupFile = new BackupFile();

        backupFile.setId(id);
        backupFile.setName(result.getString("name"));
        backupFile.setHash(result.getString("hash"));
        backupFile.setPath(result.getString("path"));
        backupFile.setType(result.getString("type"));

        statement.close();

        return backupFile;
    }

    /**
     * Deletes the {@link BackupFile} entity from the database.
     *
     * @param id The backup file ID
     * @return True if the entity was deleted and false otherwise
     * @throws SQLException in case it fails to delete the entity from the
     *                      database
     */
    public boolean delete(Long id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(BACKUP_FILE_DELETE_SQL);

        statement.setLong(1, id);

        boolean isSuccess = statement.executeUpdate() == 1;

        statement.close();

        return isSuccess;
    }

    /**
     * Returns a page of {@link BackupFile} entities in a {@link List}.
     *
     * @param id       Last read {@link BackupFile} ID
     * @param pageSize The size of the page to return
     * @return Returns a page of {@link BackupFile} entities in a {@link List}.
     * @throws SQLException in case it could not page data in the database
     */
    public List<BackupFile> page(Long id, int pageSize) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(BACKUP_FILE_PAGE_SQL);

        statement.setLong(1, id);
        statement.setInt(2, pageSize);

        ResultSet resultSet = statement.executeQuery();
        List<BackupFile> backupFiles = new ArrayList<>();

        while (resultSet.next()) {
            BackupFile backupFile = new BackupFile();

            backupFile.setId(resultSet.getLong("id"));
            backupFile.setName(resultSet.getString("name"));
            backupFile.setHash(resultSet.getString("hash"));
            backupFile.setPath(resultSet.getString("path"));
            backupFile.setType(resultSet.getString("type"));
            backupFile.setNoted(resultSet.getTimestamp("noted").toLocalDateTime());

            backupFiles.add(backupFile);
        }

        statement.close();

        return backupFiles;
    }

    /**
     * Checks if the {@link BackupFile} entity table exists. If not then it creates it.
     *
     * @throws SQLException in case it fails to check or create the table.
     */
    private void configureTable() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(BACKUP_FILE_TABLE_SQL);

        statement.executeUpdate();

        statement.close();
    }

    /**
     * Generates the H2 JDBC URL {@link String} from the environment or user
     * variables if they are set and if not uses default ones.
     *
     * @param name User provided name of the database
     * @return H2 JDBC URL {@link String}
     */
    private String generateJdbcUrl(String name) {
        String dbPahName = System.getenv(DATABASE_ENVIRONMENT_NAME);

        if (name != null) {
            // H2 does not recommend pure name when creating with JDBC parameters
            String sanitizedName = name.matches("^[\\w+.\\-_]+$") ? ("./" + name) : name;

            sanitizedName = sanitizedName.replaceAll(" ", "-");

            return DATABASE_BASE_JDBC_URL + sanitizedName;
        } else if (dbPahName != null) {
            return DATABASE_BASE_JDBC_URL + dbPahName;
        } else {
            return DATABASE_BASE_JDBC_URL + DEFAULT_DATABASE_NAME;
        }
    }
}
