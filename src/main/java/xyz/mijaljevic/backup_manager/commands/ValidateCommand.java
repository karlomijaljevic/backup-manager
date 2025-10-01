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

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.mijaljevic.backup_manager.Defaults;
import xyz.mijaljevic.backup_manager.database.BackupDatabase;
import xyz.mijaljevic.backup_manager.database.BackupFile;
import xyz.mijaljevic.backup_manager.utilities.Logger;
import xyz.mijaljevic.backup_manager.utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * Command for validating a directory against a backup database.
 *
 * <p>
 * This command checks if the files in the specified directory exist in the
 * backup database and if their checksums match. It generates a report of the
 * validation results.
 * </p>
 */
@Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        descriptionHeading = "%nDescription:%n",
        description = """
                Validates a directories files against the files in the
                specified database and generates a report.
                
                If no database is provided the command will first look
                for an environment variable named BACKUP_DB and if not
                found it will exit with an error.
                
                The database username and password are optional. If not
                provided, the command will use the default values for
                the database. The default values are:
                - username: backup
                - password: backup
                
                It works similar to the compare command, but instead of
                comparing two directories, it checks if the files in the
                specified directory exist in the database and if their
                checksums match.
                
                The report is generated in a human-readable format. The
                report can be saved to a file or printed to the console.
                The report contains the list of files that are different,
                missing, or extra in the directory compared to the
                database.
                """,
        optionListHeading = "%nValidate Options:%n",
        parameterListHeading = "%nParameters:%n",
        exitCodeListHeading = "%nExit Codes:%n",
        exitCodeList = {
                "0 - Validation completed successfully",
                "1 - Directory not specified or invalid",
                "2 - Database not specified or invalid",
                "3 - Failed to create report file"
        }
)
final class ValidateCommand implements Callable<Integer> {
    /**
     * Database pathname to use for validation.
     */
    @Option(
            names = {"-b", "--backup"},
            paramLabel = "DATABASE",
            description = "Pathname of the backup database"
    )
    String dbPath;

    /**
     * Database user to use for validation.
     */
    @Option(
            names = {"-u", "--user"},
            paramLabel = "USER",
            description = "Database user"
    )
    String user;

    /**
     * Database password to use for validation.
     */
    @Option(
            names = {"-p", "--password"},
            paramLabel = "PASSWORD",
            description = "Database password"
    )
    String password;

    /**
     * Report file name.
     */
    @Option(
            names = {"-r", "--report"},
            paramLabel = "REPORT",
            description = """
                    Name of the report file. If not specified, the report
                    is printed to the console. If specified but without a
                    value then the report is saved in the working
                    directory with the name "report.txt".
                    """
    )
    String reportFileName;

    /**
     * Verbose output option. If enabled, the command will print the name
     * of each file as it is validated.
     */
    @Option(
            names = {"-v", "--verbose"},
            description = """
                    Enable verbose output. This will print the name of
                    each file as it is validated. If you wish to print
                    only directory names, use the -d option.
                    """
    )
    boolean verbose;

    /**
     * Directory pathname to validate.
     */
    @Parameters(
            paramLabel = "DIRECTORY",
            description = "A directory to validate"
    )
    String directory;

    /**
     * Command line entry point.
     *
     * @return exit code
     */
    @Override
    public Integer call() {
        final Instant start = Instant.now();
        final File dir;

        if (directory == null || !(dir = new File(directory)).isDirectory()) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Please specify directory for validation!"
            );
        }

        if (dbPath == null) {
            dbPath = System.getenv(Defaults.DATABASE_ENVIRONMENT_NAME);

            if (dbPath == null) {
                return Utils.processExitAndCalculateTime(
                        start,
                        2,
                        "Please specify database for validation!"
                );
            }
        }

        if (Utils.initReportFile(reportFileName) != 0) {
            return Utils.processExitAndCalculateTime(
                    start,
                    3,
                    "Failed to create report file!"
            );
        }

        final BackupDatabase database;

        try {
            database = BackupDatabase.Builder.builder()
                    .setPassword(password)
                    .setUsername(user)
                    .setName(dbPath)
                    .appendIfExists()
                    .build();
        } catch (SQLException e) {
            return Utils.processExitAndCalculateTime(
                    start,
                    2,
                    "Failed to connect to the database: " + e.getMessage()
            );
        }

        final String rootDirPath = dir.getAbsolutePath();

        prepareReportFile(rootDirPath);

        processDirectory(
                dir,
                rootDirPath,
                database
        );

        try {
            workOnDatabaseFiles(
                    database,
                    rootDirPath
            );
        } catch (SQLException e) {
            Logger.error("SQL exception while validating: " + e.getMessage());
        } finally {
            database.close();
        }

        return Utils.processExitAndCalculateTime(
                start,
                0,
                "Successfully validated directory: " + rootDirPath
        );
    }

    /**
     * Processes a directory and validates its files against the backup
     * database.
     *
     * @param dir         The directory to process.
     * @param rootDirPath The root directory path being validated.
     * @param database    The backup database to validate against.
     */
    private void processDirectory(
            final File dir,
            final String rootDirPath,
            final BackupDatabase database
    ) {
        Utils.processDirectory(dir, file -> {
            if (verbose) Logger.info("Validating file: " + file.getName());

            final String relativePath = Utils.resolveAbsoluteParentPathFromChild(rootDirPath, file);

            try {
                final BackupFile backupFile = database.findByPath(relativePath);

                if (backupFile == null) {
                    Utils.writeReport(reportFileName, "EXTRA: " + relativePath);
                } else {
                    final String checksum = Utils.generateCrc32Checksum(file);

                    if (!checksum.equals(backupFile.getHash())) {
                        Utils.writeReport(reportFileName, "DIFF: " + relativePath);
                    }
                }
            } catch (SQLException e) {
                Logger.error("SQL exception while validating: " + e.getMessage());
            } catch (IOException e) {
                Logger.error("IO exception while validating: " + e.getMessage());
            }
        });
    }

    /**
     * Works on the database files and checks if they exist in the
     * specified directory. If a file is missing, it is reported.
     *
     * @param database    The backup database to work on.
     * @param rootDirPath The root directory path being validated.
     * @throws SQLException If an SQL error occurs while accessing the database.
     */
    private void workOnDatabaseFiles(
            final BackupDatabase database,
            final String rootDirPath
    ) throws SQLException {
        Utils.workOnDatabaseFiles(database, backupFile -> {
            final String sanitizeRootPath = rootDirPath.charAt(rootDirPath.length() - 1) == '/'
                    ? rootDirPath.substring(0, rootDirPath.length() - 1)
                    : rootDirPath;

            final File file = new File(sanitizeRootPath + backupFile.getPath());

            if (!file.exists()) {
                Utils.writeReport(reportFileName, "MISS: " + backupFile.getPath());
            }
        });
    }

    /**
     * Prepares the report file by writing the header information.
     *
     * @param rootDirPath The root directory path being validated.
     */
    private void prepareReportFile(
            String rootDirPath
    ) {
        Utils.writeReport(reportFileName, "==================== VALIDATION REPORT ===================");
        Utils.writeReport(reportFileName, "Report generated on: " + LocalDateTime.now());
        Utils.writeReport(reportFileName, "Directory: " + rootDirPath);
        Utils.writeReport(reportFileName, "Database: " + dbPath);
        Utils.writeReport(reportFileName, "DIFF - Stands for different files due to CRC32 checksum");
        Utils.writeReport(reportFileName, "MISS - Stands for missing files in the directory");
        Utils.writeReport(reportFileName, "EXTRA - Stands for extra files in the directory");
        Utils.writeReport(reportFileName, "==========================================================");
    }
}
