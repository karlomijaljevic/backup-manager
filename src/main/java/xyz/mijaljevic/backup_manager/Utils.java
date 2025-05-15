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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.CRC32;

/**
 * Utility class for various helper methods.
 */
final class Utils {
    /**
     * Default buffer size for file operations.
     */
    private static final int BUFFER_SIZE = 65536;

    /**
     * Private constructor to prevent instantiation.
     */
    private Utils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated!");
    }

    /**
     * Checks if a string is empty or null. If the string is null or empty
     * returns true, otherwise false.
     *
     * @param str the string to check
     * @return true if the string is null or empty, false otherwise
     */
    public static boolean isStringEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Removes the parents absolute path from the child file path. Leaving
     * a relative path to the child file from the parent.
     *
     * @param parent The parent absolute path
     * @param child  The child file
     * @return The relative path to the child file from the parent.
     */
    public static String resolveAbsoluteParentPathFromChild(String parent, File child) {
        return child.getAbsolutePath().replace(parent, "");
    }

    /**
     * Generates a CRC-32 checksum for the given file.
     *
     * @param file The file to generate the checksum for
     * @return The CRC-32 checksum as a hexadecimal string
     * @throws IOException If an I/O error occurs
     */
    public static String generateCrc32Checksum(File file) throws IOException {
        CRC32 crc = new CRC32();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }

        return String.format("%08X", crc.getValue()).toUpperCase();
    }

    /**
     * Traverses a directory and all of its subdirectories. For each file in
     * the directory, the provided callback is called. No operation is executed
     * on directories.
     *
     * @param directory The root directory to traverse
     * @param callback  The callback to call for each file
     */
    public static void traverseDirectoryRecursively(File directory, Consumer<File> callback) {
        File[] files = directory.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                traverseDirectoryRecursively(file, callback);
            } else {
                callback.accept(file);
            }
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory Directory to delete.
     * @return True if it is successful false otherwise.
     */
    public static boolean deleteDirectoryRecursively(File directory) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                deleteDirectoryRecursively(file);
            }
        }

        return directory.delete();
    }

    /**
     * Initializes the report file by creating it if it does not exist. If the
     * report file name is null, the report will be printed to the console.
     *
     * @param reportFileName The name of the report file
     * @return 0 if successful, 1 if there was an error
     */
    public static int initReportFile(String reportFileName) {
        if (reportFileName == null) {
            System.out.println("Report will be printed to the console.");
        } else {
            reportFileName = reportFileName.isEmpty() ? Defaults.REPORT_NAME : reportFileName;

            try {
                Files.newOutputStream(
                        Path.of(reportFileName),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                ).close();

                System.out.println("Report will be saved to: " + reportFileName);
            } catch (IOException e) {
                System.err.println("Error creating report file: " + e.getMessage());
                return 1;
            }
        }

        return 0;
    }

    /**
     * Prints the content to the report file or console. If the report file
     * name is not provided, the content is printed to the console.
     *
     * @param reportFileName The name of the report file
     * @param reportContent  The content to be printed.
     */
    public static void writeReport(
            String reportFileName,
            String reportContent
    ) {
        if (reportFileName == null) {
            System.out.println(reportContent);
        } else {
            try {
                File fileToWrite = new File(reportFileName);

                if (!fileToWrite.exists()) {
                    System.err.println("Error: File does not exist.");
                    return;
                }

                try {
                    Files.writeString(
                            fileToWrite.toPath(),
                            reportContent,
                            StandardOpenOption.APPEND
                    );

                    Files.writeString(
                            fileToWrite.toPath(),
                            System.lineSeparator(),
                            StandardOpenOption.APPEND
                    );
                } catch (IOException e) {
                    System.err.println("Error writing to file: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Error writing to report file: " + e.getMessage());
            }
        }
    }

    /**
     * Iterates over all database files in chunks and applies the provided
     * callback to each file.
     *
     * @param database The database to iterate over
     * @param callback The callback to apply to each file
     * @throws SQLException If a database error occurs
     */
    public static void workOnDatabaseFiles(
            BackupDatabase database,
            Consumer<BackupFile> callback
    ) throws SQLException {
        final long count = database.count();
        final int pageSize = 100;
        long position = 0;

        while (position <= count) {
            List<BackupFile> files = database.page(position, pageSize);

            files.forEach(callback);

            position += pageSize;
        }
    }

    /**
     * Reports the exit code and calculates the time taken for the program to
     * execute. It also prints a message indicating the duration of the
     * program.
     * <p>
     * Depending on the exit code, it will print the message to either the
     * standard output or the error output stream. Any non-zero exit code
     * indicates an error.
     *
     * @param start    The start time of the program
     * @param exitCode The exit code of the program
     * @param message  The message to print
     * @return The exit code of the program
     */
    public static int reportExitAndCalculateTime(
            Instant start,
            int exitCode,
            String message
    ) {
        long duration = Duration.between(start, Instant.now()).toMillis();
        String response = "Program lasted for ";

        if (duration < 1000) {
            response = response + duration + " ms";
        } else if (duration < 60000) {
            response = response + (duration / 1000) + " s";
        } else if (duration < 3600000) {
            response = response + (duration / 60000) + " min";
        } else {
            response = response + (duration / 3600000) + " h";
        }

        if (exitCode != 0) {
            System.err.println(message);
        } else {
            System.out.println(message);
        }

        System.out.println(response);

        return exitCode;
    }
}
