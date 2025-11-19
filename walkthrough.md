# Forge Headless Game Environment Walkthrough

This document outlines the implementation of a headless Magic: The Gathering game environment using the Forge engine.

## Objective
Create a headless environment to:
1. Set up a 1v1 Standard match.
2. Generate random Standard-legal decks.
3. Initialize the game to the state immediately after opening hands are dealt (bypassing mulligans).
4. Extract the initial game state as a JSON object.

## Solution Overview

The solution involves a new class `forge.view.ForgeHeadless` in the `forge-gui-desktop` module. This class acts as the entry point and orchestrates the game setup without initializing the full GUI.

### Key Components

1.  **`ForgeHeadless.java`**: The main entry point.
    *   Initializes Forge's global state (`FModel`) using a custom `HeadlessGui`.
    *   Generates random Standard decks using `DeckgenUtil`.
    *   Sets up a `Match` with two players.
    *   Starts the game and halts it immediately after initialization using a `StopGameException`.
    *   Extracts the game state into a JSON object.

2.  **`HeadlessGui`**: An implementation of `IGuiBase` that provides dummy methods for GUI operations. This is required because `FModel` and other core Forge components depend on `GuiBase.getInterface()` being non-null.

3.  **`HeadlessLobbyPlayer` & `HeadlessPlayerController`**: Custom implementations of `LobbyPlayerAi` and `PlayerControllerAi`.
    *   `HeadlessPlayerController` overrides `mulliganKeepHand` to always return `true`, ensuring that players keep their opening hands and skipping the mulligan phase.

### Execution Flow

1.  **Initialization**: `GuiBase.setInterface(new HeadlessGui())` is called first to satisfy static dependencies. Then `FModel.initialize(null, null)` loads card data and resources.
2.  **Deck Generation**: `DeckgenUtil.getRandomColorDeck` is called with the Standard format filter to create legal decks.
3.  **Player Setup**: `RegisteredPlayer` objects are created with `HeadlessLobbyPlayer` to ensure the custom controller is used.
4.  **Game Start**: `match.startGame(game)` is called. A `StartGameHook` is registered to throw `StopGameException` as soon as the game loop starts (which happens after initial setup and hand dealing).
5.  **State Extraction**: The `StopGameException` is caught, and the `Game` object is passed to `extractGameState`, which serializes the relevant data (players, hands, life, etc.) to JSON.

## Usage

To run the headless environment:

```bash
export JAVA_HOME="/path/to/jdk-17"
mvn compile -pl forge-gui-desktop -Drevision=2.0.07-SNAPSHOT
mvn exec:java -pl forge-gui-desktop -Dexec.mainClass="forge.view.ForgeHeadless" -Drevision=2.0.07-SNAPSHOT
```

## JSON Output Format

The output is a JSON object printed to stdout:

```json
{
  "turn": 1,
  "phase": "UNTAP",
  "activePlayerId": 1,
  "players": [
    {
      "id": 0,
      "name": "Player 1",
      "life": 20,
      "libraryCount": 53,
      "hand": [ ... 7 cards ... ],
      "graveyard": [],
      "battlefield": [],
      "exile": []
    },
    {
      "id": 1,
      "name": "Player 2",
      "life": 20,
      "libraryCount": 53,
      "hand": [ ... 7 cards ... ],
      "graveyard": [],
      "battlefield": [],
      "exile": []
    }
  ]
}
```
