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
import xyz.mijaljevic.backup_manager.utilities.Utils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the compare command.
 */
final class CompareCommandTest {
    /**
     * Name of the base test directory for comparison testing.
     */
    private static final String BASE_TEST_DIRECTORY = "base-test-directory";

    /**
     * Name of the other test directory for comparison testing.
     */
    private static final String OTHER_TEST_DIRECTORY = "other-test-directory";

    /**
     * Prepares the test environment for comparison.
     */
    @BeforeAll
    static void prepareTestEnvironment() {
        File baseTestDir = new File(BASE_TEST_DIRECTORY);
        assertTrue(baseTestDir.mkdirs(), "Failed to create base test directory!");

        File otherTestDir = new File(OTHER_TEST_DIRECTORY);
        assertTrue(otherTestDir.mkdirs(), "Failed to create other test directory!");

        File baseFile1 = new File(baseTestDir, "file1.txt");
        File baseFile2 = new File(baseTestDir, "file2.txt");
        assertDoesNotThrow(
                () -> {
                    baseFile1.createNewFile();
                    baseFile2.createNewFile();
                },
                "Failed to create files in the base test directory!"
        );

        File otherFile1 = new File(otherTestDir, "file1.txt");
        File otherFile2 = new File(otherTestDir, "file3.txt");
        assertDoesNotThrow(
                () -> {
                    otherFile1.createNewFile();
                    otherFile2.createNewFile();
                },
                "Failed to create files in the other test directory!"
        );

        File baseSubDir = new File(baseTestDir, "sub-directory");
        assertTrue(baseSubDir.mkdirs(), "Failed to create sub-directory in the base test directory!");

        File baseSubFile = new File(baseSubDir, "subFile.txt");
        assertDoesNotThrow(
                baseSubFile::createNewFile,
                "Failed to create file in the sub-directory of the base test directory!"
        );
    }

    /**
     * Tests the comparison process of the compare subcommand.
     */
    @Test
    void testComparison() {
        final String reportFileName = "report.txt";

        String[] args = {"compare", BASE_TEST_DIRECTORY, OTHER_TEST_DIRECTORY, "-c", "-v", "-r", reportFileName};

        CommandLine commandLine = new CommandLine(new MainCommand());

        assertEquals(
                0,
                commandLine.execute(args),
                "Compare command should have been executed successfully!"
        );

        File reportFile = new File(reportFileName);
        assertTrue(reportFile.exists(), "Report file should exist after command execution!");

        assertTrue(
                reportFile.delete(),
                "Failed to delete the report file after command execution!"
        );
    }


    /**
     * Clean up the test environment.
     */
    @AfterAll
    static void cleanUpTestEnvironment() {
        File baseTestDir = new File(BASE_TEST_DIRECTORY);
        assertTrue(
                Utils.deleteDirectoryRecursively(baseTestDir),
                "Failed to delete the base test directory!"
        );
        assertFalse(baseTestDir.exists(), "Failed to delete the base test directory!");

        File otherTestDir = new File(OTHER_TEST_DIRECTORY);
        assertTrue(
                Utils.deleteDirectoryRecursively(otherTestDir),
                "Failed to delete the other test directory!"
        );
        assertFalse(otherTestDir.exists(), "Failed to delete the other test directory!");
    }
}
