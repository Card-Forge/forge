# ForgeHeadless Documentation

## Overview
ForgeHeadless is a command-line interface for running Magic: The Gathering games in a headless environment. It supports both AI vs AI simulations and interactive human gameplay modes, with comprehensive game state logging.

## Installation & Setup

### Building
```bash
mvn clean install -DskipTests
```

### Running
```bash
./forge-headless [options]
```

## Usage Modes

### 1. AI vs AI Mode
Run a fully automated game between two AI players:
```bash
./forge-headless --both-ai
```

**Output:**
- Game progresses automatically
- Detailed event log written to `headless_game.log`
- Process exits when game completes

### 2. Interactive Mode (Default)
One human player against one AI player:
```bash
./forge-headless
```

**Available Commands:**
- `get_state` - Returns current game state as JSON
- `possible_actions` - Returns all available actions as JSON
- `play_action <index>` - Executes action at given index
- `pass_priority` - Passes priority to opponent
- `concede` - Concedes the game

### 3. Hotseat Mode
Two human players:
```bash
./forge-headless --both-human
```

### 4. AI as Player 1
AI plays as Player 1, human as Player 2:
```bash
./forge-headless --p1-ai
```

## Game State API

### `get_state` Command
Returns comprehensive JSON representation of current game state.

**Response Structure:**
```json
{
  "turn": 5,
  "phase": "MAIN1",
  "active_player": "Player 1",
  "priority_player": "Player 1",
  "stack_size": 0,
  "stack": [],
  "players": [
    {
      "id": 0,
      "name": "Player 1",
      "life": 18,
      "libraryCount": 52,
      "hand": [...],
      "graveyard": [...],
      "battlefield": [...],
      "exile": [...]
    }
  ]
}
```

**Card Object Structure:**
```json
{
  "name": "Forest",
  "id": 12345,
  "zone": "Battlefield"
}
```

### `possible_actions` Command
Returns all legal actions the current player can take.

**Response Structure:**
```json
{
  "actions": [
    {
      "type": "play_land",
      "card_id": 123,
      "card_name": "Forest"
    },
    {
      "type": "cast_spell",
      "card_id": 456,
      "card_name": "Lightning Bolt",
      "ability_description": "Lightning Bolt deals 3 damage to any target.",
      "mana_cost": "R"
    },
    {
      "type": "activate_ability",
      "card_id": 789,
      "card_name": "Llanowar Elves",
      "ability_description": "Add {G}.",
      "mana_cost": "T"
    },
    {
      "type": "pass_priority"
    }
  ],
  "count": 4
}
```

**Action Types:**
- `play_land` - Play a land card
- `cast_spell` - Cast a spell from hand
- `activate_ability` - Activate an ability of a permanent
- `pass_priority` - Pass priority (always available)

### `play_action <index>` Command
Executes the action at the specified index from the `possible_actions` list.

**Example Workflow:**
```bash
# Get possible actions
possible_actions
# Returns actions with indices 0-3

# Play the first action (index 0)
play_action 0

# Game processes the action and returns control
```

## Logging System

### File Output: `headless_game.log`
Automatically generated during AI vs AI games. Contains detailed event log.

**Logged Events:**
- **Turn markers**: `=== Turn N - Player Name ===`
- **Phase transitions**: `Phase: MAIN1`, `Phase: COMBAT_BEGIN`, etc.
- **Land plays**: `LAND: Forest played by Player 1`
- **Spell casts**: `CAST: Lightning Bolt by Player 2`
- **Combat events**:
  - `COMBAT: Attackers declared by Player 1`
  - `COMBAT: Blockers declared by Player 2`
- **Damage**: `DAMAGE: Player 1 took 3 damage from Lightning Bolt`
- **Life changes**: `LIFE: Player 1 is now at 17`
- **Game end**: `*** GAME OVER ***` with outcome details

### Example Log Output
```
=== Turn 4 - AI Player 1 ===
Phase: UNTAP
Phase: UPKEEP
Phase: DRAW
Phase: MAIN1
LAND: Forest played by AI Player 1
CAST: Llanowar Elves by AI Player 1
Phase: COMBAT_BEGIN
Phase: COMBAT_DECLARE_ATTACKERS
COMBAT: Attackers declared by AI Player 1
  Target: AI Player 2
    - Grizzly Bears (2/2)
Phase: COMBAT_DECLARE_BLOCKERS
COMBAT: Blockers declared by AI Player 2
Phase: COMBAT_DAMAGE
DAMAGE: AI Player 2 took 2 damage from Grizzly Bears
LIFE: AI Player 2 is now at 18
Phase: COMBAT_END
Phase: MAIN2
Phase: END_OF_TURN
Phase: CLEANUP
```

## Targeting

### Interactive Target Selection
When you cast a spell or activate an ability that requires targets, the game will interactively prompt you to select targets.

**Example Workflow:**
```bash
# Cast a spell requiring targets (e.g., Shock)
play_action 3

# Game prompts for target selection
Targets required: {
  "min": 1,
  "max": 1,
  "title": "Select targets for Shock",
  "targets": [
    {
      "index": 0,
      "type": "Player",
      "name": "Player 1",
      "id": 0,
      "life": 20
    },
    {
      "index": 1,
      "type": "Player",
      "name": "AI Player 2",
      "id": 1,
      "life": 18
    }
  ]
}
Choose target index (0/1): 1

# Spell is cast targeting AI Player 2
```

**Target Selection:**
- The game displays all valid targets with indexed options
- Enter the index number of your chosen target
- For spells requiring multiple targets, you'll be prompted multiple times
- Enter `-1` to cancel (only if minimum targets already selected)
- Invalid selections will prompt you to try again

**Target Information:**
Each target option includes:
- **index**: Selection number to enter
- **type**: "Player", "Card", or other game object type
- **name**: Display name of the target
- **id**: Unique game object ID
- Additional context (life total for players, power/toughness for creatures, etc.)

**Notes:**
- Mana costs are paid automatically from available mana
- Targets must be selected before the spell can be cast
- The game validates target legality automatically

## Technical Details

### Architecture
- **Main Class**: `forge.view.ForgeHeadless`
- **Player Controller**: `HeadlessPlayerController` extends `PlayerControllerAi`
- **Game Observer**: `HeadlessGameObserver` implements event logging
- **Deck Generation**: Random decks generated via `DeckgenUtil.generateRandomDeck()`

### Event System
Uses Guava's `EventBus` with `@Subscribe` pattern:
- `HeadlessGameObserver` subscribes to game events
- Events processed via visitor pattern (`IGameEventVisitor`)
- Logs written to file in real-time with auto-flush

### Game Flow
1. Initialize game with specified player types
2. Generate random decks for both players
3. Create match and game objects
4. Register event observer (for logging)
5. Start game loop
6. Process player actions via controller
7. Game ends when win condition met

## Files

### Core Files
- `forge-gui-desktop/src/main/java/forge/view/ForgeHeadless.java` - Main implementation
- `forge-headless` - Launch script

### Generated Files
- `headless_game.log` - Game event log (gitignored)

## Limitations & Known Issues

1. **Random Decks Only**: Currently generates random decks; custom deck loading not supported
2. **No Replay**: Games cannot be replayed or saved/loaded mid-game
3. **Automatic Targeting**: Interactive mode uses AI targeting logic
4. **Limited Error Handling**: Invalid commands print error but don't exit gracefully
5. **No Turn Timeout**: Interactive games can wait indefinitely for input

## Future Enhancements

- [ ] Manual target selection for interactive mode
- [ ] Custom deck loading from file
- [ ] Game state save/load functionality
- [ ] Multiplayer support (3+ players)
- [ ] Replay system
- [ ] Enhanced error messages and validation
- [ ] Optional logging verbosity levels
- [ ] Match statistics and analysis tools

## Development

### Adding New Commands
1. Add command handler in `HeadlessPlayerController.chooseSpellAbilityToPlay()`
2. Parse command arguments
3. Execute appropriate game logic
4. Return result or continue game loop

### Extending Logging
1. Override additional visit methods in `HeadlessGameObserver`
2. Format log output in visit method
3. Call `log()` helper to write to file

### Testing
```bash
# Build project
mvn clean install -DskipTests

# Run AI vs AI game
./forge-headless --both-ai

# Check log output
cat headless_game.log
```

## Troubleshooting

**Problem**: `forge-headless` script fails
- **Solution**: Ensure Java 17+ is installed and in PATH
- **Check**: `java -version`

**Problem**: Permission denied on `forge-headless`
- **Solution**: `chmod +x forge-headless`

**Problem**: No log file generated
- **Solution**: Check write permissions in project directory

**Problem**: Game hangs in interactive mode
- **Solution**: Type `pass_priority` to continue or `concede` to exit

## License & Credits

Part of the Forge MTG project. See main project README for full license information.
