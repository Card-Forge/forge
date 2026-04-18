# Forge - Magic: The Gathering Engine


## Build Commands

```bash
# Compile only (fast dev cycle, no installer)
mvn -B compile -DskipTests

# Compile specific modules
mvn -B compile -pl forge-game,forge-gui -am -DskipTests

# Full installer (.jar for Windows, .tar.bz2 for Linux/macOS)
mvn -U -B clean -P windows-linux install -DskipTests

# Quick rebuild (skip clean, reuse compiled modules)
mvn -B -P windows-linux install -DskipTests -pl forge-installer -am
```

## Architecture

Multi-module Maven project (~2400 Java files, JDK 17+, version 2.0.12):

Dependency flow: `core → game → ai → gui → gui-desktop/gui-mobile → installer`

| Module | Purpose |
|--------|---------|
| forge-core | Rules engine, card mechanics, game state |
| forge-game | Game session, multiplayer, game flow |
| forge-ai | Computer opponent logic |
| forge-gui | UI components + card scripts (`res/`) |
| forge-gui-desktop | Desktop GUI (Swing) |
| forge-gui-mobile | Mobile GUI (libgdx) |
| forge-gui-mobile-dev | Desktop dev for mobile (LWJGL) |
| forge-gui-android | Android backend |
| forge-gui-ios | iOS backend |
| forge-installer | IzPack installer packaging |
| forge-lda | LDA tooling |
| adventure-editor | Adventure mode editor |

## Build Profiles

- `windows-linux` — Generates IzPack installer (`.jar`) + portable archive (`.tar.bz2`) in `forge-installer/target/`
- `android-test-build` — Android APK

## Release Workflow

Use the `forge-release` skill — covers upstream sync, Maven installer build, `package-release.sh`, and `gh release create` with the correct flags.

- Always build with `mvn clean` (full command) before packaging a release — skipping `clean` leaves stale artifacts (`forge-commander.*`, old JARs, `logs/`) in `forge-installer/target/forge-installer-*-SNAPSHOT/` that end up in the tarball and release zip
- To patch a release zip without rebuilding: `zip -d Forge-X.Y.Z.zip <path/to/file>` removes entries in-place; re-upload with `gh release upload vX.Y.Z file.zip --clobber`

## Git Remotes

| Remote | Repo | Purpose |
|--------|------|---------|
| `origin` | `RafaelHGOliveira/forge` (SSH) | Personal fork, push target |
| `upstream` | `Card-Forge/forge` | Official upstream |
| `mostcromulent` | `MostCromulent/forge` | Community fork with experimental features (e.g. YieldRework) |

## Adding a New GUI Backend

- Implement `IGuiGame` (~80 methods) — blocking methods use `sendAndWait`, display methods use fire-and-forget `send`
- Implement `IGuiBase` — factory: `getNewGuiGame()` returns your `IGuiGame` impl, `hostMatch()` returns `HostedMatch`
- Reference impl: `RemoteClientGuiGame` (server-side proxy for Netty multiplayer) shows the send/sendAndWait pattern
- `GuiBase.setInterface(yourImpl)` registers the GUI globally
- `HostedMatch.startGame()` calls `openView()` per GUI — must be parallelized for 3+ remote players; sequential `sendAndWait` calls cause hard lock where 3rd player never receives init; pattern: spawn threads for `isRemoteGuiProxy()` GUIs, call local GUI on EDT, then `thread.join()` all

## Gotchas

- `FDialog` subclasses with manual geometry: call `setSize(w, h)` directly — never `pack()` before `setSize()`; `innerPanel` uses `MigLayout("..., fill")` which computes a huge preferred width with absolute-positioned components
- `FButton` in yield/action panels: always call `setUseHighlightMode(true)` — without it the button keeps UP images (red/orange) while peers use FOCUS images (blue) after `setEnabled(true)`, causing visual inconsistency
- Check `upstream` and `mostcromulent` remotes for branches to merge
- `-pl` alone fails for single modules due to `${revision}` parent POM — always use `-am` flag
- `Thread.ofVirtual()` is Java 21+ — use `new Thread(() -> {})` + `setDaemon(true)` for Java 17 compat (project min is 17)
- Card image cache on macOS: `~/Library/Caches/Forge/pics/cards/` — use `ImageKeys.getImageFile(imageKey)` (forge-core) to resolve `CardStateView.getImageKey(null)` to a `java.io.File`
- Java `.jar` processes launched via Bash tool die when the tool shell exits — use `open forge.command` to launch in a persistent Terminal window instead
- Card scripts live in `forge-gui/res/`, not in Java source
- Installer images (`ic_launcher.png`, `side.png`) referenced by name in `forge-installer/libs/install.xml`
- Checkstyle runs on build — config in `checkstyle.xml` at root; use `-Dcheckstyle.skip=true` for dev builds (22k+ pre-existing errors in `sun_checks`)
- `-DskipTests` required — GUI tests (PanelTest) fail in headless environments
- Auto yield system (`YieldController`) checks available actions but not attackers — changes to yield logic must account for combat eligibility
- `TriggerChoice` enum (`forge-gui/src/main/java/forge/gamemodes/match/TriggerChoice.java`) — substitui magic ints em `notifyTriggerChoiceChanged`; valores: `ASK`, `ALWAYS_YES`, `ALWAYS_NO`
- `YieldPrefs` (`forge-gui/src/main/java/forge/gamemodes/match/YieldPrefs.java`) — snapshot imutável das preferências de yield; usar `YieldPrefs.fromCurrentPreferences()` ao notificar yield state; `RemoteClientGuiGame` armazena `volatile YieldPrefs remoteYieldPrefs` para o host avaliar interrupções com as prefs reais do jogador remoto
- `PlayerView.findById(GameView, PlayerView)` — resolve PlayerView deserializado ao tracker correto no GameView do host; necessário no caminho `setYieldMode(fromRemote=true)`
- `setYieldMode` returns `boolean` — callers must check before passing priority (`selectButtonOk`/`passPriority`), otherwise rejected modes (e.g. empty stack for UNTIL_STACK_CLEARS) cause unintended phase skips
- `updateHasAvailableActions` is expensive (scans all zones + abilities + mana costs) — called every priority pass when YIELD_AUTO_PASS_NO_ACTIONS is on; don't cache naively (other players' boards affect valid targets); results cached via `Game.getTimestamp()`
- Installer `.tar.bz2` tem arquivos na raiz (sem diretório wrapper) — extrair com `tar -xjf ... -C destdir` sem `--strip-components`
- Custom dialog retornando valor arbitrário: instanciar `FOptionPane` diretamente + `FutureTask<T>` + `FThreads.invokeInEdtAndWait`; usar `final FOptionPane[] paneHolder = {null}` para referenciar o pane de dentro de lambdas de botões (`setResult` fecha o diálogo)
- Always sync fork before release: `git fetch upstream master && git merge upstream/master --no-edit && git push origin master`
- `AbstractGuiGame` has `final` methods: `autoPassUntilEndOfTurn`, `mayAutoPass`, `autoPassCancel`, `updateSingleCard`, `awaitNextInput`, `cancelAwaitNextInput`, `setCurrentPlayer`, `getCurrentPlayer`, `getGameView`, `setYieldMode`, all trigger/yield methods — do NOT override
- `AbstractGuiGame` has one `protected abstract` method: `updateCurrentPlayer(PlayerView)` — subclasses MUST implement
- `CardView` properties (`getManaCost`, `getType`, `isCreature`, `getPower`, `getToughness`) live on `CardView.getCurrentState()` (CardStateView), not on CardView directly
- `GameView.getWinningPlayerName()` returns `String` — there is no `getWinningPlayer()` method
- Auto-pass in `chooseSpellAbilityToPlay` must call `updateAutoPassPrompt()` before returning null — otherwise the prompt stays stuck on the previous message (e.g. a resolved trigger)
- `updateHasAvailableActions` takes `Predicate<SpellAbility> canAffordMana` and uses `ComputerUtilMana.canPayManaCost` at call sites — accounts for cost reducers/increases; caches result via `Game.getTimestamp()`; `shouldAutoPassNoActions` has conservative guard for main phases (cards in hand = never auto-pass)
- `TargetSelection.chooseTargets()`: when `validTargets` (cards) is empty, checks `nonCardTargets = tgt.getAllCandidates(ability, true, true)` — size==1: auto-targets silently; size>1: shows `InputSelectTargets`; size==0: returns false → trigger silently dropped (file: `forge-gui/src/main/java/forge/player/TargetSelection.java` ~line 159)
- `TargetSelection.chooseTargets()`: when `nonCardTargets.size() > 1` (3+ opponents in multiplayer), falls through to `InputSelectTargets` with empty card list — no player highlights visible without explicit `setHighlighted` calls; fix: iterate `tgt.getAllCandidates(sa, true, true)` in `InputSelectTargets` constructor when `choices.isEmpty() && tgt.canTgtPlayer()`
- Deck URL import: Moxfield API (api2.moxfield.com) bloqueada por Cloudflare (403) — não é possível testar programaticamente; verificar com o Forge rodando
- Deck URL import: TappedOut `?fmt=txt` é lista plana alfabética — sem headers de seção; Commander/Sideboard/Maybeboard não detectáveis; outros formatos (`?fmt=dek`, `?fmt=mwDeck`, `?fmt=markdown`) retornam HTML via curl
- Deck URL import: MTGGoldfish `/deck/download/<id>` não tem header "Commander" — constructed funciona (blank-line separa sideboard); commander decks importam sem designação de commander
- `FetchResult.okWithNote(deckText, siteName, cardCount, note)` — variante de `ok()` que inclui nota na mensagem de sucesso exibida em `urlStatusLabel` (`DeckImport.java:609`)
- `FButton.resetImg()`: guard `isFocusOwner()` com `isFocusable() &&` — sem isso, botões com `setFocusable(false)` ainda renderizam FOCUS images quando perdem/ganham foco via outros componentes
- `HandArea` constructor: `new HandArea(matchUI, handScroller)` — segundo arg é o `FScrollPane` que o envolve; sempre chamar `handScroller.setViewportView(handArea)` logo após
- Adding a new Windows `.exe` requires two sync changes: (1) new `<execution>` in `forge-gui-desktop/pom.xml` (Launch4j plugin), (2) explicit `<fileset>` copy + entries in both tarfileset blocks in `forge-installer/pom.xml`; Launch4j `dontWrapJar=true` means the exe references the JAR by name at runtime — jar need not exist at build time
- `FDeckChooser` `RANDOM_CARDGEN_COMMANDER_DECK`: only runs `updateRandomCardGenCommander()` when `FModel.isdeckGenMatrixLoaded()` is true — without a fallback, AI deck slots stay empty and never auto-select; add `else { updateRandomCommander(); }` if matrix isn't loaded
- INVALID PROPERTY errors em menus = chave faltando em `en-US.properties`; achar todas as ausentes: `grep -roh 'getMessage("[^"]*")' forge-gui-desktop/src forge-gui/src --include="*.java" | grep -oP '(?<=getMessage\(")[^"]+' | sort -u > /tmp/used.txt && grep -oP '^[^=]+' forge-gui/res/languages/en-US.properties | sort -u > /tmp/defined.txt && comm -23 /tmp/used.txt /tmp/defined.txt`
- `en-US.properties` é resource file — mudanças não precisam rebuild, só reiniciar o Forge
- `UI_MULTIPLAYER_FIELD_LAYOUT` (`"ROWS"` / `"GRID"` / `"OFF"`) + `UI_MULTIPLAYER_FIELD_PANELS` (`"SPLIT"` / `"TABBED"`) — controlam layout 3+ jogadores; acessível em Layout menu durante partida; em GRID com 4 jogadores, o player que age antes do local fica na mesma célula (CCW seating intencional)
- `CYield.initialize()` — executado a cada partida; usar pra force-reset de prefs sem controle UI (padrão: `FModel.getPreferences().setPref(FPref.X, value)`)

### Network Multiplayer

- Always ask early if the game is local or online (ZeroTier/LAN) — online context changes the entire debugging path for targeting, priority, and yield issues
- `openView` must be `sendAndWait` (not fire-and-forget `send`) — client needs to finish registering controllers before game thread proceeds (coin flip deadlock)
- `write()` (no flush) vs `send()` (flush) — use `write()` + `send()` to batch TCP: e.g. `write(setGameView)` then `send(handleGameEvents)` coalesces into one TCP flush; two `send()` calls forces two round-trips
- `RemoteClientGuiGame.paused = true` during reconnect — all `send()`/`sendAndWait()` become no-ops (return null); callers of `sendAndWait` must handle null return
- `ReplyPool.cancelAll()` on disconnect unblocks all pending `sendAndWait` with null immediately — RemoteClientGuiGame methods return safe defaults (empty lists, false, etc.)
- Priority state in `AbstractGuiGame` may lag behind server state — don't cache resolved player names, re-resolve dynamically
- `IGuiGame.isRemoteGuiProxy()` — default `false`; `RemoteClientGuiGame` returns `true`; use this to distinguish server-side proxy GUIs from local player GUIs; `FModel.getPreferences()` reads HOST's prefs even for remote players — guard yield/auto-pass logic with this check
- Heartbeat asymmetry — client sends every 15s (write-idle), server detects silence after 45s (read-idle); both configurable via JVM system properties
- To broadcast a GUI call to all players except one slot, iterate `game.getPlayers()`, call `hostedMatch.getGuiForPlayer(p)`, skip `RemoteClientGuiGame` instances matching the target slot index
- Adding a new Server→Client GUI method requires 3 changes in sync: (1) `IGuiGame` interface with default no-op, (2) `ProtocolMethod` enum entry with arg types, (3) `RemoteClientGuiGame` implementation calling `send()`; then implement in each GUI backend (`CMatchUI`, JavaFX, etc.)
- Host-only GUI calls (e.g. `showPlayerDisconnected`) don't need `ProtocolMethod` — add as `default` no-op in `IGuiGame` and call directly on local GUI instances via `hostedMatch.getGuiForPlayer(p)`; only add to `ProtocolMethod` if remote clients also need to receive it
- `ProtocolMethod` has NO `chooseTargetsFor` — target selection for remote players goes through same `InputSelectTargets` path; `setSelectables` sends only cards; for player-only targets (`ValidTgts$ Opponent`) the list is empty and the client must click a player avatar to send `selectPlayer` back to server

## UI Card Selection Flow (In-Game)

Two paths when player must choose cards (e.g. fetch land search):
- `UI_SELECT_FROM_CARD_DISPLAYS` ON (desktop default): opens `FloatingZone` window, player clicks cards directly
- OFF: uses `ListChooser` dialog (text list with search field)
Control flow: `PlayerControllerHuman.chooseSingleEntityForEffect` → `useSelectCardsInput()` decides which path

## Key Files

- `forge-game/src/main/java/forge/game/GameAction.java` — core game actions (draw, play, sacrifice)
- `forge-game/src/main/java/forge/game/phase/PhaseHandler.java` — turn/phase state machine
- `forge-game/src/main/java/forge/game/player/Player.java` — player state and actions
- `forge-game/src/main/java/forge/game/cost/CostAdjustment.java` — mana cost reductions/increases
- `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java` — human player input handling
- `forge-gui/res/cardsfolder/` — card script definitions (one .txt per card)
- `forge-ai/src/main/java/forge/ai/AiController.java` — AI decision entry point
- `forge-gui/src/main/java/forge/gamemodes/match/YieldController.java` — auto-pass/yield logic
- `forge-gui/src/main/java/forge/deck/DeckSiteFetcher.java` — abstract base for URL deck fetchers (HTTP, JSON utilities)
- `forge-gui/src/main/java/forge/deck/DeckUrlFetcher.java` — dispatcher that routes URLs to site-specific fetchers
- `forge-installer/libs/install.xml` — IzPack installer configuration
- `forge-gui-desktop/src/main/java/forge/view/DedicatedServer.java` — headless server entry point
- `forge-gui/src/main/java/forge/gamemodes/net/server/FServerManager.java` — Netty TCP server, lobby, reconnection; `getAllLocalAddresses()` returns `LinkedHashMap<String,String>` (friendly name → IPv4) with VPN/Wi-Fi detection; `replaceDisconnectedWithAI(username)` converts a disconnected slot to AI
- `forge-gui-desktop/src/main/java/forge/net/HeadlessGuiDesktop.java` — headless GUI for server mode
- `forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java` — GUI game state, waiting timer, prompt messages
- `forge-gui-desktop/src/main/java/forge/view/arcane/FloatingZone.java` — floating zone window (library/graveyard/exile display during match)
- `forge-gui-desktop/src/main/java/forge/gui/ListChooser.java` — modal list chooser dialog with search field
- `forge-gui/src/main/java/forge/gamemodes/net/ProtocolMethod.java` — network protocol method definitions; controls which methods return `boolean` for `sendAndWait`
- `forge-gui/src/main/java/forge/gamemodes/net/server/RemoteClientGuiGame.java` — server-side GUI proxy for remote players; `isRemoteGuiProxy()` returns `true` (renamed from `NetGuiGame.java` in PR #9642)
- `forge-gui/src/main/java/forge/gamemodes/net/ReplyPool.java` — request-response synchronization for network protocol; used by `sendAndWait`
- `forge-gui/src/main/java/forge/gamemodes/net/GameProtocolHandler.java` — client-side protocol handler; auto-sends `ReplyEvent` for non-void return methods
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputPassPriority.java` — priority passing logic; displays floating mana warning with visual mana symbols
- `forge-gui/src/main/java/forge/gui/interfaces/IGuiGame.java` — main game GUI interface; defines `isRemoteGuiProxy()`
- `forge-localinstance/src/main/java/forge/localinstance/properties/ForgePreferences.java` — game preferences (FPref enums); add new prefs here
- `forge-gui/res/defaults/match.xml` — default match screen layout (DragCell positions); user custom layouts override this
- `forge-gui-desktop/src/main/java/forge/screens/match/VMatchUI.java` — match UI view; manages dynamic cell creation for yield panel and multiplayer field splitting
- `forge-gui-desktop/src/main/java/forge/screens/match/controllers/CYield.java` — yield panel controller; resets auto-pass each match, manages button state
- `forge-gui-desktop/src/main/java/forge/screens/match/views/VField.java` — per-player match panel (avatar, life, counters); MigLayout with `hidemode 3` for show/hide widgets; `FLabel.ButtonBuilder` + `setCommand(Runnable)` for clickable buttons; `setDisconnected(boolean, Runnable)` shows/hides disconnect indicator and Replace AI button
- `scripts/package-release.sh` — packages release zip from installer tarball

## Dedicated Server (Headless)

```bash
# Default: Commander, 4 players, port 36743
java -jar forge.jar server

# Custom configuration
java -jar forge.jar server --port 9999 --players 3 --mode commander
```

Modes: `commander`, `constructed`, `oathbreaker`, `brawl`, `tinyLeaders`
Terminal commands: `help`, `status`, `start`, `kick <slot>`, `stop`
Server does not occupy a player slot — all slots are OPEN for remote players.

## Running (Dev)

Use the `forge-run-local` skill — covers the build → extract `.tar.bz2` → `open forge.command` cycle.

## Code Style

- Java 17+ (no unsupported API calls like `StringBuilder.isEmpty()` for Android compat)
- Checkstyle enforced via Maven plugin
- Run checkstyle only: `mvn checkstyle:check -pl <module>`
- Art files must be copyright-free / public domain
- Maven incremental compile may show "Nothing to compile" even after file edits — use `mvn clean compile` to force recompile when verifying correctness
- **Porting checklist** — ao portar uma feature de outra branch/PR, antes de dar o commit: (1) `mvn -B compile -pl forge-game,forge-gui -am -DskipTests` para validar, (2) se a feature toca rede, verificar `IGuiGame` + `ProtocolMethod` enum + `RemoteClientGuiGame` (3-step sync documentado em Network Multiplayer), (3) `grep` por métodos novos em todas as implementações de interfaces tocadas
- Compilation errors mais comuns ao portar: `ProtocolMethod` enum faltando entry (applyDelta, requestResync, setHighlighted foram casos reais), interface method sem default e sem override nas subclasses, field ordering em enum
- Cherry-picking upstream PRs: use `gh pr view <num> --repo Card-Forge/forge --json commits --jq '.commits[].oid'` to get commit SHAs — never cherry-pick a merge commit directly (needs `-m` and pulls unrelated changes); pick only the feature commit SHA
- `git cat-file -e <sha>` — verificar se um commit já está na nossa história (fast membership check); mais rápido que grep no log
- Quando upstream squasha um PR que já temos em first-parent: `git merge upstream/master` é suficiente — usar `git checkout --ours` nos arquivos de rede conflitantes (RemoteClientGuiGame, IGuiGame, ProtocolMethod, FServerManager, HostedMatch, CMatchUI, IGameController, PlayerControllerHuman, OnlineMenu); rebase para remover os commits originais é inviável com 400+ commits empilhados
- PRs grandes (50+ commits): `git fetch <fork-url> <branch>:pr/<N>` + `git checkout pr/<N> -- <arquivos>` para arquivos novos/intocados; arquivos que nós também modificamos precisam de merge manual (subagente com contexto das customizações)
- `git filter-repo` reescreve TODOS os SHAs incluindo commits do upstream — nunca usar para mudar email/autor em forks; causa fork a mostrar 28k ahead/28k behind no GitHub; recover: `git reset --hard upstream/master` + cherry-pick ou checkout de arquivos
- `.git-rewrite/` criado pelo filter-repo pode ficar tracked no repo (56k arquivos); remover com `git rm -r --cached .git-rewrite/ && git commit`
- Para portar features de uma release tag sem conflitos de merge: `git checkout <tag> -- <arquivos>` por grupo de feature + commit com mensagem descritiva — evita cherry-pick individual e merge conflicts
- Branches locais `pr/9751`, `YieldRework`, `serverurlscreen`, etc. estão baseadas no upstream real (merge-base está em upstream) — seguro fazer cherry-pick delas sem causar divergência
- RTK trunca output de `git log` — usar `python3 -c "import subprocess; r = subprocess.run(['git','log','--oneline',...], capture_output=True, text=True); print(r.stdout)"` para output completo
- Fetching upstream PRs as branches: `git fetch upstream refs/pull/<N>/head:pr/<N>` — se der non-fast-forward (branch existe), usar `git fetch upstream refs/pull/<N>/head && git branch -f pr/<N> FETCH_HEAD`
- Para resetar master ao upstream e preservar trabalho: `git branch <nova-branch> HEAD && git reset --hard upstream/master && git push origin master --force && git push origin <nova-branch>`
- Resolução de conflito em múltiplos arquivos de uma vez: usar `python3` com `str.replace()` — mais confiável que múltiplos `Edit` quando os markers de conflito têm espaços/tabs inconsistentes
- `forge-commander` branch — arquivo de features customizadas do v2.0.19; usar `git show forge-commander:<path>` para ler código a portar; não fazer merge direto (tem arena UI e outros que não queremos); antes de `git checkout forge-commander -- <file>`, verificar refs arena: `git show forge-commander:<path> | grep -n "arena\|Arena\|ArenaLayout"` — arquivos mistos: CMatchUI, VMatchUI, VField, CHand
- Portando de `forge-commander`: NÃO portar mudança de `IGuiGame.openView` de `void` para `boolean` — quebra todas as implementações
- `autoPassCancelLegacy()` — existe em `PlayerController`, `PlayerControllerHuman`, `AbstractGuiGame`, `IGuiGame` (default); `MagicStack` chama isso (em vez de `autoPassCancel`) quando non-triggered ability é adicionada, preservando yield modes experimentais
- `CardPanelContainer.lastHoveredPanel` — campo `static volatile CardPanel`, atualizado em `mouseOver`/`mouseOut`; usado por `KeyboardShortcuts` para Z-zoom saber qual carta está sob o cursor
- PR #9806 (card info tooltips) adiciona: `KeywordAction.java` (forge-game/keyword), `CardInfoPopup.java` + `CardInfoPopupMenu.java` + `CardOverlaySettingsDialog.java` (forge-gui-desktop/screens/match/menus e arcane), `KeywordInfoUtil.java` (forge-gui/gui/card); modifica CardPanelContainer (hover trigger), VStack (tooltips), GameLogPanel (inline images), KeyboardShortcuts (hotkeys toggle), CMatchUI/VMatchUI (popup lifecycle)
- `git merge upstream/master -X theirs` perde símbolos customizados em arquivos de rede — verificar após cada sync: `IGuiGame` (`isRemoteGuiProxy()`, `isAutoPassingNoActions()`, `showPlayerDisconnected()`); `RemoteClientGuiGame` (`clientIndex` field + `getSlotIndex()`); `CompatibleObjectDecoder`/`CompatibleObjectEncoder` (import `forge.trackable.Tracker`); `RemoteClient` (imports `CompatibleObjectEncoder/Decoder`); `InputPassPriority` (import `forge.localinstance.properties.ForgePreferences` — unqualified); `PlayerControllerHuman` (imports `FServerManager` + `forge.gamemodes.net.event.MessageEvent`)
- Conflito `modify/delete` em `git merge -X theirs` **não é resolvido automaticamente** — rodar `git checkout --theirs <arquivo> && git add <arquivo>` para cada
- `AbstractGuiGame` setters (`setShouldAutoYield`, `setShouldAlwaysAcceptTrigger/Decline/Ask`) contêm guard `instanceof NetGameController` para sync de rede (PR #10355) — GUI layers (VStack, VAutoYields, VGameMenu) **não devem** chamar `notifyAutoYieldChanged`/`notifyTriggerChoiceChanged` explicitamente
