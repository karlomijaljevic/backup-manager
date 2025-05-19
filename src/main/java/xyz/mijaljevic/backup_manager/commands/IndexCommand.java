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
package xyz.mijaljevic.backup_manager.commands;

import org.apache.tika.Tika;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.mijaljevic.backup_manager.Defaults;
import xyz.mijaljevic.backup_manager.utilities.Utils;
import xyz.mijaljevic.backup_manager.utilities.Logger;
import xyz.mijaljevic.backup_manager.database.BackupDatabase;
import xyz.mijaljevic.backup_manager.database.BackupDatabase.BackupDatabaseBuilder;
import xyz.mijaljevic.backup_manager.database.BackupFile;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * Subcommand for indexing a directory against a database.
 * <p>
 * This command will index the provided directory and all of its
 * subdirectories. If no database is provided, a new one will be created
 * in the working directory named backup.db.
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
                          each file as it is indexed. If you wish to print only
                          directory names, use the -d option.
                          """
    )
    boolean verbose;

    /**
     * Directory output option. If enabled, the command will print the name
     * of each directory as it is indexed.
     */
    @Option(
            names = {"-d", "--directory"},
            description = """
                          Enable directory output. This will print the name of
                          each directory as it is indexed. If you wish to print
                          file names as well, use the -v option.
                          """
    )
    boolean directoryOutput;

    /**
     * Directory pathname to index.
     */
    @Parameters(
            paramLabel = "DIRECTORY",
            description = "A directory to index"
    )
    String directory;

    /**
     * A {@link BackupDatabase} initialized during the {@link #call()} method.
     */
    private BackupDatabase database;

    /**
     * Absolute path of the root directory. The root directory is the one for
     * whom indexing was requested with the <i>directory</i> {@link Parameters}
     * field.
     */
    private String rootDirPath;

    /**
     * {@link Tika} instance used to detect file MIME types.
     */
    private Tika tika;

    /**
     * The main method of the {@link IndexCommand} class. It is called
     * when the command is executed.
     *
     * @return An integer representing the exit code of the command.
     */
    @Override
    public Integer call() {
        Instant start = Instant.now();
        File dir;

        if (directory == null || !(dir = new File(directory)).isDirectory()) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Please specify directory for indexing!"
            );
        }

        rootDirPath = dir.getAbsolutePath();

        tika = new Tika();

        if (dbPath == null) {
            dbPath = System.getenv(Defaults.DATABASE_ENVIRONMENT_NAME);

            if (dbPath == null) {
                dbPath = Defaults.DATABASE_NAME;
            }
        }

        try {
            database = BackupDatabaseBuilder.builder()
                    .setPassword(password)
                    .setUsername(user)
                    .setName(dbPath)
                    .build();

            Utils.traverseDirectoryRecursively(dir, file -> {
                try {
                    if (verbose) Logger.info("Indexing file: " + file.getName());

                    String path = Utils.resolveAbsoluteParentPathFromChild(rootDirPath, file);

                    BackupFile backupFile = database.findByPath(path);

                    if (backupFile != null) {
                        backupFile.setHash(Utils.generateCrc32Checksum(file));
                        backupFile.setType(tika.detect(file));
                        backupFile.setUpdated(LocalDateTime.now());

                        database.update(backupFile);
                    } else {
                        backupFile = new BackupFile();

                        backupFile.setName(file.getName());
                        backupFile.setHash(Utils.generateCrc32Checksum(file));
                        backupFile.setPath(Utils.resolveAbsoluteParentPathFromChild(rootDirPath, file));
                        backupFile.setType(tika.detect(file));
                        backupFile.setCreated(LocalDateTime.now());

                        database.save(backupFile);
                    }
                } catch (IOException | SQLException e) {
                    Logger.error("Error while indexing file: " + file.getAbsolutePath() + " - " + e.getMessage());
                }
            }, (directory, processing) -> {
                if (verbose || directoryOutput) {
                    String message = processing
                            ? "Indexing directory: "
                            : "Finished indexing directory: ";
                    Logger.info(message + directory.getName());
                }
            });
        } catch (SQLException e) {
            return Utils.processExitAndCalculateTime(
                    start,
                    2,
                    "Database error: " + e.getMessage()
            );
        } finally {
            if (database != null) {
                database.close();
            }
        }

        return Utils.processExitAndCalculateTime(
                start,
                0,
                "Successfully indexed directory: " + rootDirPath
        );
    }
}
