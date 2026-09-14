package java.nio.file;

import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.Set;

/**
 * Minimal FileSystem: exists so probes like guava's TempFileCreator can ask
 * what attribute views are supported. Reporting only "basic" steers such
 * probes onto their java.io fallback paths.
 */
public abstract class FileSystem {
    protected FileSystem() {
    }

    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("user principals not supported");
    }

    public Path getPath(String first, String... more) {
        return Paths.get(first, more);
    }
}
