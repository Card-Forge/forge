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
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

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

    // The commander tests below play real Commander and Tiny Leaders games. In Commander a commander put into
    // exile or a graveyard reaches the command zone through the state-based action (CR 903.9a); in Tiny Leaders
    // through the replacement effect. Both ask the AI.

    @Test
    public void testPortcullisKeepsCommanderInExileUntilItReturns() {
        for (GameType variant : new GameType[] {GameType.Commander, GameType.TinyLeaders}) {
            Game game = commanderGame(variant, "Isamaru, Hound of Konda");
            Player ai = game.getPlayers().get(1);
            Player opponent = game.getPlayers().get(0);
            Card portcullis = addCard("Portcullis", opponent);
            addCards("Grizzly Bears", 2, opponent);
            game.getAction().checkStateEffects(true);
            Card commander = ai.getCommanders().get(0);

            game.getAction().moveToPlay(commander, ai, null, AbilityKey.newMap());
            resolveTriggers(game);
            AssertJUnit.assertEquals(variant + ": Portcullis's return needs the commander in exile",
                    ZoneType.Exile, zoneOf(game, commander));

            game.getAction().moveToGraveyard(game.getCardState(portcullis), null, AbilityKey.newMap());
            resolveTriggers(game);
            AssertJUnit.assertEquals(variant + ": the commander comes back when Portcullis leaves",
                    ZoneType.Battlefield, zoneOf(game, commander));
        }
    }

    @Test
    public void testAirbendKeepsCommanderInExileToCastForTwo() {
        for (GameType variant : new GameType[] {GameType.Commander, GameType.TinyLeaders}) {
            Game game = commanderGame(variant, "Isamaru, Hound of Konda");
            Player ai = game.getPlayers().get(1);
            Card commander = ai.getCommanders().get(0);
            Card onBattlefield = game.getAction().moveToPlay(commander, ai, null, AbilityKey.newMap());

            castAt(game, "Airbending Lesson", game.getPlayers().get(0), onBattlefield);
            AssertJUnit.assertEquals(variant + ": an airbent commander stays in exile",
                    ZoneType.Exile, zoneOf(game, commander));
            AssertJUnit.assertFalse(variant + ": and its owner may cast it from there",
                    game.getCardState(commander).mayPlay(ai).isEmpty());
        }
    }

    @Test
    public void testNorinStaysInExileForItsEndStepReturn() {
        Game game = commanderGame(GameType.Commander, "Norin the Wary");
        Player ai = game.getPlayers().get(1);
        Card norin = game.getAction().moveToPlay(ai.getCommanders().get(0), ai, null, AbilityKey.newMap());

        SpellAbility exile = triggerExecute(norin, "TrigExile");
        AbilityUtils.resolve(exile);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Norin's end step return should keep it in exile",
                ZoneType.Exile, zoneOf(game, norin));
    }

    @Test
    public void testExileWithoutAReturnStillGoesToCommandZone() {
        for (GameType variant : new GameType[] {GameType.Commander, GameType.TinyLeaders}) {
            Game game = commanderGame(variant, "Isamaru, Hound of Konda");
            Player ai = game.getPlayers().get(1);
            Card commander = ai.getCommanders().get(0);
            Card onBattlefield = game.getAction().moveToPlay(commander, ai, null, AbilityKey.newMap());

            castAt(game, "Swords to Plowshares", game.getPlayers().get(0), onBattlefield);
            AssertJUnit.assertEquals(variant + ": a plain exile still sends the commander to the command zone",
                    ZoneType.Command, zoneOf(game, commander));
        }
    }

    private Game commanderGame(GameType variant, String commander) {
        Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(commander, 1);
        List<RegisteredPlayer> players = Lists.newArrayList(
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p2", null)),
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p1", null)));
        GameRules rules = new GameRules(variant);
        rules.addAppliedVariant(variant);
        Game game = new Game(players, rules, new Match(rules, players, "Test"));
        for (Player p : game.getPlayers()) {
            p.initVariantsZones(p.getRegisteredPlayer());
            fillLibrary(p, 5);
        }
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private void castAt(Game game, String spell, Player caster, Card target) {
        SpellAbility sa = addCardToZone(spell, caster, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
    }

    private void resolveTriggers(Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private ZoneType zoneOf(Game game, Card card) {
        return game.getCardState(card).getZone().getZoneType();
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

}
