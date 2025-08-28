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

import java.nio.file.FileSystems;
import java.time.format.DateTimeFormatter;

/**
 * Application level defaults.
 */
public final class Defaults {
    /**
     * Environment name where the database pathname is stored.
     */
    public static final String DATABASE_ENVIRONMENT_NAME = "BACKUP_DB";

    /**
     * Default H2 database name to use in case there is no user provided
     * database pathname.
     */
    public static final String DATABASE_NAME = resolveDefaultDatabaseName();

    /**
     * Default database username.
     */
    public static final String DATABASE_USERNAME = "backup";

    /**
     * Default database password.
     */
    public static final String DATABASE_PASSWORD = "backup";

    /**
     * Default report file name for the compare command.
     */
    public static final String REPORT_NAME = "report.txt";

    /**
     * Date format to use for the exported file name.
     */
    public static final DateTimeFormatter EXPORT_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Date format to use for the exported data.
     */
    public static final DateTimeFormatter EXPORT_DATA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Default number of threads to use for file operations.
     *
     * <p>
     * This is set to half the number of available processors to balance
     * performance and resource usage.
     * </p>
     */
    public static final int THREAD_NUMBER = (int) Math.ceil(
            Runtime.getRuntime().availableProcessors() / 2.0
    );

    /**
     * Resolve the default database name based on the FS separator.
     *
     * @return The default database name.
     */
    private static String resolveDefaultDatabaseName() {
        return "." + FileSystems.getDefault().getSeparator() + "backup.db";
    }
}
