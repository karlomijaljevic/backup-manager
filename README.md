# Backup Manager

Simple backup manager to check and compare *"RADI0"* backups. I will use (or I
am using, depends on if the project is finished or not :D) this manager to
compare and index my external drives. There are a couple of usages:

- Index files into an H2 DB
- Compare two directories (external drive or FS) and report differences
- Compare directory (external drive or FS) to the H2 DB and report differences
- Export H2 DB as an `.xlsx` file

In the showcases the `backup-manager` command is an alias for:

```shell
java -jar backup-manager.jar
```

Lastly the default username and password for the H2 database are:

- username: backup
- password: backup

## Backup file indexing

The application will index files and store them in the H2 DB. The entity
representing a file is `BackupFile`:

| Column Name | Data Type     | Constraints | Description                          |
|-------------|---------------|-------------|--------------------------------------|
| id          | BIGINT        | PRIMARY KEY | Unique identifier for each record    |
| name        | VARCHAR(256)  | NOT NULL    | Name of the file                     |
| hash        | VARCHAR(256)  | NOT NULL    | MD5 file hash                        |
| path        | VARCHAR(1024) | NOT NULL    | Full path from the root of the drive |
| type        | VARCHAR(256)  | NOT NULL    | MIME type of the file                |
| noted       | TIMESTAMP     | NOT NULL    | When the entity was noted/created    |

As you can see all the important file information is stored here. The MIME type
is extracted using the [Apache Tika](https://tika.apache.org/) dependency.

```shell
# Indexing the directory into the database
backup-manager -i directory/

# Indexing the directory into the database, provide database path/name
backup-manager -i directory/ ./backup.db

# Indexing flag long name
backup-manager --index directory/ ./backup.db
```

As you can see to index a directory only specifying the directory to index is
enough. If the only parameter is the directory the application will generate a
database in the current directory named `backup.db`. If the environment
property `BACKUP_DB` is set the database will be generated there with that name.

Dully note that when running this command that if there was a prior database it
will be *replaced* with the new one. Since any files not present in the
directory but present in the DB will be removed from the DB. While files present
in the directory and not in the DB will be added.

## Comparing two external drives or directories

The application can compare two directories (which can be external drives or
file systems). Dully note that in both cases of comparison specified bellow the
program will report hash differences by specifying the hash of the compared to
as the *wrong* hash.

### Default comparison

During comparison, it will use one directory as the base (the first parameter)
of comparison while the other will be compared to and only the files missing
from the second one will be reported.

```shell
# Compare two directories
backup-manager -c first-directory/ second-directory/

# Comparing flag long name
backup-manager --compare first-directory/ second-directory/
```

### Show all differences

During comparison, it will report all the files present in one and not the
other for both the directories.

```shell
# Compare two directories, report all differences
backup-manager -ca first-directory/ second-directory/

# Comparing all flags long name
backup-manager --compare --all first-directory/ second-directory/
```

## Compare directory to H2 DB

This option will compare a directory to the H2 DB index. The DB is the
source of truth and anything not matching the DB or not being in th DB is
reported as an *issue*.

```shell
# Compares directory to H2 DB (DB is default choice if none specified)
backup-manager -c "directory/"

# Compares directory to H2 DB
backup-manager -c directory/ ./backup.db

# Comparing flag long name, same as with directories
backup-manager --compare directory/ ./backup.db
```

As you can see to compare with the H2 DB only specifying the directory to
compare with is enough. But for this to work you should either specify the
database location using the environment variable `BACKUP_DB` or by executing
the command in the directory where the database is located.

## Generating an `.xlsx` file

To achieve this the [fastexcel](https://github.com/dhatim/fastexcel) library
was used. The application generates an `.xlsx` file containing all the data
from the H2 DB.

```shell
# Generates .xlsx file from H2 DB
backup-manager -g

# Generates .xlsx file from H2 DB, provide database path/name
backup-manager -g ./backup.db

# Generating flag long name
backup-manager --generate ./backup.db
```

As you can see to generate an H2 DB `.xlsx` file only specifying the flag to
generate is enough. But for this to work you should either specify the database
location using the environment variable `BACKUP_DB` or by executing the command
in the directory where the database is located.

## Additional flags and parameters

In this section additional flags and parameters used by the program are
specified.

### Database parameters

The H2 database can be further *customized* when called during the command line
for all operations associated with the database.

#### 1. Provide username and password

In case the database is your own e.g. either you have created it or you wish
that upon creation it uses a different username and password you can provide it
via the command line:

```shell
# Once you execute this you will be prompted for the username and password
# for the backup.db database
backup-manager --compare directory/ ./backup.db --auth
```