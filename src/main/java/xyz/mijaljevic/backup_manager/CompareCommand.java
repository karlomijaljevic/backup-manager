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

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * CompareCommand is a command-line utility that compares two directories and
 * generates a report of the differences. The report can be saved to a file or
 * printed to the console.
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
     * Directory pathnames to compare.
     */
    @Parameters(
            paramLabel = "DIRECTORY",
            description = "Pathnames of the directories to compare"
    )
    String[] directory;

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
        if (directory == null || directory.length != 2) {
            System.err.println("Error: Two directories must be specified.");
            return 1;
        }

        for (String dir : directory) {
            if (Utils.isStringEmpty(dir)) {
                System.err.println("Error: Directory not specified or invalid.");
                return 1;
            }
        }

        File base = new File(directory[0]);
        File other = new File(directory[1]);

        if (!base.exists() || !base.isDirectory()) {
            System.err.println("Error: Base directory does not exist or is not a directory.");
            return 1;
        }

        if (!other.exists() || !other.isDirectory()) {
            System.err.println("Error: Other directory does not exist or is not a directory.");
            return 1;
        }

        baseDirAbsolutePath = base.getAbsolutePath();
        otherDirAbsolutePath = other.getAbsolutePath();

        if (reportFileName == null) {
            System.out.println("Report will be printed to the console.");
        } else {
            reportFileName = reportFileName.isEmpty() ? Defaults.DEFAULT_REPORT_NAME : reportFileName;

            try {
                Files.newOutputStream(
                        Path.of(reportFileName),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                ).close();

                System.out.println("Report will be saved to: " + reportFileName);
            } catch (IOException e) {
                System.err.println("Error creating report file: " + e.getMessage());
                return 2;
            }
        }

        prepareReportFile();

        Utils.traverseDirectoryRecursively(base, file -> {
            String relativePath = Utils.resolveAbsoluteParentPathFromChild(baseDirAbsolutePath, file);
            File otherFile = new File(otherDirAbsolutePath + relativePath);

            if (!otherFile.exists()) {
                writeReport("MISS: " + relativePath);
            } else {
                try {
                    String baseChecksum = Utils.generateMd5Checksum(file);
                    String otherChecksum = Utils.generateMd5Checksum(otherFile);

                    if (!baseChecksum.equals(otherChecksum)) {
                        writeReport("DIFF: " + relativePath);
                    }
                } catch (Exception e) {
                    System.err.println("Error generating checksum for file: " + relativePath);
                }
            }
        });

        return 0;
    }

    /**
     * Prepares the report file by writing the header information.
     */
    private void prepareReportFile() {
        writeReport("======================= DIFF REPORT ======================");
        writeReport("Report generated on: " + LocalDateTime.now());
        writeReport("Base directory: " + baseDirAbsolutePath);
        writeReport("Other directory: " + otherDirAbsolutePath);
        writeReport("DIFF - Stands for different files due to MD5 checksum");
        writeReport("MISS - Stands for missing files in the other directory");
        writeReport("==========================================================");
    }

    /**
     * Prints the content to the report file or console. If the report file
     * name is not provided during command initialization, the content is
     * printed to the console.
     *
     * @param reportContent The content to be printed.
     */
    private void writeReport(String reportContent) {
        if (reportFileName == null) {
            System.out.println(reportContent);
        } else {
            try {
                Utils.writeToFile(reportFileName, reportContent);
            } catch (Exception e) {
                System.err.println("Error writing to report file: " + e.getMessage());
            }
        }
    }
}
