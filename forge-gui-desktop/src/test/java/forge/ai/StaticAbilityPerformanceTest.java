package forge.ai;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

/**
 * Performance benchmarks for measuring where time is spent during AI turns
 * with large board states. Run manually (enabled = false).
 */
public class StaticAbilityPerformanceTest extends AITest {

    private Game setupGame() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }
        return game;
    }

    private long timePhase(Game game, PhaseType targetPhase) {
        long start = System.nanoTime();
        playUntilPhase(game, targetPhase);
        return System.nanoTime() - start;
    }

    /**
     * Profile WHERE time is spent during an AI turn with realistic board states.
     * Measures Main1, Combat, and Main2 phases separately.
     */
    @Test
    public void profileAITurnPhases() {
        System.out.println();
        System.out.println("=== AI Turn Phase Profiling ===");
        System.out.println();

        // --- Scenario A: Realistic Shandalar mid-game (tokens + lords) ---
        {
            System.out.println("Scenario A: Realistic mid-game (lords + tokens + removal)");
            System.out.println("  AI: 2 lords, 15 tokens, 5 lands, Lightning Bolt in hand");
            System.out.println("  Opp: 10 creatures, 5 lands");
            Game game = setupGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);

            // AI board: lords + vanilla creatures + mana + spell
            addCard("Glorious Anthem", ai);
            addCard("Crusade", ai);
            for (int i = 0; i < 15; i++) addCard("Savannah Lions", ai);
            for (int i = 0; i < 5; i++) addCard("Plains", ai);
            addCard("Mountain", ai);
            addCardToZone("Lightning Bolt", ai, ZoneType.Hand);

            // Opponent board: mixed creatures
            for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", opp);
            for (int i = 0; i < 3; i++) addCard("Serra Angel", opp);
            for (int i = 0; i < 2; i++) addCard("Llanowar Elves", opp);
            for (int i = 0; i < 5; i++) addCard("Forest", opp);

            game.getAction().checkStaticAbilities();
            game.getAction().checkStateEffects(true);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

            long main1 = timePhase(game, PhaseType.COMBAT_BEGIN);
            long combat = timePhase(game, PhaseType.MAIN2);
            long main2 = timePhase(game, PhaseType.END_OF_TURN);

            System.out.printf("  Main1: %6.1fms  Combat: %6.1fms  Main2: %6.1fms  Total: %6.1fms%n",
                    main1/1e6, combat/1e6, main2/1e6, (main1+combat+main2)/1e6);
            System.out.println();
        }

        // --- Scenario B: Token swarm (30 creatures per side) ---
        {
            System.out.println("Scenario B: Token swarm (30 AI + 20 opp creatures)");
            System.out.println("  AI: 2 lords, 30 creatures, spells in hand");
            System.out.println("  Opp: 20 creatures");
            Game game = setupGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);

            addCard("Glorious Anthem", ai);
            addCard("Honor of the Pure", ai);
            for (int i = 0; i < 30; i++) addCard("Savannah Lions", ai);
            for (int i = 0; i < 6; i++) addCard("Plains", ai);
            addCard("Mountain", ai);
            addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
            addCardToZone("Shock", ai, ZoneType.Hand);

            for (int i = 0; i < 10; i++) addCard("Runeclaw Bear", opp);
            for (int i = 0; i < 5; i++) addCard("Serra Angel", opp);
            for (int i = 0; i < 5; i++) addCard("Llanowar Elves", opp);
            for (int i = 0; i < 5; i++) addCard("Forest", opp);

            game.getAction().checkStaticAbilities();
            game.getAction().checkStateEffects(true);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

            long main1 = timePhase(game, PhaseType.COMBAT_BEGIN);
            long combat = timePhase(game, PhaseType.MAIN2);
            long main2 = timePhase(game, PhaseType.END_OF_TURN);

            System.out.printf("  Main1: %6.1fms  Combat: %6.1fms  Main2: %6.1fms  Total: %6.1fms%n",
                    main1/1e6, combat/1e6, main2/1e6, (main1+combat+main2)/1e6);
            System.out.println();
        }

        // --- Scenario C: Massive board with sub-phase breakdown ---
        {
            System.out.println("Scenario C: Massive board (50 AI + 30 opp creatures) - sub-phase breakdown");
            Game game = setupGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);

            addCard("Glorious Anthem", ai);
            addCard("Crusade", ai);
            addCard("Honor of the Pure", ai);
            for (int i = 0; i < 50; i++) addCard("Savannah Lions", ai);
            for (int i = 0; i < 6; i++) addCard("Plains", ai);
            addCard("Mountain", ai);
            addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
            addCardToZone("Shock", ai, ZoneType.Hand);

            for (int i = 0; i < 15; i++) addCard("Runeclaw Bear", opp);
            for (int i = 0; i < 10; i++) addCard("Serra Angel", opp);
            for (int i = 0; i < 5; i++) addCard("Llanowar Elves", opp);
            for (int i = 0; i < 6; i++) addCard("Forest", opp);

            game.getAction().checkStaticAbilities();
            game.getAction().checkStateEffects(true);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

            long main1 = timePhase(game, PhaseType.COMBAT_BEGIN);
            long beginCombat = timePhase(game, PhaseType.COMBAT_DECLARE_ATTACKERS);
            long declareAttackers = timePhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
            long declareBlockers = timePhase(game, PhaseType.COMBAT_END);
            long combatEnd = timePhase(game, PhaseType.MAIN2);
            long main2 = timePhase(game, PhaseType.END_OF_TURN);

            System.out.printf("  Main1:           %8.1fms%n", main1/1e6);
            System.out.printf("  Begin Combat:    %8.1fms%n", beginCombat/1e6);
            System.out.printf("  Declare Attack:  %8.1fms%n", declareAttackers/1e6);
            System.out.printf("  Declare Block:   %8.1fms%n", declareBlockers/1e6);
            System.out.printf("  Combat End:      %8.1fms%n", combatEnd/1e6);
            System.out.printf("  Main2:           %8.1fms%n", main2/1e6);
            System.out.printf("  TOTAL:           %8.1fms%n", (main1+beginCombat+declareAttackers+declareBlockers+combatEnd+main2)/1e6);
            System.out.println();
        }

        // --- Scenario D: Counter-heavy (Grumgully + doublers + creatures) ---
        {
            System.out.println("Scenario D: Counter-heavy board (counter sources + 30 creatures)");
            System.out.println("  AI: Grumgully, Hardened Scales, 30 creatures with +1/+1 counters");
            System.out.println("  Opp: 15 creatures");
            Game game = setupGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);

            addCard("Grumgully, the Generous", ai);
            addCard("Hardened Scales", ai);
            addCard("Glorious Anthem", ai);
            for (int i = 0; i < 30; i++) {
                addCard("Savannah Lions", ai);
            }
            for (int i = 0; i < 6; i++) addCard("Plains", ai);
            addCard("Mountain", ai);
            addCardToZone("Lightning Bolt", ai, ZoneType.Hand);

            for (int i = 0; i < 10; i++) addCard("Runeclaw Bear", opp);
            for (int i = 0; i < 5; i++) addCard("Serra Angel", opp);
            for (int i = 0; i < 5; i++) addCard("Forest", opp);

            game.getAction().checkStaticAbilities();
            game.getAction().checkStateEffects(true);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

            long main1 = timePhase(game, PhaseType.COMBAT_BEGIN);
            long combat = timePhase(game, PhaseType.MAIN2);
            long main2 = timePhase(game, PhaseType.END_OF_TURN);

            System.out.printf("  Main1: %6.1fms  Combat: %6.1fms  Main2: %6.1fms  Total: %6.1fms%n",
                    main1/1e6, combat/1e6, main2/1e6, (main1+combat+main2)/1e6);
            System.out.println();
        }

        // --- Scenario E: Mixed abilities (not just vanilla tokens) ---
        {
            System.out.println("Scenario E: Mixed abilities (flyers, first strikers, vigilance)");
            System.out.println("  AI: 20 mixed creatures, anthem, spells");
            System.out.println("  Opp: 15 mixed creatures");
            Game game = setupGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);

            addCard("Glorious Anthem", ai);
            for (int i = 0; i < 8; i++) addCard("Savannah Lions", ai);     // 2/1
            for (int i = 0; i < 6; i++) addCard("Serra Angel", ai);        // 4/4 flying vigilance
            for (int i = 0; i < 4; i++) addCard("White Knight", ai);       // 2/2 first strike prot black
            for (int i = 0; i < 2; i++) addCard("Soltari Priest", ai);     // 2/1 shadow prot red
            for (int i = 0; i < 6; i++) addCard("Plains", ai);
            addCard("Mountain", ai);
            addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
            addCardToZone("Wrath of God", ai, ZoneType.Hand);

            for (int i = 0; i < 5; i++) addCard("Runeclaw Bear", opp);
            for (int i = 0; i < 5; i++) addCard("Serra Angel", opp);
            for (int i = 0; i < 3; i++) addCard("Black Knight", opp);
            for (int i = 0; i < 2; i++) addCard("Hypnotic Specter", opp);
            for (int i = 0; i < 5; i++) addCard("Swamp", opp);

            game.getAction().checkStaticAbilities();
            game.getAction().checkStateEffects(true);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

            long main1 = timePhase(game, PhaseType.COMBAT_BEGIN);
            long combat = timePhase(game, PhaseType.MAIN2);
            long main2 = timePhase(game, PhaseType.END_OF_TURN);

            System.out.printf("  Main1: %6.1fms  Combat: %6.1fms  Main2: %6.1fms  Total: %6.1fms%n",
                    main1/1e6, combat/1e6, main2/1e6, (main1+combat+main2)/1e6);
            System.out.println();
        }

        System.out.println("=== Phase profiling complete ===");
        System.out.println();
    }
}
