# Backup Manager

Simple backup manager to check and compare *"RADI0"* backups. I will use (or I
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
database path/name by setting the `BACKUP_DB` environment variable.

## Usage

### Index

The index command will create a new H2 database or update an existing one with
the contents of the specified directory. The database will be created in the
current working directory with the name `backup.db` unless a different pathname
is specified using the `-b` or `--backup` option. This can also be achieved by
setting the `BACKUP_DB` environment variable.

The username and password for the database can be specified using the `-u` and
`-p` options, respectively. If not specified, the default username and password
will be used.

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

The report file can be specified using the `-r` option. If not specified, the
report will be written to the console. The report file will be created in the
current working directory with the name `report.txt` unless a different
pathname is specified using the `-r` option.

```shell
backup-manager compare <directory1> <directory2> [-r <report>]
```