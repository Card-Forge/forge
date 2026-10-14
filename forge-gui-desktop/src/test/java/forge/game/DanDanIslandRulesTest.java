package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DanDanIslandRulesTest extends AITest {

    @Test
    public void dandanCanAttackWhenDefenderOnlyControlsMysticSanctuaryTappedOrUntapped() {
        Game game = newGame(true);
        Player attacker = game.getPlayers().get(0);
        Player defender = game.getPlayers().get(1);
        addCard("Island", attacker);
        Card dandan = addCard("Dandân", attacker);
        dandan.setSickness(false);
        game.getAction().checkStateEffects(true);

        assertFalse(CombatUtil.canAttack(dandan, defender), "the defender controls no Island");

        Card sanctuaryInHand = addCardToZone("Mystic Sanctuary", defender, ZoneType.Hand);
        game.copyLastState();
        Card sanctuary = game.getAction().moveToPlay(sanctuaryInHand, defender, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);

        assertTrue(sanctuary.isTapped(), "with no other Islands, Sanctuary enters tapped");
        assertEquals(defender.getCardsIn(ZoneType.Battlefield).size(), 1,
                "Mystic Sanctuary is the defender's only permanent");
        assertTrue(CombatUtil.canAttack(dandan, defender),
                "the defender's only Island counts even while it is tapped");

        assertTrue(sanctuary.untap(), "untap the same only Island without changing control or type");
        assertTrue(CombatUtil.canAttack(dandan, defender),
                "the defender's only Island still counts while untapped");
        assertTrue(sanctuary.tap(false, null, defender), "tap the same only Island again");
        assertTrue(CombatUtil.canAttack(dandan, defender),
                "tapping the only Island does not change attack legality");
    }

    @Test
    public void dandanAttackChecksBasicIslandControlInOrdinaryConstructedGames() {
        Game game = newGame(false);
        Player attacker = game.getPlayers().get(0);
        Player defender = game.getPlayers().get(1);
        addCard("Island", attacker);
        Card dandan = addCard("Dandân", attacker);
        dandan.setSickness(false);
        game.getAction().checkStateEffects(true);

        assertFalse(CombatUtil.canAttack(dandan, defender), "the defender has no Island");

        Card defenderIsland = addCard("Island", defender);
        game.getAction().checkStateEffects(true);
        dandan.setSickness(true);
        assertFalse(CombatUtil.canAttack(dandan, defender),
                "summoning sickness prevents attacking even when the defender controls an Island");
        dandan.setSickness(false);
        assertTrue(CombatUtil.canAttack(dandan, defender),
                "a basic Island controlled by the defender permits the attack after sickness is cleared");

        game.copyLastState();
        game.getAction().moveToGraveyard(defenderIsland, null);
        game.getAction().checkStateEffects(true);
        assertFalse(CombatUtil.canAttack(dandan, defender),
                "the attack is prohibited again after the defender loses its only Island");
    }

    @Test
    public void zeroIslandStateTriggerQueuesOnEntryAndWhenLastIslandLeaves() {
        Game ordinary = newGame(false);
        Player ordinaryController = ordinary.getPlayers().get(0);
        Card ordinaryDandan = addCardToZone("Dandân", ordinaryController, ZoneType.Hand);
        ordinary.copyLastState();
        ordinaryDandan = ordinary.getAction().moveToPlay(ordinaryDandan, ordinaryController, null,
                AbilityKey.newMap());
        ordinary.getAction().checkStateEffects(true);
        assertSacrificeQueuedAndResolved(ordinary, ordinaryDandan, ordinaryController);

        Game entering = newGame(true);
        Player enterController = entering.getPlayers().get(0);
        Card enteringDandan = addCardToZone("Dandân", enterController, ZoneType.Hand);
        entering.copyLastState();
        enteringDandan = entering.getAction().moveToPlay(enteringDandan, enterController, null, AbilityKey.newMap());
        entering.getAction().checkStateEffects(true);

        assertSacrificeQueuedAndResolved(entering, enteringDandan, enterController);

        Game losing = newGame(true);
        Player controller = losing.getPlayers().get(0);
        Card lastIsland = addCard("Island", controller);
        Card dandan = addCard("Dandân", controller);
        losing.getAction().checkStateEffects(true);
        assertFalse(losing.getStack().hasSimultaneousStackEntries(),
                "Dandân stays in play while its controller has an Island");

        losing.copyLastState();
        losing.getAction().moveToGraveyard(lastIsland, null);
        losing.getAction().checkStateEffects(true);

        assertSacrificeQueuedAndResolved(losing, dandan, controller);
    }

    @Test
    public void zeroIslandTriggerUsesCurrentControllerAfterControlChanges() {
        Game game = newGame(true);
        Player owner = game.getPlayers().get(0);
        Player newController = game.getPlayers().get(1);
        addCard("Island", owner);
        Card dandan = addCard("Dandân", owner);
        game.getAction().checkStateEffects(true);
        assertFalse(game.getStack().hasSimultaneousStackEntries(), "the owner initially controls an Island");

        Card controlMagic = addCard("Control Magic", newController);
        game.copyLastState();
        controlMagic.attachToEntity(dandan, null);
        game.getAction().checkStateEffects(true);

        Card currentDandan = currentCard(game, dandan.getId());
        assertSame(currentDandan.getOwner(), owner);
        assertSame(currentDandan.getController(), newController,
                "the real Control Magic static ability changes who controls Dandân");
        assertSacrificeQueuedAndResolved(game, currentDandan, newController);
        assertTrue(owner.getCardsIn(ZoneType.Graveyard).stream().anyMatch(card -> card.getId() == dandan.getId()),
                "the sacrificed permanent is put into its owner's graveyard");
    }

    @Test
    public void actualMagicalHackTextChangeMakesDandanSeeNoIslands() {
        Game game = newGame(true);
        Player controller = game.getPlayers().get(0);
        addCard("Island", controller);
        Card dandan = addCard("Dandân", controller);
        game.getAction().checkStateEffects(true);
        assertFalse(game.getStack().hasSimultaneousStackEntries(),
                "the printed Always condition is false while the controller has an Island");

        chooseIslandThenForest(controller);
        Card hack = addCardToZone("Magical Hack", controller, ZoneType.Hand);
        SpellAbility hackSpell = hack.getSpells().get(0);
        hackSpell.setActivatingPlayer(controller);
        assertTrue(hackSpell.canTarget(dandan), "the actual Magical Hack spell can target Dandân");
        hackSpell.getTargets().add(dandan);
        resolveStacked(game, hackSpell);

        assertTrue(controller.getCardsIn(ZoneType.Battlefield).contains(currentCard(game, dandan.getId())),
                "Dandân remains until its state trigger resolves");
        assertSacrificeQueuedAndResolved(game, currentCard(game, dandan.getId()), controller);
    }

    private Game newGame(boolean danDanVariant) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("island rules first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("island rules second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDanVariant) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan Island rules");
        Game game = match.createGame();
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        return game;
    }

    private void assertSacrificeQueuedAndResolved(Game game, Card dandan, Player expectedController) {
        int dandanId = dandan.getId();
        assertTrue(game.getStack().hasSimultaneousStackEntries(),
                "the zero-Island Always state trigger is queued separately from its resolution");
        assertTrue(game.getPlayers().get(0).getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(card -> card.getId() == dandanId)
                        || game.getPlayers().get(1).getCardsIn(ZoneType.Battlefield).stream()
                        .anyMatch(card -> card.getId() == dandanId),
                "the queued trigger has not sacrificed Dandân yet");

        assertTrue(game.getStack().addAllTriggeredAbilitiesToStack(),
                "the queued state trigger is placed on the real stack before resolution");
        SpellAbilityStackInstance trigger = findStateTrigger(game, dandanId);
        assertNotNull(trigger, "the real Dandân Always trigger has a stack instance");
        assertSame(trigger.getSpellAbility().getActivatingPlayer(), expectedController,
                "the state trigger is controlled by Dandân's current controller");
        assertEquals(trigger.getSpellAbility().getTrigger().getMode(), TriggerType.Always);

        game.getStack().resolveStack();
        assertTrue(game.getCardsIn(ZoneType.Graveyard).stream()
                        .anyMatch(card -> card.getId() == dandanId),
                "Dandân reaches the graveyard only after the state trigger resolves");
    }

    private SpellAbilityStackInstance findStateTrigger(Game game, int hostId) {
        for (SpellAbilityStackInstance instance : game.getStack()) {
            if (instance.isTrigger() && instance.getSourceCard().getId() == hostId
                    && instance.getSpellAbility().getTrigger().getMode() == TriggerType.Always) {
                return instance;
            }
        }
        return null;
    }

    private Card currentCard(Game game, int cardId) {
        Card current = game.findById(cardId);
        assertNotNull(current, "the physical card remains registered after its zone transition");
        return current;
    }

    private void resolveStacked(Game game, SpellAbility ability) {
        game.copyLastState();
        game.getStack().addAndUnfreeze(ability);
        SpellAbility registered = game.getStack().getSpellMatchingHost(ability.getHostCard());
        assertNotNull(registered, "the real card ability is registered on the game stack");
        assertNotNull(game.getStack().getInstanceMatchingSpellAbilityID(registered),
                "the real ability has a current stack instance before resolution");
        game.getStack().resolveStack();
    }

    private void chooseIslandThenForest(Player player) {
        PlayerControllerAi controller = new PlayerControllerAi(player.getGame(), player,
                new LobbyPlayerAi("scripted type choices", null)) {
            private int choiceIndex;

            @Override
            public String chooseSomeType(String kindOfType, SpellAbility ability,
                    Collection<String> validTypes, boolean isOptional) {
                String choice = choiceIndex++ == 0 ? "Island" : "Forest";
                assertTrue(validTypes.contains(choice), "the actual Magical Hack choice offers " + choice);
                return choice;
            }
        };
        player.dangerouslySetController(controller);
    }
}
