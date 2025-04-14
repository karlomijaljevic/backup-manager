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
package xyz.mijaljevic;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Backup manager command. The main and only {@link Callable} served by
 * Picocli.
 */
@Command(
        name = "backup-manager",
        mixinStandardHelpOptions = true,
        version = "1.0-SNAPSHOT",
        description = """
                      Manages the backup files database for drives in RAID-0 configuration. This
                      program does not perform any backups itself it just compares external/local file
                      systems where the user points for configured directories and checks that files
                      are present on both devices.
                      """,
        exitCodeListHeading = "Exit codes (meaning):%n",
        exitCodeList = {
                "0: Program executed successfully"
        }
)
final class BackupManager implements Callable<Integer> {
    @Override
    public Integer call() {
        return 0;
    }
}
