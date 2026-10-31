package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellApiToAi;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class ChangeZoneAiTest extends AITest {

    @Test
    public void testXTargetCountIgnoresTheAisOwnPermanents() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Island", 12, ai);
        // Distorting Wake targets any nonland permanent, so everything here is a legal target, but
        // the AI only wants the opponent's. Its own used to inflate X past the number it would
        // actually target, leaving it unable to cast the spell at all.
        addCard("Ornithopter", ai);
        addCard("Sol Ring", ai);
        addCard("Runeclaw Bear", ai);

        addCard("Colossal Dreadmaw", opponent);
        addCard("Ancient Brontodon", opponent);

        Card wake = addCardToZone("Distorting Wake", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (SpellAbility s : wake.getSpellAbilities()) {
            if (s.getApi() == ApiType.ChangeZone) {
                sa = s;
                break;
            }
        }
        AssertJUnit.assertNotNull("Distorting Wake should have a ChangeZone spell ability", sa);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should still cast an X-targeting bounce while it controls permanents",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals("X should match the permanents the AI actually wants to target",
                Integer.valueOf(2), sa.getXManaCostPaid());
        for (Card t : sa.getTargets().getTargetCards()) {
            AssertJUnit.assertFalse("AI should never target its own permanent with this",
                    t.getController().equals(ai));
        }
    }

    @Test
    public void testExhumeWithEmptyOpponentGraveyard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 2, ai);
        addCardToZone("Akroma, Angel of Wrath", ai, ZoneType.Graveyard);
        Card exhume = addCardToZone("Exhume", ai, ZoneType.Hand);
        SpellAbility sa = exhume.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should cast Exhume when only its graveyard has a creature",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
    }

    @Test
    public void testExhumeRequiresAnOwnCreature() {
        for (String ownCard : new String[] {null, "Swamp"}) {
            Game game = initAndCreateGame();
            Player ai = game.getPlayers().get(1);
            Player opponent = game.getPlayers().get(0);

            addCards("Swamp", 2, ai);
            if (ownCard != null) {
                addCardToZone(ownCard, ai, ZoneType.Graveyard);
            }
            addCardToZone("Grizzly Bears", opponent, ZoneType.Graveyard);
            Card exhume = addCardToZone("Exhume", ai, ZoneType.Hand);
            SpellAbility sa = exhume.getSpellAbilities().get(0);
            sa.setActivatingPlayer(ai);

            AssertJUnit.assertFalse("AI should not cast Exhume without an own creature",
                    ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa) == AiPlayDecision.WillPlay);
        }
    }

    @Test
    public void testNorinDelayedReturnDoesNotSendCommanderToCommandZone() {
        Game game = commanderGame("Norin the Wary");
        Player ai = game.getPlayers().get(1);
        Card norin = ai.getCommanders().get(0);
        // Exile ability whose sub-ability is the end-step delayed return (Origin$ Exile,Command).
        SpellAbility exile = triggerExecute(norin, "TrigExile");

        AssertJUnit.assertFalse("Norin's delayed battlefield return should decline the command zone",
                acceptsCommandZone(ai, norin, exile, ZoneType.Exile));
    }

    @Test
    public void testPortcullisDelayedReturnAllIsABlink() {
        Game game = commanderGame("Norin the Wary");
        Player ai = game.getPlayers().get(1);
        Card norin = ai.getCommanders().get(0);
        Card portcullis = addCard("Portcullis", ai);
        SpellAbility exile = triggerExecute(portcullis, "TrigExile");
        // The delayed trigger's execute ability itself, not the exile that created it.
        SpellAbility ret = exile.getSubAbility().getAdditionalAbility("Execute");

        AssertJUnit.assertNotNull(ret);
        AssertJUnit.assertEquals(ApiType.ChangeZoneAll, ret.getApi());
        AssertJUnit.assertFalse("Portcullis ChangeZoneAll should decline the command zone",
                acceptsCommandZone(ai, norin, ret, ZoneType.Battlefield));
    }

    @Test
    public void testDestructiveExileAndGraveyardStillGoToCommandZone() {
        Game game = commanderGame("Norin the Wary");
        Player ai = game.getPlayers().get(1);
        Card norin = ai.getCommanders().get(0);
        SpellAbility swords = spellWithApi(addCardToZone("Swords to Plowshares", ai, ZoneType.Hand), ApiType.ChangeZone);
        SpellAbility murder = spellWithApi(addCardToZone("Murder", ai, ZoneType.Hand), ApiType.Destroy);

        AssertJUnit.assertTrue("a normal exile still goes to the command zone",
                acceptsCommandZone(ai, norin, swords, ZoneType.Exile));
        AssertJUnit.assertTrue("a normal graveyard move still goes to the command zone",
                acceptsCommandZone(ai, norin, murder, ZoneType.Graveyard));
    }

    @Test
    public void testReturnToHandDeclinesCommandZone() {
        Game game = commanderGame("Acererak the Archlich");
        Player ai = game.getPlayers().get(1);
        Card acer = ai.getCommanders().get(0);
        SpellAbility bounce = triggerExecute(acer, "TrigBounce");

        AssertJUnit.assertFalse("returning to hand should decline the command zone",
                acceptsCommandZone(ai, acer, bounce, ZoneType.Hand));
    }

    @Test
    public void testDelayedNonBattlefieldReturnIsNotABlink() {
        Game game = commanderGame("The Scorpion God");
        Player ai = game.getPlayers().get(1);
        Card god = ai.getCommanders().get(0);
        // Dies, then a delayed trigger returns it to hand — not a battlefield blink.
        SpellAbility delay = triggerExecute(god, "TrigDelay");

        AssertJUnit.assertTrue("a delayed non-battlefield return is not a blink",
                acceptsCommandZone(ai, god, delay, ZoneType.Graveyard));
    }

    private Game commanderGame(String commander) {
        Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(commander, 1);
        List<RegisteredPlayer> players = Lists.newArrayList(
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p2", null)),
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p1", null)));
        GameRules rules = new GameRules(GameType.Commander);
        Game game = new Game(players, rules, new Match(rules, players, "Test"));
        for (Player p : game.getPlayers()) {
            p.initVariantsZones(p.getRegisteredPlayer());
        }
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    /** The real commander replacement, decided by {@code AiController.doTrigger}. */
    private boolean acceptsCommandZone(Player ai, Card commander, SpellAbility cause, ZoneType destination) {
        SpellAbility replacement = null;
        for (Card c : ai.getCardsIn(ZoneType.Command)) {
            for (ReplacementEffect re : c.getReplacementEffects()) {
                if (re.hasParam("CommanderMoveReplacement")) {
                    replacement = re.ensureAbility();
                    replacement.setReplacementEffect(re);
                    replacement.setActivatingPlayer(ai);
                }
            }
        }
        AssertJUnit.assertNotNull("commander replacement ability", replacement);

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.Affected, commander);
        params.put(AbilityKey.Cause, cause);
        params.put(AbilityKey.Destination, destination);
        params.put(AbilityKey.Origin, ZoneType.Battlefield);
        replacement.setReplacingObject(AbilityKey.OriginalParams, params);
        replacement.setReplacingObject(AbilityKey.Card, commander);
        if (cause != null && cause.getActivatingPlayer() == null) {
            cause.setActivatingPlayer(commander.getController());
        }
        return ((PlayerControllerAi) ai.getController()).getAi().doTrigger(replacement, false);
    }

    private SpellAbility triggerExecute(Card card, String execute) {
        for (Trigger t : card.getTriggers()) {
            if (execute.equals(t.getParam("Execute"))) {
                SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(card.getController());
                return sa;
            }
        }
        AssertJUnit.fail("missing trigger execute " + execute + " on " + card.getName());
        return null;
    }

    private SpellAbility spellWithApi(Card card, ApiType api) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == api) {
                sa.setActivatingPlayer(card.getController());
                return sa;
            }
        }
        AssertJUnit.fail("missing " + api + " on " + card.getName());
        return null;
    }

}
