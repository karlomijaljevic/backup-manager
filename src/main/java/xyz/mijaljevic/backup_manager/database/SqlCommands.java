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
package xyz.mijaljevic.backup_manager.database;

/**
 * SQL commands used in the database. Supported commands are:
 * <ul>
 *     <li>CREATE_TABLE</li>
 *     <li>INSERT</li>
 *     <li>SELECT</li>
 *     <li>DELETE</li>
 *     <li>UPDATE</li>
 *     <li>PAGE</li>
 *     <li>COUNT</li>
 *     <li>FIND_BY_PATH</li>
 *     <li>LIST_ALL_IDS</li>
 * </ul>
 * The SQL commands are used to create the table, insert data, select data,
 * delete data and page data from the database. Operates only on the
 * {@link BackupFile} entity.
 */
enum SqlCommands {
    CREATE_TABLE("""
            CREATE TABLE IF NOT EXISTS backup_files (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(256) NOT NULL,
                hash VARCHAR(256) NOT NULL,
                path VARCHAR(1024) UNIQUE NOT NULL,
                type VARCHAR(256) NOT NULL,
                created TIMESTAMP NOT NULL,
                updated TIMESTAMP
            );
            """
    ),
    INSERT("""
            INSERT INTO backup_files
            (name, hash, path, type, created, updated) VALUES
            (?, ?, ?, ?, ?, ?);
            """
    ),
    SELECT("""
            SELECT * FROM backup_files
            WHERE id = ?;
            """
    ),
    DELETE("""
            DELETE FROM backup_files
            WHERE id = ?;
            """
    ),
    UPDATE("""
            UPDATE backup_files
            SET name = ?, hash = ?, path = ?, type = ?, created = ?, updated = ?
            WHERE id = ?;
            """
    ),
    PAGE("""
            SELECT * FROM backup_files
            WHERE id > ?
            ORDER BY id
            LIMIT ?;
            """
    ),
    COUNT("""
            SELECT COUNT(*) FROM backup_files;
            """
    ),
    FIND_BY_PATH("""
            SELECT * FROM backup_files
            WHERE path = ?;
            """
    ),
    LIST_ALL_IDS("""
            SELECT id FROM backup_files;
            """
    );

    final String sql;

    SqlCommands(String sql) {
        this.sql = sql;
    }
}
