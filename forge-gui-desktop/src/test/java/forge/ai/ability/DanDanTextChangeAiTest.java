package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiController;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellApiToAi;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.item.PaperCard;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class DanDanTextChangeAiTest extends AITest {
    @Test
    public void magicalHackTargetsOpponentFishAndUsesItsCurrentLandWord() {
        Game game = newGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 2, ai);
        addCard("Island", opponent);
        Card ownFish = addCard("Dandân", ai);
        Card opponentFish = addCard("Dandân", opponent);
        Card hack = addCardToZone("Magical Hack", ai, ZoneType.Hand);
        SpellAbility ability = findAbility(hack, ApiType.ChangeText);
        ability.setActivatingPlayer(ai);

        assertEquals(getAiDecision(ai, ability), AiPlayDecision.WillPlay,
                "Magical Hack should be playable when it can strand the opponent's public Dandân");
        assertEquals(ability.getTargetCard(), opponentFish,
                "targeting should choose the opponent's fish and preserve the AI's fish");

        AbilityUtils.resolve(ability);

        String newRequirement = opponentFish.getChangedTextTypeWords().get("Island");
        assertNotEquals(newRequirement, "Island", "the effect should change Dandân's current required type");
        assertFalse(hasBasicLandType(opponent, newRequirement),
                "the selected current-state rewrite should strand only the opposing fish");
        assertEquals(ownFish.getChangedTextTypeWords().get("Island"), null,
                "the AI's fish text must remain unchanged");

        resolveQueuedDandanSacrifice(game, opponent, opponentFish);
        assertTrue(ai.getCardsIn(ZoneType.Battlefield).contains(ownFish),
                "the AI should not sacrifice its own fish to make the rewrite");
    }

    @Test
    public void crystalSprayChoosesHelpfulTypeModeAndResolvesItsCantrip() {
        Game game = newGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Island", opponent);
        Card fish = addCard("Dandân", opponent);
        addCardToZone("Memory Lapse", opponent, ZoneType.Library);
        Card spray = addCardToZone("Crystal Spray", ai, ZoneType.Hand);
        SpellAbility ability = findAbility(spray, ApiType.GenericChoice);
        ability.setActivatingPlayer(ai);

        assertEquals(getAiDecision(ai, ability), AiPlayDecision.WillPlay,
                "Crystal Spray should choose a public, useful Dandân rewrite in DanDan");
        assertEquals(ability.getTargetCard(), fish,
                "the generic-choice root should target the fish that benefits from its type mode");
        AbilitySub typeMode = ability.getAdditionalAbilityList("Choices").stream()
                .filter(choice -> choice.hasParam("ChangeTypeWord"))
                .findFirst().orElseThrow();
        AiAbilityDecision typeModeDecision = SpellApiToAi.Converter.get(typeMode).chkDrawback(ai, typeMode);
        assertEquals(typeModeDecision.decision(), AiPlayDecision.WillPlay,
                "the explicitly supported DanDan Crystal Spray type subability should pass its drawback check");

        AbilityUtils.resolve(ability);

        String newRequirement = fish.getChangedTextTypeWords().get("Island");
        assertNotEquals(newRequirement, "Island", "the chosen generic mode should execute ChangeType");
        assertFalse(hasBasicLandType(opponent, newRequirement));
        assertEquals(ai.getCardsIn(ZoneType.Hand).size(), 2,
                "the stock draw subability should resolve after the text rewrite");
        assertTrue(ai.getCardsIn(ZoneType.Hand).stream().anyMatch(card -> "Memory Lapse".equals(card.getName())),
                "the shared-pile card drawn by the cantrip should enter the caster's hand");

        resolveQueuedDandanSacrifice(game, opponent, fish);
    }

    @Test
    public void textChangeCardsRemainUnsupportedInOrdinaryFormats() {
        Game game = newGame(false);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Island", opponent);
        addCard("Dandân", opponent);

        Card hack = addCardToZone("Magical Hack", ai, ZoneType.Hand);
        SpellAbility hackAbility = findAbility(hack, ApiType.ChangeText);
        hackAbility.setActivatingPlayer(ai);
        assertFalse(getAiDecision(ai, hackAbility) == AiPlayDecision.WillPlay,
                "ordinary-format Magical Hack should keep its existing CannotPlay behavior");

        Card spray = addCardToZone("Crystal Spray", ai, ZoneType.Hand);
        SpellAbility sprayAbility = findAbility(spray, ApiType.GenericChoice);
        sprayAbility.setActivatingPlayer(ai);
        assertFalse(getAiDecision(ai, sprayAbility) == AiPlayDecision.WillPlay,
                "ordinary-format Crystal Spray should keep its existing no-AILogic behavior");
    }

    @Test
    public void ordinaryChangeTextSubabilityRetainsCannotPlayDrawback() {
        Game game = newGame(false);
        Player ai = game.getPlayers().get(1);
        Card newBlood = addCardToZone("New Blood", ai, ZoneType.Hand);
        SpellAbility root = newBlood.getSpellAbilities().get(0);
        root.setActivatingPlayer(ai);
        AbilitySub changeText = root.getSubAbility();

        assertEquals(changeText.getApi(), ApiType.ChangeText,
                "New Blood should exercise its real ChangeText subability");
        assertEquals(SpellApiToAi.Converter.get(changeText).chkDrawback(ai, changeText).decision(),
                AiPlayDecision.CantPlayAi,
                "ordinary ChangeText subabilities must retain the former CannotPlayAi drawback behavior");
    }

    @Test
    public void magicalHackDoesNotTargetItsOwnFishWhenNoOpponentFishCanBeChanged() {
        Game game = newGame(true);
        Player ai = game.getPlayers().get(1);
        addCard("Island", ai);
        addCard("Dandân", ai);
        Card hack = addCardToZone("Magical Hack", ai, ZoneType.Hand);
        SpellAbility ability = findAbility(hack, ApiType.ChangeText);
        ability.setActivatingPlayer(ai);

        assertFalse(getAiDecision(ai, ability) == AiPlayDecision.WillPlay,
                "a harmless own fish is not a target for a harmful rewrite");
        assertTrue(ability.getTargets().getTargetCards().isEmpty(),
                "declining the spell must leave the AI's Dandân unselected");
    }

    @Test
    public void crystalSprayDeclinesWhenItsCantripWouldDrawFromAnEmptyLibrary() {
        Game game = newGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Island", opponent);
        addCard("Dandân", opponent);
        Card spray = addCardToZone("Crystal Spray", ai, ZoneType.Hand);
        SpellAbility ability = findAbility(spray, ApiType.GenericChoice);
        ability.setActivatingPlayer(ai);

        assertFalse(getAiDecision(ai, ability) == AiPlayDecision.WillPlay,
                "Crystal Spray should decline the mandatory draw that would lose with an empty library");
    }

    @Test
    public void crystalSprayMayCantripFromEmptyLibraryWhenAiCannotLoseToMilling() {
        Game game = newGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Platinum Angel", ai);
        addCard("Island", opponent);
        addCard("Dandân", opponent);
        Card spray = addCardToZone("Crystal Spray", ai, ZoneType.Hand);
        SpellAbility ability = findAbility(spray, ApiType.GenericChoice);
        ability.setActivatingPlayer(ai);

        assertEquals(getAiDecision(ai, ability), AiPlayDecision.WillPlay,
                "the public helpful rewrite remains available when the AI cannot lose to its empty-library draw");
    }

    @Test
    public void sixSupportedCardHintsAreIgnoredOnlyForDanDanEligibilityAndWarnings() {
        String[] supported = {"Predict", "Magical Hack", "Crystal Spray", "Day's Undoing", "Metamorphose", "Svyelunite Temple"};
        Deck deck = new Deck();
        for (String name : supported) {
            deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard(name));
        }
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Sleight of Mind"));

        Game danDanGame = newGame(true);
        Player danDanAi = danDanGame.getPlayers().get(1);
        Map<DeckSection, List<? extends PaperCard>> danDanWarnings =
                getAi(danDanAi).complainCardsCantPlayWell(deck);
        List<String> danDanWarnedNames = danDanWarnings.values().stream().flatMap(List::stream)
                .map(PaperCard::getName).collect(Collectors.toList());
        assertTrue(danDanWarnedNames.contains("Sleight of Mind"),
                "DanDan warning filtering must preserve unrelated unsupported cards");
        for (String name : supported) {
            assertFalse(danDanWarnedNames.contains(name),
                    name + " should be omitted from DanDan's AI warning");
        }
        for (String name : supported) {
            Card card = addCardToZone(name, danDanAi, ZoneType.Hand);
            assertFalse(ComputerUtilCard.isCardRemAIDeck(card),
                    name + " should be eligible for the DanDan AI despite its general-format hint");
        }

        Game ordinaryGame = newGame(false);
        Player ordinaryAi = ordinaryGame.getPlayers().get(1);
        Map<DeckSection, List<? extends PaperCard>> ordinaryWarnings =
                getAi(ordinaryAi).complainCardsCantPlayWell(deck);
        List<String> ordinaryWarnedNames = ordinaryWarnings.values().stream().flatMap(List::stream)
                .map(PaperCard::getName).collect(Collectors.toList());
        assertTrue(ordinaryWarnedNames.contains("Sleight of Mind"),
                "ordinary-format warnings should retain both supported-card hints and unrelated exclusions");
        for (String name : supported) {
            assertTrue(ordinaryWarnedNames.contains(name),
                    name + " should retain its ordinary-format AI warning");
        }
        for (String name : supported) {
            Card card = addCardToZone(name, ordinaryAi, ZoneType.Hand);
            assertEquals(ComputerUtilCard.isCardRemAIDeck(card),
                    card.getRules().getAiHints().getRemAIDecks(),
                    name + " should retain its original hint-based eligibility in ordinary formats");
        }
    }

    private boolean hasBasicLandType(Player player, String type) {
        return player.getCardsIn(ZoneType.Battlefield).stream()
                .anyMatch(card -> card.getType().hasSubtype(type));
    }

    private SpellAbility findAbility(Card card, ApiType api) {
        return card.getSpellAbilities().stream()
                .filter(ability -> ability.getApi() == api)
                .findFirst().orElseThrow();
    }

    private AiPlayDecision getAiDecision(Player ai, SpellAbility ability) {
        ai.getGame().getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(ability);
    }

    private AiController getAi(Player player) {
        return ((PlayerControllerAi) player.getController()).getAi();
    }

    private void resolveQueuedDandanSacrifice(Game game, Player controller, Card fish) {
        game.getAction().checkStateEffects(true);
        assertTrue(controller.getCardsIn(ZoneType.Battlefield).contains(fish),
                "the lethal text rewrite should leave Dandân in play while its state trigger is queued");
        assertTrue(game.getStack().hasSimultaneousStackEntries(),
                "Dandân's Always trigger should be queued before it is put on the stack");
        assertTrue(game.getStack().addAllTriggeredAbilitiesToStack(),
                "the queued state trigger should be added to the stack");

        SpellAbility sacrifice = game.getStack().peekAbility();
        assertTrue(sacrifice.isTrigger(), "the stack entry should be a real triggered ability");
        assertEquals(sacrifice.getTrigger().getMode(), TriggerType.Always,
                "the lethal ability should be Dandân's printed Always trigger");
        assertEquals(sacrifice.getHostCard(), fish, "the trigger source should be the rewritten fish");
        assertEquals(sacrifice.getActivatingPlayer(), controller,
                "the opponent should be the controller of Dandân's sacrifice trigger");
        assertEquals(sacrifice.getHostCard().getController(), controller,
                "the trigger source should remain controlled by the opponent");

        game.getStack().resolveStack();
        assertFalse(controller.getCardsIn(ZoneType.Battlefield).contains(fish),
                "resolving Dandân's actual Always trigger should sacrifice the fish");
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new forge.ai.LobbyPlayerAi("text first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new forge.ai.LobbyPlayerAi("text second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan text-change AI test");
        Game game = match.createGame();
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        return game;
    }
}
