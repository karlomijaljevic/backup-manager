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

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.mijaljevic.backup_manager.Defaults;
import xyz.mijaljevic.backup_manager.utilities.Utils;
import xyz.mijaljevic.backup_manager.utilities.Logger;
import xyz.mijaljevic.backup_manager.database.BackupDatabase;
import xyz.mijaljevic.backup_manager.database.BackupFile;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * Command for validating a directory against a backup database.
 * <p>
 * This command checks if the files in the specified directory exist in the
 * backup database and if their checksums match. It generates a report of the
 * validation results.
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
     * Directory output option. If enabled, the command will print the name
     * of each directory as it is validated.
     */
    @Option(
            names = {"-d", "--directory"},
            description = """
                          Enable directory output. This will print the name of
                          each directory as it is validated. If you wish to
                          print file names as well, use the -v option.
                          """
    )
    boolean directoryOutput;

    /**
     * Directory pathname to validate.
     */
    @Parameters(
            paramLabel = "DIRECTORY",
            description = "A directory to validate"
    )
    String directory;

    /**
     * A {@link BackupDatabase} initialized during the {@link #call()} method.
     */
    private BackupDatabase database;

    /**
     * Absolute path of the root directory. The root directory is the one for
     * whom validation was requested with the <i>directory</i>
     * {@link Parameters} field.
     */
    private String rootDirPath;

    /**
     * Command line entry point.
     *
     * @return exit code
     */
    @Override
    public Integer call() {
        Instant start = Instant.now();
        File dir;

        if (directory == null || !(dir = new File(directory)).isDirectory()) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Please specify directory for validation!"
            );
        }

        rootDirPath = dir.getAbsolutePath();

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

        try {
            database = BackupDatabase.BackupDatabaseBuilder.builder()
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

        if (Utils.initReportFile(reportFileName) != 0) {
            return Utils.processExitAndCalculateTime(
                    start,
                    3,
                    "Failed to create report file!"
            );
        }

        prepareReportFile();

        Utils.traverseDirectoryRecursively(dir, file -> {
            if (verbose) Logger.info("Validating file: " + file.getName());

            String relativePath = Utils.resolveAbsoluteParentPathFromChild(rootDirPath, file);

            try {
                BackupFile backupFile = database.findByPath(relativePath);

                if (backupFile == null) {
                    Utils.writeReport(reportFileName, "EXTRA: " + relativePath);
                } else {
                    String checksum = Utils.generateCrc32Checksum(file);

                    if (!checksum.equals(backupFile.getHash())) {
                        Utils.writeReport(reportFileName, "DIFF: " + relativePath);
                    }
                }
            } catch (SQLException e) {
                Logger.error("SQL exception while validating: " + e.getMessage());
            } catch (IOException e) {
                Logger.error("IO exception while validating: " + e.getMessage());
            }
        }, (directory, processing) -> {
            if (verbose || directoryOutput) {
                String message = processing
                        ? "Validating directory: "
                        : "Finished validating directory: ";
                Logger.info(message + directory.getName());
            }
        });

        try {
            Utils.workOnDatabaseFiles(database, backupFile -> {
                String sanitizeRootPath = rootDirPath.charAt(rootDirPath.length() - 1) == '/'
                        ? rootDirPath.substring(0, rootDirPath.length() - 1)
                        : rootDirPath;

                File file = new File(sanitizeRootPath + backupFile.getPath());

                if (!file.exists()) {
                    Utils.writeReport(reportFileName, "MISS: " + backupFile.getPath());
                }
            });
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
     * Prepares the report file by writing the header information.
     */
    private void prepareReportFile() {
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
