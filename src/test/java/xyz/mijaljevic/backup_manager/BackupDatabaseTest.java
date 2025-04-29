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

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link BackupDatabase} class and its methods.
 */
class BackupDatabaseTest {
    /**
     * Single {@link BackupDatabase} instance used for all the tests and
     * generated in the <i>testDatabaseCreation()</i> method.
     */
    private static BackupDatabase db;

    /**
     * Database name for testing.
     */
    private static final String DATABASE_NAME = "./test.db";

    /**
     * Database username for testing.
     */
    private static final String DATABASE_USER = "test";

    /**
     * Database password for testing.
     */
    private static final String DATABASE_PASSWORD = "test";

    /**
     * Tests the database and table creation.
     */
    @BeforeAll
    static void testDatabaseCreation() {
        assertDoesNotThrow(
                () -> {
                    db = BackupDatabase.BackupDatabaseBuilder.builder()
                            .setPassword(DATABASE_PASSWORD)
                            .setUsername(DATABASE_USER)
                            .setName(DATABASE_NAME)
                            .build();
                },
                "SQL exception occurred while trying to initialize a BackupDatabase"
        );
    }

    /**
     * Tests the database CRUD operations.
     */
    @Test
    void testBackupFileCrud() {
        BackupFile backupFile = new BackupFile();

        backupFile.setName("test.txt");
        backupFile.setHash("VERY_MD5_HASH");
        backupFile.setPath("/tmp/test.txt");
        backupFile.setType("text/plain");
        backupFile.setCreated(LocalDateTime.now());

        BackupFile createdFile = null;
        try {
            createdFile = db.save(backupFile);

            assertNotNull(createdFile.getId());
        } catch (SQLException e) {
            fail("Test threw an SQLException while trying to create the backup file entity!");
        }

        createdFile.setHash("NEW_HASH");
        createdFile.setType("text/markdown");
        createdFile.setUpdated(LocalDateTime.now());

        try {
            assertTrue(db.update(createdFile));
        } catch (SQLException e) {
            fail("Test threw an SQLException while trying to update the backup file entity!");
        }

        try {
            assertTrue(db.delete(createdFile.getId()));
        } catch (SQLException e) {
            fail("Test threw an SQLException while trying to delete the backup file entity!");
        }
    }

    /**
     * Tests database paging
     */
    @Test
    void testBackupFilePaging() {
        assertDoesNotThrow(
                () -> db.page(0L, 100),
                "Test threw an SQLException while trying to page the database!"
        );
    }

    /**
     * Closes the database
     */
    @AfterAll
    static void testDatabaseClose() {
        assertDoesNotThrow(
                () -> db.close(),
                "Failed to close the database!"
        );

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
