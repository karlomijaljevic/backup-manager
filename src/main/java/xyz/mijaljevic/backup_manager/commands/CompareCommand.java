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
package xyz.mijaljevic.backup_manager.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import xyz.mijaljevic.backup_manager.utilities.Logger;
import xyz.mijaljevic.backup_manager.utilities.Utils;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * CompareCommand is a command-line utility that compares two directories and
 * generates a report of the differences. The report can be saved to a file or
 * printed to the console.
 * <p>
 * The command can also be used to copy files that are different in the second
 * directory compared to the first directory. This is done by using the
 * --copy-on-diff option. Take heed that this option will overwrite the files
 * on the second directory if they are different. Use with caution.
 */
@Command(
        name = "compare",
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        descriptionHeading = "%nDescription:%n",
        description = """
                      Compares two directories and generates a report of the
                      differences. The report is either saved in the working
                      directory or printed to the console. The report contains
                      the list of files that are different, missing, or
                      extra in the second directory compared to the first
                      directory. The report is generated in a human-readable
                      format.
                      
                      The command can also be used to copy the files that are
                      different in the second directory compared to the first
                      directory. This is done by using the --copy-on-diff
                      option. Take heed that this option will overwrite the
                      files on the second directory if they are different. Use
                      with caution.
                      
                      Lastly it will not copy empty directories, only files.
                      """,
        optionListHeading = "%nCompare Options:%n",
        parameterListHeading = "%nParameters:%n",
        exitCodeListHeading = "%nExit Codes:%n",
        exitCodeList = {
                "0 - Comparison completed successfully",
                "1 - Directory not specified or invalid",
                "2 - Failed to create report file"
        }
)
final class CompareCommand implements Callable<Integer> {
    /**
     * Report file name.
     */
    @Option(
            names = {"-r", "--report"},
            paramLabel = "REPORT",
            description = """
                          Name of the report file. If not specified, the report
                          is printed to the console. If specified but without a
                          value then the report is saved in the working
                          directory with the name "report.txt".
                          """
    )
    String reportFileName;

    /**
     * Verbose output option. If enabled, the command will print the name
     * of each file as it is compared.
     */
    @Option(
            names = {"-v", "--verbose"},
            description = """
                          Enable verbose output. This will print the name of
                          each file as it is compared. If you wish to print
                          only directory names, use the -d option.
                          """
    )
    boolean verbose;

    /**
     * Directory output option. If enabled, the command will print the name
     * of each directory as it is compared.
     */
    @Option(
            names = {"-d", "--directory"},
            description = """
                          Enable directory output. This will print the name of
                          each directory as it is compared. If you wish to
                          print file names as well, use the -v option.
                          """
    )
    boolean directoryOutput;

    /**
     * Copy on diff option. If enabled, the command will copy the files
     * that are different in the second directory compared to the first
     * directory.
     */
    @Option(
            names = {"-c", "--copy-on-diff"},
            description = """
                          Enable copy on diff. This will copy the files that
                          are different on the other directory compared to the
                          base directory.
                          
                          Take heed that this option will overwrite the files
                          on the other directory if they are different. Use
                          with caution.
                          
                          Lastly it will not copy empty directories, only
                          files.
                          """
    )
    boolean copyOnDiff;

    /**
     * Directory pathnames to compare.
     */
    @Parameters(
            paramLabel = "DIRECTORY",
            description = "Pathnames of the directories to compare"
    )
    String[] directory;

    /**
     * Base directory absolute path.
     */
    private String baseDirAbsolutePath;

    /**
     * Other directory absolute path.
     */
    private String otherDirAbsolutePath;

    /**
     * Executes the comparison of two directories and optionally generates a
     * report.
     *
     * @return 0 if the comparison was successful, 1 if there was an error.
     */
    @Override
    public Integer call() {
        Instant start = Instant.now();

        if (directory == null || directory.length != 2) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Two directories must be specified."
            );
        }

        for (String dir : directory) {
            if (Utils.isStringEmpty(dir)) {
                return Utils.processExitAndCalculateTime(
                        start,
                        1,
                        "Directory not specified or invalid."
                );
            }
        }

        File base = new File(directory[0]);
        File other = new File(directory[1]);

        if (!base.exists() || !base.isDirectory()) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Base directory does not exist or is not a directory."
            );
        }

        if (!other.exists() || !other.isDirectory()) {
            return Utils.processExitAndCalculateTime(
                    start,
                    1,
                    "Other directory does not exist or is not a directory."
            );
        }

        baseDirAbsolutePath = base.getAbsolutePath();
        otherDirAbsolutePath = other.getAbsolutePath();

        if (Utils.initReportFile(reportFileName) != 0) {
            return Utils.processExitAndCalculateTime(
                    start,
                    2,
                    "Failed to create report file."
            );
        }

        prepareReportFile();

        Utils.traverseDirectoryRecursively(base, file -> {
            if (verbose) Logger.info("Comparing file: " + file.getName());

            String relativePath = Utils.resolveAbsoluteParentPathFromChild(baseDirAbsolutePath, file);
            File otherFile = new File(otherDirAbsolutePath + relativePath);

            if (!otherFile.exists()) {
                Utils.writeReport(reportFileName, "MISS: " + relativePath);
                copyOnDiff(file, otherFile);
            } else {
                try {
                    String baseChecksum = Utils.generateCrc32Checksum(file);
                    String otherChecksum = Utils.generateCrc32Checksum(otherFile);

                    if (!baseChecksum.equals(otherChecksum)) {
                        Utils.writeReport(reportFileName, "DIFF: " + relativePath);
                        copyOnDiff(file, otherFile);
                    }
                } catch (Exception e) {
                    Logger.error("Error generating checksum for file: " + relativePath);
                }
            }
        }, (directory, processing) -> {
            if (verbose || directoryOutput) {
                String message = processing
                        ? "Comparing directory: "
                        : "Finished comparing directory: ";
                Logger.info(message + directory.getName());
            }
        });

        Utils.traverseDirectoryRecursively(other, file -> {
            if (verbose) Logger.info("Checking for extra file: " + file.getName());

            String relativePath = Utils.resolveAbsoluteParentPathFromChild(otherDirAbsolutePath, file);
            File baseFile = new File(baseDirAbsolutePath + relativePath);

            if (!baseFile.exists()) Utils.writeReport(reportFileName, "EXTRA: " + relativePath);
        }, (directory, processing) -> {
            if (verbose || directoryOutput) {
                String message = processing
                        ? "Processing other root for extra files in directory: "
                        : "Finished processing other root for extra files in directory: ";
                Logger.info(message + directory.getName());
            }
        });

        return Utils.processExitAndCalculateTime(
                start,
                0,
                "Successfully compared directories."
        );
    }

    /**
     * Prepares the report file by writing the header information.
     */
    private void prepareReportFile() {
        Utils.writeReport(reportFileName, "======================== DIFF REPORT =======================");
        Utils.writeReport(reportFileName, "Report generated on: " + LocalDateTime.now());
        Utils.writeReport(reportFileName, "Base directory: " + baseDirAbsolutePath);
        Utils.writeReport(reportFileName, "Other directory: " + otherDirAbsolutePath);
        Utils.writeReport(reportFileName, "DIFF - Stands for different files due to CRC32 checksum");
        Utils.writeReport(reportFileName, "MISS - Stands for missing files in the other directory");
        Utils.writeReport(reportFileName, "EXTRA - Stands for extra files in the other directory");
        if (copyOnDiff) {
            Utils.writeReport(reportFileName, "MISS and DIFF files will be copied to the other directory");
        }
        Utils.writeReport(reportFileName, "============================================================");
    }

    /**
     * Copies the file from the source to the destination if the
     * copyOnDiff option is enabled. Logs errors if the copy fails.
     *
     * @param source      The source file to copy.
     * @param destination The destination file to copy to.
     */
    private void copyOnDiff(File source, File destination) {
        if (copyOnDiff) {
            if (!Utils.copyFile(source, destination)) {
                Logger.error("Failed to copy file: " + source.getName());
            }
        }
    }
}
