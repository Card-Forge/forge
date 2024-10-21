package forge.util;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.List;

public class JVMOptions {

    static StringBuilder sb;
    static HashSet<String> mandatoryArgs = Sets.newHashSet(
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/java.beans=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing.border=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED"
    );
    public static StringBuilder getStringBuilder() {
        if (sb == null) {
            sb = new StringBuilder();
            sb.append("Forge failed to initialize JVM arguments.\n")
                    .append("Use forge.exe | forge.sh | forge.cmd for Desktop UI.\n")
                    .append("Use forge-adventure.exe | forge-adventure.sh | forge-adventure.cmd for Mobile UI.\n")
                    .append("(Use forge-adventure-mac.sh is for macOS to run properly).\n")
                    .append("Alternatively, add all these JVM Options in your Command line: \n");
            for (String arg : mandatoryArgs)
                sb.append(arg.replace("=", " ")).append("\n");
        }
        return sb;
    }
    public static boolean checkRuntime(List<String> arguments) {
        if (arguments.isEmpty())
            return false;
        return Sets.newHashSet(arguments).containsAll(mandatoryArgs);
    }
}
