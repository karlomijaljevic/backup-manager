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
package xyz.mijaljevic.backup_manager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import xyz.mijaljevic.backup_manager.commands.MainCommand;
import xyz.mijaljevic.backup_manager.database.BackupDatabase;
import xyz.mijaljevic.backup_manager.database.BackupFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the export command.
 */
final class ExportCommandTest {
    /**
     * Database name for testing.
     */
    private static final String DATABASE_NAME = "./test.db";

    /**
     * Prepares the test environment for export testing.
     */
    @BeforeAll
    static void prepareTestEnvironment() {
        try {
            BackupDatabase database = BackupDatabase.BackupDatabaseBuilder.builder()
                    .setName(DATABASE_NAME)
                    .build();

            for (int i = 0; i < 10; i++) {
                BackupFile backupFile = new BackupFile();

                backupFile.setName("test_file_" + i);
                backupFile.setPath("path_" + i);
                backupFile.setHash("hash_" + i);
                backupFile.setType("type_" + i);
                backupFile.setCreated(LocalDateTime.now());
                backupFile.setUpdated(LocalDateTime.now());

                database.save(backupFile);
            }

            database.close();
        } catch (SQLException e) {
            fail("Failed to create database: " + e.getMessage());
        }
    }

    /**
     * Tests the export command.
     */
    @Test
    void testValidation() {
        String[] args = {"export", DATABASE_NAME};

        CommandLine commandLine = new CommandLine(new MainCommand());

        assertEquals(
                0,
                commandLine.execute(args),
                "Export command should have been executed successfully!"
        );

        String path = LocalDateTime.now().format(Defaults.EXPORT_FILE_FORMAT) + "_test.xlsx";

        try {
            assertTrue(Files.deleteIfExists(Paths.get(path)));
        } catch (IOException e) {
            fail("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Clean up the test environment.
     */
    @AfterAll
    static void cleanup() {
        File database = new File(DATABASE_NAME + ".mv.db");

        if (database.exists()) {
            assertTrue(database.delete(), "Failed to delete the database file!");
        }

        File traceDatabase = new File(DATABASE_NAME + ".trace.db");

        if (traceDatabase.exists()) {
            assertTrue(traceDatabase.delete(), "Failed to delete the trace database file!");
        }
    }
}
