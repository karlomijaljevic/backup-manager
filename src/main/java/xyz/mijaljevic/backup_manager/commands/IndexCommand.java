/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */
package xyz.mijaljevic.backup_manager.commands;

import org.apache.tika.Tika;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.mijaljevic.backup_manager.Defaults;
import xyz.mijaljevic.backup_manager.database.BackupDatabase;
import xyz.mijaljevic.backup_manager.database.BackupDatabase.Builder;
import xyz.mijaljevic.backup_manager.database.BackupFile;
import xyz.mijaljevic.backup_manager.utilities.Logger;
import xyz.mijaljevic.backup_manager.utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Subcommand for indexing a directory against a database.
 *
 * <p>
 * This command will index the provided directory and all of its
 * subdirectories. If no database is provided, a new one will be created
 * in the working directory named backup.db.
 * </p>
 */
@Command(
        name = "index",
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        descriptionHeading = "%nDescription:%n",
        description = """
                Indexes a directory against a database. If no database
                name is provided the command generates a new database
                during the indexing process in the working directory
                named backup.db.
                
                The database username and password are optional. If not
                provided, the command will use the default values for
                the database. The default values are:
                - username: backup
                - password: backup
                """,
        optionListHeading = "%nIndex Options:%n",
        parameterListHeading = "%nParameters:%n",
        exitCodeListHeading = "%nExit Codes:%n",
        exitCodeList = {
                "0 - Indexing completed successfully",
                "1 - Directory not specified or invalid",
                "2 - Database error"
        }
)
final class IndexCommand implements Callable<Integer> {
    /**
     * Database pathname to use for indexing.
     */
    @Option(
            names = {"-b", "--backup"},
            paramLabel = "DATABASE",
            description = "Pathname of the backup database"
    )
    String dbPath;

    /**
     * Database user to use for indexing.
     */
    @Option(
            names = {"-u", "--user"},
            paramLabel = "USER",
            description = "Database user"
    )
    String user;

    /**
     * Database password to use for indexing.
     */
    @Option(
            names = {"-p", "--password"},
            paramLabel = "PASSWORD",
            description = "Database password"
    )
    String password;

    /**
     * Verbose output option. If enabled, the command will print the name
     * of each file as it is indexed.
     */
    @Option(
            names = {"-v", "--verbose"},
            description = """
                    Enable verbose output. This will print the name of
                    each file as it is indexed.
                    """
    )
    boolean verbose;

    @Option(
            names = {"--no-update"},
            description = """
                    Disable updating existing entries in the database.
                    If a file already exists in the database, it will
                    not be updated with new information.
                    """
    )
    boolean noUpdate;

    @Option(
            names = {"--remove-missing"},
            paramLabel = "REMOVE_MISSING",
            description = """
                    Remove entries from the database that are no longer
                    present in the indexed directory.
                    """
    )
    boolean removeMissing;

    /**
     * Directory pathname to index.
     */
    @Parameters(
            paramLabel = "DIRECTORY",
            description = "A directory to index"
    )
    String directory;

    /**
     * The main method of the {@link IndexCommand} class. It is called
     * when the command is executed.
     *
     * @return An integer representing the exit code of the command.
     */
    @Override
    public Integer call() {
        final Instant start = Instant.now();
        final File dir;

        if (directory == null || !(dir = new File(directory)).isDirectory()) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Please specify directory for indexing!"
            );
        }

        final String rootDirPath = dir.getAbsolutePath();
        final Tika tika = new Tika();

        if (dbPath == null) {
            dbPath = System.getenv(Defaults.DATABASE_ENVIRONMENT_NAME);

            if (dbPath == null) {
                dbPath = Defaults.DATABASE_NAME;
            }
        }

        final BackupDatabase database;
        final List<Long> existingIds;

        try {
            database = Builder.builder()
                    .setPassword(password)
                    .setUsername(user)
                    .setName(dbPath)
                    .build();
        } catch (SQLException e) {
            return Utils.processExitAndCalculateTime(
                    start,
                    2,
                    "Database error: " + e.getMessage()
            );
        }

        if (removeMissing) {
            try {
                existingIds = database.listAllIds();
            } catch (SQLException e) {
                database.close();

                return Utils.processExitAndCalculateTime(
                        start,
                        2,
                        "Database error: " + e.getMessage()
                );
            }
        } else {
            existingIds = new ArrayList<>();
        }

        Logger.info("Staring to index directory: " + rootDirPath);

        Utils.processDirectory(dir, file -> {
            try {
                if (verbose) Logger.info("Indexing file: " + file.getName());

                String path = Utils.resolveAbsoluteParentPathFromChild(
                        rootDirPath,
                        file
                );

                BackupFile backupFile = database.findByPath(path);

                if (backupFile != null) {
                    if (noUpdate) return;

                    backupFile.setHash(Utils.generateCrc32Checksum(file));
                    backupFile.setType(tika.detect(file));
                    backupFile.setUpdated(LocalDateTime.now());

                    database.update(backupFile);

                    if (removeMissing) existingIds.remove(backupFile.getId());
                } else {
                    backupFile = new BackupFile();

                    backupFile.setName(file.getName());
                    backupFile.setHash(Utils.generateCrc32Checksum(file));
                    backupFile.setPath(Utils.resolveAbsoluteParentPathFromChild(
                            rootDirPath,
                            file
                    ));
                    backupFile.setType(tika.detect(file));
                    backupFile.setCreated(LocalDateTime.now());

                    database.save(backupFile);
                }
            } catch (IOException | SQLException e) {
                Logger.error("Error while indexing file: "
                        + file.getAbsolutePath()
                        + " - "
                        + e.getMessage()
                );
            }
        });

        if (removeMissing) removeMissing(database, existingIds);

        database.close();

        return Utils.processExitAndCalculateTime(
                start,
                0,
                "Successfully indexed directory: " + rootDirPath
        );
    }

    /**
     * Remove files from the database that are no longer present
     * in the indexed directory.
     *
     * @param database    The database to remove files from.
     * @param existingIds List of IDs of files that are no longer present.
     */
    private void removeMissing(
            BackupDatabase database,
            List<Long> existingIds
    ) {
        if (database == null || existingIds == null) {
            return;
        }

        for (Long id : existingIds) {
            try {
                if (database.delete(id)) {
                    Logger.info(
                            "Missing file with ID "
                                    + id
                                    + " removed from database."
                    );
                } else {
                    Logger.error(
                            "Failed to remove missing file with ID "
                                    + id
                                    + " from database."
                    );
                }
            } catch (SQLException e) {
                Logger.error("Database error while removing missing file with ID "
                        + id
                        + ": "
                        + e.getMessage()
                );
            }
        }
    }
}
