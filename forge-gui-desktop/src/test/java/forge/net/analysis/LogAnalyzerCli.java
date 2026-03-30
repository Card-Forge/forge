package forge.net.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI wrapper around {@link NetworkLogAnalyzer} for standalone log analysis.
 * Accepts log files or directories and produces a markdown report.
 *
 * <p>Options: -r (recurse), -o file (output path)
 */
public class LogAnalyzerCli {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: LogAnalyzerCli [options] <file|dir>...");
            System.err.println("Options:");
            System.err.println("  -r         Recurse into subdirectories");
            System.err.println("  -o <file>  Write report to file (default: network-log-analysis.md in same directory as input file)");
            System.exit(1);
        }

        boolean recursive = false;
        String outputPath = null;
        List<String> paths = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("-r".equals(args[i])) {
                recursive = true;
            } else if ("-o".equals(args[i])) {
                if (i + 1 < args.length) {
                    outputPath = args[++i];
                } else {
                    System.err.println("Error: -o requires a file path argument");
                    System.exit(1);
                }
            } else {
                paths.add(args[i]);
            }
        }

        if (paths.isEmpty()) {
            System.err.println("Error: no input files or directories specified");
            System.exit(1);
        }

        // Collect all log files
        List<File> logFiles = new ArrayList<>();
        for (String path : paths) {
            File f = new File(path);
            if (!f.exists()) {
                System.err.println("Warning: skipping non-existent path: " + path);
                continue;
            }
            if (f.isFile()) {
                logFiles.add(f);
            } else if (f.isDirectory()) {
                collectLogFiles(f, logFiles, recursive);
            }
        }

        if (logFiles.isEmpty()) {
            System.err.println("Error: no log files found");
            System.exit(1);
        }

        System.err.printf("Analyzing %d file(s)...%n", logFiles.size());

        // Analyze each file
        NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
        List<GameLogMetrics> allMetrics = new ArrayList<>();
        for (File logFile : logFiles) {
            GameLogMetrics metrics = analyzer.analyzeLogFile(logFile);
            allMetrics.add(metrics);
        }

        // Generate report
        AnalysisResult result = analyzer.buildAnalysisResult(allMetrics);
        String report = result.generateReport();

        if (outputPath == null) {
            // Default: write next to the first input file
            File firstInput = logFiles.get(0);
            File parentDir = firstInput.getParentFile();
            if (parentDir == null) parentDir = new File(".");
            outputPath = new File(parentDir, "network-log-analysis.md").getPath();
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.print(report);
            System.err.println("Report written to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error writing report: " + e.getMessage());
            System.exit(1);
        }

        System.err.println(result.toSummary());
    }

    private static void collectLogFiles(File dir, List<File> out, boolean recursive) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isFile() && isLogFile(f.getName())) {
                out.add(f);
            } else if (f.isDirectory() && recursive) {
                collectLogFiles(f, out, true);
            }
        }
    }

    private static boolean isLogFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".log") || lower.endsWith(".txt");
    }
}
