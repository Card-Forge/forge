package fake

// Matches no group glob, so it is exempt from every rule unless the tool
// treats that as a failure.
func orphaned() {}
