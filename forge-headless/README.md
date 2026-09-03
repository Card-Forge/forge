# Forge Headless

`forge-headless` runs Forge games without the desktop or mobile user interface. It supports:

- normal Forge AI simulations through `sim`;
- a terminal controller through `tui`; and
- a lightweight random controller for simulation benchmarks.

Build and test the module and its dependencies from the repository root:

```sh
mvn -pl forge-headless -am test
mvn -pl forge-headless -am package -DskipTests
```

After packaging, the repository-level launcher selects the assembled headless JAR:

```sh
./headless.sh --help
./headless.sh sim -d forge-headless/test_decks/monored.dck forge-headless/test_decks/monored.dck -n 1 -s 42
./headless.sh tui forge-headless/test_decks/monored.dck forge-headless/test_decks/monored.dck --seed 42
```

Run `./headless.sh <command> --help` for a description of each command and its options.

Simulation retains the desktop runner's options, including seeded runs (`-s`) and per-player AI
profiles (`-a`). Add `-r` to replace the normal AI seats with faster random controllers. Random
controller mode cannot be combined with AI profiles or tournament mode.

The Java tests use the small decks under `test_decks/` and exercise pass, random, targeting, and
scripted terminal-controller paths in-process.
