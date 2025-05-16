package xyz.mijaljevic.backup_manager.commands;

import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Main command class. This is the entry point for the application.
 * It defines the main command and its subcommands.
 */
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        descriptionHeading = "%nDescription:%n",
        version = "1.0",
        description = """
                      Simple backup manager to check and compare RADI-0
                      backups. There are a couple of usages for this
                      application:
                      
                      1. Index files into an H2 DB
                      2. Compare two directories and report differences
                      3. Validate directory against the H2 DB and report
                         differences
                      4. Export H2 DB as an .xlsx file
                      
                      Most common use-case is to check if files on one external
                      drive match files on the other by comparing them. This
                      compares all CRC32 file hashes and reports differences.
                      """,
        subcommands = {
                IndexCommand.class,
                CompareCommand.class,
                ValidateCommand.class,
                ExportCommand.class
        }
)
public final class MainCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        return 0;
    }
}