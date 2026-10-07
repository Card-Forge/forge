package java.nio.file.attribute;

import java.io.IOException;

public abstract class UserPrincipalLookupService {
    protected UserPrincipalLookupService() {
    }

    public UserPrincipal lookupPrincipalByName(String name) throws IOException {
        throw new UnsupportedOperationException("user principals not supported");
    }
}
