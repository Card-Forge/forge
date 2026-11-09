package forge.ai;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Two-Headed Giant (CR 805, 810) with four AI players. Seats 0 and 1 are team A, seats 2 and 3 team B. */
public class TwoHeadedGiantTest extends AITest {

    private Game twoHeadedGiant() {
        return teamGame(true);
    }

    private Game teamGame(boolean sharedTeamTurns) {
        List<RegisteredPlayer> players = Lists.newArrayList();
        String[] names = {"a1", "a2", "b1", "b2"};
        for (int i = 0; i < names.length; i++) {
            RegisteredPlayer rp = new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi(names[i], null));
            rp.setTeamNumber(i / 2);
            if (sharedTeamTurns) {
                rp.setStartingLife(30);
            }
            players.add(rp);
        }
        GameRules rules = new GameRules(GameType.Constructed);
        if (sharedTeamTurns) {
            rules.addAppliedVariant(GameType.TwoHeadedGiant);
        }
        Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 10);
            p.updateOpponentsForView();
        }
        return game;
    }

    /** Replaces the player's attack or block declaration with a fixed one; a null declaration keeps the AI's. */
    private static void scriptCombat(Player p, BiConsumer<Player, Combat> attacks, BiConsumer<Player, Combat> blocks) {
        p.dangerouslySetController(new PlayerControllerAi(p.getGame(), p, p.getLobbyPlayer()) {
            @Override
            public void declareAttackers(Player attacker, Combat combat) {
                if (attacks == null) {
                    super.declareAttackers(attacker, combat);
                } else {
                    attacks.accept(attacker, combat);
                }
            }
            @Override
            public void declareBlockers(Player defender, Combat combat) {
                if (blocks == null) {
                    super.declareBlockers(defender, combat);
                } else {
                    blocks.accept(defender, combat);
                }
            }
        });
    }

    /** Each player of team A attacks b1 with a Grizzly Bears, played through to the declare blockers step. */
    private void bothAttackWithABear(Game game) {
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);
        Card bear1 = ready(addCard("Grizzly Bears", a1));
        Card bear2 = ready(addCard("Grizzly Bears", a2));
        scriptCombat(a1, (p, combat) -> combat.addAttacker(bear1, b1), null);
        scriptCombat(a2, (p, combat) -> combat.addAttacker(bear2, b1), null);
        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
    }

    private static Card ready(Card c) {
        c.setSickness(false);
        return c;
    }

    private static long countSubtype(Player p, String subtype) {
        return p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype(subtype)).count();
    }

    private static void poison(Player p, int n) {
        GameEntityCounterTable table = new GameEntityCounterTable();
        p.addPoisonCounters(n, p, table);
        table.replaceCounterEffect(p.getGame(), null);
    }

    /** Fails if life lost or poison given to one player does not change the teammate's total too (CR 810.9, 810.10). */
    @Test
    public void teammatesShareLifeAndPoison() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);

        a1.loseLife(4, false, false, null);
        a2.gainLife(1, null, null);
        poison(a1, 3);

        assertEquals(a1.getLife(), 27);
        assertEquals(a2.getLife(), 27);
        assertEquals(a2.getPoisonCounters(), 3);
        assertEquals(b1.getLife(), 30, "the other team's life changed");
    }

    /** Fails if a team loses at ten poison counters, or survives at fifteen (CR 810.8d). */
    @Test
    public void aTeamLosesAtFifteenPoison() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);

        poison(a1, 10);
        game.getAction().checkStateEffects(true);
        assertFalse(a1.hasLost(), "the team lost at ten poison counters");

        poison(a2, 5);
        game.getAction().checkStateEffects(true);
        assertTrue(a1.hasLost() && a2.hasLost(), "the team survived fifteen poison counters");
    }

    /** Fails if a player who loses on their own, here by drawing from an empty library, does not take the teammate out (CR 810.8a). */
    @Test
    public void aPlayerWhoLosesTakesTheTeamOut() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);

        for (Card c : Lists.newArrayList(a2.getCardsIn(ZoneType.Library))) {
            game.getAction().exile(c, null, null);
        }
        a2.drawCard();
        game.getAction().checkStateEffects(true);

        assertTrue(a1.hasLost(), "the teammate stayed in the game");
        assertTrue(game.isGameOver());
        assertTrue(b1.getOutcome().hasWon());
    }

    /** Fails if a teammate's Platinum Angel keeps the team in the game after one player concedes (CR 810.8b). */
    @Test
    public void aConcessionTakesTheTeamOut() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        addCard("Platinum Angel", a2);
        game.getAction().checkStaticAbilities();

        a1.concede();
        game.getAction().checkStateEffects(true);

        assertTrue(a2.hasLost(), "the teammate stayed in the game");
    }

    /** Fails if the second team member does not untap and draw on the team's turn, or the turn passes to a teammate (CR 805.4). */
    @Test
    public void theTeamTakesTheTurnTogether() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        Card b2Land = addCard("Forest", b2);
        b2Land.setTapped(true);
        int b2Hand = b2.getCardsIn(ZoneType.Hand).size();

        playUntilNextTurn(game);
        assertEquals(game.getPhaseHandler().getPlayerTurn(), b1);
        assertTrue(game.getPhaseHandler().isPlayerTurn(b2));
        assertFalse(game.getPhaseHandler().isPlayerTurn(a1));
        playUntilPhase(game, PhaseType.MAIN1);
        assertFalse(b2Land.isTapped(), "the second member's land did not untap");
        assertEquals(b2.getCardsIn(ZoneType.Hand).size(), b2Hand + 1, "the second member did not draw");

        playUntilNextTurn(game);
        assertEquals(game.getPhaseHandler().getPlayerTurn(), a1, "the turn did not pass to the other team");
    }

    /** Fails if a player cannot block a creature attacking their teammate (CR 805.10d). */
    @Test
    public void aPlayerCanBlockForTheirTeammate() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        Card attacker = addCard("Grizzly Bears", a1);
        attacker.setSickness(false);
        Card blocker = addCard("Grizzly Bears", b2);

        Combat combat = new Combat(a1);
        combat.addAttacker(attacker, b1);

        assertTrue(combat.isPlayerAttacked(b2));
        assertTrue(CombatUtil.canBlock(attacker, blocker, combat));
    }

    /** Fails if an "each player's upkeep" ability that does not refer to that player triggers once per team member, or one that does refer to them triggers only once (CR 805.4d). */
    @Test
    public void eachPlayersStepTriggersOncePerTeamUnlessItNamesThatPlayer() {
        Game game = twoHeadedGiant();
        Player b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        addCard("Verdant Kraken", b1);
        addCard("Howling Mine", b1);
        int b2Hand = b2.getCardsIn(ZoneType.Hand).size();

        playUntilNextTurn(game);
        playUntilPhase(game, PhaseType.MAIN1);

        assertEquals(countCardsWithName(game, "Forest Tentacle Token"), 1, "the upkeep trigger did not fire exactly once for the team");
        assertEquals(b2.getCardsIn(ZoneType.Hand).size(), b2Hand + 2, "Howling Mine did not give the second member their extra draw");
    }

    /** Fails if an intervening "if that player ..." condition reads the whole team rather than that player (CR 603.4, 805.4d). */
    @Test
    public void anIfThatPlayerConditionChecksOnlyThatPlayer() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), b1 = game.getPlayers().get(2);
        addCard("Misers' Cage", a1);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Runeclaw Bear", b1, ZoneType.Hand);
        }

        playUntilNextTurn(game);
        playUntilPhase(game, PhaseType.MAIN1);

        assertEquals(b1.getLife(), 28, "Misers' Cage did not damage only the player with five or more cards in hand");
    }

    /** Fails if a turn skipped by the team's second player is not skipped by the team (CR 805.8). */
    @Test
    public void aTurnSkippedByEitherPlayerIsSkippedByTheTeam() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), b2 = game.getPlayers().get(3);
        Card chronatog = addCard("Chronatog", b2);
        SpellAbility skip = chronatog.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        skip.setActivatingPlayer(b2);
        AbilityUtils.resolve(skip);

        int turn = game.getPhaseHandler().getTurn();
        while (!game.isGameOver() && game.getPhaseHandler().getTurn() == turn) {
            game.getPhaseHandler().mainLoopStep();
        }
        assertEquals(game.getPhaseHandler().getPlayerTurn(), a1, "team B took the turn its second player skipped");
    }

    /** Fails if a limit on how many creatures can attack is checked per player instead of against the team's one combined attack (CR 805.10b). */
    @Test
    public void attackLimitsApplyToTheTeamsCombinedAttack() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);
        addCard("Silent Arbiter", b1);
        Card bear1 = addCard("Grizzly Bears", a1), bear2 = addCard("Grizzly Bears", a2);
        bear1.setSickness(false);
        bear2.setSickness(false);
        game.getAction().checkStaticAbilities();

        Combat combat = new Combat(a1);
        combat.initConstraints();
        combat.addAttacker(bear1, b1);
        combat.addAttacker(bear2, b1);
        assertFalse(CombatUtil.validateAttackers(combat), "each teammate attacked with one creature past a limit of one");
    }

    /** Fails if one teammate's attack satisfies a requirement that each opponent attack, instead of each teammate meeting their own (CR 805.10a). */
    @Test
    public void eachTeammateMeetsTheirOwnAttackRequirement() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);
        addCard("Trove of Temptation", b1);
        Card bear1 = addCard("Grizzly Bears", a1), bear2 = addCard("Grizzly Bears", a2);
        bear1.setSickness(false);
        bear2.setSickness(false);
        game.getAction().checkStaticAbilities();

        Combat combat = new Combat(a1);
        combat.initConstraints();
        combat.addAttacker(bear2, b1);
        assertFalse(CombatUtil.validateAttackers(combat), "the first player skipped the attack Trove of Temptation requires");
        combat.addAttacker(bear1, b1);
        assertTrue(CombatUtil.validateAttackers(combat));
    }

    /** Fails if a lone blocker on a menace creature is rejected while the teammate can still add the second blocker (CR 805.10d). */
    @Test
    public void aTeammateCanCompleteAMenaceBlock() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        Card brute = addCard("Boggart Brute", a1);
        Card bear1 = addCard("Grizzly Bears", b1);
        addCard("Grizzly Bears", b2);

        Combat combat = new Combat(a1);
        combat.addAttacker(brute, b1);
        combat.addBlocker(brute, bear1);
        assertEquals(CombatUtil.validateBlocks(combat, b1), null, "the first defender's half of a joint block was rejected");
    }

    /** Fails if one effect that makes each opponent skip their untap step makes the team skip it more than once (CR 805.8). */
    @Test
    public void oneSkipEffectSkipsATeamsStepOnce() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        Card brine = addCard("Brine Elemental", a1);

        SpellAbility skip = AbilityFactory.getAbility(brine.getSVar("TrigSkipUntap"), brine);
        skip.setActivatingPlayer(a1);
        AbilityUtils.resolve(skip);

        assertEquals(b1.getCardsIn(ZoneType.Command).size() + b2.getCardsIn(ZoneType.Command).size(), 1, "the team got more than one skip");
    }

    /** Fails if a teammate's Platinum Angel does not keep the team in the game when its shared life reaches 0 (CR 810.8a). */
    @Test
    public void aTeammatesCantLoseEffectProtectsTheTeam() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        addCard("Platinum Angel", a1);
        game.getAction().checkStaticAbilities();

        a2.loseLife(30, false, false, null);
        game.getAction().checkStateEffects(true);

        assertFalse(a1.hasLost() || a2.hasLost(), "a player of the protected team lost the game");
    }

    /** Fails if a player can pay life while a teammate's Platinum Emperion stops the team's life total changing (CR 810.9h). */
    @Test
    public void aTeammatesCantLoseLifeEffectStopsPayingLife() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        addCard("Platinum Emperion", a1);
        game.getAction().checkStaticAbilities();

        assertFalse(a2.canPayLife(2, false, null), "the teammate could pay life the team can't lose");
    }

    /** Fails if a teammate can't see a hand card, or an opponent can (CR 810.5, 402.3). */
    @Test
    public void teammatesSeeEachOthersHands() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2);
        Card bear = addCardToZone("Runeclaw Bear", a1, ZoneType.Hand);

        assertTrue(bear.getView().canBeShownTo(a2.getView()), "the teammate can't see the hand card");
        assertFalse(bear.getView().canBeShownTo(b1.getView()), "an opponent can see the hand card");
    }

    /** Fails if an illegal team block is cut down instead of declared again: the second defender adds the missing menace blocker on the redo (CR 805.10d, 733). */
    @Test
    public void anIllegalTeamBlockIsDeclaredAgain() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        Card brute = ready(addCard("Boggart Brute", a1));
        Card bear1 = addCard("Grizzly Bears", b1);
        Card bear2 = addCard("Grizzly Bears", b2);
        int[] b2Declarations = {0};
        scriptCombat(a1, (p, combat) -> combat.addAttacker(brute, b1), null);
        scriptCombat(a2, (p, combat) -> { }, null);
        scriptCombat(b1, null, (p, combat) -> {
            if (!combat.isBlocking(bear1)) {
                combat.addBlocker(brute, bear1);
            }
        });
        scriptCombat(b2, null, (p, combat) -> {
            if (++b2Declarations[0] > 1) {
                combat.addBlocker(brute, bear2);
            }
        });

        playUntilPhase(game, PhaseType.COMBAT_END);

        assertEquals(b2Declarations[0], 2, "the team was not asked to declare its block again");
        assertEquals(b1.getLife(), 30, "the menace creature got through unblocked");
        assertTrue(brute.isInZone(ZoneType.Graveyard), "the redeclared double block did not happen");
    }

    /** Fails if a "whenever a player attacks" trigger fires once for the team rather than once for each attacking player (CR 805.10c). */
    @Test
    public void aPlayerAttacksTriggerFiresForEachAttackingPlayer() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        addCard("Combat Calligrapher", a1);

        bothAttackWithABear(game);

        assertEquals(countSubtype(a1, "Inkling"), 1, "the first attacking player got no Inkling");
        assertEquals(countSubtype(a2, "Inkling"), 1, "the second attacking player got no Inkling");
    }

    /** Fails if the second attacking player's "whenever you attack" trigger does not fire for their own attack (CR 805.10c). */
    @Test
    public void theSecondAttackingPlayersWheneverYouAttackFires() {
        Game game = twoHeadedGiant();
        Player a2 = game.getPlayers().get(1);
        addCard("Adeline, Resplendent Cathar", a2);

        bothAttackWithABear(game);

        assertEquals(countSubtype(a2, "Human"), 3, "Adeline did not create a Human for each opponent");
    }

    /** Fails if a team loses priority when one member acts, or the stack resolves before every player passes in succession (CR 805.5). */
    @Test
    public void aTeamKeepsPriorityAfterEitherMemberActs() {
        Game game = twoHeadedGiant();
        List<String> priority = Lists.newArrayList();
        for (Player p : game.getPlayers()) {
            addCardToZone("Island", p, ZoneType.Battlefield);
            Card note = addCardToZone("Mental Note", p, ZoneType.Hand);
            // a2 casts on an empty stack and b2 responds, each the first time they get priority
            boolean casts = p.getName().equals("a2") || p.getName().equals("b2");
            p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
                @Override
                public List<SpellAbility> chooseSpellAbilityToPlay() {
                    priority.add(p.getName());
                    if (!casts || !note.isInZone(ZoneType.Hand)) {
                        return null;
                    }
                    SpellAbility sa = note.getFirstSpellAbility();
                    sa.setActivatingPlayer(p);
                    return List.of(sa);
                }
            });
        }

        Card response = game.getPlayers().get(3).getCardsIn(ZoneType.Hand).getFirst();
        for (int i = 0; i < 30 && !game.getCardState(response).isInZone(ZoneType.Graveyard); i++) {
            game.getPhaseHandler().mainLoopStep();
        }

        assertEquals(priority, List.of("a1", "a2", "a1", "a2", "b1", "b2", "b1", "b2", "a1", "a2"));
        assertEquals(game.getStack().size(), 1, "b2's response did not resolve alone");
    }

    /** Fails if the teammates' simultaneous triggers go on the stack as two batches instead of one batch the first player orders (CR 805.7). */
    @Test
    public void aTeamOrdersItsTriggersTogether() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        addCard("Phyrexian Arena", a1);
        addCard("Phyrexian Arena", a2);
        boolean[] orderedTogether = {false};
        a1.dangerouslySetController(new PlayerControllerAi(game, a1, a1.getLobbyPlayer()) {
            @Override
            public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
                orderedTogether[0] |= activePlayerSAs.stream().anyMatch(sa -> sa.getHostCard().getController() == a2);
                super.orderAndPlaySimultaneousSa(activePlayerSAs);
            }
        });

        playUntilNextTurn(game);
        playUntilNextTurn(game);
        playUntilPhase(game, PhaseType.MAIN1);

        assertTrue(orderedTogether[0], "the first player did not order the teammate's upkeep trigger with their own");
        assertEquals(a1.getLife(), 28, "both Phyrexian Arenas did not resolve");
    }

    /** Fails if the team's first player, not the attacking creature's controller, assigns its combat damage (CR 510.1, 805.10c). */
    @Test
    public void eachAttackersControllerAssignsItsDamage() {
        Game game = twoHeadedGiant();
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1), b1 = game.getPlayers().get(2), b2 = game.getPlayers().get(3);
        Card wurm = ready(addCard("Craw Wurm", a2));
        Card bear1 = addCard("Grizzly Bears", b1), bear2 = addCard("Grizzly Bears", b2);
        List<Player> assigners = Lists.newArrayList();
        for (Player p : List.of(a1, a2)) {
            p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
                @Override
                public void declareAttackers(Player attacker, Combat combat) {
                    if (attacker == a2) {
                        combat.addAttacker(wurm, b1);
                    }
                }
                @Override
                public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
                    assigners.add(p);
                    return super.assignCombatDamage(attacker, blockers, remaining, damageDealt, defender, overrideOrder);
                }
            });
        }
        scriptCombat(b1, null, (p, combat) -> combat.addBlocker(wurm, bear1));
        scriptCombat(b2, null, (p, combat) -> combat.addBlocker(wurm, bear2));

        playUntilPhase(game, PhaseType.COMBAT_END);

        assertEquals(assigners, List.of(a2), "the wurm's damage was not assigned by its controller alone");
    }

    /** Fails if teammates in an ordinary team game share turns, life or hands, which only Two-Headed Giant allows (CR 805, 810). */
    @Test
    public void ordinaryTeamGamesKeepSeparateTurnsAndLife() {
        Game game = teamGame(false);
        Player a1 = game.getPlayers().get(0), a2 = game.getPlayers().get(1);
        Card bear = addCardToZone("Runeclaw Bear", a1, ZoneType.Hand);

        a1.loseLife(4, false, false, null);
        assertEquals(a2.getLife(), 20, "the teammate's life changed");
        assertFalse(game.getPhaseHandler().isPlayerTurn(a2), "the teammate shares the turn");
        assertFalse(bear.getView().canBeShownTo(a2.getView()), "the teammate can see the hand card");

        playUntilNextTurn(game);
        assertEquals(game.getPhaseHandler().getPlayerTurn(), a2, "the teammate did not take their own turn");
    }

}
