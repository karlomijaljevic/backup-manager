# Backup Manager

Simple backup manager to check and compare *"RAID1"* backups. I will use (or I
am using, depends on if the project is finished or not :D) this manager to
compare and index my external drives. There are a couple of usages:

- Index directories into an H2 DB
- Compare two directories and report differences
- Validate directory against the H2 DB and report differences
- Export H2 DB as an `.xlsx` file

In the showcases the `backup-manager` command is an alias for:

```shell
java -jar backup-manager.jar
```

Lastly the default username and password for the H2 database are:

- username: backup
- password: backup

While the default database name is `backup`. You can change the default
database pathname by setting the `BACKUP_DB` environment variable or by using a
command line option. Consult the usage section for more as well as the `-h` or
`--help` option of each command.

## Current Status

The project is in a working state. The following features are implemented:

- Indexing directories into an H2 database
- Comparing two directories and reporting differences
- Validating a directory against the H2 database and reporting differences
- Exporting the H2 database to an `.xlsx` file

The project is built using Java 21 and Maven. The dependencies are managed
using Maven. The project uses the following dependencies:

- H2 Database Engine
- Picocli
- Apache Tika
- fastexcel
- log4j-slf4j2-impl
- log4j-api
- log4j-core
- JUnit 5 (for testing)

Currently, the project is considered stable and ready for use. However, there
may be some bugs or issues that need to be fixed. If you encounter any issues,
please report them on the issues page.

## Note

The hashing algorithm used to check for differences is CRC32. This is a fast
and efficient algorithm that is suitable for checking the integrity of files.
However, it is not suitable for cryptographic purposes. If you need a more
secure hashing algorithm, you can change the hashing algorithm in the code.

Verbose is disabled by default. You can enable it by providing the `-v` or
`--verbose` option. This will enable verbose output for the given command. If
you wish to only see the currently processed directory and not the files
you can provide the `-d` or `--directory` option. This will only show the
directory name and not the files. This is useful if you have a lot of files,
and you only want to see the directories being processed.

## Usage

### Index

The index command will create a new H2 database or update an existing one with
the contents of the specified directory. The database will be created in the
current working directory with the name `backup.db` unless a different pathname
is specified using the `-b` or `--backup` option. This can also be achieved by
setting the `BACKUP_DB` environment variable.

The username and password for the database can be specified using the `-u` or
`--username` and `-p` or `--password` options, respectively. If not specified,
the default username and password will be used.

The directory to be indexed is specified as the first argument. The command will
recursively traverse the directory and add all files and subdirectories to the
database. The command will also check for any existing entries in the database
and update them if necessary.

```shell
backup-manager index <directory> [-b <backup_db>] [-u <username>] [-p <password>]
```

### Compare

The compare command will compare the contents of two directories and report any
differences. The first directory is specified as the first argument, and the
second directory is specified as the second argument. The command will
recursively traverse both directories and compare the contents of each file
and subdirectory. Any differences will be reported to the console or written to
a report file.

The report file can be specified using the `-r` or `--report` option. If not
specified, the report will be written to the console. The report file will be
created in the current working directory with the name `report.txt` unless a
different pathname is specified using the `-r` or `--report` option.

The compare command can also copy the missing files from the first directory to
the second directory. This can be done by providing the `-c` or `--copy-on-diff`
option. This will copy the missing files from the first directory to the
second directory. note that this will overwrite any existing files in the
second directory. Lastly this will not copy over empty directories.

```shell
backup-manager compare [-c] <directory1> <directory2> [-r <report>]
```

### Validate

The validate command will validate the contents of a directory against the
H2 database. The directory to be validated is specified as the first argument.
The command will recursively traverse the directory and compare the contents of
each file and subdirectory with the entries in the database. Any differences
will be reported to the console or written to a report file.

The report file can be specified using the `-r` or `--report` option. If not
specified, the report will be written to the console. The report file will be
created in the current working directory with the name `report.txt` unless a
different pathname is specified using the `-r` or `--report` option.

The database to be validated against can be specified using the `-b` or
`--backup` option. If not specified, the default database will be used. The
username and password for the database can be specified using the `-u` or
`--username` and `-p` or `--password` options, respectively. If not specified,
the default username and password will be used.

If the database is not specified either by the `-b` or `--backup` option or by
the `BACKUP_DB` environment variable, the command will return with a non-zero
exit code.

```shell
backup-manager validate <directory> [-b <backup_db>] [-u <username>] [-p <password>] [-r <report>]
```

### Export

The export command will export the contents of the H2 database to an `.xlsx`
file. The database to be exported is specified as the first and only argument.
The command will create an `.xlsx` file in the current working directory with
the name `yyyy-MM-dd_DB-NAME.xlsx`. The database name will be replaced with the
name of the database being exported. The date will be the current date.

If the database name is not specified, the command will look for the `BACKUP_DB`
environment variable. If the environment variable is not set, the command will
return with a non-zero exit code.

If the database requires a username and password, they can be specified using
the `-u` or `--username` and `-p` or `--password` options, respectively. If not
specified, the default username and password will be used.

```shell
backup-manager export <backup_db> [-u <username>] [-p <password>]
```

## Build

To build the project, you need to have Java 21 and Maven installed. You can
build the project using the following command:

```shell
mvn clean package
```

This will create a JAR file in the `target` directory. The dependencies can be
found in the `libs` directory. You can run the JAR file using the following
command:

```shell
java -jar target/backup-manager-1.0-SNAPSHOT.jar
```

Together with the JAR file, two more files will be created:

- `backup-manager-1.0-SNAPSHOT-jar-with-dependencies.jar`: This is the UBER JAR
  file that contains all the dependencies of the project.
- `backup-manager-1.0-SNAPSHOT-binary.tar.gz`: This is a tarball that contains
  the JAR file and dependencies.

## License

This project is licensed under the GNU General Public License v3.0. See the
[LICENSE](LICENSE) file for details.