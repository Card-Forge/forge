# Port Log

- **Status:** Active (empty)

One note per ported unit, named after the unit: `game-action.md`, `card-rules-reader.md`.

Required **before** the implementing PR merges (PORT-3, PORT-4). Format and a worked example:
[../../guidelines/02-java-to-go-translation.md](../../guidelines/02-java-to-go-translation.md).

The note records what the Java unit actually does, where Go deliberately diverges, how each Java `null` was translated,
and which unexplained Java behaviors were pinned by fixture rather than fixed (PORT-7).

Reason: in six months nobody remembers why Go diverged, and the note is the only defence against a "fix" that
reintroduces a Java quirk.
