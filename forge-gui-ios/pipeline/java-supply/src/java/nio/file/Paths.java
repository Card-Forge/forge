package java.nio.file;

import java.io.File;

public final class Paths {
    private Paths() { }

    public static Path get(String first, String... more) {
        File f = new File(first);
        for (String m : more) {
            f = new File(f, m);
        }
        return new ForgeFilePath(f);
    }
}
