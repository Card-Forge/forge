# forge-gui-web

Forge played in a web browser. One Java process runs Forge and serves a page, and each browser that opens the page is one player.

This file describes the module's architecture and the rules it is built on. Class and package Javadoc, and the header of each TypeScript file, describe the parts.

## Building and running

    mvn -Pweb -pl forge-gui-web -am install -DskipTests
    java -jar forge-gui-web/target/forge-gui-web-<version>-jar-with-dependencies.jar

Forge's default build leaves this module out, so `-Pweb` is needed. Maven downloads its own Node and runs npm. The build also writes the `forge-web` launchers beside the jar, as the desktop download ships them. The jar looks for Forge's `res` folder in the folders above where it runs. It takes options as `-Dforge.web.*` system properties, such as `forge.web.port`.

To work on the page, start the jar with `-Dforge.web.pageDir=forge-gui-web/src/main/resources/web`, so the page is served from the source folder, and run `npm run watch` in this folder. A change to the page then needs only a reload.

## Architecture

### Every seat is a netplay client

The web GUI adds no game logic and no game protocol of its own. Every player, the host included, plays through Forge's existing netplay.

The host's browser starts Forge's netplay server inside the process, on a Netty local channel with no socket of its own, and joins it as a client. Each guest's browser joins the same server the same way. Desktop and mobile Forge can join too, through a WebSocket tunnel on the web port that the netplay server adopts as an ordinary client. The lobby, drafts, state updates and `IGuiGame` calls are the ones desktop netplay uses, so a fix to netplay reaches the web, and the web cannot drift from the other clients' rules.

```
browser ──WebSocket──▶ WebServer ─▶ WebSession ─▶ WebGuiGame (IGuiGame) ◀─ netplay client ─┐
browser ──WebSocket──▶ WebServer ─▶ WebSession ─▶ WebGuiGame (IGuiGame) ◀─ netplay client ─┤ in-process
desktop / mobile Forge ──WebSocket tunnel──▶ WebServer ─────────────────────────────────────┤
                                                                         netplay server ◀─┘ (FServerManager)
                                                                         and the game
```

### The server side

`WebServer` serves the page, images, sounds and sockets on one port. Every request must carry the link token. `PageBundle` builds the page from its files, and `ImageReplies` and `AudioReplies` answer the requests for pictures and sounds.

Each browser has one `WebSession`, which survives reloads. A session is always at one stage (the menu, a table, a match, an event), and it routes the browser's messages to the class that owns that stage. During a match the browser's seat is a `WebGuiGame`, the web's `IGuiGame`, which turns game state, events and questions into messages.

The game's questions block. When the engine asks a seat something, the netplay client's call waits in `WebGuiGame` until the browser answers. A browser that reloads is sent the whole table again with every question still open, so a reload never loses a game.

### Who can reach it

The server listens on every network interface, over plain HTTP, on Forge's netplay port 36743 by default. Hosting from desktop Forge at the same time needs another `forge.web.port`. Each launch makes one random link token, shared by every player. A request without it is refused, the token is compared in constant time, the cookie that holds it is `HttpOnly` and `SameSite=Strict`, and a WebSocket must come from the page's own origin. Desktop and mobile Forge join with the same token, through the same port, so the netplay tunnel is open to no one the link does not already admit. Any browser with the link may take a free host seat, unless a host password is set in the console. A browser on the server's own machine never needs it. A request through a proxy counts as another machine.

A small Swing window shows the links and the number of players connected. It also starts and stops the server and exports Forge's logs. From it the host can forward the port by UPnP, for players outside the network. It is off until the host turns it on.

### The host's profile

The web host is Forge itself, so it reads and writes the same user profile as desktop Forge: preferences, decks, saved games and the card image cache. A deck saved in the browser is there in desktop Forge, and the other way round.

### Threads

Forge's models were written for a single UI thread, and most have no locks. The module keeps that true rather than adding locks to Forge:

- Forge's UI calls (`invokeInEdt`) run on one thread that stands in for Swing's.
- Each part that drives an unguarded Forge model has its own single thread, such as an offline draft.
- Browser messages arrive on the web server's threads. A handler that is slow, or that touches one of the models above, hands its work to the owning thread instead of running it there.
- Deck lists, deck and card searches and the card catalogue run on each session's deck thread, in the order they were asked for, because the web server's threads also serve every picture.

Each seat's netplay client runs its calls on a thread of its own, so a game question waiting on a browser holds only that seat's thread, never a web server thread.

The table a browser sits at (`Lobby` and `NetplaySeat`) is changed only on the UI thread, which also opens, starts and leaves it. Other threads may read it, which is why its maps are concurrent and its fields volatile.

Forge's preferences have no lock, and two saves at once can garble their file, so the host's preferences are also written on the UI thread. A seat's settings and stops during a match are the exception. The match writes them as they change, under the seat's `mirrorLock`.

A few locks remain where several threads share one thing:

- `WebHub`' own lock covers the list of sessions, the host's seat and the chat.
- `DeckCatalog.DECKS` guards the deck lists and stores every session reads.
- `WebSession`'s own lock covers moving between stages and a browser arriving or leaving. `OfflineLimited` also uses it to guard its draft.
- `DeckWorkspace`'s own lock covers the open deck editor.
- `Lobby`'s `sentLock` keeps the deck list and table messages to one browser in order.
- `WebGuiGame`'s `mirrorLock` keeps the server's copy of the browser's game in step with what was sent. `PromptState` and `ShownZones` each lock themselves under it, and `PromptState` takes `ShownZones` when it sends.

They are taken in that order: `WebHub`, the session's lock, then `DeckWorkspace`'s lock, then `sentLock`, then `DECKS`. `mirrorLock` comes after the session's lock, then `PromptState`, then `ShownZones`.

### The page

The page is TypeScript, bundled by esbuild. Only `main.ts` talks to the server. Components never send messages themselves. They call the functions in `actions.ts`.

State lives in one of three places, and each change belongs in one of them:

- `model` holds what the server said. The message handlers write it, and so do the few actions in `main.ts` that clear an answered question at once.
- `ui` holds what this browser has arranged and the server never hears of, such as an open pile or a pinned preview. It is changed through `changeUi` and its variants in `ui.ts`, which redraw the page. A renderer that changes it while drawing sets the field directly, because the frame it changes is already being drawn.
- A component's own state holds what only that component uses.

Nothing draws when it changes. Messages and `ui` changes schedule one redraw for the next animation frame, which draws the whole page from the state. The game's events since the last frame go to that frame, so each is shown once. During a match, game messages can be held back while the page shows something the player should see first, such as a turn's banner. Input that depends on the board is not sent while they are held.

The pages outside a match use Preact. The match screen is drawn by hand, because it is laid out by measuring the table, and its cards move between places in animations that a re-created element would restart. `match/reconcile.ts` keeps each card's element across redraws. A new part of the match screen follows its neighbours.

The card preview is shared by every page. Elements join it through data attributes rather than code, as `preview.ts` describes. The match's cards call `hoverable()` and carry `data-zoom`, `data-key` and `data-from` instead.

## The protocol

The messages are Java records in `ToBrowser` and `FromBrowser`. Each build generates the page's TypeScript types from them, so a change to a record fails the build wherever the page uses it. A field may be null only when it is marked `@Nullable`.

Each command the page sends has one owner, which handles it from its `Commands` table: the session, the table (`TableCommands`), the deck editor, the Limited pages or the match. The tests check that every `IGuiGame` method is answered and that every command has exactly one owner.

Game state needs no protocol work. It travels as Forge's own `TrackableProperty` values, so a new property reaches the page without a change here.

## Common changes

A new page:

- a `<main id>` for it in `index.html`, and a stylesheet linked there
- its `render` call, its place in `DEPTH`, and its name in the two return types in `screens.tsx`
- its id in the list `render()` in `main.ts` shows and hides

A new message from the server:

- a record marked `@Message` in `ToBrowser`, then `process-classes` to write its type
- a `case` for it in `apply()` in `main.ts`, and a field for it in `Model` in `model.ts`

A new command from the page:

- a record marked `@Command` in `FromBrowser`, or a constant of `Plain` for one with no fields
- a handler in its owner's `Commands` table
- a function for it in `Actions` and `createActions` in `actions.ts`

A new setting:

- an entry in `SETTINGS` in `settings.ts`
- for one marked `server: true`, which the game must know, its preference in `Options` and its value in `ServerSettings`

## Text and languages

All text a player sees comes from Forge's language files. The page's own keys sit at the end of `en-US.properties` under `#forge-gui-web`, and new keys go into `en-US.properties` only, because Forge falls back to English. The build fails if the page uses a key the file lacks. The texts follow `java.text.MessageFormat`. Write whole sentences with arguments in them, so a translation can change the word order.

## Logging

The module logs through `netLog` from Forge's `IHasForgeLog`, whatever the subject, so every line it writes reaches the network log file as well as the console.

## Generated files

Git ignores the page bundle (`src/main/resources/web/js/`) and the two generated TypeScript files (`*.gen.ts`). Maven writes them during the build. After changing a message record or adding a language key, `mvn -Pweb -pl forge-gui-web -am process-classes` writes the generated files again. The bundle is written into `src/` so that a server run from the source folder serves it.

## Tests

The Java tests and Vitest run with `mvn -Pweb -pl forge-gui-web -am test`, and CI runs them. They catch changes elsewhere in Forge that would break this module without anyone noticing.

A few tests fail on purpose when something outside this module changes, because the page depends on it. When one of them fails after a change elsewhere, the fix belongs in this module:

- `CardDetailTextTest` pins the English lines from `CardDetailUtil` that `match/rulestext.ts` turns into chips.
- `BrowserSoundsTest` pins the sound names the page plays for a land and for a block.
- `WebGuiGameTest.everyHostCallThatReturnsAValueIsImplementedHere` fails when a new `ProtocolMethod` on `IGuiGame` returns a value and `WebGuiGame` does not override it.
