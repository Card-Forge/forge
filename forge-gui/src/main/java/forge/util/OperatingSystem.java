package forge.util;

public class OperatingSystem {
    private static String os = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return os.contains("win");
    }
 
    public static boolean isMac() {
        return os.contains("mac");
    }
 
    public static boolean isUnix() {
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }
 
    public static boolean isSolaris() {
        return os.contains("sunos");
    }
}
