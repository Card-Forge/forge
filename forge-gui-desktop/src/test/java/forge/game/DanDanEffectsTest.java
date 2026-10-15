package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.card.ICardFace;
import forge.util.collect.FCollectionView;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Focused execution tests for the shipped DanDan shared-pile effects. */
public class DanDanEffectsTest extends AITest {
    @Test
    public void fengrafFindsCreatureInCommonGraveyardAndReturnsItToActivator() {
        Game game = newGame(true);
        Player caster = game.getPlayers().get(0);
        Player previousOwner = game.getPlayers().get(1);
        Card fish = addCardToZone("Dandân", previousOwner, ZoneType.Graveyard);
        Card fengraf = addCard("Haunted Fengraf", caster);

        SpellAbility ability = findChangeZoneAbility(fengraf);
        ability.setActivatingPlayer(caster);
        AbilityUtils.resolve(ability);

        assertTrue(caster.getCardsIn(ZoneType.Hand).contains(fish),
                "the Fengraf activation resolves from the shared graveyard to its activator's hand");
        assertEquals(game.getCardState(fish).getOwner(), caster,
                "the recipient acquires a card entering their hand from the common graveyard");
        assertFalse(previousOwner.getCardsIn(ZoneType.Graveyard).contains(fish));

        Game ordinary = newGame(false);
        Player ordinaryCaster = ordinary.getPlayers().get(0);
        Player ordinaryOpponent = ordinary.getPlayers().get(1);
        Card opponentFish = addCardToZone("Dandân", ordinaryOpponent, ZoneType.Graveyard);
        SpellAbility ordinaryAbility = findChangeZoneAbility(addCard("Haunted Fengraf", ordinaryCaster));
        ordinaryAbility.setActivatingPlayer(ordinaryCaster);
        AbilityUtils.resolve(ordinaryAbility);
        assertTrue(ordinaryOpponent.getCardsIn(ZoneType.Graveyard).contains(opponentFish),
                "ordinary Fengraf retains its own-graveyard restriction");
        assertFalse(ordinaryCaster.getCardsIn(ZoneType.Hand).contains(opponentFish));
    }

    @Test
    public void sanctuaryCanTargetAnOpponentsCardInCommonGraveyardButNotAnotherGraveyard() {
        Game danDan = newGame(true);
        Player caster = danDan.getPlayers().get(0);
        Player formerOwner = danDan.getPlayers().get(1);
        Card sanctuary = enterUntappedSanctuary(danDan, caster);
        Card spell = addCardToZone("Predict", formerOwner, ZoneType.Graveyard);
        SpellAbility ability = findSanctuaryTriggerAbility(sanctuary);
        ability.setActivatingPlayer(caster);

        assertFalse(sanctuary.isTapped(), "three other Islands enable the real untapped-entry trigger condition");
        assertTrue(ability.canTarget(spell),
                "the official 'your graveyard' target refers to the actual common graveyard in DanDan");
        assertTrue(spell.isValid("Instant.YouOwn", caster, sanctuary, ability),
                "each shared graveyard participant owns the card");
        ability.getTargets().add(spell);
        AbilityUtils.resolve(ability);
        assertEquals(danDan.getCardsIn(ZoneType.Library).get(0), spell,
                "Sanctuary puts the chosen common-graveyard card on the shared library top");
        assertEquals(danDan.getCardState(spell).getOwners(), Set.of(caster, formerOwner),
                "movement between shared pools preserves joint ownership");

        Game ordinary = newGame(false);
        Player ordinaryCaster = ordinary.getPlayers().get(0);
        Player ordinaryOpponent = ordinary.getPlayers().get(1);
        Card ordinarySanctuary = enterUntappedSanctuary(ordinary, ordinaryCaster);
        Card opponentsSpell = addCardToZone("Predict", ordinaryOpponent, ZoneType.Graveyard);
        SpellAbility ordinaryAbility = findSanctuaryTriggerAbility(ordinarySanctuary);
        ordinaryAbility.setActivatingPlayer(ordinaryCaster);

        assertFalse(ordinaryAbility.canTarget(opponentsSpell),
                "ordinary 'your graveyard' keeps its own-zone restriction");
    }

    @Test
    public void predictMillsItsTargetAndDrawsForItsCaster() {
        Game game = newGame(true);
        Player caster = game.getPlayers().get(0);
        Player target = game.getPlayers().get(1);
        setNamedIslandController(caster);
        Card milled = addCardToZone("Island", caster, ZoneType.Library);
        Card draw1 = addCardToZone("Memory Lapse", target, ZoneType.Library);
        Card draw2 = addCardToZone("Unsubstantiate", caster, ZoneType.Library);
        assertEquals(cardIds(game.getCardsIn(ZoneType.Library)),
                List.of(milled.getId(), draw1.getId(), draw2.getId()), "the named Island is known to be on top");
        assertTrue(target.getZone(ZoneType.Graveyard) == caster.getZone(ZoneType.Graveyard),
                "both players refer to the same physical DanDan graveyard");
        Card predict = addCardToZone("Predict", caster, ZoneType.Hand);
        predict.addNamedCard("Island");
        SpellAbility ability = predict.getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        SpellAbility mill = ability.getSubAbility();
        assertTrue(mill.canTarget(target), "Predict's mill instruction can target the opponent");
        mill.getTargets().add(target);

        AbilityUtils.resolve(ability);

        Card currentMilled = game.getCardState(milled);
        assertEquals(currentMilled.getZone().getZoneType(), ZoneType.Graveyard,
                "Predict moves the known top card to the graveyard");
        assertTrue(target.getCardsIn(ZoneType.Graveyard).stream().anyMatch(card -> card.getId() == milled.getId()),
                "Predict mills the targeted player's card into their physical graveyard");
        assertTrue(game.getCardsIn(ZoneType.Graveyard).stream().anyMatch(card -> card.getId() == currentMilled.getId()),
                "the milled card is physically present in the common graveyard");
        assertTrue(caster.getCardsIn(ZoneType.Hand).contains(draw1));
        assertTrue(caster.getCardsIn(ZoneType.Hand).contains(draw2));
        assertEquals(game.getCardState(draw1).getOwner(), caster);
        assertEquals(game.getCardState(draw2).getOwner(), caster);
        assertFalse(target.getCardsIn(ZoneType.Hand).contains(draw1));
        assertFalse(target.getCardsIn(ZoneType.Hand).contains(draw2));
        assertTrue(game.getGameLog().getAllEntries().stream().anyMatch(entry ->
                        entry.message().contains(target.getName()) && entry.message().contains("milled")
                                && entry.message().contains(milled.getName())),
                "the mill log identifies the targeted player as the player who milled the named card");

        Game mismatch = newGame(true);
        Player mismatchCaster = mismatch.getPlayers().get(0);
        Player mismatchTarget = mismatch.getPlayers().get(1);
        setNamedIslandController(mismatchCaster);
        Card nonmatching = addCardToZone("Memory Lapse", mismatchTarget, ZoneType.Library);
        Card namedDraw = addCardToZone("Island", mismatchCaster, ZoneType.Library);
        Card residual = addCardToZone("Unsubstantiate", mismatchTarget, ZoneType.Library);
        assertEquals(cardIds(mismatch.getCardsIn(ZoneType.Library)),
                List.of(nonmatching.getId(), namedDraw.getId(), residual.getId()));
        Card mismatchPredict = addCardToZone("Predict", mismatchCaster, ZoneType.Hand);
        mismatchPredict.addNamedCard("Island");
        SpellAbility mismatchAbility = mismatchPredict.getFirstSpellAbility();
        mismatchAbility.setActivatingPlayer(mismatchCaster);
        mismatchAbility.getSubAbility().getTargets().add(mismatchTarget);
        AbilityUtils.resolve(mismatchAbility);
        assertTrue(mismatchCaster.getCardsIn(ZoneType.Hand).contains(namedDraw),
                "a name mismatch draws exactly one card after milling");
        assertEquals(mismatch.getCardState(nonmatching).getZone().getZoneType(), ZoneType.Graveyard);
        assertTrue(mismatch.getGameLog().getAllEntries().stream().anyMatch(entry ->
                        entry.message().contains(mismatchTarget.getName()) && entry.message().contains("milled")
                                && entry.message().contains(nonmatching.getName())),
                "the mismatch branch still attributes the mill to its targeted player");
        assertEquals(cardIds(mismatch.getCardsIn(ZoneType.Library)), List.of(residual.getId()),
                "the one-card branch leaves the third known card in the library");
    }

    @Test
    public void tellingTimePartitionsCommonTopWithoutDrawingAndGivesSelectionToCaster() {
        Game game = newGame(true);
        Player caster = game.getPlayers().get(0);
        Player other = game.getPlayers().get(1);
        Card top = addCardToZone("Island", other, ZoneType.Library);
        Card middle = addCardToZone("Memory Lapse", other, ZoneType.Library);
        Card bottom = addCardToZone("Unsubstantiate", other, ZoneType.Library);
        useIdChoiceController(caster, List.of(top.getId(), bottom.getId()));
        useIdChoiceController(other, List.of());
        Card tellingTime = addCardToZone("Telling Time", caster, ZoneType.Hand);
        SpellAbility ability = tellingTime.getFirstSpellAbility();
        ability.setActivatingPlayer(caster);

        AbilityUtils.resolve(ability);

        Card selected = top;
        assertTrue(caster.getCardsIn(ZoneType.Hand).contains(selected),
                "the chosen card enters the resolving player's hand");
        assertFalse(other.getCardsIn(ZoneType.Hand).contains(selected),
                "the old card controller does not determine the hand recipient");
        assertEquals(game.getCardsIn(ZoneType.Library).size(), 2);
        assertEquals(cardIds(game.getCardsIn(ZoneType.Library)), List.of(bottom.getId(), middle.getId()),
                "Telling Time returns one unselected card to the top and the other to the bottom");
        assertEquals(caster.getCardsIn(ZoneType.Hand).size(), 2,
                "the effect adds one card and does not perform a draw");
        assertEquals(caster.getNumDrawnThisTurn(), 0, "Telling Time does not increment the caster's draw count");
        assertEquals(other.getNumDrawnThisTurn(), 0);
        RecordingIdChoiceController casterChoices = (RecordingIdChoiceController) caster.getController();
        RecordingIdChoiceController otherChoices = (RecordingIdChoiceController) other.getController();
        List<Set<Integer>> expectedChoices = List.of(
                Set.of(top.getId(), middle.getId(), bottom.getId()), Set.of(middle.getId(), bottom.getId()));
        assertEquals(idSets(casterChoices.tempShownIds), expectedChoices);
        assertEquals(idSets(casterChoices.optionIds), expectedChoices);
        assertTrue(casterChoices.revealedIds.isEmpty(), "the chooser uses the private look rather than a public reveal");
        assertTrue(otherChoices.tempShownIds.isEmpty(), "only the chooser sees the looked-at cards");
        assertTrue(otherChoices.optionIds.isEmpty(), "only the chooser receives the private choice options");
        assertTrue(otherChoices.revealedIds.isEmpty(), "the looked-at cards are not revealed to the opponent");
        assertEquals(game.getCardState(selected).getOwner(), caster,
                "Telling Time's hand recipient acquires the selected card from the common library");
    }

    @Test
    public void metamorphoseUsesTargetControllerHandAndOptionalChoice() {
        Game game = newGame(true);
        Player caster = game.getPlayers().get(0);
        Player targetController = game.getPlayers().get(1);
        Card fish = addCard("Dandân", caster);
        Card controlMagic = addCard("Control Magic", targetController);
        controlMagic.attachToEntity(fish, null);
        game.getAction().checkStateEffects(true);
        assertEquals(fish.getOwner(), caster);
        assertEquals(fish.getController(), targetController);

        Card ownIsland = addCard("Island", caster);
        Card opposingIsland = addCard("Island", targetController);
        Card morph = addCardToZone("Metamorphose", caster, ZoneType.Hand);
        SpellAbility ability = morph.getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        assertFalse(ability.canTarget(ownIsland), "a player cannot target their own permanent");
        assertTrue(ability.canTarget(opposingIsland), "an opponent-controlled permanent is a legal target");
        assertTrue(ability.canTarget(fish), "controller, rather than owner, determines the legal target");

        Card chosen = addCardToZone("Island", targetController, ZoneType.Hand);
        Card casterChoice = addCardToZone("Grizzly Bears", caster, ZoneType.Hand);
        useIdChoiceController(targetController, List.of(chosen.getId()));
        useDeclineController(caster);
        ability.getTargets().add(fish);
        game.getStack().addAndUnfreeze(ability);
        game.getStack().resolveStack();

        assertEquals(game.getCardsIn(ZoneType.Library).get(0), fish,
                "Metamorphose returns the target to the common library top");
        assertTrue(targetController.getCardsIn(ZoneType.Battlefield).contains(chosen),
                "the targeted permanent's controller chooses from their hand and controls the entering card");
        assertEquals(game.getCardState(chosen).getOwner(), targetController,
                "the player receiving the card in hand owns it when Metamorphose puts it onto the battlefield");
        assertEquals(chosen.getController(), targetController);
        assertTrue(caster.getCardsIn(ZoneType.Hand).contains(casterChoice),
                "the caster's private hand is not used for the opponent's choice");

        Game declined = newGame(true);
        Player declineCaster = declined.getPlayers().get(0);
        Player declineController = declined.getPlayers().get(1);
        Card declineFish = addCard("Dandân", declineCaster);
        Card declineAura = addCard("Control Magic", declineController);
        declineAura.attachToEntity(declineFish, null);
        declined.getAction().checkStateEffects(true);
        Card declinedCard = addCardToZone("Island", declineController, ZoneType.Hand);
        useDeclineController(declineController);
        SpellAbility declinedAbility = addCardToZone("Metamorphose", declineCaster, ZoneType.Hand).getFirstSpellAbility();
        declinedAbility.setActivatingPlayer(declineCaster);
        declinedAbility.getTargets().add(declineFish);
        declined.getStack().addAndUnfreeze(declinedAbility);
        declined.getStack().resolveStack();

        assertTrue(declineController.getCardsIn(ZoneType.Hand).contains(declinedCard),
                "the targeted controller may decline the optional hand entry");
        assertFalse(declineController.getCardsIn(ZoneType.Battlefield).contains(declinedCard));
        assertEquals(declined.getCardsIn(ZoneType.Library).get(0), declineFish);
    }

    @Test
    public void regrowthAndMerchantScrollGiveSharedSelectionToCaster() {
        for (String name : List.of("Regrowth", "Merchant Scroll")) {
            Game game = newGame(true);
            Player first = game.getPlayers().get(0);
            Player caster = game.getPlayers().get(1);
            Card selected = addCardToZone("Memory Lapse", first,
                    name.equals("Regrowth") ? ZoneType.Graveyard : ZoneType.Library);
            SpellAbility ability = addCardToZone(name, caster, ZoneType.Hand).getFirstSpellAbility();
            ability.setActivatingPlayer(caster);
            if (ability.usesTargeting()) {
                assertTrue(ability.canTarget(selected));
                ability.getTargets().add(selected);
            }
            AbilityUtils.resolve(ability);
            assertTrue(caster.getCardsIn(ZoneType.Hand).contains(selected), name + " gives its caster the selected card");
            assertEquals(game.getCardState(selected).getOwner(), caster);
            assertFalse(first.getCardsIn(ZoneType.Hand).contains(selected));
        }
    }

    @Test
    public void stolenExpanseSearchEntersTappedUnderActivatingPlayer() {
        Game game = newGame(true);
        Player owner = game.getPlayers().get(0);
        Player activator = game.getPlayers().get(1);
        Card land = addCardToZone("Island", owner, ZoneType.Library);
        Card expanse = addCard("Terramorphic Expanse", owner);
        expanse.setController(activator, game.getNextTimestamp());
        SpellAbility ability = findChangeZoneAbility(expanse);
        ability.setActivatingPlayer(activator);
        AbilityUtils.resolve(ability);

        Card entered = game.getCardState(land);
        assertEquals(expanse.getOwner(), owner, "the activation does not change the Expanse's owner");
        assertTrue(activator.getCardsIn(ZoneType.Battlefield).contains(entered));
        assertEquals(entered.getController(), activator);
        assertEquals(entered.getOwner(), activator);
        assertTrue(entered.isTapped());
        assertFalse(land.isTapped(), "entry preparation does not mutate the source object");
    }

    @Test
    public void polymorphGivesFoundCreatureToTheTargetController() {
        Game game = newGame(true);
        Player caster = game.getPlayers().get(0);
        Player recipient = game.getPlayers().get(1);
        Card selected = addCardToZone("Colossal Dreadmaw", caster, ZoneType.Library);
        Card target = addCard("Grizzly Bears", recipient);
        SpellAbility ability = addCardToZone("Polymorph", caster, ZoneType.Hand).getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        ability.getTargets().add(target);
        game.getStack().addAndUnfreeze(ability);
        game.getStack().resolveStack();
        Card entered = game.getCardState(selected);
        assertTrue(recipient.getCardsIn(ZoneType.Battlefield).contains(entered));
        assertEquals(entered.getOwners(), Set.of(recipient));
        assertEquals(entered.getController(), recipient);
        assertEquals(selected.getOwners(), Set.of(caster, recipient), "the shared source snapshot keeps both owners");
        assertEquals(selected.getExplicitController(), null, "the source gains no actual controller");
    }

    @Test
    public void dackGoadsTheAcceptedCreatureBeforeGivingItToTheOpponent() {
        Game game = newGame(true);
        Player opponent = game.getPlayers().get(0);
        Player caster = game.getPlayers().get(1);
        Card selected = addCardToZone("Colossal Dreadmaw", opponent, ZoneType.Library);
        Card dack = addCard("Dack Fayden, Helping Hand", caster);
        SpellAbility ability = dack.getTriggers().get(0).ensureAbility();
        ability.setActivatingPlayer(caster);
        AbilityUtils.resolve(ability);
        Card entered = game.getCardState(selected);
        assertTrue(entered.isGoadedBy(caster), "the follow-up ability uses the accepted incoming card");
        assertEquals(entered.getOwners(), Set.of(caster));
        assertEquals(entered.getController(), opponent);
    }

    @Test
    public void relentlessPursuitGivesBothSelectedTypesToItsCaster() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player caster = game.getPlayers().get(1);
        Card creature = addCardToZone("Grizzly Bears", first, ZoneType.Library);
        Card land = addCardToZone("Island", first, ZoneType.Library);
        Card rest = addCardToZone("Memory Lapse", first, ZoneType.Library);
        SpellAbility ability = addCardToZone("Relentless Pursuit", caster, ZoneType.Hand).getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        AbilityUtils.resolve(ability);
        for (Card selected : List.of(creature, land)) {
            assertTrue(caster.getCardsIn(ZoneType.Hand).contains(selected));
            assertEquals(game.getCardState(selected).getOwners(), Set.of(caster));
            assertFalse(first.getCardsIn(ZoneType.Hand).contains(selected));
        }
        assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(rest));
    }

    @Test
    public void digAndMassReturnPrepareTappedCardsForTheirRecipient() {
        for (String name : List.of("Expand the Sphere", "Splendid Reclamation")) {
            Game game = newGame(true);
            Player first = game.getPlayers().get(0);
            Player caster = game.getPlayers().get(1);
            ZoneType origin = name.equals("Expand the Sphere") ? ZoneType.Library : ZoneType.Graveyard;
            Card island = addCardToZone("Island", first, origin);
            Card forest = addCardToZone("Forest", first, origin);
            SpellAbility ability = addCardToZone(name, caster, ZoneType.Hand).getFirstSpellAbility();
            ability.setActivatingPlayer(caster);
            AbilityUtils.resolve(ability);
            for (Card source : List.of(island, forest)) {
                Card entered = game.getCardState(source);
                assertTrue(caster.getCardsIn(ZoneType.Battlefield).contains(entered), name);
                assertEquals(entered.getOwners(), Set.of(caster));
                assertEquals(entered.getController(), caster);
                assertTrue(entered.isTapped());
                assertFalse(source.isTapped(), "tapped preparation leaves the live source snapshot unchanged");
            }
        }
    }

    @Test
    public void endlessDetourUsesCasterForJointOwnerChoiceAndMovesOnce() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player caster = game.getPlayers().get(1);
        Card selected = addCardToZone("Dandân", first, ZoneType.Graveyard);
        Card top = addCardToZone("Island", first, ZoneType.Library);
        List<Player> choices = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            player.dangerouslySetController(new PlayerControllerAi(game, player,
                    new LobbyPlayerAi(player.getName(), null)) {
                @Override
                public boolean confirmAction(SpellAbility sa, forge.game.player.PlayerActionConfirmMode mode,
                        String message, List<String> options, Card cardToShow, java.util.Map<String, Object> params) {
                    if (mode == forge.game.player.PlayerActionConfirmMode.ChangeZoneToAltDestination) {
                        choices.add(player);
                        return false;
                    }
                    return super.confirmAction(sa, mode, message, options, cardToShow, params);
                }
            });
        }
        SpellAbility ability = addCardToZone("Endless Detour", caster, ZoneType.Hand).getFirstSpellAbility();
        ability.setActivatingPlayer(caster);
        ability.getTargets().add(selected);
        AbilityUtils.resolve(ability);
        assertEquals(choices, List.of(caster));
        assertEquals(cardIds(game.getCardsIn(ZoneType.Library)), List.of(top.getId(), selected.getId()));
    }

    @Test
    public void carrionLocustMakesOnlyTheOpponentLoseLife() {
        Game game = newGame(true);
        Player caster = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        Card selected = addCardToZone("Dandân", caster, ZoneType.Graveyard);
        Card locust = addCard("Carrion Locust", caster);
        SpellAbility ability = locust.getTriggers().get(0).ensureAbility();
        ability.setActivatingPlayer(caster);
        assertTrue(ability.canTarget(selected), "the joint card is in the opponent's graveyard");
        ability.getTargets().add(selected);
        int casterLife = caster.getLife();
        int opponentLife = opponent.getLife();
        AbilityUtils.resolve(ability);
        assertEquals(game.getCardState(selected).getZone().getZoneType(), ZoneType.Exile);
        assertEquals(caster.getLife(), casterLife);
        assertEquals(opponent.getLife(), opponentLife - 1);
    }

    @Test
    public void reanimatePreventionAndRedirectionPreserveUnpreparedSource() {
        for (boolean redirect : List.of(false, true)) {
            Game game = newGame(true);
            Player first = game.getPlayers().get(0);
            Player caster = game.getPlayers().get(1);
            Card selected = addCardToZone("Grizzly Bears", first, ZoneType.Graveyard);
            addCard(redirect ? "Containment Priest" : "Grafdigger's Cage", first);
            SpellAbility ability = addCardToZone("Reanimate", caster, ZoneType.Hand).getFirstSpellAbility();
            ability.setActivatingPlayer(caster);
            ability.getTargets().add(selected);
            Player sourceController = selected.getController();
            game.getStack().addAndUnfreeze(ability);
            game.getStack().resolveStack();
            assertEquals(selected.getController(), sourceController, "Reanimate must not prepare control on the live source");
            assertEquals(game.getCardState(selected).getZone().getZoneType(), redirect ? ZoneType.Exile : ZoneType.Graveyard);
            assertFalse(caster.getCardsIn(ZoneType.Battlefield).contains(selected));
        }
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("effects first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("effects second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan effects test");
        Game game = match.createGame();
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        return game;
    }

    private SpellAbility findChangeZoneAbility(Card card) {
        return card.getSpellAbilities().stream()
                .filter(sa -> sa.getApi() != null && sa.getApi().name().equals("ChangeZone"))
                .findFirst().orElseThrow();
    }

    private SpellAbility findSanctuaryTriggerAbility(Card sanctuary) {
        return sanctuary.getTriggers().stream().findFirst().orElseThrow().ensureAbility();
    }

    private Card enterUntappedSanctuary(Game game, Player player) {
        for (int i = 0; i < 3; i++) {
            Card island = addCardToZone("Island", player, ZoneType.Hand);
            game.getAction().moveToPlay(island, player, null, AbilityKey.newMap());
        }
        game.getAction().checkStateEffects(true);
        assertEquals(player.getCardsIn(ZoneType.Battlefield).stream()
                .filter(card -> card.getType().hasSubtype("Island")).count(), 3L,
                "three Islands are on the battlefield before Mystic Sanctuary enters");
        game.copyLastState();
        Card sanctuary = addCardToZone("Mystic Sanctuary", player, ZoneType.Hand);
        game.getAction().moveToPlay(sanctuary, player, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        return game.getCardState(sanctuary);
    }

    private void setNamedIslandController(Player player) {
        PlayerControllerAi controller = new PlayerControllerAi(player.getGame(), player,
                new LobbyPlayerAi(player.getName(), null)) {
            @Override
            public String chooseCardName(SpellAbility sa, java.util.function.Predicate<ICardFace> valid,
                    String restriction, String message) {
                return "Island";
            }
        };
        player.dangerouslySetController(controller);
    }

    private void useIdChoiceController(Player player, List<Integer> choiceIds) {
        PlayerControllerAi controller = new RecordingIdChoiceController(player, choiceIds);
        player.dangerouslySetController(controller);
    }

    private static final class RecordingIdChoiceController extends PlayerControllerAi {
        private final List<Integer> choiceIds;
        private int nextChoice;
        private final List<List<Integer>> tempShownIds = new ArrayList<>();
        private final List<List<Integer>> optionIds = new ArrayList<>();
        private final List<Integer> revealedIds = new ArrayList<>();

        private RecordingIdChoiceController(Player player, List<Integer> choiceIds) {
            super(player.getGame(), player, new LobbyPlayerAi(player.getName(), null));
            this.choiceIds = choiceIds;
        }

        @Override
        public void tempShowCards(Iterable<Card> cards) {
            List<Integer> ids = new ArrayList<>();
            cards.forEach(card -> ids.add(card.getId()));
            tempShownIds.add(ids);
        }

        @Override
        public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> options, int min, int max,
                DelayedReveal delayedReveal, SpellAbility sa, String title, Player targetedPlayer,
                java.util.Map<String, Object> params) {
            List<Integer> ids = new ArrayList<>();
            options.forEach(option -> {
                if (option instanceof Card card) {
                    ids.add(card.getId());
                }
            });
            optionIds.add(ids);
            return super.chooseEntitiesForEffect(options, min, max, delayedReveal, sa, title, targetedPlayer, params);
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> options,
                DelayedReveal delayedReveal, SpellAbility sa, String title, boolean optional,
                Player targetedPlayer, java.util.Map<String, Object> params) {
            int selectedId = choiceIds.get(nextChoice++);
            for (T option : options) {
                if (option instanceof Card card && card.getId() == selectedId) {
                    return option;
                }
            }
            throw new AssertionError("Expected effect choice " + selectedId + " among " + options);
        }

        @Override
        public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix,
                boolean addSuffix) {
            cards.forEach(card -> revealedIds.add(card.getId()));
            super.reveal(cards, zone, owner, messagePrefix, addSuffix);
        }
    }

    private List<Integer> cardIds(Iterable<Card> cards) {
        List<Integer> ids = new ArrayList<>();
        for (Card card : cards) {
            ids.add(card.getId());
        }
        return ids;
    }

    private List<Set<Integer>> idSets(List<List<Integer>> ids) {
        return ids.stream().map(Set::copyOf).toList();
    }

    private void useDeclineController(Player player) {
        PlayerControllerAi controller = new PlayerControllerAi(player.getGame(), player,
                new LobbyPlayerAi(player.getName(), null)) {
            @Override
            public boolean confirmAction(SpellAbility sa, forge.game.player.PlayerActionConfirmMode mode,
                    String message, List<String> options, Card cardToShow, java.util.Map<String, Object> params) {
                return mode != forge.game.player.PlayerActionConfirmMode.ChangeZoneGeneral
                        && super.confirmAction(sa, mode, message, options, cardToShow, params);
            }

            @Override
            public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> options,
                    forge.game.player.DelayedReveal delayedReveal, SpellAbility sa, String title, boolean optional,
                    Player targetedPlayer, java.util.Map<String, Object> params) {
                return optional ? null : super.chooseSingleEntityForEffect(options, delayedReveal, sa, title,
                        false, targetedPlayer, params);
            }
        };
        player.dangerouslySetController(controller);
    }
}
