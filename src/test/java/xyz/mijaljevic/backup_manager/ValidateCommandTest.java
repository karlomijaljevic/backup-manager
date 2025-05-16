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
import xyz.mijaljevic.backup_manager.utilities.Utils;

import java.io.File;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the validate command.
 */
final class ValidateCommandTest {
    /**
     * Database name for testing.
     */
    private static final String DATABASE_NAME = "./test.db";

    /**
     * Name of the test directory for validation testing.
     */
    private static final String TEST_DIRECTORY = "test-directory";

    /**
     * Prepares the test environment for validation by creating a test
     * directory with a couple of files.
     */
    @BeforeAll
    static void prepareTestEnvironment() {
        File testDir = new File(TEST_DIRECTORY);
        assertTrue(testDir.mkdirs(), "Failed to create test directory!");

        File first = new File(testDir, "file1.txt");
        File second = new File(testDir, "file2.txt");

        assertDoesNotThrow(
                () -> {
                    first.createNewFile();
                    second.createNewFile();
                },
                "Failed to create test files!"
        );

        File subDir = new File(testDir, "sub-directory");
        assertTrue(subDir.mkdirs(), "Failed to create sub-directory!");

        File subFile = new File(subDir, "subFile.txt");

        assertDoesNotThrow(subFile::createNewFile, "Failed to create test files!");

        try {
            BackupDatabase database = BackupDatabase.BackupDatabaseBuilder.builder()
                    .setName(DATABASE_NAME)
                    .build();

            database.close();
        } catch (SQLException e) {
            fail("Failed to create database: " + e.getMessage());
        }
    }

    /**
     * Tests the validate command.
     */
    @Test
    void testValidation() {
        String[] args = {"validate", "-b", DATABASE_NAME, TEST_DIRECTORY};

        CommandLine commandLine = new CommandLine(new MainCommand());

        assertEquals(
                0,
                commandLine.execute(args),
                "Validate command should have been executed successfully!"
        );
    }

    /**
     * Clean up the test environment.
     */
    @AfterAll
    static void cleanup() {
        File testDir = new File(TEST_DIRECTORY);

        assertTrue(
                Utils.deleteDirectoryRecursively(testDir),
                "Failed to delete the test directory!"
        );

        assertFalse(testDir.exists(), "Failed to delete the test directory!");

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
