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

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.mijaljevic.backup_manager.Defaults;
import xyz.mijaljevic.backup_manager.database.BackupDatabase;
import xyz.mijaljevic.backup_manager.utilities.Logger;
import xyz.mijaljevic.backup_manager.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Command for exporting the backup database to an Excel file.
 * <p>
 * This command exports the backup database to an Excel file in the current
 * working directory. The file name is the current date and time in the format
 * yyyy-MM-dd_DB-NAME.xlsx.
 */
@Command(
        name = "export",
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        descriptionHeading = "%nDescription:%n",
        description = """
                      Exports the backup database to a Excel file. The database
                      is exported to a file in the current working directory.
                      The file name is the current date and time in the format
                      yyyy-MM-dd_DB-NAME.xlsx.
                      
                      The database pathname is specified as the first argument.
                      If the database is not specified, the environment variable
                      'BACKUP_DB' is used. If the environment variable is not
                      set and no pathname is provided, the command will exit
                      with an error.
                      """,
        optionListHeading = "%nExport Options:%n",
        parameterListHeading = "%nParameters:%n",
        exitCodeListHeading = "%nExit Codes:%n",
        exitCodeList = {
                "0 - Export completed successfully",
                "1 - Database not specified or invalid",
                "2 - Failed to connect to the database",
                "3 - Failed to export the database",
                "4 - Failed to create the export file"
        }
)
final class ExportCommand implements Callable<Integer> {
    /**
     * Database user to use for export.
     */
    @Option(
            names = {"-u", "--user"},
            paramLabel = "USER",
            description = "Database user"
    )
    String user;

    /**
     * Database password to use for export.
     */
    @Option(
            names = {"-p", "--password"},
            paramLabel = "PASSWORD",
            description = "Database password"
    )
    String password;

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
     * Database pathname to use for export.
     */
    @Parameters(
            paramLabel = "DATABASE",
            description = """
                          Pathname of the backup database to export to an Excel
                          file. If not specified, the environment variable
                          'BACKUP_DB' is used. If the environment variable is
                          not set and no pathname is provided, the command will
                          exit with an error.
                          """,
            defaultValue = "NOT_SET"
    )
    String dbPath;

    /**
     * The main method of the export command.
     *
     * @return 0 if the export was successful, 1 if the database was not
     * specified, 2 if the database could not be connected to, 3 if
     * the export failed, 4 if the export file could not be created.
     */
    @Override
    public Integer call() {
        Instant start = Instant.now();

        if (dbPath == null || "NOT_SET".equals(dbPath)) {
            dbPath = System.getenv(Defaults.DATABASE_ENVIRONMENT_NAME);

            if (dbPath == null) {
                return Utils.processExitAndCalculateTime(
                        start,
                        1,
                        "Please specify a database to export!"
                );
            }
        }

        BackupDatabase database;

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

        String[] dbTokens = dbPath.split(Pattern.quote(File.separator));
        String dbName = dbTokens[dbTokens.length - 1].replace(".db", "");

        StringBuilder exportName = new StringBuilder();

        exportName.append(LocalDate.now().format(Defaults.EXPORT_FILE_FORMAT));
        exportName.append("_");
        exportName.append(dbName);
        exportName.append(".xlsx");

        try {
            Path outputPath = Path.of(exportName.toString());
            FileOutputStream stream = new FileOutputStream(outputPath.toFile());

            Workbook workbook = new Workbook(
                    stream,
                    "backup-manager",
                    "1.0"
            );

            Worksheet sheet = workbook.newWorksheet("export");

            AtomicInteger rowAccumulator = new AtomicInteger(1);

            sheet.value(0, 0, "ID");
            sheet.value(0, 1, "Name");
            sheet.value(0, 2, "Hash");
            sheet.value(0, 3, "Type");
            sheet.value(0, 4, "Path");
            sheet.value(0, 5, "Created");
            sheet.value(0, 6, "Updated");

            Utils.workOnDatabaseFiles(database, backupFile -> {
                if (verbose) Logger.info("Exporting file: " + backupFile.getName());

                int row = rowAccumulator.getAndIncrement();

                sheet.value(row, 0, backupFile.getId());
                sheet.value(row, 1, backupFile.getName());
                sheet.value(row, 2, backupFile.getHash());
                sheet.value(row, 3, backupFile.getType());
                sheet.value(row, 4, backupFile.getPath());
                sheet.value(row, 5, backupFile.getCreated().format(Defaults.EXPORT_DATA_FORMAT));
                sheet.value(row, 6, backupFile.getUpdated().format(Defaults.EXPORT_DATA_FORMAT));
            });

            workbook.finish();
            workbook.close();
        } catch (SQLException e) {
            return Utils.processExitAndCalculateTime(
                    start,
                    3,
                    "SQL exception while exporting: " + e.getMessage()
            );
        } catch (IOException e) {
            return Utils.processExitAndCalculateTime(
                    start,
                    4,
                    "IO exception while exporting: " + e.getMessage()
            );
        } finally {
            database.close();
        }

        return Utils.processExitAndCalculateTime(
                start,
                0,
                "Successfully exported database: " + dbPath
        );
    }
}
