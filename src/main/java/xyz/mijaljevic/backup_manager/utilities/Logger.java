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
package xyz.mijaljevic.backup_manager.utilities;

import org.slf4j.LoggerFactory;

/**
 * Logger class for logging purposes.
 * This class is a singleton and provides a single instance of the logger.
 */
public final class Logger {
    /**
     * A single instance of the org.slf4j.Logger class for logging purposes.
     */
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private Logger() {
        throw new UnsupportedOperationException("Logger class cannot be instantiated!");
    }

    /**
     * Logs an INFO message.
     *
     * @param message The INFO message to log
     */
    public static void info(String message) {
        LOGGER.info(message);
    }

    /**
     * Logs an ERROR message.
     *
     * @param message The ERROR message to log
     */
    public static void error(String message) {
        LOGGER.error(message);
    }
}
