/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */
package xyz.mijaljevic.backup_manager;

import picocli.CommandLine;
import xyz.mijaljevic.backup_manager.commands.MainCommand;

/**
 * Main application class
 */
public final class Main {
    /**
     * Main method. Class the {@link MainCommand} class while passing command
     * line arguments.
     *
     * @param args Command line {@link String} arguments
     */
    static void main(final String[] args) {
        System.exit(new CommandLine(new MainCommand()).execute(args));
    }
}
