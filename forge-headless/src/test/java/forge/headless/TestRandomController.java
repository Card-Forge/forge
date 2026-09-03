package forge.headless;

import forge.LobbyPlayer;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Test controller that makes random valid choices.
 * Used for testing that the game handles various paths correctly.
 */
public class TestRandomController extends PlayerControllerAi {

    private final Random random;
    private int choicesMade = 0;
    private int totalOptions = 0;

    public TestRandomController(Game game, Player p, LobbyPlayer lp) {
        this(game, p, lp, new Random());
    }

    public TestRandomController(Game game, Player p, LobbyPlayer lp, Random random) {
        super(game, p, lp);
        this.random = random;
    }

    @Override
    public boolean isAI() {
        return false; // Act like a human player for testing
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        PhaseHandler ph = getGame().getPhaseHandler();
        boolean isMainPhase = ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2);

        // Get playable actions from hand
        List<SpellAbility> landAbilities = getPlayableLands();
        List<SpellAbility> spellAbilities = new ArrayList<>();

        // In any main phase, check for castable sorcery-speed spells
        if (isMainPhase) {
            spellAbilities.addAll(getCastableCreatures());
            spellAbilities.addAll(getCastableSorceries());
        }

        // Instants can be cast at any time
        spellAbilities.addAll(getCastableInstants());

        // Combine all options
        List<SpellAbility> allOptions = new ArrayList<>();
        allOptions.addAll(landAbilities);
        allOptions.addAll(spellAbilities);

        // Count this choice
        choicesMade++;
        totalOptions += (allOptions.size() + 1); // +1 for pass option

        // If no options, pass
        if (allOptions.isEmpty()) {
            return null;
        }

        // Randomly choose: 30% chance to pass, 70% chance to do something
        if (random.nextDouble() < 0.3) {
            return null; // Pass
        }

        // Choose a random action
        int choice = random.nextInt(allOptions.size());
        return Collections.singletonList(allOptions.get(choice));
    }

    private List<SpellAbility> getPlayableLands() {
        List<SpellAbility> lands = new ArrayList<>();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                    // Ensure activator is set before checking
                    if (sa.getActivatingPlayer() == null) {
                        sa.setActivatingPlayer(player);
                    }
                    if (sa.isLandAbility() && sa.canPlay()) {
                        lands.add(sa);
                        break;
                    }
                }
            }
        }
        return lands;
    }

    private List<SpellAbility> getCastableCreatures() {
        List<SpellAbility> spells = new ArrayList<>();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isCreature() && !c.isLand()) {
                for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                    // Ensure activator is set before checking
                    if (sa.getActivatingPlayer() == null) {
                        sa.setActivatingPlayer(player);
                    }
                    // Skip spells that use targeting (not supported by random controller)
                    if (sa.usesTargeting()) {
                        continue;
                    }
                    // Check if can play AND can actually pay the mana cost
                    if (sa.isSpell() && sa.canPlay() && ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        spells.add(sa);
                        break;
                    }
                }
            }
        }
        return spells;
    }

    private List<SpellAbility> getCastableSorceries() {
        List<SpellAbility> spells = new ArrayList<>();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isSorcery()) {
                for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                    // Ensure activator is set before checking
                    if (sa.getActivatingPlayer() == null) {
                        sa.setActivatingPlayer(player);
                    }
                    // Skip spells that use targeting (not supported by random controller)
                    if (sa.usesTargeting()) {
                        continue;
                    }
                    // Check if can play AND can actually pay the mana cost
                    if (sa.isSpell() && sa.canPlay() && ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        spells.add(sa);
                        break;
                    }
                }
            }
        }
        return spells;
    }

    private List<SpellAbility> getCastableInstants() {
        List<SpellAbility> spells = new ArrayList<>();
        for (Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isInstant()) {
                for (SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
                    // Ensure activator is set before checking
                    if (sa.getActivatingPlayer() == null) {
                        sa.setActivatingPlayer(player);
                    }
                    // Skip spells that use targeting (not supported by random controller)
                    if (sa.usesTargeting()) {
                        continue;
                    }
                    // Check if can play AND can actually pay the mana cost
                    if (sa.isSpell() && sa.canPlay() && ComputerUtilMana.canPayManaCost(sa, player, 0, false)) {
                        spells.add(sa);
                        break;
                    }
                }
            }
        }
        return spells;
    }

    public int getChoicesMade() {
        return choicesMade;
    }

    public int getTotalOptions() {
        return totalOptions;
    }
}
