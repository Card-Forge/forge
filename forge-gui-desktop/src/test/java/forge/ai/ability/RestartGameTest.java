package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.StaticData;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.card.CardType;
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
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/** Karn Liberated's ultimate restarts a Commander game. Each commander starts the new game in the command zone, unless Karn exiled it. */
public class RestartGameTest extends AITest {

    private Game commanderGame() {
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add("Grizzly Bears", 1);
        final List<RegisteredPlayer> players = Lists.newArrayList(
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p2", null)),
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p1", null)));
        final GameRules rules = new GameRules(GameType.Commander);
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        for (final Player p : game.getPlayers()) {
            p.initVariantsZones(p.getRegisteredPlayer());
        }
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private void restartWithKarn(final Card karn, final Player p) {
        final SpellAbility restart = karn.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.RestartGame).findFirst().orElseThrow();
        restart.setActivatingPlayer(p);
        AbilityUtils.resolve(restart);
        p.getGame().getAction().checkStaticAbilities();
    }

    private static List<Card> commandersOwnedBy(final Player p) {
        return p.getGame().getCardsInGame().stream().filter(c -> c.getOwner() == p && c.isCommander()).toList();
    }

    @Test
    public void commanderCanBeCastFromTheCommandZoneAfterARestart() {
        final Game game = commanderGame();
        final Player p = game.getPlayers().get(1);

        restartWithKarn(addCard("Karn Liberated", p), p);

        final List<Card> commanders = commandersOwnedBy(p);
        AssertJUnit.assertEquals("one commander after the restart", 1, commanders.size());
        AssertJUnit.assertTrue("the commander is in the command zone", commanders.get(0).isInZone(ZoneType.Command));
        AssertJUnit.assertFalse("the commander can be cast from the command zone", commanders.get(0).mayPlay(p).isEmpty());
    }

    @Test
    public void commanderOnTheBattlefieldReturnsToTheCommandZone() {
        final Game game = commanderGame();
        final Player p = game.getPlayers().get(1);
        game.getAction().moveTo(ZoneType.Battlefield, p.getCommanders().get(0), null, AbilityKey.newMap());

        restartWithKarn(addCard("Karn Liberated", p), p);

        final List<Card> commanders = commandersOwnedBy(p);
        AssertJUnit.assertEquals("one commander after the restart", 1, commanders.size());
        AssertJUnit.assertTrue("the commander is in the command zone", commanders.get(0).isInZone(ZoneType.Command));
        AssertJUnit.assertFalse("the commander can be cast from the command zone", commanders.get(0).mayPlay(p).isEmpty());
    }

    @Test
    public void mutatedCommanderSplitsAndOnlyTheCommanderGoesToTheCommandZone() {
        final Game game = commanderGame();
        final Player p = game.getPlayers().get(1);
        final Card commander = game.getAction().moveTo(ZoneType.Battlefield, p.getCommanders().get(0), null, AbilityKey.newMap());
        final Card gemrazer = addCardToZone("Gemrazer", p, ZoneType.Hand);
        final SpellAbility mutate = gemrazer.getSpells().stream().filter(SpellAbility::isMutate).findFirst().orElseThrow();
        mutate.setActivatingPlayer(p);
        mutate.getTargets().add(commander);
        AbilityUtils.resolve(mutate);

        restartWithKarn(addCard("Karn Liberated", p), p);

        final List<Card> commanders = commandersOwnedBy(p);
        AssertJUnit.assertEquals("one commander after the restart", 1, commanders.size());
        AssertJUnit.assertTrue("the commander is in the command zone", commanders.get(0).isInZone(ZoneType.Command));
        AssertJUnit.assertEquals("Gemrazer is shuffled into the library", 1, countCardsWithName(game, "Gemrazer", ZoneType.Library));
        AssertJUnit.assertEquals("Gemrazer is not in the command zone", 0, countCardsWithName(game, "Gemrazer", ZoneType.Command));
    }

    @Test
    public void commanderExiledWithKarnReturnsToTheBattlefield() {
        final Game game = commanderGame();
        final Player p = game.getPlayers().get(1);
        final Card karn = addCard("Karn Liberated", p);
        final Card exiled = game.getAction().moveTo(ZoneType.Exile, p.getCommanders().get(0), null, AbilityKey.newMap());
        exiled.setExiledWith(karn);
        karn.addExiledCard(exiled);

        restartWithKarn(karn, p);

        final List<Card> commanders = commandersOwnedBy(p);
        AssertJUnit.assertEquals("one commander after the restart", 1, commanders.size());
        AssertJUnit.assertTrue("Karn puts the commander onto the battlefield", commanders.get(0).isInZone(ZoneType.Battlefield));
    }

    private static List<PaperCard> variantCards(final Predicate<CardType> kind) {
        return StaticData.instance().getVariantCards().getAllCards().stream()
                .filter(pc -> kind.test(pc.getRules().getType())).limit(4).toList();
    }

    /** CR 728.2 - nontraditional cards are in the new game once, so a restart rebuilds each deck instead of adding to it. */
    @Test
    public void nontraditionalDecksAreNotDoubledByARestart() {
        final List<PaperCard> schemes = variantCards(CardType::isScheme);
        final List<PaperCard> planes = variantCards(CardType::isPlane);
        final List<PaperCard> attractions = variantCards(CardType::isAttraction);
        final List<PaperCard> contraptions = variantCards(CardType::isContraption);
        final Deck deck = new Deck();
        for (final PaperCard pc : attractions) {
            deck.getOrCreate(DeckSection.Attractions).add(pc);
        }
        for (final PaperCard pc : contraptions) {
            deck.getOrCreate(DeckSection.Contraptions).add(pc);
        }
        final List<RegisteredPlayer> players = Lists.newArrayList(
                RegisteredPlayer.forVariants(2, EnumSet.of(GameType.ArchenemyRumble, GameType.Planechase), deck, schemes,
                        false, planes, null).setPlayer(new LobbyPlayerAi("p1", null)),
                RegisteredPlayer.forVariants(2, EnumSet.of(GameType.ArchenemyRumble, GameType.Planechase), deck, schemes,
                        false, planes, null).setPlayer(new LobbyPlayerAi("p2", null)));
        final GameRules rules = new GameRules(GameType.Constructed);
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        for (final Player p : game.getPlayers()) {
            p.initVariantsZones(p.getRegisteredPlayer());
        }
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();

        final Player p = game.getPlayers().get(0);
        restartWithKarn(addCard("Karn Liberated", p), p);

        for (final Player each : game.getPlayers()) {
            AssertJUnit.assertEquals("scheme deck", schemes.size(), each.getZone(ZoneType.SchemeDeck).size());
            AssertJUnit.assertEquals("planar deck", planes.size(), each.getZone(ZoneType.PlanarDeck).size());
            AssertJUnit.assertEquals("attraction deck", attractions.size(), each.getZone(ZoneType.AttractionDeck).size());
            AssertJUnit.assertEquals("contraption deck", contraptions.size(), each.getZone(ZoneType.ContraptionDeck).size());
        }
    }
}
