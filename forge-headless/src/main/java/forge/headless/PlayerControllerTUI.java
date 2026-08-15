package forge.headless;

import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.ai.ComputerUtilMana;
import forge.card.MagicColor;
import forge.game.GameEntity;
import forge.ai.PlayerControllerAi;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.collect.FCollectionView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TUI Player Controller - Extends AI controller but overrides key methods
 * to provide interactive text-based gameplay.
 *
 * For now, this only supports two actions:
 * 1. Pass priority (do nothing)
 * 2. Play a land from hand
 */
public class PlayerControllerTUI extends PlayerControllerAi {

    private final BufferedReader reader;
    private final boolean askMana;  // Whether to prompt for mana abilities
    private final boolean numericChoices;  // Whether to use numeric-only mode

    // Choice tracking statistics
    private int totalChoicesMade = 0;
    private int totalChoiceOptions = 0;

    public PlayerControllerTUI(Game game, Player p, LobbyPlayer lp) {
        this(game, p, lp, false, false);
    }

    public PlayerControllerTUI(Game game, Player p, LobbyPlayer lp, boolean askMana) {
        this(game, p, lp, askMana, false);
    }

    public PlayerControllerTUI(Game game, Player p, LobbyPlayer lp, boolean askMana, boolean numericChoices) {
        this(game, p, lp, askMana, numericChoices, new BufferedReader(new InputStreamReader(System.in)));
    }

    /**
     * Constructor with injectable reader for testing.
     */
    public PlayerControllerTUI(Game game, Player p, LobbyPlayer lp, BufferedReader reader) {
        this(game, p, lp, false, false, reader);
    }

    /**
     * Full constructor with all options.
     */
    public PlayerControllerTUI(Game game, Player p, LobbyPlayer lp, boolean askMana, boolean numericChoices, BufferedReader reader) {
        super(game, p, lp);
        this.askMana = askMana;
        this.numericChoices = numericChoices;
        this.reader = reader;
    }

    /**
     * Get statistics about choices made during the game.
     */
    public int getTotalChoicesMade() {
        return totalChoicesMade;
    }

    public int getTotalChoiceOptions() {
        return totalChoiceOptions;
    }

    @Override
    public boolean isAI() {
        return false; // We're a human player
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // No-op for TUI - we don't need to show ante cards
        System.out.println("[Ante] " + message);
    }

    @Override
    public void revealAISkipCards(String message, Map<Player, Map<DeckSection, List<? extends PaperCard>>> deckCards) {
        // No-op for TUI - we don't need to show AI-unplayable cards
        // This is called during game setup to show cards the AI can't use
    }

    @Override
    public void revealUnsupported(Map<Player, List<PaperCard>> unsupported) {
        // No-op for TUI
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return true;  // No targeting needed
        }

        forge.game.spellability.TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            return true;  // No target restrictions
        }

        System.out.println("\n=== Choose Target for " + sa.getHostCard().getName() + " ===");

        // Collect all valid targets
        List<forge.game.GameObject> validTargets = new ArrayList<>();
        Game game = getGame();

        // Check all players as potential targets
        for (Player p : game.getPlayers()) {
            if (sa.canTarget(p)) {
                validTargets.add(p);
            }
        }

        // Check all cards on battlefield as potential targets
        for (Player p : game.getPlayers()) {
            for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
                if (sa.canTarget(c)) {
                    validTargets.add(c);
                }
            }
        }

        // Check spells on the stack as potential targets (for counterspells, etc.)
        for (var stackItem : game.getStack()) {
            SpellAbility stackSa = stackItem.getSpellAbility();
            if (stackSa != null && sa.canTarget(stackSa)) {
                validTargets.add(stackSa);
            }
        }

        // Check other zones if needed (graveyard, hand, etc.)
        // For most burn spells, battlefield and players are enough

        if (validTargets.isEmpty()) {
            System.out.println("  No valid targets available.");
            return false;
        }

        // Show valid targets
        System.out.println("Select a target:");
        for (int i = 0; i < validTargets.size(); i++) {
            forge.game.GameObject target = validTargets.get(i);
            String desc = formatTarget(target);
            System.out.println("  " + i + ". " + desc);
        }

        // Get user choice
        int choice = getIntInput(0, validTargets.size() - 1);
        forge.game.GameObject chosen = validTargets.get(choice);

        // Set the target
        sa.getTargets().add(chosen);

        System.out.println(">> Targeting " + formatTarget(chosen) + "\n");
        return true;
    }

    /**
     * Format a game object (player, card, or spell) for display.
     */
    private String formatTarget(forge.game.GameObject target) {
        if (target instanceof Player) {
            Player p = (Player) target;
            return p.getName() + " (Life: " + p.getLife() + ")";
        } else if (target instanceof SpellAbility) {
            SpellAbility sa = (SpellAbility) target;
            Card source = sa.getHostCard();
            String desc = "Spell: " + source.getName();
            if (sa.getActivatingPlayer() != null) {
                desc += " [" + sa.getActivatingPlayer().getName() + "]";
            }
            return desc;
        } else if (target instanceof Card) {
            Card c = (Card) target;
            String desc = c.getName();
            if (c.isCreature()) {
                desc += " (" + c.getNetPower() + "/" + c.getNetToughness() + ")";
            }
            if (c.getController() != null) {
                desc += " [" + c.getController().getName() + "]";
            }
            return desc;
        }
        return target.toString();
    }

    /**
     * Check if a spell ability has valid targets available.
     * Returns true if the spell doesn't use targeting, or if it has at least one valid target.
     */
    private boolean hasValidTargets(SpellAbility sa) {
        // If doesn't use targeting, it's always valid
        if (!sa.usesTargeting()) {
            return true;
        }

        forge.game.spellability.TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            return true;  // No target restrictions
        }

        Game game = getGame();

        // Check if any players are valid targets
        for (Player p : game.getPlayers()) {
            if (sa.canTarget(p)) {
                return true;
            }
        }

        // Check if any cards on battlefield are valid targets
        for (Player p : game.getPlayers()) {
            for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
                if (sa.canTarget(c)) {
                    return true;
                }
            }
        }

        // Check other zones if needed (graveyard, hand, etc.)
        // Most targeted spells check battlefield and players, but some may target other zones
        for (Player p : game.getPlayers()) {
            for (Card c : p.getCardsIn(ZoneType.Graveyard)) {
                if (sa.canTarget(c)) {
                    return true;
                }
            }
        }

        // Check spells on the stack (important for counterspells!)
        for (var stackItem : game.getStack()) {
            SpellAbility stackSa = stackItem.getSpellAbility();
            if (stackSa != null && sa.canTarget(stackSa)) {
                return true;
            }
        }

        // No valid targets found
        return false;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // Print any new game log entries
        TUIGuiBase.printNewLogEntries();

        Game game = getGame();
        PhaseHandler ph = game.getPhaseHandler();
        boolean isMyTurn = ph.isPlayerTurn(player);
        boolean isMainPhase = ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2);
        boolean isPostCombatMain = ph.is(forge.game.phase.PhaseType.MAIN2);
        boolean isEndStep = ph.is(forge.game.phase.PhaseType.END_OF_TURN);
        boolean stackHasItems = !game.getStack().isEmpty();

        // Get playable actions from hand
        List<SpellAbility> landAbilities = getPlayableLands();
        List<SpellAbility> creatureAbilities = new ArrayList<>();
        List<SpellAbility> artifactAbilities = new ArrayList<>();
        List<SpellAbility> instantAbilities = new ArrayList<>();
        List<SpellAbility> sorceryAbilities = new ArrayList<>();

        // In any main phase, check for castable sorcery-speed spells
        if (isMainPhase) {
            creatureAbilities = getCastableCreaturesAndArtifacts(true);
            artifactAbilities = getCastableCreaturesAndArtifacts(false);
            sorceryAbilities = getCastableSorceries();
        }

        // Instants can be cast at any time we have priority
        instantAbilities = getCastableInstants();

        // Get activated abilities from permanents on the battlefield
        List<SpellAbility> activatedAbilities = getActivatedAbilities();

        // Count total available actions
        int totalActions = landAbilities.size() + creatureAbilities.size() + artifactAbilities.size() +
                           instantAbilities.size() + sorceryAbilities.size() + activatedAbilities.size();

        // Smart auto-passing on opponent's turn
        if (!isMyTurn) {
            // Only prompt on opponent's turn if:
            // 1. Stack has items (we might want to respond), OR
            // 2. It's end step AND we have instants (classic "at end of your turn" timing), OR
            // 3. We have activated abilities

            boolean hasInstantSpeedActions = !instantAbilities.isEmpty() || !activatedAbilities.isEmpty();
            boolean shouldPrompt = stackHasItems || (isEndStep && hasInstantSpeedActions);

            if (!shouldPrompt) {
                // Silently pass - don't spam output during opponent's turn
                return null;
            }

            // If no actions available even though we should prompt, auto-pass with minimal output
            if (totalActions == 0) {
                return null;
            }
        } else {
            // On our turn, if there are no options besides passing, auto-pass without prompting
            if (totalActions == 0) {
                System.out.println(">> Auto-passing priority (no actions available)...\n");
                return null;
            }
        }

        // Deduplicate abilities - only show unique actions
        List<SpellAbility> uniqueLands = deduplicateByCardName(landAbilities);
        List<SpellAbility> uniqueCreatures = deduplicateByCardName(creatureAbilities);
        List<SpellAbility> uniqueArtifacts = deduplicateByCardName(artifactAbilities);
        List<SpellAbility> uniqueSorceries = deduplicateByCardName(sorceryAbilities);
        List<SpellAbility> uniqueInstants = deduplicateByCardName(instantAbilities);
        List<SpellAbility> uniqueActivated = deduplicateByDescription(activatedAbilities);

        // Recalculate total actions after deduplication
        int uniqueActions = uniqueLands.size() + uniqueCreatures.size() + uniqueArtifacts.size() +
                           uniqueInstants.size() + uniqueSorceries.size() + uniqueActivated.size();

        // Show options to user with context-appropriate header
        if (!isMyTurn) {
            System.out.println("\n=== OPPONENT'S TURN ===");
            if (stackHasItems) {
                System.out.println("[Respond to spell on stack]");
            } else if (isEndStep) {
                System.out.println("[End of opponent's turn]");
            }
        } else {
            // Only display full game state on our own turn
            displayGameState();

            System.out.println("\n=== YOUR TURN ===");
            if (isPostCombatMain) {
                System.out.println("[Post-Combat Main Phase]");
            } else if (isMainPhase) {
                System.out.println("[Pre-Combat Main Phase]");
            }
        }

        System.out.println("What would you like to do?");
        System.out.println("  0. Pass priority (do nothing)");

        int optionNum = 1;

        // Show land options
        for (SpellAbility sa : uniqueLands) {
            Card land = sa.getHostCard();
            System.out.println("  " + optionNum + ". Play land: " + land.getName());
            optionNum++;
        }

        // Show creature options
        for (SpellAbility sa : uniqueCreatures) {
            Card creature = sa.getHostCard();
            System.out.println("  " + optionNum + ". Cast creature: " + creature.getName() +
                " (" + creature.getNetPower() + "/" + creature.getNetToughness() + ") - " +
                creature.getManaCost());
            optionNum++;
        }

        // Show artifact options
        for (SpellAbility sa : uniqueArtifacts) {
            Card artifact = sa.getHostCard();
            System.out.println("  " + optionNum + ". Cast artifact: " + artifact.getName() +
                " - " + artifact.getManaCost());
            optionNum++;
        }

        // Show sorcery options
        for (SpellAbility sa : uniqueSorceries) {
            Card sorcery = sa.getHostCard();
            System.out.println("  " + optionNum + ". Cast sorcery: " + sorcery.getName() +
                " - " + sorcery.getManaCost());
            optionNum++;
        }

        // Show instant options
        for (SpellAbility sa : uniqueInstants) {
            Card instant = sa.getHostCard();
            System.out.println("  " + optionNum + ". Cast instant: " + instant.getName() +
                " - " + instant.getManaCost());
            optionNum++;
        }

        // Show activated ability options
        for (SpellAbility sa : uniqueActivated) {
            Card source = sa.getHostCard();
            String description = sa.getDescription();
            if (description == null || description.isEmpty()) {
                description = "Activate ability";
            }
            System.out.println("  " + optionNum + ". " + source.getName() + ": " + description);
            optionNum++;
        }

        // Get user input
        int choice = getIntInput(0, uniqueActions);

        // Track choice statistics
        totalChoicesMade++;
        totalChoiceOptions += (uniqueActions + 1); // +1 for pass option

        if (choice == 0) {
            System.out.println(">> Passing priority...\n");
            return null; // Pass priority
        } else {
            // Find the chosen ability
            SpellAbility chosen = null;
            int idx = choice - 1;

            if (idx < uniqueLands.size()) {
                chosen = uniqueLands.get(idx);
                System.out.println(">> Playing " + chosen.getHostCard().getName() + "...\n");
            } else if ((idx -= uniqueLands.size()) < uniqueCreatures.size()) {
                chosen = uniqueCreatures.get(idx);
                System.out.println(">> Casting " + chosen.getHostCard().getName() + "...\n");
            } else if ((idx -= uniqueCreatures.size()) < uniqueArtifacts.size()) {
                chosen = uniqueArtifacts.get(idx);
                System.out.println(">> Casting " + chosen.getHostCard().getName() + "...\n");
            } else if ((idx -= uniqueArtifacts.size()) < uniqueSorceries.size()) {
                chosen = uniqueSorceries.get(idx);
                System.out.println(">> Casting " + chosen.getHostCard().getName() + "...\n");
            } else if ((idx -= uniqueSorceries.size()) < uniqueInstants.size()) {
                chosen = uniqueInstants.get(idx);
                System.out.println(">> Casting " + chosen.getHostCard().getName() + "...\n");
            } else if ((idx -= uniqueInstants.size()) < uniqueActivated.size()) {
                chosen = uniqueActivated.get(idx);
                System.out.println(">> Activating " + chosen.getHostCard().getName() + "...\n");
            }

            // For spells that require targeting, choose targets now before returning
            if (chosen != null && chosen.isSpell() && chosen.usesTargeting()) {
                if (!chooseTargetsFor(chosen)) {
                    // Targeting failed - don't cast the spell
                    System.out.println("Targeting cancelled. Spell not cast.\n");
                    return null;
                }
            }

            return Collections.singletonList(chosen);
        }
    }

    /**
     * Get playable land abilities from the player's hand.
     */
    private List<SpellAbility> getPlayableLands() {
        List<SpellAbility> lands = new ArrayList<>();

        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                    if (sa.isLandAbility() && sa.canPlay()) {
                        lands.add(sa);
                        break; // Only need one land ability per land
                    }
                }
            }
        }

        return lands;
    }

    /**
     * Get castable creature or artifact spells from the player's hand.
     * @param creatures if true, get creatures; if false, get artifacts
     */
    private List<SpellAbility> getCastableCreaturesAndArtifacts(boolean creatures) {
        List<SpellAbility> spells = new ArrayList<>();

        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            // Check if we're looking for the right type
            if (creatures && !c.isCreature()) continue;
            if (!creatures && !c.isArtifact()) continue;
            // Skip if it's also a land (like artifact lands)
            if (c.isLand()) continue;

            // Get the main spell ability (casting the card)
            for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                // We want spell abilities that can be cast from hand
                if (sa.isSpell() && sa.canPlay()) {
                    // Check if player can actually pay the mana cost right now
                    sa.setActivatingPlayer(player);
                    if (ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        spells.add(sa);
                    }
                    break; // Only need the first castable ability
                }
            }
        }

        return spells;
    }

    /**
     * Get castable sorcery spells from the player's hand.
     */
    private List<SpellAbility> getCastableSorceries() {
        List<SpellAbility> spells = new ArrayList<>();

        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (!c.isSorcery()) continue;

            // Get the main spell ability (casting the card)
            for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                // We want spell abilities that can be cast from hand
                if (sa.isSpell() && sa.canPlay()) {
                    // Check if player can actually pay the mana cost right now
                    sa.setActivatingPlayer(player);
                    if (ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        // Only include if spell has valid targets (or doesn't need targets)
                        if (hasValidTargets(sa)) {
                            spells.add(sa);
                        }
                    }
                    break; // Only need the first castable ability
                }
            }
        }

        return spells;
    }

    /**
     * Get castable instant spells from the player's hand.
     */
    private List<SpellAbility> getCastableInstants() {
        List<SpellAbility> spells = new ArrayList<>();

        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (!c.isInstant()) continue;

            // Get the main spell ability (casting the card)
            for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                // We want spell abilities that can be cast from hand
                if (sa.isSpell() && sa.canPlay()) {
                    // Check if player can actually pay the mana cost right now
                    sa.setActivatingPlayer(player);
                    if (ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        // Only include if spell has valid targets (or doesn't need targets)
                        if (hasValidTargets(sa)) {
                            spells.add(sa);
                        }
                    }
                    break; // Only need the first castable ability
                }
            }
        }

        return spells;
    }

    /**
     * Get activated abilities from permanents on the battlefield.
     * Filters out mana abilities unless askMana is true.
     */
    private List<SpellAbility> getActivatedAbilities() {
        List<SpellAbility> abilities = new ArrayList<>();

        // Check all permanents we control
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
            // Get all abilities for this permanent
            for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                // We want activated abilities (not spells, not triggered)
                // Activated abilities are not spells and can be played from the battlefield
                if (!sa.isSpell() && sa.canPlay() && !sa.isTrigger()) {
                    // Skip mana abilities unless askMana is enabled
                    if (!askMana && sa.isManaAbility()) {
                        continue;
                    }

                    // Check if player can pay the activation cost
                    sa.setActivatingPlayer(player);
                    // Check if player can actually afford the cost right now
                    if (ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        abilities.add(sa);
                    }
                }
            }
        }

        return abilities;
    }

    /**
     * Display the current game state to the user.
     */
    private void displayGameState() {
        Game game = getGame();
        PhaseHandler ph = game.getPhaseHandler();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("Turn " + ph.getTurn() + " - " + ph.getPlayerTurn().getName() + "'s turn");
        System.out.println("Phase: " + ph.getPhase().nameForUi);

        Player priorityPlayer = ph.getPriorityPlayer();
        if (priorityPlayer != null) {
            System.out.println("Priority: " + priorityPlayer.getName());
        }

        // Display stack contents if not empty
        if (game.getStack().isEmpty()) {
            System.out.println("Stack: Empty");
        } else {
            System.out.println("Stack: " + game.getStack().size() + " item(s)");
            // Show stack contents from top to bottom
            var stackItems = game.getStack();
            int stackPos = stackItems.size();
            for (var item : stackItems) {
                SpellAbility sa = item.getSpellAbility();
                if (sa != null) {
                    Card source = sa.getHostCard();
                    String controller = item.getActivatingPlayer() != null ?
                        item.getActivatingPlayer().getName() : "Unknown";
                    System.out.println("  " + stackPos + ". " + source.getName() +
                        " (" + controller + ")");
                    if (sa.hasParam("Description")) {
                        System.out.println("      " + sa.getParam("Description"));
                    }
                    stackPos--;
                }
            }
        }
        System.out.println();

        // Display all players
        for (Player p : game.getPlayers()) {
            displayPlayerInfo(p, game);
        }

        System.out.println("-".repeat(60));
    }

    /**
     * Display information about a single player.
     */
    private void displayPlayerInfo(Player p, Game game) {
        boolean isCurrentPlayer = p == game.getPhaseHandler().getPlayerTurn();
        boolean isThisPlayer = p == player;

        String marker;
        if (isThisPlayer) {
            marker = ">>> [YOU] ";
        } else if (isCurrentPlayer) {
            marker = ">>> ";
        } else {
            marker = "    ";
        }

        System.out.println(marker + p.getName());
        System.out.println(marker + "  Life: " + p.getLife());
        System.out.println(marker + "  Hand: " + p.getZone(ZoneType.Hand).size() + " cards");

        if (isThisPlayer) {
            // Show the human player their hand
            List<Card> hand = new ArrayList<>();
            for (Card c : p.getCardsIn(ZoneType.Hand)) {
                hand.add(c);
            }
            if (!hand.isEmpty()) {
                System.out.println(marker + "  Your hand:");
                for (Card c : hand) {
                    System.out.println(marker + "    - " + c.getName());
                }
            }
        }

        System.out.println(marker + "  Library: " + p.getZone(ZoneType.Library).size() + " cards");
        System.out.println(marker + "  Graveyard: " + p.getZone(ZoneType.Graveyard).size() + " cards");
        System.out.println(marker + "  Lands played this turn: " + p.getLandsPlayedThisTurn());

        // Show mana pool if non-empty (only for this player)
        if (isThisPlayer) {
            int white = p.getManaPool().getAmountOfColor(MagicColor.WHITE);
            int blue = p.getManaPool().getAmountOfColor(MagicColor.BLUE);
            int black = p.getManaPool().getAmountOfColor(MagicColor.BLACK);
            int red = p.getManaPool().getAmountOfColor(MagicColor.RED);
            int green = p.getManaPool().getAmountOfColor(MagicColor.GREEN);
            int colorless = p.getManaPool().getAmountOfColor(MagicColor.COLORLESS);
            int total = white + blue + black + red + green + colorless;

            if (total > 0) {
                StringBuilder manaPoolStr = new StringBuilder();
                manaPoolStr.append(marker).append("  Mana pool: ");
                List<String> manaComponents = new ArrayList<>();
                if (white > 0) manaComponents.add(white + " White");
                if (blue > 0) manaComponents.add(blue + " Blue");
                if (black > 0) manaComponents.add(black + " Black");
                if (red > 0) manaComponents.add(red + " Red");
                if (green > 0) manaComponents.add(green + " Green");
                if (colorless > 0) manaComponents.add(colorless + " Colorless");
                manaPoolStr.append(String.join(", ", manaComponents));
                System.out.println(manaPoolStr.toString());
            }
        }

        // Show battlefield
        List<Card> lands = new ArrayList<>();
        List<Card> creatures = new ArrayList<>();
        List<Card> others = new ArrayList<>();

        for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                lands.add(c);
            } else if (c.isCreature()) {
                creatures.add(c);
            } else {
                others.add(c);
            }
        }

        System.out.println(marker + "  Lands in play: " + lands.size());
        if (!lands.isEmpty() && (isThisPlayer || lands.size() <= 10)) {
            for (Card land : lands) {
                System.out.println(marker + "    - " + land.getName() + (land.isTapped() ? " (tapped)" : ""));
            }
        }

        System.out.println(marker + "  Creatures: " + creatures.size());
        if (!creatures.isEmpty() && (isThisPlayer || creatures.size() <= 10)) {
            for (Card creature : creatures) {
                System.out.println(marker + "    - " + creature.getName() +
                    " (" + creature.getNetPower() + "/" + creature.getNetToughness() + ")" +
                    (creature.isTapped() ? " (tapped)" : "") +
                    (creature.isSick() ? " (summoning sickness)" : ""));
            }
        }

        if (!others.isEmpty()) {
            System.out.println(marker + "  Other permanents: " + others.size());
            if (isThisPlayer || others.size() <= 5) {
                for (Card other : others) {
                    System.out.println(marker + "    - " + other.getName());
                }
            }
        }

        System.out.println();
    }

    /**
     * Get integer input from the user within a specified range.
     * In numeric mode, only accepts numeric input.
     * In normal mode, also handles special commands: "?" for help, "v" for viewing cards, "g" for graveyards.
     */
    private int getIntInput(int min, int max) {
        while (true) {
            // Inform the reader about the valid range (for random/scripted agents)
            if (reader instanceof PlayerControllerRandom.RandomChoiceReader) {
                ((PlayerControllerRandom.RandomChoiceReader) reader).setRange(min, max);
            }

            // Standardized prompt format - always show for logging/debugging
            if (numericChoices) {
                System.out.print("Enter choice (" + min + "-" + max + "): ");
            } else {
                System.out.print("Enter choice (" + min + "-" + max + ", or ?): ");
            }

            try {
                String line = reader.readLine();
                if (line == null) {
                    System.out.println("Input error, using default choice: " + min);
                    return min;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    System.out.println("No input provided, using default choice: " + min);
                    return min;
                }

                // Handle special commands only in normal (non-numeric) mode
                if (!numericChoices) {
                    if (line.equals("?")) {
                        showHelp();
                        continue;
                    }

                    if (line.equalsIgnoreCase("v")) {
                        viewCard();
                        continue;
                    }

                    if (line.equalsIgnoreCase("g")) {
                        viewGraveyards();
                        continue;
                    }

                    if (line.equalsIgnoreCase("b")) {
                        displayGameState();
                        continue;
                    }

                    if (line.equalsIgnoreCase("s")) {
                        viewStack();
                        continue;
                    }
                }

                int choice = Integer.parseInt(line);
                if (choice >= min && choice <= max) {
                    return choice;
                }

                System.out.println("Invalid choice. Please enter a number between " + min + " and " + max + ".");
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                System.out.println("Using default choice: " + min);
                return min;
            } catch (NumberFormatException e) {
                if (numericChoices) {
                    System.out.println("Invalid input. Please enter a number.");
                } else {
                    System.out.println("Invalid input. Please enter a number or '?' for help.");
                }
            }
        }
    }

    /**
     * Display help information for the TUI interface.
     */
    private void showHelp() {
        System.out.println();
        System.out.println("=== HELP ===");
        System.out.println("Commands:");
        System.out.println("  0-9       - Select an action by number");
        System.out.println("  ?         - Show this help");
        System.out.println("  v         - View a card (see detailed card text)");
        System.out.println("  g         - View all graveyards");
        System.out.println("  b         - View battlefield / game state");
        System.out.println("  s         - View the stack");
        System.out.println();
        System.out.println("During your turn, you can:");
        System.out.println("  - Play lands (if you haven't used your land drop)");
        System.out.println("  - Cast spells from your hand");
        System.out.println("  - Pass priority (0) to move to the next phase");
        System.out.println();
        System.out.println("You will be prompted repeatedly until you pass priority.");
        System.out.println("============");
        System.out.println();
    }

    /**
     * Allow the user to view detailed information about a card.
     * Shows cards from hand, both players' battlefields, and graveyards.
     */
    private void viewCard() {
        Game game = getGame();
        List<Card> allCards = new ArrayList<>();

        // Collect cards from player's hand
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            allCards.add(c);
        }

        // Collect cards from all battlefields
        for (Player p : game.getPlayers()) {
            for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
                allCards.add(c);
            }
        }

        // Collect cards from all graveyards
        for (Player p : game.getPlayers()) {
            for (Card c : p.getCardsIn(ZoneType.Graveyard)) {
                allCards.add(c);
            }
        }

        if (allCards.isEmpty()) {
            System.out.println("No cards to view.");
            return;
        }

        // Remove duplicates by card name (keep first occurrence)
        List<Card> uniqueCards = new ArrayList<>();
        java.util.Set<String> seenNames = new java.util.HashSet<>();
        for (Card c : allCards) {
            if (!seenNames.contains(c.getName())) {
                uniqueCards.add(c);
                seenNames.add(c.getName());
            }
        }

        // Sort alphabetically by name
        uniqueCards.sort((c1, c2) -> c1.getName().compareTo(c2.getName()));

        System.out.println();
        System.out.println("=== VIEW CARD ===");
        System.out.println("Select a card to view:");

        for (int i = 0; i < uniqueCards.size(); i++) {
            Card c = uniqueCards.get(i);
            String location;
            if (c.getZone().is(ZoneType.Hand)) {
                location = "[Hand]";
            } else if (c.getZone().is(ZoneType.Graveyard)) {
                location = "[Graveyard - " + c.getController().getName() + "]";
            } else {
                location = "[Battlefield - " + c.getController().getName() + "]";
            }
            System.out.println("  " + i + ". " + c.getName() + " " + location);
        }

        System.out.print("Enter card number (or press Enter to cancel): ");
        try {
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty()) {
                System.out.println("Cancelled.");
                return;
            }

            int cardIndex = Integer.parseInt(line.trim());
            if (cardIndex < 0 || cardIndex >= uniqueCards.size()) {
                System.out.println("Invalid card number.");
                return;
            }

            Card selectedCard = uniqueCards.get(cardIndex);
            displayCardDetails(selectedCard);

        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    /**
     * Display detailed information about a card.
     */
    private void displayCardDetails(Card card) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println(card.getName() + " " + card.getManaCost());
        System.out.println("-".repeat(60));

        // Show card type line
        StringBuilder typeLine = new StringBuilder();
        if (!card.getType().isEmpty()) {
            typeLine.append(card.getType().toString());
        }

        System.out.println("Type: " + typeLine.toString());

        // Show creature stats if applicable
        if (card.isCreature()) {
            System.out.println("Power/Toughness: " + card.getNetPower() + "/" + card.getNetToughness());
        }

        // Show oracle text
        String oracleText = card.getOracleText();
        if (oracleText != null && !oracleText.isEmpty()) {
            System.out.println();
            System.out.println("Text:");
            // Replace literal \n with actual newlines
            String formattedText = oracleText.replace("\\n", "\n");
            System.out.println(formattedText);
        }

        // Show current state and location
        if (card.getZone().is(ZoneType.Battlefield)) {
            System.out.println();
            System.out.println("Current State:");
            System.out.println("  Controller: " + card.getController().getName());
            if (card.isTapped()) {
                System.out.println("  [TAPPED]");
            }
            if (card.isSick()) {
                System.out.println("  [Summoning Sickness]");
            }
        } else if (card.getZone().is(ZoneType.Hand)) {
            System.out.println();
            System.out.println("Location: In hand");
        } else if (card.getZone().is(ZoneType.Graveyard)) {
            System.out.println();
            System.out.println("Location: In " + card.getController().getName() + "'s graveyard");
        }

        System.out.println("=".repeat(60));
        System.out.println();
    }

    /**
     * Display the contents of all players' graveyards.
     */
    private void viewGraveyards() {
        Game game = getGame();
        System.out.println();
        System.out.println("=== GRAVEYARDS ===");

        for (Player p : game.getPlayers()) {
            List<Card> graveyard = new ArrayList<>();
            for (Card c : p.getCardsIn(ZoneType.Graveyard)) {
                graveyard.add(c);
            }

            System.out.println();
            System.out.println(p.getName() + "'s Graveyard (" + graveyard.size() + " cards):");

            if (graveyard.isEmpty()) {
                System.out.println("  (empty)");
            } else {
                // Sort by card type for better readability
                graveyard.sort((c1, c2) -> {
                    // Sort order: Creature, Instant, Sorcery, Artifact, Enchantment, Land, Other
                    int priority1 = getCardTypePriority(c1);
                    int priority2 = getCardTypePriority(c2);
                    if (priority1 != priority2) {
                        return Integer.compare(priority1, priority2);
                    }
                    return c1.getName().compareTo(c2.getName());
                });

                for (Card c : graveyard) {
                    String cardInfo = "  - " + c.getName();
                    if (c.isCreature()) {
                        cardInfo += " (" + c.getNetPower() + "/" + c.getNetToughness() + ")";
                    }
                    cardInfo += " - " + getCardTypeString(c);
                    System.out.println(cardInfo);
                }
            }
        }

        System.out.println();
        System.out.println("==================");
        System.out.println();
    }

    /**
     * Display the current stack contents.
     */
    private void viewStack() {
        Game game = getGame();
        System.out.println();
        System.out.println("=== STACK ===");

        if (game.getStack().isEmpty()) {
            System.out.println("Stack is empty.");
        } else {
            System.out.println("Stack contents (top to bottom):");
            System.out.println();

            var stackItems = game.getStack();
            int stackPos = stackItems.size();
            for (var item : stackItems) {
                SpellAbility sa = item.getSpellAbility();
                if (sa != null) {
                    Card source = sa.getHostCard();
                    String controller = item.getActivatingPlayer() != null ?
                        item.getActivatingPlayer().getName() : "Unknown";

                    System.out.println("  " + stackPos + ". " + source.getName() + " (" + controller + ")");

                    // Show spell description if available
                    if (sa.hasParam("Description")) {
                        System.out.println("      " + sa.getParam("Description"));
                    }

                    // Show oracle text for more context
                    String oracleText = source.getOracleText();
                    if (oracleText != null && !oracleText.isEmpty()) {
                        String formattedText = oracleText.replace("\\n", " ");
                        // Truncate if too long
                        if (formattedText.length() > 150) {
                            formattedText = formattedText.substring(0, 147) + "...";
                        }
                        System.out.println("      " + formattedText);
                    }

                    System.out.println();
                    stackPos--;
                }
            }
        }

        System.out.println("=============");
        System.out.println();
    }

    /**
     * Get a priority value for sorting cards by type.
     */
    private int getCardTypePriority(Card c) {
        if (c.isCreature()) return 1;
        if (c.isInstant()) return 2;
        if (c.isSorcery()) return 3;
        if (c.isArtifact()) return 4;
        if (c.isEnchantment()) return 5;
        if (c.isLand()) return 6;
        return 7;
    }

    /**
     * Get a simple type string for a card.
     */
    private String getCardTypeString(Card c) {
        if (c.isCreature()) return "Creature";
        if (c.isInstant()) return "Instant";
        if (c.isSorcery()) return "Sorcery";
        if (c.isArtifact()) return "Artifact";
        if (c.isEnchantment()) return "Enchantment";
        if (c.isLand()) return "Land";
        return "Other";
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        // Print any new game log entries
        TUIGuiBase.printNewLogEntries();

        // Get all possible attackers
        CardCollection possibleAttackers = CombatUtil.getPossibleAttackers(attacker);

        if (possibleAttackers.isEmpty()) {
            System.out.println(">> No creatures available to attack. Skipping combat...\n");
            return;
        }

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("=== DECLARE ATTACKERS ===");
        System.out.println("=".repeat(60));

        // Get possible defenders
        FCollectionView<GameEntity> possibleDefenders = CombatUtil.getAllPossibleDefenders(attacker);
        List<GameEntity> defendersList = new ArrayList<>();
        for (GameEntity defender : possibleDefenders) {
            defendersList.add(defender);
        }

        List<Card> selectedAttackers = new ArrayList<>();

        if (numericChoices) {
            // Numeric-only mode: use structured decision tree
            while (true) {
                // Show available attackers with 0 = "No further attackers"
                System.out.println("\nDeclare attackers:");
                System.out.println("  0. No further attackers");
                for (int i = 0; i < possibleAttackers.size(); i++) {
                    Card creature = possibleAttackers.get(i);
                    String status = "";
                    if (selectedAttackers.contains(creature)) {
                        status = " [ATTACKING]";
                    }
                    System.out.println("  " + (i + 1) + ". " + creature.getName() +
                        " (" + creature.getNetPower() + "/" + creature.getNetToughness() + ")" + status);
                }

                int choice = getIntInput(0, possibleAttackers.size());

                if (choice == 0) {
                    // Done selecting attackers
                    break;
                }

                Card selectedAttacker = possibleAttackers.get(choice - 1);

                // Check if already selected
                if (selectedAttackers.contains(selectedAttacker)) {
                    System.out.println(">> " + selectedAttacker.getName() + " is already attacking.");
                    continue;
                }

                // Choose defender
                GameEntity defender = null;
                if (defendersList.size() == 1) {
                    defender = defendersList.get(0);
                } else {
                    // Multiple defenders - let user choose
                    System.out.println("\nChoose defender for " + selectedAttacker.getName() + ":");
                    for (int i = 0; i < defendersList.size(); i++) {
                        System.out.println("  " + i + ". " + getDefenderName(defendersList.get(i)));
                    }

                    int defenderChoice = getIntInput(0, defendersList.size() - 1);
                    defender = defendersList.get(defenderChoice);
                }

                // Add the attacker
                combat.addAttacker(selectedAttacker, defender);
                selectedAttackers.add(selectedAttacker);
                System.out.println(">> " + selectedAttacker.getName() + " attacks " + getDefenderName(defender));
            }
        } else {
            // Text mode: allow "done" command
            // Show available attackers
            System.out.println("\nAvailable attackers:");
            for (int i = 0; i < possibleAttackers.size(); i++) {
                Card creature = possibleAttackers.get(i);
                System.out.println("  " + i + ". " + creature.getName() +
                    " (" + creature.getNetPower() + "/" + creature.getNetToughness() + ")" +
                    (creature.isTapped() ? " (tapped)" : "") +
                    (creature.isSick() ? " (summoning sickness)" : ""));
            }

            System.out.println("\nChoose attackers one at a time (or enter 'done' when finished):");

            // Let user select attackers
            while (true) {
                // Set range for random agents
                if (reader instanceof PlayerControllerRandom.RandomChoiceReader) {
                    ((PlayerControllerRandom.RandomChoiceReader) reader).setRange(0, possibleAttackers.size() - 1);
                }

                System.out.print("Enter attacker number (or 'done'): ");
                try {
                    String line = reader.readLine();
                    if (line == null || line.trim().isEmpty() || line.trim().equalsIgnoreCase("done")) {
                        break;
                    }

                    int attackerIndex = Integer.parseInt(line.trim());
                    if (attackerIndex < 0 || attackerIndex >= possibleAttackers.size()) {
                        System.out.println("Invalid attacker number.");
                        continue;
                    }

                    Card selectedAttacker = possibleAttackers.get(attackerIndex);

                    // Check if already selected
                    if (selectedAttackers.contains(selectedAttacker)) {
                        System.out.println(selectedAttacker.getName() + " is already attacking.");
                        continue;
                    }

                    // If only one defender, attack them automatically
                    GameEntity defender = null;
                    if (defendersList.size() == 1) {
                        defender = defendersList.get(0);
                        System.out.println(">> " + selectedAttacker.getName() + " attacks " + getDefenderName(defender));
                    } else {
                        // Multiple defenders - let user choose
                        System.out.println("\nChoose defender for " + selectedAttacker.getName() + ":");
                        for (int i = 0; i < defendersList.size(); i++) {
                            System.out.println("  " + i + ". " + getDefenderName(defendersList.get(i)));
                        }

                        // Set range for random agents
                        if (reader instanceof PlayerControllerRandom.RandomChoiceReader) {
                            ((PlayerControllerRandom.RandomChoiceReader) reader).setRange(0, defendersList.size() - 1);
                        }

                        System.out.print("Enter defender number: ");
                        String defenderLine = reader.readLine();
                        if (defenderLine == null || defenderLine.trim().isEmpty()) {
                            System.out.println("Cancelled attacker selection.");
                            continue;
                        }

                        int defenderIndex = Integer.parseInt(defenderLine.trim());
                        if (defenderIndex < 0 || defenderIndex >= defendersList.size()) {
                            System.out.println("Invalid defender number.");
                            continue;
                        }
                        defender = defendersList.get(defenderIndex);
                        System.out.println(">> " + selectedAttacker.getName() + " attacks " + getDefenderName(defender));
                    }

                    // Add the attacker
                    combat.addAttacker(selectedAttacker, defender);
                    selectedAttackers.add(selectedAttacker);

                    System.out.println("  Current attackers: " + selectedAttackers.size());

                } catch (IOException e) {
                    System.err.println("Error reading input: " + e.getMessage());
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Enter a number or 'done'.");
                }
            }
        }

        if (selectedAttackers.isEmpty()) {
            System.out.println(">> No attackers declared.\n");
        } else {
            System.out.println(">> Declared " + selectedAttackers.size() + " attacker(s).\n");
        }
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        // Print any new game log entries
        TUIGuiBase.printNewLogEntries();

        // Get all attackers
        CardCollection attackers = combat.getAttackers();

        if (attackers.isEmpty()) {
            System.out.println(">> No attackers to block.\n");
            return;
        }

        // Get possible blockers
        CardCollection possibleBlockers = defender.getCreaturesInPlay();
        if (possibleBlockers.isEmpty()) {
            System.out.println(">> No creatures available to block.\n");
            return;
        }

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("=== DECLARE BLOCKERS ===");
        System.out.println("=".repeat(60));

        if (numericChoices) {
            // Numeric-only mode: ask for each attacker individually
            for (int attackerIdx = 0; attackerIdx < attackers.size(); attackerIdx++) {
                Card attacker = attackers.get(attackerIdx);
                GameEntity defendingEntity = combat.getDefenderByAttacker(attacker);

                System.out.println("\nAttacker " + attackerIdx + ": " + attacker.getName() +
                    " (" + attacker.getNetPower() + "/" + attacker.getNetToughness() + ")" +
                    " attacking " + getDefenderName(defendingEntity));

                // Show available blockers with 0 = "No further blockers"
                while (true) {
                    System.out.println("Who should block this attacker?");
                    System.out.println("  0. No further blockers");
                    for (int i = 0; i < possibleBlockers.size(); i++) {
                        Card creature = possibleBlockers.get(i);
                        String status = "";
                        if (combat.isBlocking(creature)) {
                            status = " [BLOCKING]";
                        }
                        System.out.println("  " + (i + 1) + ". " + creature.getName() +
                            " (" + creature.getNetPower() + "/" + creature.getNetToughness() + ")" + status);
                    }

                    int choice = getIntInput(0, possibleBlockers.size());

                    if (choice == 0) {
                        // No more blockers for this attacker
                        break;
                    }

                    Card blocker = possibleBlockers.get(choice - 1);

                    // Check if blocker can block this attacker
                    if (!CombatUtil.canBlock(attacker, blocker, combat)) {
                        System.out.println(">> " + blocker.getName() + " cannot block " + attacker.getName() + ".");
                        continue;
                    }

                    // Add the blocker
                    combat.addBlocker(attacker, blocker);
                    System.out.println(">> " + blocker.getName() + " blocks " + attacker.getName());
                }
            }
        } else {
            // Text mode: allow "X blocks Y" format
            // Show attackers
            System.out.println("\nAttacking creatures:");
            for (int i = 0; i < attackers.size(); i++) {
                Card attacker = attackers.get(i);
                GameEntity defendingEntity = combat.getDefenderByAttacker(attacker);
                System.out.println("  " + i + ". " + attacker.getName() +
                    " (" + attacker.getNetPower() + "/" + attacker.getNetToughness() + ")" +
                    " attacking " + getDefenderName(defendingEntity));
            }

            // Show available blockers
            System.out.println("\nAvailable blockers:");
            for (int i = 0; i < possibleBlockers.size(); i++) {
                Card creature = possibleBlockers.get(i);
                System.out.println("  " + i + ". " + creature.getName() +
                    " (" + creature.getNetPower() + "/" + creature.getNetToughness() + ")" +
                    (creature.isTapped() ? " (tapped)" : ""));
            }

            System.out.println("\nDeclare blockers (or enter 'done' when finished):");
            System.out.println("Format: <blocker_num> blocks <attacker_num>");
            System.out.println("Example: 0 blocks 1");

            // Let user assign blockers
            while (true) {
                System.out.print("Enter block assignment (or 'done'): ");
                try {
                    String line = reader.readLine();
                    if (line == null || line.trim().isEmpty() || line.trim().equalsIgnoreCase("done")) {
                        break;
                    }

                    // Parse "X blocks Y" format
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length != 3 || !parts[1].equalsIgnoreCase("blocks")) {
                        System.out.println("Invalid format. Use: <blocker_num> blocks <attacker_num>");
                        continue;
                    }

                    int blockerIndex = Integer.parseInt(parts[0]);
                    int attackerIndex = Integer.parseInt(parts[2]);

                    if (blockerIndex < 0 || blockerIndex >= possibleBlockers.size()) {
                        System.out.println("Invalid blocker number.");
                        continue;
                    }

                    if (attackerIndex < 0 || attackerIndex >= attackers.size()) {
                        System.out.println("Invalid attacker number.");
                        continue;
                    }

                    Card blocker = possibleBlockers.get(blockerIndex);
                    Card attacker = attackers.get(attackerIndex);

                    // Check if blocker can block this attacker
                    if (!CombatUtil.canBlock(attacker, blocker, combat)) {
                        System.out.println(blocker.getName() + " cannot block " + attacker.getName() + ".");
                        continue;
                    }

                    // Add the blocker
                    combat.addBlocker(attacker, blocker);
                    System.out.println(">> " + blocker.getName() + " blocks " + attacker.getName());

                } catch (IOException e) {
                    System.err.println("Error reading input: " + e.getMessage());
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Use: <blocker_num> blocks <attacker_num>");
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Invalid format. Use: <blocker_num> blocks <attacker_num>");
                }
            }
        }

        System.out.println(">> Blockers declared.\n");
    }

    /**
     * Get a human-readable name for a defender (player or planeswalker).
     */
    private String getDefenderName(GameEntity defender) {
        if (defender instanceof Player) {
            Player p = (Player) defender;
            return p.getName() + " (Life: " + p.getLife() + ")";
        }
        // For planeswalkers and other entities
        return defender.toString();
    }

    @Override
    public boolean confirmTrigger(forge.game.trigger.WrappedAbility wrapper) {
        // Mandatory triggers always fire
        if (wrapper.isMandatory()) {
            return true;
        }

        // Optional triggers - ask the user
        SpellAbility sa = wrapper.getWrappedAbility();
        Card source = sa.getHostCard();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("=== OPTIONAL TRIGGER ===");
        System.out.println("=".repeat(60));
        System.out.println("Source: " + source.getName());
        System.out.println("Ability: " + sa.getDescription());
        System.out.println();
        System.out.print("Do you want to use this trigger? (y/n): ");

        try {
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty()) {
                return false; // Default to no
            }

            String response = line.trim().toLowerCase();
            boolean result = response.startsWith("y");

            if (result) {
                System.out.println(">> Trigger accepted\n");
            } else {
                System.out.println(">> Trigger declined\n");
            }

            return result;
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
            return false; // Default to no on error
        }
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        // Print any new game log entries
        TUIGuiBase.printNewLogEntries();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("=== DISCARD TO HAND SIZE ===");
        System.out.println("=".repeat(60));

        // Get cards in hand
        List<Card> hand = new ArrayList<>();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            hand.add(c);
        }

        CardCollection toDiscard = new CardCollection();

        // Let user select cards to discard
        for (int discardNum = 1; discardNum <= numDiscard; discardNum++) {
            // Show remaining cards with combined prompt
            List<Card> remaining = new ArrayList<>();
            for (Card c : hand) {
                if (!toDiscard.contains(c)) {
                    remaining.add(c);
                }
            }

            System.out.println("Your hand (" + remaining.size() + " cards), select " + discardNum + " of " + numDiscard + " to discard:");
            for (int i = 0; i < remaining.size(); i++) {
                System.out.println("  " + i + ". " + remaining.get(i).getName());
            }

            int choice = getIntInput(0, remaining.size() - 1);
            Card chosen = remaining.get(choice);
            toDiscard.add(chosen);

            System.out.println(">> Will discard: " + chosen.getName());
            System.out.println();
        }

        System.out.println(">> Discarding " + numDiscard + " card(s)...\n");
        return toDiscard;
    }

    /**
     * Deduplicate spell abilities by card name, keeping first occurrence.
     * Used for lands, creatures, artifacts, sorceries, and instants.
     */
    private List<SpellAbility> deduplicateByCardName(List<SpellAbility> abilities) {
        List<SpellAbility> unique = new ArrayList<>();
        java.util.Set<String> seenNames = new java.util.HashSet<>();
        for (SpellAbility sa : abilities) {
            String name = sa.getHostCard().getName();
            if (!seenNames.contains(name)) {
                unique.add(sa);
                seenNames.add(name);
            }
        }
        return unique;
    }

    /**
     * Deduplicate activated abilities by card name + description.
     * Different abilities on the same card will show separately.
     */
    private List<SpellAbility> deduplicateByDescription(List<SpellAbility> abilities) {
        List<SpellAbility> unique = new ArrayList<>();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        for (SpellAbility sa : abilities) {
            String key = sa.getHostCard().getName() + ":" + sa.getDescription();
            if (!seenKeys.contains(key)) {
                unique.add(sa);
                seenKeys.add(key);
            }
        }
        return unique;
    }
}
