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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

/**
 * Utility class for various helper methods.
 */
final class Utils {
    /**
     * Private constructor to prevent instantiation.
     */
    private Utils() {
        throw new UnsupportedOperationException("Utility class");
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
     * a relative path to the child file.
     *
     * @param parent The parent absolute path
     * @param child  The child file
     * @return The relative path to the child file from the parent.
     */
    public static String resolveAbsoluteParentPathFromChild(String parent, File child) {
        return child.getAbsolutePath().replace(parent, "");
    }

    /**
     * Generates a MD5 checksum {@link String} from the provided file.
     *
     * @param file A {@link File} for whom to generate a checksum.
     * @return A MD5 checksum {@link String}
     * @throws IOException              In case a file operation fails
     * @throws NoSuchAlgorithmException In case the MD5 algorithm is not supported.
     */
    public static String generateMd5Checksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = md.digest();

        StringBuilder hexString = new StringBuilder();

        for (byte b : digest) {
            hexString.append(String.format("%02x", b & 0xff));
        }

        return hexString.toString().toUpperCase();
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
     * Writes the content to a file by appending it to the end of the file. The
     * file must exist and be writable. If the file does not exist, an error
     * message is printed.
     *
     * @param file    The name of the file
     * @param content The content to write to the report file
     */
    public static void writeToFile(String file, String content) {
        File fileToWrite = new File(file);

        if (!fileToWrite.exists()) {
            System.err.println("Error: File does not exist.");
            return;
        }

        try {
            Files.writeString(
                    fileToWrite.toPath(),
                    content,
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
    }
}
