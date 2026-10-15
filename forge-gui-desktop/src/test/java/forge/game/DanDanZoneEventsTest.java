package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/** Zone events retain the historical source and the accepted destination independently. */
public class DanDanZoneEventsTest extends AITest {
    @Test
    public void sharedGraveyardTriggersUseDestinationButDeathTriggersKeepSourceControl() {
        Game game = newGame(true);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        RecordingController ca = record(a), cb = record(b);
        Card purgatoryA = addCard("Purgatory", a), purgatoryB = addCard("Purgatory", b);
        Card piaA = addCard("Pia's Revolution", a), piaB = addCard("Pia's Revolution", b);
        Card pactA = addCard("Grave Pact", a), pactB = addCard("Grave Pact", b);
        Card ownerWatcherA = historicalOwnerWatcher(a), ownerWatcherB = historicalOwnerWatcher(b);
        Card creature = addCard("Ornithopter", a);
        game.copyLastState();
        game.getTriggerHandler().resetActiveTriggers();
        Card departed = game.getAction().moveToGraveyard(creature, null);
        game.getAction().moveTo(b.getZone(ZoneType.Hand), departed, null, AbilityKey.newMap());
        flushTriggers(game);

        assertEquals(count(ca, purgatoryA), 1L);
        assertEquals(count(cb, purgatoryB), 1L, "both players receive a card in their shared graveyard");
        assertEquals(count(ca, piaA), 1L);
        assertEquals(count(cb, piaB), 1L);
        assertEquals(count(ca, pactA), 1L, "the source snapshot still records A's creature dying");
        assertEquals(count(cb, pactB), 0L, "joint ownership after entry does not change death control");
        assertEquals(count(ca, ownerWatcherA), 1L);
        assertEquals(count(cb, ownerWatcherB), 0L, "historical YouOwn uses the source owner, not joint destination owners");
        assertEquals(a.getDescended(), 1);
        assertEquals(b.getDescended(), 1);
    }

    @Test
    public void batchedDestinationFactsSurviveLaterMovesAndSeparateOrdinaryGraveyards() {
        for (boolean shared : List.of(true, false)) {
            Game game = newGame(shared);
            Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
            RecordingController ca = record(a), cb = record(b);
            Card watcherA = batchWatcher(a), watcherB = batchWatcher(b);
            Card first = addCard("Grizzly Bears", a), second = addCard("Dandân", b);
            game.copyLastState();
            game.getTriggerHandler().resetActiveTriggers();
            game.getStack().freezeStack(null);
            CardZoneTable table = new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard());
            Map<AbilityKey, Object> params = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(params, table);
            Card movedFirst = game.getAction().moveTo(ZoneType.Graveyard, first, null, params);
            game.getAction().moveTo(ZoneType.Graveyard, second, null, params);
            // A later acquisition must not rewrite where the first event arrived.
            game.getAction().moveTo(b.getZone(ZoneType.Hand), movedFirst, null, AbilityKey.newMap());
            table.triggerChangesZoneAll(game, null);
            table.clear();
            flushTriggers(game);

            SpellAbility aTrigger = ca.triggers.stream().filter(sa -> sa.getHostCard().equals(watcherA)).findFirst().orElseThrow();
            SpellAbility bTrigger = cb.triggers.stream().filter(sa -> sa.getHostCard().equals(watcherB)).findFirst().orElseThrow();
            assertEquals(aTrigger.getTriggeringObject(AbilityKey.Amount), shared ? 2 : 1);
            assertEquals(bTrigger.getTriggeringObject(AbilityKey.Amount), shared ? 2 : 1);
            assertEquals(a.getDescended(), shared ? 2 : 1);
            assertEquals(b.getDescended(), shared ? 2 : 1);

            game.copyLastState();
            CardZoneTable leavingGraveyard = new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard());
            Map<AbilityKey, Object> leavingParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(leavingParams, leavingGraveyard);
            game.getAction().moveTo(b.getZone(ZoneType.Hand), game.getCardState(second), null, leavingParams);
            assertEquals(leavingGraveyard.filterCards(List.of(ZoneType.Graveyard), List.of(ZoneType.Hand),
                    "Creature.YouOwn", watcherA, watcherA.getTriggers().get(0)).size(), shared ? 1 : 0,
                    "a graveyard departure uses source ownership before B acquires the card in hand");
        }
    }

    @Test
    public void opponentsMillControlsNarcomoebaTriggerAndOptionalChoice() {
        Game game = newGame(true);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        RecordingController ca = record(a), cb = record(b);
        Card narcomoeba = addCardToZone("Narcomoeba", a, ZoneType.Library);
        millOpponent(game, a, b);
        flushTriggers(game);

        assertEquals(ca.triggers.size(), 0, "the spell's caster does not control the milled card's trigger");
        assertEquals(cb.triggers.size(), 1);
        WrappedAbility trigger = (WrappedAbility) cb.triggers.get(0);
        assertEquals(trigger.getActivatingPlayer(), b);
        assertEquals(trigger.getDecider(), b);
        trigger.resolve();
        assertEquals(cb.optionalChoices, 1);
        assertEquals(ca.optionalChoices, 0);
        Card entered = game.getCardState(narcomoeba);
        assertTrue(b.getCardsIn(ZoneType.Battlefield).contains(entered));
        assertEquals(entered.getOwner(), b);
        assertEquals(entered.getController(), b);
    }

    @Test
    public void opponentsMillChoosesAndExecutesColossusReplacement() {
        Game game = newGame(true);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        RecordingController ca = record(a), cb = record(b);
        addCard("Rest in Peace", a);
        Card colossus = addCardToZone("Darksteel Colossus", a, ZoneType.Library);
        millOpponent(game, a, b);

        assertEquals(ca.replacementChoices, 0);
        assertEquals(cb.replacementChoices, 1, "the player milling chooses between Colossus and Rest in Peace");
        assertEquals(cb.replacementOptionCount, 2);
        assertEquals(cb.replacementControllers, List.of(b), "Colossus's replacement executes for the milling player");
        assertTrue(game.getCardsIn(ZoneType.Library).contains(game.getCardState(colossus)));
        assertTrue(game.getCardsIn(ZoneType.Graveyard).isEmpty());
    }

    @Test
    public void stolenPermanentControllerChoosesDeathReplacementBeforeDestroyingPlayer() {
        for (boolean shared : List.of(false, true)) {
            Game game = newGame(shared);
            Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
            RecordingController ca = record(a), cb = record(b);
            addCard("Rest in Peace", a);
            Card progenitus = addCard("Progenitus", a);
            progenitus.setController(b, game.getNextTimestamp());
            game.getAction().controllerChangeZoneCorrection(progenitus);
            SpellAbility wrath = addCardToZone("Wrath of God", a, ZoneType.Hand).getFirstSpellAbility();
            wrath.setActivatingPlayer(a);
            game.copyLastState();

            AbilityUtils.resolve(wrath);

            assertEquals(ca.replacementChoices, 0, "the owner/caster does not choose for B's dying permanent");
            assertEquals(cb.replacementChoices, 1);
            assertEquals(cb.replacementOptionCount, 2);
            assertEquals(cb.replacementControllers, List.of(b));
            assertTrue(game.getCardsIn(ZoneType.Library).contains(game.getCardState(progenitus)));
        }
    }

    private Card batchWatcher(Player player) {
        Card watcher = addCard("Island", player);
        watcher.addTrigger(TriggerHandler.parseTrigger("Mode$ ChangesZoneAll | Origin$ Battlefield | Destination$ Graveyard"
                + " | ValidCards$ Creature | ValidDestinationPlayer$ You | TriggerZones$ Battlefield", watcher, true));
        return watcher;
    }

    private Card historicalOwnerWatcher(Player player) {
        Card watcher = addCard("Island", player);
        watcher.addTrigger(TriggerHandler.parseTrigger("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                + " | ValidCard$ Creature.YouOwn | TriggerZones$ Battlefield", watcher, true));
        return watcher;
    }

    private void millOpponent(Game game, Player caster, Player target) {
        SpellAbility ability = addCardToZone("Tome Scour", caster, ZoneType.Hand).getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        ability.getTargets().add(target);
        game.copyLastState();
        game.getTriggerHandler().resetActiveTriggers();
        game.getStack().freezeStack(ability);
        AbilityUtils.resolve(ability);
        game.getTriggerHandler().collectTriggerForWaiting();
    }

    private void flushTriggers(Game game) {
        game.getTriggerHandler().collectTriggerForWaiting();
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
    }

    private long count(RecordingController controller, Card source) {
        return controller.triggers.stream().filter(sa -> sa.getHostCard().equals(source)).count();
    }

    private RecordingController record(Player player) {
        RecordingController controller = new RecordingController(player);
        player.dangerouslySetController(controller);
        return controller;
    }

    private static final class RecordingController extends PlayerControllerAi {
        private final List<SpellAbility> triggers = new ArrayList<>();
        private final List<Player> replacementControllers = new ArrayList<>();
        private int optionalChoices;
        private int replacementChoices;
        private int replacementOptionCount;

        private RecordingController(Player player) {
            super(player.getGame(), player, new LobbyPlayerAi(player.getName(), null));
        }

        @Override
        public void orderAndPlaySimultaneousSa(List<SpellAbility> abilities) {
            triggers.addAll(abilities);
        }

        @Override
        public boolean confirmTrigger(WrappedAbility wrapper) {
            optionalChoices++;
            return true;
        }

        @Override
        public ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> effects) {
            replacementChoices++;
            replacementOptionCount = effects.size();
            return effects.stream().filter(re -> re.getHostCard().getName().equals("Darksteel Colossus")
                            || re.getHostCard().getName().equals("Progenitus"))
                    .findFirst().orElse(effects.get(0));
        }

        @Override
        public void playSpellAbilityNoStack(SpellAbility ability, boolean canSetupTargets) {
            if (ability.isReplacementAbility()) {
                replacementControllers.add(ability.getActivatingPlayer());
            }
            super.playSpellAbilityNoStack(ability, canSetupTargets);
        }
    }

    private Game newGame(boolean shared) {
        List<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("events A", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("events B", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (shared) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Game game = new Match(rules, players, "zone event test").createGame();
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        return game;
    }
}
