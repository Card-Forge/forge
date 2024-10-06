package forge.util;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.List;

public class JVMOptions {

    static StringBuilder sb;
    static HashSet<String> mandatoryArgs = Sets.newHashSet(
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/java.math=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
            "--add-opens=java.desktop/java.beans=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing.border=ALL-UNNAMED"
    );
    public static StringBuilder getStringBuilder() {
        if (sb == null) {
            sb = new StringBuilder();
            sb.append("Forge failed to initialize JVM arguments.\n" +
                    "Use either Forge.exe | Forge.sh | Forge.cmd to run properly.\n" +
                    "Alternatively, add all these JVM Options in your Command line: \n" +
                    "    --add-opens java.base/java.util=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.lang=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.lang.reflect=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.text=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.nio=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.math=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.util.concurrent=ALL-UNNAMED\n" +
                    "    --add-opens java.base/java.net=ALL-UNNAMED\n" +
                    "    --add-opens java.base/jdk.internal.misc=ALL-UNNAMED\n" +
                    "    --add-opens java.base/sun.nio.ch=ALL-UNNAMED\n" +
                    "    --add-opens java.desktop/java.awt=ALL-UNNAMED\n" +
                    "    --add-opens java.desktop/java.awt.font=ALL-UNNAMED\n" +
                    "    --add-opens java.desktop/java.beans=ALL-UNNAMED\n" +
                    "    --add-opens java.desktop/javax.swing=ALL-UNNAMED\n" +
                    "    --add-opens java.desktop/javax.swing.border=ALL-UNNAMED\n");
        }
        return sb;
    }
    public static boolean checkRuntime(List<String> arguments) {
        if (arguments.isEmpty())
            return false;
        return Sets.newHashSet(arguments).containsAll(mandatoryArgs);
    }
}
