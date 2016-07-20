package forge.player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import forge.FThreads;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.achievement.AchievementCollection;
import forge.ai.GameState;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.control.FControlGamePlayback;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.events.UiEventNextGameDecision;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.GameLogEntryType;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.PlanarDice;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardShields;
import forge.game.card.CardView;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.player.PlayerView;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IGuiGame;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.match.NextGameDecision;
import forge.match.input.Input;
import forge.match.input.InputAttack;
import forge.match.input.InputBlock;
import forge.match.input.InputConfirm;
import forge.match.input.InputConfirmMulligan;
import forge.match.input.InputPassPriority;
import forge.match.input.InputPayMana;
import forge.match.input.InputProliferate;
import forge.match.input.InputProxy;
import forge.match.input.InputQueue;
import forge.match.input.InputSelectCardsForConvoke;
import forge.match.input.InputSelectCardsFromList;
import forge.match.input.InputSelectEntitiesFromList;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;
import forge.util.Lang;
import forge.util.MessageUtil;
import forge.util.TextUtil;
import forge.util.gui.SOptionPane;

/**
 * A prototype for player controller class
 *
 * Handles phase skips for now.
 */
public class PlayerControllerHuman
    extends PlayerController
    implements IGameController {
    /**
     * Cards this player may look at right now, for example when searching a
     * library.
     */
    private boolean mayLookAtAllCards = false;
    private boolean disableAutoYields = false;

    private IGuiGame gui;

    protected final InputQueue inputQueue;
    protected final InputProxy inputProxy;
    public PlayerControllerHuman(final Game game0, final Player p, final LobbyPlayer lp) {
        super(game0, p, lp);
        inputProxy = new InputProxy(this);
        inputQueue = new InputQueue(game, inputProxy);
    }
    public PlayerControllerHuman(final Player p, final LobbyPlayer lp, final PlayerControllerHuman owner) {
        super(owner.getGame(), p, lp);
        gui = owner.gui;
        inputProxy = owner.inputProxy;
        inputQueue = owner.getInputQueue();
    }

    public final IGuiGame getGui() {
        return gui;
    }
    public final void setGui(final IGuiGame gui) {
        this.gui = gui;
    }

    public final InputQueue getInputQueue() {
        return inputQueue;
    }

    public InputProxy getInputProxy() {
        return inputProxy;
    }

    public PlayerView getLocalPlayerView() {
        return player == null ? null : player.getView();
    }

    public boolean getDisableAutoYields() {
        return disableAutoYields;
    }
    public void setDisableAutoYields(final boolean disableAutoYields0) {
        disableAutoYields = disableAutoYields0;
    }

    @Override
    public boolean mayLookAtAllCards() {
        return mayLookAtAllCards;
    }

    private final Set<Card> tempShownCards = new HashSet<Card>();
    public <T> void tempShow(final Iterable<T> objects) {
        for (final T t : objects) {
            // assume you may see any card passed through here
            if (t instanceof Card) {
                tempShowCard((Card) t);
            } else if (t instanceof CardView) {
                tempShowCard(game.getCard((CardView) t));
            }
        }
    }
    private void tempShowCard(final Card c) {
        if (c == null) { return; }
        tempShownCards.add(c);
        c.setMayLookAt(player, true, true);
    }
    private void tempShowCards(final Iterable<Card> cards) {
        if (mayLookAtAllCards) { return; } //no needed if this is set

        for (final Card c : cards) {
            tempShowCard(c);
        }
    }
    private void endTempShowCards() {
        if (tempShownCards.isEmpty()) { return; }

        for (final Card c : tempShownCards) {
            c.setMayLookAt(player, false, true);
        }
        tempShownCards.clear();
    }

    /**
     * Set this to {@code true} to enable this player to see all cards any other
     * player can see.
     *
     * @param mayLookAtAllCards
     *            the mayLookAtAllCards to set
     */
    public void setMayLookAtAllCards(final boolean mayLookAtAllCards) {
        this.mayLookAtAllCards = mayLookAtAllCards;
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    @Override
    public SpellAbility getAbilityToPlay(final Card hostCard, final List<SpellAbility> abilities, final ITriggerEvent triggerEvent) {
        final SpellAbilityView resultView = getGui().getAbilityToPlay(CardView.get(hostCard), SpellAbilityView.getCollection(abilities), triggerEvent);
        return getGame().getSpellAbility(resultView);
    }

    @Override
    public void playSpellAbilityForFree(final SpellAbility copySA, final boolean mayChoseNewTargets) {
        HumanPlay.playSaWithoutPayingManaCost(this, player.getGame(), copySA, mayChoseNewTargets);
    }

    @Override
    public void playSpellAbilityNoStack(final SpellAbility effectSA, final boolean canSetupTargets) {
        HumanPlay.playSpellAbilityNoStack(this, player, effectSA, !canSetupTargets);
    }

    @Override
    public List<PaperCard> sideboard(final Deck deck, final GameType gameType) {
        CardPool sideboard = deck.get(DeckSection.Sideboard);
        if (sideboard == null) {
            // Use an empty cardpool instead of null for 75/0 sideboarding scenario.
            sideboard = new CardPool();
        }

        final CardPool main = deck.get(DeckSection.Main);

        final int mainSize = main.countAll();
        final int sbSize = sideboard.countAll();
        final int combinedDeckSize = mainSize + sbSize;

        final int deckMinSize = Math.min(mainSize, gameType.getDeckFormat().getMainRange().getMinimum());
        final Range<Integer> sbRange = gameType.getDeckFormat().getSideRange();
        // Limited doesn't have a sideboard max, so let the Main min take care of things.
        final int sbMax = sbRange == null ? combinedDeckSize : sbRange.getMaximum();

        List<PaperCard> newMain = null;

        //Skip sideboard loop if there are no sideboarding opportunities
        if (sbSize == 0 && mainSize == deckMinSize) { return null; }

        // conformance should not be checked here
        final boolean conform = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);
        do {
            if (newMain != null) {
                String errMsg;
                if (newMain.size() < deckMinSize) {
                    errMsg = String.format("Too few cards in your main deck (minimum %d), please make modifications to your deck again.", deckMinSize);
                }
                else {
                    errMsg = String.format("Too many cards in your sideboard (maximum %d), please make modifications to your deck again.", sbMax);
                }
                getGui().showErrorDialog(errMsg, "Invalid Deck");
            }
            // Sideboard rules have changed for M14, just need to consider min maindeck and max sideboard sizes
            // No longer need 1:1 sideboarding in non-limited formats
            newMain = getGui().sideboard(sideboard, main);
        } while (conform && (newMain.size() < deckMinSize || combinedDeckSize - newMain.size() > sbMax));

        return newMain;
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(final Card attacker,
            final CardCollectionView blockers, final int damageDealt,
            final GameEntity defender, final boolean overrideOrder) {
        // Attacker is a poor name here, since the creature assigning damage
        // could just as easily be the blocker.
        final Map<Card, Integer> map = Maps.newHashMap();
        if (defender != null && assignDamageAsIfNotBlocked(attacker)) {
            map.put(null, damageDealt);
        }
        else {
            final List<CardView> vBlockers = CardView.getCollection(blockers);
            if ((attacker.hasKeyword("Trample") && defender != null) || (blockers.size() > 1)) {
                final CardView vAttacker = CardView.get(attacker);
                final GameEntityView vDefender = GameEntityView.get(defender);
                final Map<CardView, Integer> result = getGui().assignDamage(vAttacker, vBlockers, damageDealt, vDefender, overrideOrder);
                for (final Entry<CardView, Integer> e : result.entrySet()) {
                    map.put(game.getCard(e.getKey()), e.getValue());
                }
            }
            else {
                map.put(blockers.get(0), damageDealt);
            }
        }
        return map;
    }

    private final boolean assignDamageAsIfNotBlocked(final Card attacker) {
        return attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")
                || (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                && getGui().confirm(CardView.get(attacker), "Do you want to assign its combat damage as though it weren't blocked?"));
    }

    @Override
    public Integer announceRequirements(final SpellAbility ability, final String announce, final boolean canChooseZero) {
        final int min = canChooseZero ? 0 : 1;
        return getGui().getInteger("Choose " + announce + " for " + ability.getHostCard().getName(),
                min, Integer.MAX_VALUE, min + 9);
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(final SpellAbility sa, final int min, final int max, final CardCollectionView valid, final String message) {
        return choosePermanentsTo(min, max, valid, message, "sacrifice");
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(final SpellAbility sa, final int min, final int max, final CardCollectionView valid, final String message) {
        return choosePermanentsTo(min, max, valid, message, "destroy");
    }

    private CardCollectionView choosePermanentsTo(final int min, int max, final CardCollectionView valid, final String message, final String action) {
        max = Math.min(max, valid.size());
        if (max <= 0) {
            return CardCollection.EMPTY;
        }

        final StringBuilder builder = new StringBuilder("Select ");
        if (min == 0) {
            builder.append("up to ");
        }
        builder.append("%d " + message + "(s) to " + action + ".");

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage(builder.toString());
        inp.setCancelAllowed(min == 0);
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public CardCollectionView chooseCardsForEffect(final CardCollectionView sourceList, final SpellAbility sa, final String title, final int min, final int max, final boolean isOptional) {
        // If only one card to choose, use a dialog box.
        // Otherwise, use the order dialog to be able to grab multiple cards in one shot

        if (max == 1) {
            final Card singleChosen = chooseSingleEntityForEffect(sourceList, sa, title, isOptional);
            return singleChosen == null ? CardCollection.EMPTY : new CardCollection(singleChosen);
        }

        getGui().setPanelSelection(CardView.get(sa.getHostCard()));

        // try to use InputSelectCardsFromList when possible
        boolean cardsAreInMyHandOrBattlefield = true;
        for (final Card c : sourceList) {
            final Zone z = c.getZone();
            if (z != null && (z.is(ZoneType.Battlefield) || z.is(ZoneType.Hand, player))) {
                continue;
            }
            cardsAreInMyHandOrBattlefield = false;
            break;
        }

        if (cardsAreInMyHandOrBattlefield) {
            final InputSelectCardsFromList sc = new InputSelectCardsFromList(this, min, max, sourceList);
            sc.setMessage(title);
            sc.setCancelAllowed(isOptional);
            sc.showAndWait();
            return new CardCollection(sc.getSelected());
        }

        tempShowCards(sourceList);
        final CardCollection choices = getGame().getCardList(getGui().many(title, "Chosen Cards", min, max, CardView.getCollection(sourceList), CardView.get(sa.getHostCard())));
        endTempShowCards();

        return choices;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(final FCollectionView<T> optionList, final DelayedReveal delayedReveal, final SpellAbility sa, final String title, final boolean isOptional, final Player targetedPlayer) {
        // Human is supposed to read the message and understand from it what to choose
        if (optionList.isEmpty()) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
            }
            return null;
        }
        if (!isOptional && optionList.size() == 1) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
            }
            return Iterables.getFirst(optionList, null);
        }

        boolean canUseSelectCardsInput = true;
        for (final GameEntity c : optionList) {
            if (c instanceof Player) {
                continue;
            }
            final Zone cz = ((Card)c).getZone();
            // can point at cards in own hand and anyone's battlefield
            final boolean canUiPointAtCards = cz != null && (cz.is(ZoneType.Hand) && cz.getPlayer() == player || cz.is(ZoneType.Battlefield));
            if (!canUiPointAtCards) {
                canUseSelectCardsInput = false;
                break;
            }
        }

        if (canUseSelectCardsInput) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
            }
            final InputSelectEntitiesFromList<T> input = new InputSelectEntitiesFromList<T>(this, isOptional ? 0 : 1, 1, optionList);
            input.setCancelAllowed(isOptional);
            input.setMessage(MessageUtil.formatMessage(title, player, targetedPlayer));
            input.showAndWait();
            return Iterables.getFirst(input.getSelected(), null);
        }

        tempShow(optionList);
        if (delayedReveal != null) {
            tempShow(delayedReveal.getCards());
        }
        final GameEntityView result = getGui().chooseSingleEntityForEffect(title, GameEntityView.getEntityCollection(optionList), delayedReveal, isOptional);
        endTempShowCards();

        if (result instanceof CardView) {
            return (T) game.getCard((CardView)result);
        }
        if (result instanceof PlayerView) {
            return (T) game.getPlayer((PlayerView)result);
        }
        return null;
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final int min, final int max) {
        if (min >= max) {
            return min;
        }
        final ImmutableList.Builder<Integer> choices = ImmutableList.builder();
        for (int i = 0; i <= max - min; i++) {
            choices.add(Integer.valueOf(i + min));
        }
        return getGui().one(title, choices.build()).intValue();
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final List<Integer> choices, final Player relatedPlayer) {
        return getGui().one(title, choices).intValue();
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(final List<SpellAbility> spells, final SpellAbility sa, final String title) {
        if (spells.size() < 2) {
            return Iterables.getFirst(spells, null);
        }

        // Show the card that asked for this choice
        getGui().setCard(CardView.get(sa.getHostCard()));

        // Human is supposed to read the message and understand from it what to choose
        return getGui().one(title, spells);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmAction(forge.card.spellability.SpellAbility, java.lang.String, java.lang.String)
     */
    @Override
    public boolean confirmAction(final SpellAbility sa, final PlayerActionConfirmMode mode, final String message) {
        return getGui().confirm(CardView.get(sa.getHostCard()), message);
    }

    @Override
    public boolean confirmBidAction(final SpellAbility sa, final PlayerActionConfirmMode bidlife,
            final String string, final int bid, final Player winner) {
        return getGui().confirm(CardView.get(sa.getHostCard()), string + " Highest Bidder " + winner);
    }

    @Override
    public boolean confirmStaticApplication(final Card hostCard, final GameEntity affected, final String logic, final String message) {
        return getGui().confirm(CardView.get(hostCard), message);
    }

    @Override
    public boolean confirmTrigger(final SpellAbility sa, final Trigger regtrig, final Map<String, String> triggerParams, final boolean isMandatory) {
        if (getGui().shouldAlwaysAcceptTrigger(regtrig.getId())) {
            return true;
        }
        if (getGui().shouldAlwaysDeclineTrigger(regtrig.getId())) {
            return false;
        }

        // triggers with costs can always be declined by not paying the cost
        if (sa.hasParam("Cost") && !sa.getParam("Cost").equals("0")) {
            return true;
        }

        final StringBuilder buildQuestion = new StringBuilder("Use triggered ability of ");
        buildQuestion.append(regtrig.getHostCard().toString()).append("?");
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_PROMPT)) {
            //append trigger description unless prompt is compact
            buildQuestion.append("\n(");
            buildQuestion.append(triggerParams.get("TriggerDescription").replace("CARDNAME", regtrig.getHostCard().getName()));
            buildQuestion.append(")");
        }
        final Map<String, Object> tos = sa.getTriggeringObjects();
        if (tos.containsKey("Attacker")) {
            buildQuestion.append("\nAttacker: " + tos.get("Attacker"));
        }
        if (tos.containsKey("Card")) {
            final Card card = (Card) tos.get("Card");
            if (card != null && (card.getController() == player || game.getZoneOf(card) == null
                    || game.getZoneOf(card).getZoneType().isKnown())) {
                buildQuestion.append("\nTriggered by: " + tos.get("Card"));
            }
        }

        final InputConfirm inp = new InputConfirm(this, buildQuestion.toString());
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public Player chooseStartingPlayer(final boolean isFirstGame) {
        if (game.getPlayers().size() == 2) {
            final String prompt = String.format("%s, you %s\n\nWould you like to play or draw?",
                    player.getName(), isFirstGame ? " have won the coin toss." : " lost the last game.");
            final InputConfirm inp = new InputConfirm(this, prompt, "Play", "Draw");
            inp.showAndWait();
            return inp.getResult() ? this.player : this.player.getOpponents().get(0);
        }
        else {
            final String prompt = String.format("%s, you %s\n\nWho would you like to start this game? (Click on the portrait.)",
                    player.getName(), isFirstGame ? " have won the coin toss." : " lost the last game.");
            final InputSelectEntitiesFromList<Player> input = new InputSelectEntitiesFromList<Player>(this, 1, 1, new FCollection<Player>(game.getPlayersInTurnOrder()));
            input.setMessage(prompt);
            input.showAndWait();
            return input.getFirstSelected();
        }
    }

    @Override
    public CardCollection orderBlockers(final Card attacker, final CardCollection blockers) {
        final CardView vAttacker = CardView.get(attacker);
        getGui().setPanelSelection(vAttacker);
        return game.getCardList(getGui().order("Choose Damage Order for " + vAttacker, "Damaged First", CardView.getCollection(blockers), vAttacker));
    }

    @Override
    public CardCollection orderBlocker(final Card attacker, final Card blocker, final CardCollection oldBlockers) {
        final CardView vAttacker = CardView.get(attacker);
        getGui().setPanelSelection(vAttacker);
        return game.getCardList(getGui().insertInList("Choose blocker after which to place " + vAttacker + " in damage order; cancel to place it first", CardView.get(blocker), CardView.getCollection(oldBlockers)));
    }

    @Override
    public CardCollection orderAttackers(final Card blocker, final CardCollection attackers) {
        final CardView vBlocker = CardView.get(blocker);
        getGui().setPanelSelection(vBlocker);
        return game.getCardList(getGui().order("Choose Damage Order for " + vBlocker, "Damaged First", CardView.getCollection(attackers), vBlocker));
    }

    @Override
    public void reveal(final CardCollectionView cards, final ZoneType zone, final Player owner, final String message) {
        reveal(CardView.getCollection(cards), zone, PlayerView.get(owner), message);
    }

    @Override
    public void reveal(final List<CardView> cards, final ZoneType zone, final PlayerView owner, String message) {
        if (StringUtils.isBlank(message)) {
            message = "Looking at cards in {player's} " + zone.name().toLowerCase();
        } else {
            message += "{player's} " + zone.name().toLowerCase();
        }
        final String fm = MessageUtil.formatMessage(message, getLocalPlayerView(), owner);
        if (!cards.isEmpty()) {
            tempShowCards(game.getCardList(cards));
            getGui().reveal(fm, cards);
            endTempShowCards();
        } else {
            getGui().message(MessageUtil.formatMessage("There are no cards in {player's} " +
                    zone.name().toLowerCase(), player, owner), fm);
        }
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(final CardCollection topN) {
        CardCollection toBottom = null;
        CardCollection toTop = null;

        tempShowCards(topN);
        if (topN.size() == 1) {
            if (willPutCardOnTop(topN.get(0))) {
                toTop = topN;
            }
            else {
                toBottom = topN;
            }
        }
        else {
            toBottom = game.getCardList(getGui().many("Select cards to be put on the bottom of your library", "Cards to put on the bottom", -1, CardView.getCollection(topN), null));
            topN.removeAll((Collection<?>)toBottom);
            if (topN.isEmpty()) {
                toTop = null;
            }
            else if (topN.size() == 1) {
                toTop = topN;
            }
            else {
                toTop = game.getCardList(getGui().order("Arrange cards to be put on top of your library", "Top of Library", CardView.getCollection(topN), null));
            }
        }
        endTempShowCards();
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public boolean willPutCardOnTop(final Card c) {
        final CardView view = CardView.get(c);

        tempShowCard(c);
        final boolean result = getGui().confirm(view, String.format("Put %s on the top or bottom of your library?", view), ImmutableList.of("Top", "Bottom"));
        endTempShowCards();

        return result;
    }

    @Override
    public CardCollectionView orderMoveToZoneList(final CardCollectionView cards, final ZoneType destinationZone) {
        List<CardView> choices;
        tempShowCards(cards);
        switch (destinationZone) {
            case Library:
                choices = getGui().order("Choose order of cards to put into the library", "Closest to top", CardView.getCollection(cards), null);
                break;
            case Battlefield:
                choices = getGui().order("Choose order of cards to put onto the battlefield", "Put first", CardView.getCollection(cards), null);
                break;
            case Graveyard:
                choices = getGui().order("Choose order of cards to put into the graveyard", "Closest to bottom", CardView.getCollection(cards), null);
                break;
            case PlanarDeck:
                choices = getGui().order("Choose order of cards to put into the planar deck", "Closest to top", CardView.getCollection(cards), null);
                break;
            case SchemeDeck:
                choices = getGui().order("Choose order of cards to put into the scheme deck", "Closest to top", CardView.getCollection(cards), null);
                break;
            case Stack:
                choices = getGui().order("Choose order of copies to cast", "Put first", CardView.getCollection(cards), null);
                break;
            default:
                System.out.println("ZoneType " + destinationZone + " - Not Ordered");
                endTempShowCards();
                return cards;
        }
        endTempShowCards();
        return game.getCardList(choices);
    }

    @Override
    public CardCollectionView chooseCardsToDiscardFrom(final Player p, final SpellAbility sa, final CardCollection valid, final int min, final int max) {
        if (p != player) {
            tempShowCards(valid);
            final CardCollection choices = game.getCardList(getGui().many("Choose " + min + " card" + (min != 1 ? "s" : "") + " to discard",
                    "Discarded", min, min, CardView.getCollection(valid), null));
            endTempShowCards();
            return choices;
        }

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage(sa.hasParam("AnyNumber") ? "Discard up to %d card(s)" : "Discard %d card(s)");
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public void playMiracle(final SpellAbility miracle, final Card card) {
        final CardView view = CardView.get(card);
        if (getGui().confirm(view, view + " - Drawn. Play for Miracle Cost?")) {
            HumanPlay.playSpellAbility(this, player, miracle);
        }
    }

    @Override
    public CardCollectionView chooseCardsToDelve(final int genericAmount, final CardCollection grave) {
        final int cardsInGrave = Math.min(genericAmount, grave.size());
        if (cardsInGrave == 0) {
            return CardCollection.EMPTY;
        }

        final CardCollection toExile = new CardCollection();
        final ImmutableList.Builder<Integer> cntChoice = ImmutableList.builder();
        for (int i = 0; i <= cardsInGrave; i++) {
            cntChoice.add(Integer.valueOf(i));
        }
        final int chosenAmount = getGui().one("Delve how many cards?", cntChoice.build()).intValue();
        for (int i = 0; i < chosenAmount; i++) {
            final CardView nowChosen = getGui().oneOrNone("Exile which card?", CardView.getCollection(grave));

            if (nowChosen == null) {
                // User canceled,abort delving.
                toExile.clear();
                break;
            }

            final Card card = game.getCard(nowChosen);
            grave.remove(card);
            toExile.add(card);
        }
        return toExile;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseTargets(forge.card.spellability.SpellAbility, forge.card.spellability.SpellAbilityStackInstance)
     */
    @Override
    public TargetChoices chooseNewTargetsFor(final SpellAbility ability) {
        final SpellAbility sa = ability.isWrapper() ? ((WrappedAbility) ability).getWrappedAbility() : ability;
        if (sa.getTargetRestrictions() == null) {
            return null;
        }
        final TargetChoices oldTarget = sa.getTargets();
        final TargetSelection select = new TargetSelection(this, sa);
        sa.resetTargets();
        if (select.chooseTargets(oldTarget.getNumTargeted())) {
            return sa.getTargets();
        }
        else {
            // Return old target, since we had to reset them above
            return oldTarget;
        }
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseCardsToDiscardUnlessType(int, java.lang.String, forge.card.spellability.SpellAbility)
     */
    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(final int num, final CardCollectionView hand, final String uType, final SpellAbility sa) {
        final InputSelectEntitiesFromList<Card> target = new InputSelectEntitiesFromList<Card>(this, num, num, hand) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasAllTargets() {
                for (final Card c : selected) {
                    if (c.getType().hasStringType(uType)) {
                        return true;
                    }
                }
                return super.hasAllTargets();
            }
        };
        target.setMessage("Select %d card(s) to discard, unless you discard a " + uType + ".");
        target.showAndWait();
        return new CardCollection(target.getSelected());
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseManaFromPool(java.util.List)
     */
    @Override
    public Mana chooseManaFromPool(final List<Mana> manaChoices) {
        final List<String> options = new ArrayList<String>();
        for (int i = 0; i < manaChoices.size(); i++) {
            final Mana m = manaChoices.get(i);
            options.add(String.format("%d. %s mana from %s", 1+i, MagicColor.toLongString(m.getColor()), m.getSourceCard()));
        }
        final String chosen = getGui().one("Pay Mana from Mana Pool", options);
        final String idx = TextUtil.split(chosen, '.')[0];
        return manaChoices.get(Integer.parseInt(idx)-1);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseSomeType(java.lang.String, java.lang.String, java.util.List, java.util.List, java.lang.String)
     */
    @Override
    public String chooseSomeType(final String kindOfType, final SpellAbility sa, final List<String> validTypes,  final List<String> invalidTypes, final boolean isOptional) {
        final List<String> types = Lists.newArrayList(validTypes);
        if (invalidTypes != null && !invalidTypes.isEmpty()) {
            Iterables.removeAll(types, invalidTypes);
        }
        if (kindOfType.equals("Creature")) {
            sortCreatureTypes(types);
        }
        if (isOptional) {
            return getGui().oneOrNone("Choose a " + kindOfType.toLowerCase() + " type", types);
        }
        return getGui().one("Choose a " + kindOfType.toLowerCase() + " type", types);
    }

    //sort creature types such that those most prevalent in player's deck are sorted to the top
    private void sortCreatureTypes(List<String> types) {
        //build map of creature types in player's main deck against the occurrences of each
        CardPool pool = player.getRegisteredPlayer().getDeck().getMain();
        HashMap<String, Integer> typesInDeck = new HashMap<String, Integer>();
        for (Entry<PaperCard, Integer> entry : pool) {
            Set<String> cardCreatureTypes = entry.getKey().getRules().getType().getCreatureTypes();
            for (String type : cardCreatureTypes) {
                Integer count = typesInDeck.get(type);
                if (count == null) { count = 0; }
                typesInDeck.put(type, count + entry.getValue());
            }
        }

        //create sorted list from map from least to most frequent 
        List<Entry<String, Integer>> sortedList = new LinkedList<Entry<String, Integer>>(typesInDeck.entrySet());
        Collections.sort(sortedList, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        //loop through sorted list and move each type to the front of the validTypes collection
        for (Entry<String, Integer> entry : sortedList) {
            String type = entry.getKey();
            if (types.remove(type)) { //ensure an invalid type isn't introduced
                types.add(0, type);
            }
        }
    }

    @Override
    public Object vote(final SpellAbility sa, final String prompt, final List<Object> options, final ListMultimap<Object, Player> votes) {
        return getGui().one(prompt, options);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmReplacementEffect(forge.card.replacement.ReplacementEffect, forge.card.spellability.SpellAbility, java.lang.String)
     */
    @Override
    public boolean confirmReplacementEffect(final ReplacementEffect replacementEffect, final SpellAbility effectSA, final String question) {
        return getGui().confirm(CardView.get(replacementEffect.getHostCard()), question);
    }

    @Override
    public CardCollectionView getCardsToMulligan(final Player firstPlayer) {
        // Partial Paris is gone, so it being commander doesn't really matter anymore...
        final InputConfirmMulligan inp = new InputConfirmMulligan(this, player, firstPlayer);
        inp.showAndWait();
        return inp.isKeepHand() ? null : player.getCardsIn(ZoneType.Hand);
    }

    @Override
    public void declareAttackers(final Player attackingPlayer, final Combat combat) {
        if (mayAutoPass()) {
            if (CombatUtil.validateAttackers(combat)) {
                return; //don't prompt to declare attackers if user chose to end the turn and not attacking is legal
            } else {
                autoPassCancel(); //otherwise: cancel auto pass because of this unexpected attack
            }
        }

        // This input should not modify combat object itself, but should return user choice
        final InputAttack inpAttack = new InputAttack(this, attackingPlayer, combat);
        inpAttack.showAndWait();
    }

    @Override
    public void declareBlockers(final Player defender, final Combat combat) {
        // This input should not modify combat object itself, but should return user choice
        final InputBlock inpBlock = new InputBlock(this, defender, combat);
        inpBlock.showAndWait();
        getGui().updateAutoPassPrompt();
    }


    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        final MagicStack stack = game.getStack();

        if (mayAutoPass()) {
            //avoid prompting for input if current phase is set to be auto-passed
            //instead posing a short delay if needed to prevent the game jumping ahead too quick
            int delay = 0;
            if (stack.isEmpty()) {
                //make sure to briefly pause at phases you're not set up to skip
                if (!getGui().isUiSetToSkipPhase(game.getPhaseHandler().getPlayerTurn().getView(), game.getPhaseHandler().getPhase())) {
                    delay = FControlGamePlayback.phasesDelay;
                }
            }
            else {
                //pause slightly longer for spells and abilities on the stack resolving
                delay = FControlGamePlayback.resolveDelay;
            }
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        if (stack.isEmpty()) {
            if (getGui().isUiSetToSkipPhase(game.getPhaseHandler().getPlayerTurn().getView(), game.getPhaseHandler().getPhase())) {
                return null; //avoid prompt for input if stack is empty and player is set to skip the current phase
            }
        } else {
            final SpellAbility ability = stack.peekAbility();
            if (ability != null && ability.isAbility() && getGui().shouldAutoYield(ability.toUnsuppressedString())) {
                //avoid prompt for input if top ability of stack is set to auto-yield
                try {
                    Thread.sleep(FControlGamePlayback.resolveDelay);
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        final InputPassPriority defaultInput = new InputPassPriority(this);
        defaultInput.showAndWait();
        return defaultInput.getChosenSa();
    }

    @Override
    public void playChosenSpellAbility(final SpellAbility chosenSa) {
        HumanPlay.playSpellAbility(this, player, chosenSa);
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(final int nDiscard) {
        final int max = player.getMaxHandSize();

        @SuppressWarnings("serial")
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, nDiscard, nDiscard, player.getZone(ZoneType.Hand).getCards()) {
            @Override
            protected final boolean allowAwaitNextInput() {
                return true; //prevent Cleanup message getting stuck during opponent's next turn
            }
        };
        final String message = "Cleanup Phase\nSelect " + nDiscard + " card" + (nDiscard > 1 ? "s" : "") +
                " to discard to bring your hand down to the maximum of " + max + " cards.";
        inp.setMessage(message);
        inp.setCancelAllowed(false);
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, final CardCollectionView valid) {
        max = Math.min(max, valid.size());
        min = Math.min(min, max);
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage("Choose Which Cards to Reveal");
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public boolean payManaOptional(final Card c, final Cost cost, final SpellAbility sa, final String prompt, final ManaPaymentPurpose purpose) {
        if (sa == null && cost.isOnlyManaCost() && cost.getTotalMana().isZero()
                && !FModel.getPreferences().getPrefBoolean(FPref.MATCHPREF_PROMPT_FREE_BLOCKS)) {
            return true;
        }
        return HumanPlay.payCostDuringAbilityResolve(this, player, c, cost, sa, prompt);
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(final List<SpellAbility> usableFromOpeningHand) {
        final CardCollection srcCards = new CardCollection();
        for (final SpellAbility sa : usableFromOpeningHand) {
            srcCards.add(sa.getHostCard());
        }
        final List<SpellAbility> result = new ArrayList<SpellAbility>();
        if (srcCards.isEmpty()) {
            return result;
        }
        final List<CardView> chosen = getGui().many("Choose cards to activate from opening hand and their order", "Activate first", -1, CardView.getCollection(srcCards), null);
        for (final CardView view : chosen) {
            final Card c = game.getCard(view);
            for (final SpellAbility sa : usableFromOpeningHand) {
                if (sa.getHostCard() == c) {
                    result.add(sa);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean chooseBinary(final SpellAbility sa, final String question, final BinaryChoiceType kindOfChoice, final Boolean defaultVal) {
        final List<String> labels;
        switch (kindOfChoice) {
            case HeadsOrTails:       labels = ImmutableList.of("Heads",                      "Tails");        break;
            case TapOrUntap:         labels = ImmutableList.of("Tap",                        "Untap");        break;
            case OddsOrEvens:        labels = ImmutableList.of("Odds",                       "Evens");        break;
            case UntapOrLeaveTapped: labels = ImmutableList.of("Untap",                      "Leave tapped"); break;
            case UntapTimeVault:     labels = ImmutableList.of("Untap (and skip this turn)", "Leave tapped"); break;
            case PlayOrDraw:         labels = ImmutableList.of("Play",                       "Draw");         break;
            default:                 labels = ImmutableList.copyOf(kindOfChoice.toString().split("Or"));
        }
        return getGui().confirm(CardView.get(sa.getHostCard()), question, defaultVal == null || defaultVal.booleanValue(), labels);
    }

    @Override
    public boolean chooseFlipResult(final SpellAbility sa, final Player flipper, final boolean[] results, final boolean call) {
        final String[] labelsSrc = call ? new String[]{"heads", "tails"} : new String[]{"win the flip", "lose the flip"};
        final ImmutableList.Builder<String> strResults = ImmutableList.<String>builder();
        for (int i = 0; i < results.length; i++) {
            strResults.add(labelsSrc[results[i] ? 0 : 1]);
        }
        return getGui().one(sa.getHostCard().getName() + " - Choose a result", strResults.build()).equals(labelsSrc[0]);
    }

    @Override
    public Card chooseProtectionShield(final GameEntity entityBeingDamaged, final List<String> options, final Map<String, Card> choiceMap) {
        final String title = entityBeingDamaged + " - select which prevention shield to use";
        return choiceMap.get(getGui().one(title, options));
    }

    @Override
    public Pair<CounterType,String> chooseAndRemoveOrPutCounter(final Card cardWithCounter) {
        if (!cardWithCounter.hasCounters()) {
            System.out.println("chooseCounterType was reached with a card with no counters on it. Consider filtering this card out earlier");
            return null;
        }

        final String counterChoiceTitle = "Choose a counter type on " + cardWithCounter;
        final CounterType chosen = getGui().one(counterChoiceTitle, ImmutableList.copyOf(cardWithCounter.getCounters().keySet()));

        final String putOrRemoveTitle = "What to do with that '" + chosen.getName() + "' counter ";
        final String putString = "Put another " + chosen.getName() + " counter on " + cardWithCounter;
        final String removeString = "Remove a " + chosen.getName() + " counter from " + cardWithCounter;
        final String addOrRemove = getGui().one(putOrRemoveTitle, ImmutableList.of(putString, removeString));

        return new ImmutablePair<CounterType,String>(chosen,addOrRemove);
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(final SpellAbility saSpellskite, final List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        if (allTargets.size() < 2) {
            return Iterables.getFirst(allTargets, null);
        }

        final List<Pair<SpellAbilityStackInstance, GameObject>> chosen = getGui().getChoices(saSpellskite.getHostCard().getName(), 1, 1, allTargets, null, new FnTargetToString());
        return Iterables.getFirst(chosen, null);
    }

    private final static class FnTargetToString implements Function<Pair<SpellAbilityStackInstance, GameObject>, String>, Serializable {
        private static final long serialVersionUID = -4779137632302777802L;

        @Override public String apply(final Pair<SpellAbilityStackInstance, GameObject> targ) {
            return targ.getRight().toString() + " - " + targ.getLeft().getStackDescription();
        }
    }

    @Override
    public void notifyOfValue(final SpellAbility sa, final GameObject realtedTarget, final String value) {
        final String message = MessageUtil.formatNotificationMessage(sa, player, realtedTarget, value);
        if (sa != null && sa.isManaAbility()) {
            game.getGameLog().add(GameLogEntryType.LAND, message);
        } else {
            getGui().message(message, sa == null || sa.getHostCard() == null ? "" : CardView.get(sa.getHostCard()).toString());
        }
    }

    // end of not related candidates for move.

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseModeForAbility(forge.card.spellability.SpellAbility, java.util.List, int, int)
     */
    @Override
    public List<AbilitySub> chooseModeForAbility(final SpellAbility sa, final int min, final int num, boolean allowRepeat) {
        final List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
        final String modeTitle = String.format("%s activated %s - Choose a mode", sa.getActivatingPlayer(), sa.getHostCard());
        final List<AbilitySub> chosen = Lists.newArrayListWithCapacity(num);
        for (int i = 0; i < num; i++) {
            AbilitySub a;
            if (i < min) {
                a = getGui().one(modeTitle, choices);
            }
            else {
                a = getGui().oneOrNone(modeTitle, choices);
            }
            if (a == null) {
                break;
            }

            if (!allowRepeat) {
                choices.remove(a);
            }
            chosen.add(a);
        }
        return chosen;
    }

    @Override
    public List<String> chooseColors(final String message, final SpellAbility sa, final int min, final int max, final List<String> options) {
        return getGui().getChoices(message, min, max, options);
    }

    @Override
    public byte chooseColor(final String message, final SpellAbility sa, final ColorSet colors) {
        final int cntColors = colors.countColors();
        switch (cntColors) {
            case 0: return 0;
            case 1: return colors.getColor();
            default: return chooseColorCommon(message, sa == null ? null : sa.getHostCard(), colors, false);
        }
    }

    @Override
    public byte chooseColorAllowColorless(final String message, final Card c, final ColorSet colors) {
        final int cntColors = 1 + colors.countColors();
        switch (cntColors) {
            case 1: return 0;
            default: return chooseColorCommon(message, c, colors, true);
        }
    }

    private byte chooseColorCommon(final String message, final Card c, final ColorSet colors, final boolean withColorless) {
        final ImmutableList.Builder<String> colorNamesBuilder = ImmutableList.builder();
        if (withColorless) {
            colorNamesBuilder.add(MagicColor.toLongString(MagicColor.COLORLESS));
        }
        for (final Byte b : colors) {
            colorNamesBuilder.add(MagicColor.toLongString(b.byteValue()));
        }
        final ImmutableList<String> colorNames = colorNamesBuilder.build();
        if (colorNames.size() > 2) {
            return MagicColor.fromName(getGui().one(message, colorNames));
        }
        final int idxChosen = getGui().confirm(CardView.get(c), message, colorNames) ? 0 : 1;
        return MagicColor.fromName(colorNames.get(idxChosen));
    }

    @Override
    public PaperCard chooseSinglePaperCard(final SpellAbility sa, final String message, final Predicate<PaperCard> cpp, final String name) {
        final Iterable<PaperCard> cardsFromDb = FModel.getMagicDb().getCommonCards().getUniqueCards();
        final List<PaperCard> cards = Lists.newArrayList(Iterables.filter(cardsFromDb, cpp));
        Collections.sort(cards);
        return getGui().one(message, cards);
    }

    @Override
    public CounterType chooseCounterType(final List<CounterType> options, final SpellAbility sa, final String prompt) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return getGui().one(prompt, options);
    }

    @Override
    public boolean confirmPayment(final CostPart costPart, final String question) {
        final InputConfirm inp = new InputConfirm(this, question);
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(final String prompt, final List<ReplacementEffect> possibleReplacers, final Map<String, Object> runParams) {
        final ReplacementEffect first = possibleReplacers.get(0);
        if (possibleReplacers.size() == 1) {
            return first;
        }
        final String firstStr = first.toString();
        for (int i = 1; i < possibleReplacers.size(); i++) {
            if (!possibleReplacers.get(i).toString().equals(firstStr)) {
                return getGui().one(prompt, possibleReplacers); //prompt user if there are multiple different options
            }
        }
        return first; //return first option without prompting if all options are the same
    }

    @Override
    public String chooseProtectionType(final String string, final SpellAbility sa, final List<String> choices) {
        return getGui().one(string, choices);
    }

    @Override
    public boolean payCostToPreventEffect(final Cost cost, final SpellAbility sa, final boolean alreadyPaid, final FCollectionView<Player> allPayers) {
        // if it's paid by the AI already the human can pay, but it won't change anything
        return HumanPlay.payCostDuringAbilityResolve(this, player, sa.getHostCard(), cost, sa, null);
    }

    //stores saved order for different sets of SpellAbilities
    private final HashMap<String, List<Integer>> orderedSALookup = new HashMap<String, List<Integer>>();

    @Override
    public void orderAndPlaySimultaneousSa(final List<SpellAbility> activePlayerSAs) {
        List<SpellAbility> orderedSAs = activePlayerSAs;
        if (activePlayerSAs.size() > 1) {
            final String firstStr = orderedSAs.get(0).toString();
            boolean needPrompt = false;
            String saLookupKey = firstStr;
            char delim = (char)5;
            for (int i = 1; i < orderedSAs.size(); i++) {
                final String saStr = orderedSAs.get(i).toString();
                if (!needPrompt && !saStr.equals(firstStr)) {
                    needPrompt = true; //prompt by default unless all abilities are the same
                }
                saLookupKey += delim + saStr;
            }
            if (needPrompt) {
                List<Integer> savedOrder = orderedSALookup.get(saLookupKey);
                boolean sameOrder = false;

                if (savedOrder != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Would you like to keep the same order for simultaneous abilities as last time?\n\n");
                    int c = 0;
                    for (Integer index : savedOrder) {
                        if (c > 9) {
                            // do not list more than ten abilities to avoid overloading the prompt box
                            sb.append("<...>\n");
                            break;
                        }
                        sb.append(++c + ". " + activePlayerSAs.get(index).getHostCard() + "\n");
                    }
                    sameOrder = getGui().showConfirmDialog(sb.toString(), "Ordering simultaneous abilities", true);
                }

                if (savedOrder == null || !sameOrder) { //prompt if no saved order for the current set of abilities or if the player wants to change the order
                    orderedSAs = getGui().order("Select order for simultaneous abilities", "Resolve first", activePlayerSAs, null);
                    //save order to avoid needing to prompt a second time to order the same abilties
                    savedOrder = new ArrayList<Integer>(activePlayerSAs.size());
                    for (SpellAbility sa : activePlayerSAs) {
                        savedOrder.add(orderedSAs.indexOf(sa));
                    }
                    orderedSALookup.put(saLookupKey, savedOrder);
                }
                else { //avoid prompt and just apply saved order
                    orderedSAs = new ArrayList<SpellAbility>();
                    for (Integer index : savedOrder) {
                        orderedSAs.add(activePlayerSAs.get(index));
                    }
                }
            }
        }
        for (int i = orderedSAs.size() - 1; i >= 0; i--) {
            final SpellAbility next = orderedSAs.get(i);
            if (next.isTrigger()) {
                HumanPlay.playSpellAbility(this, player, next);
            }
            else {
                player.getGame().getStack().add(next);
            }
        }
    }

    @Override
    public void playTrigger(final Card host, final WrappedAbility wrapperAbility, final boolean isMandatory) {
        HumanPlay.playSpellAbilityNoStack(this, player, wrapperAbility);
    }

    @Override
    public boolean playSaFromPlayEffect(final SpellAbility tgtSA) {
        return HumanPlay.playSpellAbility(this, player, tgtSA);
    }

    @Override
    public Map<GameEntity, CounterType> chooseProliferation() {
        final InputProliferate inp = new InputProliferate(this);
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return inp.getProliferationMap();
    }

    @Override
    public boolean chooseTargetsFor(final SpellAbility currentAbility) {
        final TargetSelection select = new TargetSelection(this, currentAbility);
        return select.chooseTargets(null);
    }

    @Override
    public boolean chooseCardsPile(final SpellAbility sa, final CardCollectionView pile1, final CardCollectionView pile2, final boolean faceUp) {
        if (!faceUp) {
            final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
            final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());
            final List<String> possibleValues = ImmutableList.of(p1Str , p2Str);
            return getGui().confirm(CardView.get(sa.getHostCard()), "Choose a Pile", possibleValues);
        }

        tempShowCards(pile1);
        tempShowCards(pile2);

        final List<CardView> cards = Lists.newArrayListWithCapacity(pile1.size() + pile2.size() + 2);
        final CardView pileView1 = new CardView(Integer.MIN_VALUE, null, "--- Pile 1 ---");
        cards.add(pileView1);
        cards.addAll(CardView.getCollection(pile1));

        final CardView pileView2 = new CardView(Integer.MIN_VALUE + 1, null, "--- Pile 2 ---");
        cards.add(pileView2);
        cards.addAll(CardView.getCollection(pile2));

        // make sure Pile 1 or Pile 2 is clicked on
        boolean result;
        while (true) {
            final CardView chosen = getGui().one("Choose a pile", cards);
            if (chosen.equals(pileView1)) {
                result = true;
                break;
            }
            if (chosen.equals(pileView2)) {
                result = false;
                break;
            }
        }

        endTempShowCards();
        return result;
    }

    @Override
    public void revealAnte(final String message, final Multimap<Player, PaperCard> removedAnteCards) {
        for (final Player p : removedAnteCards.keySet()) {
            getGui().reveal(message + " from " + Lang.getPossessedObject(MessageUtil.mayBeYou(player, p), "deck"), ImmutableList.copyOf(removedAnteCards.get(p)));
        }
    }

    @Override
    public CardShields chooseRegenerationShield(final Card c) {
        if (c.getShieldCount() < 2) {
            return Iterables.getFirst(c.getShields(), null);
        }
        final List<CardShields> shields = new ArrayList<CardShields>();
        for (final CardShields shield : c.getShields()) {
            shields.add(shield);
        }
        return getGui().one("Choose a regeneration shield:", shields);
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(final List<PaperCard> losses) {
        return getGui().many("Select cards to add to your deck", "Add these to my deck", 0, losses.size(), losses, null);
    }

    @Override
    public boolean payManaCost(final ManaCost toPay, final CostPartMana costPartMana, final SpellAbility sa, final String prompt, final boolean isActivatedSa) {
        return HumanPlay.payManaCost(this, toPay, costPartMana, sa, player, prompt, isActivatedSa);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(final SpellAbility sa, final ManaCost manaCost, final CardCollectionView untappedCreats) {
        final InputSelectCardsForConvoke inp = new InputSelectCardsForConvoke(this, player, manaCost, untappedCreats);
        inp.showAndWait();
        return inp.getConvokeMap();
    }

    @Override
    public String chooseCardName(final SpellAbility sa, final Predicate<PaperCard> cpp, final String valid, final String message) {
        while (true) {
            final PaperCard cp = chooseSinglePaperCard(sa, message, cpp, sa.getHostCard().getName());
            final Card instanceForPlayer = Card.fromPaperCard(cp, player); // the Card instance for test needs a game to be tested
            if (instanceForPlayer.isValid(valid, sa.getHostCard().getController(), sa.getHostCard(), sa)) {
                return cp.getName();
            }
        }
    }

    @Override
    public Card chooseSingleCardForZoneChange(final ZoneType destination, final List<ZoneType> origin, final SpellAbility sa, final CardCollection fetchList, final DelayedReveal delayedReveal, final String selectPrompt, final boolean isOptional, final Player decider) {
        return chooseSingleEntityForEffect(fetchList, delayedReveal, sa, selectPrompt, isOptional, decider);
    }

    @Override
    public boolean isGuiPlayer() {
        return lobbyPlayer == GamePlayerUtil.getGuiPlayer();
    }

    public void updateAchievements() {
        AchievementCollection.updateAll(this);
    }

    public boolean canUndoLastAction() {
        if (!game.stack.canUndo(player)) {
            return false;
        }
        final Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
        if (priorityPlayer == null || priorityPlayer != player) {
            return false;
        }
        return true;
    }


    @Override
    public void undoLastAction() {
        tryUndoLastAction();
    }

    public boolean tryUndoLastAction() {
        if (!canUndoLastAction()) {
            return false;
        }

        if (game.getStack().undo()) {
            final Input currentInput = inputQueue.getInput();
            if (currentInput instanceof InputPassPriority) {
                currentInput.showMessageInitial(); //ensure prompt updated if needed
            }
            return true;
        }
        return false;
    }

    @Override
    public void selectButtonOk() {
        inputProxy.selectButtonOK();
    }

    @Override
    public void selectButtonCancel() {
        inputProxy.selectButtonCancel();
    }

    public void confirm() {
        if (inputQueue.getInput() instanceof InputConfirm) {
            selectButtonOk();
        }
    }

    @Override
    public void passPriority() {
        passPriority(false);
    }
    @Override
    public void passPriorityUntilEndOfTurn() {
        passPriority(true);
    }
    private void passPriority(final boolean passUntilEndOfTurn) {
        final Input inp = inputProxy.getInput();
        if (inp instanceof InputPassPriority) {
            if (passUntilEndOfTurn) {
                autoPassUntilEndOfTurn();
            }
            inp.selectButtonOK();
        } else {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override public final void run() {
                    //getGui().message("Cannot pass priority at this time.");
                }
            });
        }
    }

    @Override
    public void useMana(final byte mana) {
        final Input input = inputQueue.getInput();
        if (input instanceof InputPayMana) {
            ((InputPayMana) input).useManaFromPool(mana);
        }
    }

    @Override
    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        inputProxy.selectPlayer(playerView, triggerEvent);
    }

    @Override
    public boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect, final ITriggerEvent triggerEvent) {
        return inputProxy.selectCard(cardView, otherCardViewsToSelect, triggerEvent);
    }

    @Override
    public void selectAbility(final SpellAbilityView sa) {
        inputProxy.selectAbility(getGame().getSpellAbility(sa));
    }

    @Override
    public void alphaStrike() {
        inputProxy.alphaStrike();
    }

    @Override
    public void resetAtEndOfTurn() {
        // Not used by the human controller
    }

    //Dev Mode cheat functions
    private boolean canPlayUnlimitedLands;
    @Override
    public boolean canPlayUnlimitedLands() {
        return canPlayUnlimitedLands;
    }
    private IDevModeCheats cheats;
    @Override
    public IDevModeCheats cheat() {
        if (cheats == null) {
            cheats = new DevModeCheats();
            //TODO: In Network game, inform other players that this player is cheating
        }
        return cheats;
    }
    public boolean hasCheated() {
        return cheats != null;
    }
    public class DevModeCheats implements IDevModeCheats {
        private DevModeCheats() {
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#setCanPlayUnlimitedLands(boolean)
         */
        @Override
        public void setCanPlayUnlimitedLands(final boolean canPlayUnlimitedLands0) {
            canPlayUnlimitedLands = canPlayUnlimitedLands0;
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#setViewAllCards(boolean)
         */
        @Override
        public void setViewAllCards(final boolean canViewAll) {
            mayLookAtAllCards = canViewAll;
            for (final Player p : game.getPlayers()) {
                getGui().updateCards(CardView.getCollection(p.getAllCards()));
            }
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#generateMana()
         */
        @Override
        public void generateMana() {
            final Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message("No player has priority at the moment, so mana cannot be added to their pool.");
                return;
            }

            final Card dummy = new Card(-777777, game);
            dummy.setOwner(pPriority);
            final Map<String, String> produced = new HashMap<String, String>();
            produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
            final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
            game.getAction().invoke(new Runnable() {
                @Override public void run() { abMana.produceMana(null); }
            });
        }

        private GameState createGameStateObject() {
            return new GameState() {
                @Override public IPaperCard getPaperCard(final String cardName) {
                    return FModel.getMagicDb().getCommonCards().getCard(cardName);
                }
            };
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#dumpGameState()
         */
        @Override
        public void dumpGameState() {
            final GameState state = createGameStateObject();
            try {
                state.initFromGame(game);
                final File f = GuiBase.getInterface().getSaveFile(new File(ForgeConstants.USER_GAMES_DIR, "state.txt"));
                if (f != null && (!f.exists() || getGui().showConfirmDialog("Overwrite existing file?", "File exists!"))) {
                    final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                    bw.write(state.toString());
                    bw.close();
                }
            } catch (final Exception e) {
                String err = e.getClass().getName();
                if (e.getMessage() != null) {
                    err += ": " + e.getMessage();
                }
                getGui().showErrorDialog(err);
                e.printStackTrace();
            }
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#setupGameState()
         */
        @Override
        public void setupGameState() {
            final File gamesDir = new File(ForgeConstants.USER_GAMES_DIR);
            if (!gamesDir.exists()) { // if the directory does not exist, try to create it
                gamesDir.mkdir();
            }

            final String filename = GuiBase.getInterface().showFileDialog("Select Game State File", ForgeConstants.USER_GAMES_DIR);
            if (filename == null) {
                return;
            }

            final GameState state = createGameStateObject();
            try {
                final FileInputStream fstream = new FileInputStream(filename);
                state.parse(fstream);
                fstream.close();
            }
            catch (final FileNotFoundException fnfe) {
                SOptionPane.showErrorDialog("File not found: " + filename);
            }
            catch (final Exception e) {
                SOptionPane.showErrorDialog("Error loading battle setup file!");
                return;
            }

            final Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message("No player has priority at the moment, so game state cannot be setup.");
                return;
            }
            state.applyToGame(game);
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#tutorForCard()
         */
        @Override
        public void tutorForCard() {
            final Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message("No player has priority at the moment, so their deck can't be tutored from.");
                return;
            }

            final CardCollection lib = (CardCollection)pPriority.getCardsIn(ZoneType.Library);
            final List<ZoneType> origin = new ArrayList<ZoneType>();
            origin.add(ZoneType.Library);
            final SpellAbility sa = new SpellAbility.EmptySa(new Card(-1, game));
            final Card card = chooseSingleCardForZoneChange(ZoneType.Hand, origin, sa, lib, null, "Choose a card", true, pPriority);
            if (card == null) { return; }

            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    game.getAction().moveToHand(card);
                }
            });
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#addCountersToPermanent()
         */
        @Override
        public void addCountersToPermanent() {
            final CardCollectionView cards = game.getCardsIn(ZoneType.Battlefield);
            final Card card = game.getCard(getGui().oneOrNone("Add counters to which card?", CardView.getCollection(cards)));
            if (card == null) { return; }

            final CounterType counter = getGui().oneOrNone("Which type of counter?", CounterType.values);
            if (counter == null) { return; }

            final Integer count = getGui().getInteger("How many counters?", 1, Integer.MAX_VALUE, 10);
            if (count == null) { return; }

            card.addCounter(counter, count, false);
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#tapPermanents()
         */
        @Override
        public void tapPermanents() {
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    final CardCollectionView untapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Predicates.not(CardPredicates.Presets.TAPPED));
                    final InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0, Integer.MAX_VALUE, untapped);
                    inp.setCancelAllowed(true);
                    inp.setMessage("Choose permanents to tap");
                    inp.showAndWait();
                    if (!inp.hasCancelled()) {
                        for (final Card c : inp.getSelected()) {
                            c.tap();
                        }
                    }
                }
            });
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#untapPermanents()
         */
        @Override
        public void untapPermanents() {
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    final CardCollectionView tapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.TAPPED);
                    final InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0, Integer.MAX_VALUE, tapped);
                    inp.setCancelAllowed(true);
                    inp.setMessage("Choose permanents to untap");
                    inp.showAndWait();
                    if (!inp.hasCancelled()) {
                        for (final Card c : inp.getSelected()) {
                            c.untap();
                        }
                    }
                }
            });
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#setPlayerLife()
         */
        @Override
        public void setPlayerLife() {
            final Player player = game.getPlayer(getGui().oneOrNone("Set life for which player?", PlayerView.getCollection(game.getPlayers())));
            if (player == null) { return; }

            final Integer life = getGui().getInteger("Set life to what?", 0);
            if (life == null) { return; }

            player.setLife(life, null);
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#winGame()
         */
        @Override
        public void winGame() {
            final Input input = inputQueue.getInput();
            if (!(input instanceof InputPassPriority)) {
                getGui().message("You must have priority to use this feature.", "Win Game");
                return;
            }

            //set life of all other players to 0
            final LobbyPlayer guiPlayer = getLobbyPlayer();
            final FCollectionView<Player> players = game.getPlayers();
            for (final Player player : players) {
                if (player.getLobbyPlayer() != guiPlayer) {
                    player.setLife(0, null);
                }
            }

            //pass priority so that causes gui player to win
            input.selectButtonOK();
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#addCardToHand()
         */
        @Override
        public void addCardToHand() {
            final Player p = game.getPlayer(getGui().oneOrNone("Put card in hand for which player?", PlayerView.getCollection(game.getPlayers())));
            if (p == null) {
                return;
            }

            final List<PaperCard> cards =  Lists.newArrayList(FModel.getMagicDb().getCommonCards().getUniqueCards());
            Collections.sort(cards);

            // use standard forge's list selection dialog
            final IPaperCard c = getGui().oneOrNone("Name the card", cards);
            if (c == null) {
                return;
            }

            game.getAction().invoke(new Runnable() { @Override public void run() {
                game.getAction().moveToHand(Card.fromPaperCard(c, p));
            }});
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#addCardToBattlefield()
         */
        @Override
        public void addCardToBattlefield() {
            final Player p = game.getPlayer(getGui().oneOrNone("Put card in play for which player?", PlayerView.getCollection(game.getPlayers())));
            if (p == null) {
                return;
            }

            final List<PaperCard> cards =  Lists.newArrayList(FModel.getMagicDb().getCommonCards().getUniqueCards());
            Collections.sort(cards);

            // use standard forge's list selection dialog
            final IPaperCard c = getGui().oneOrNone("Name the card", cards);
            if (c == null) {
                return;
            }

            game.getAction().invoke(new Runnable() {
                @Override public void run() {
                    final Card forgeCard = Card.fromPaperCard(c, p);

                    if (c.getRules().getType().isLand()) {
                        game.getAction().moveToHand(forgeCard); //this is needed to ensure land abilities fire
                        game.getAction().moveToPlay(forgeCard);
                        game.getTriggerHandler().runWaitingTriggers(); //ensure triggered abilities fire
                    }
                    else {
                        final FCollectionView<SpellAbility> choices = forgeCard.getBasicSpells();
                        if (choices.isEmpty()) {
                            return; // when would it happen?
                        }

                        final SpellAbility sa;
                        if (choices.size() == 1) {
                            sa = choices.iterator().next();
                        }
                        else {
                            sa = getGui().oneOrNone("Choose", (FCollection<SpellAbility>)choices);
                        }
                        if (sa == null) {
                            return; // happens if cancelled
                        }

                        game.getAction().moveToHand(forgeCard); // this is really needed (for rollbacks at least)
                        // Human player is choosing targets for an ability controlled by chosen player.
                        sa.setActivatingPlayer(p);
                        HumanPlay.playSaWithoutPayingManaCost(PlayerControllerHuman.this, game, sa, true);
                    }
                    game.getStack().addAllTriggeredAbilitiesToStack(); // playSa could fire some triggers
                }
            });
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#riggedPlanarRoll()
         */
        @Override
        public void riggedPlanarRoll() {
            final Player player = game.getPlayer(getGui().oneOrNone("Which player should roll?", PlayerView.getCollection(game.getPlayers())));
            if (player == null) { return; }

            final PlanarDice res = getGui().oneOrNone("Choose result", PlanarDice.values);
            if (res == null) { return; }

            System.out.println("Rigging planar dice roll: " + res.toString());

            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    PlanarDice.roll(player, res);
                }
            });
        }

        /* (non-Javadoc)
         * @see forge.player.IDevModeCheats#planeswalkTo()
         */
        @Override
        public void planeswalkTo() {
            if (!game.getRules().hasAppliedVariant(GameType.Planechase)) { return; }
            final Player p = game.getPhaseHandler().getPlayerTurn();

            final List<PaperCard> allPlanars = new ArrayList<PaperCard>();
            for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                if (c.getRules().getType().isPlane() || c.getRules().getType().isPhenomenon()) {
                    allPlanars.add(c);
                }
            }
            Collections.sort(allPlanars);

            // use standard forge's list selection dialog
            final IPaperCard c = getGui().oneOrNone("Name the card", allPlanars);
            if (c == null) { return; }
            final Card forgeCard = Card.fromPaperCard(c, p);

            forgeCard.setOwner(p);
            game.getAction().invoke(new Runnable() {
                @Override public void run() {
                    game.getAction().changeZone(null, p.getZone(ZoneType.PlanarDeck), forgeCard, 0);
                    PlanarDice.roll(p, PlanarDice.Planeswalk);
                }
            });
        }
    }

    @Override
    public void concede() {
        if (player != null) {
            player.concede();
            getGame().getAction().checkGameOverCondition();
        }
    }
    public boolean mayAutoPass() {
        return getGui().mayAutoPass(getLocalPlayerView());
    }
    public void autoPassUntilEndOfTurn() {
        getGui().autoPassUntilEndOfTurn(getLocalPlayerView());
    }
    @Override
    public void autoPassCancel() {
        getGui().autoPassCancel(getLocalPlayerView());
    }
    @Override
    public void awaitNextInput() {
        getGui().awaitNextInput();
    }
    @Override
    public void cancelAwaitNextInput() {
        getGui().cancelAwaitNextInput();
    }

    @Override
    public void nextGameDecision(final NextGameDecision decision) {
        game.fireEvent(new UiEventNextGameDecision(this, decision));
    }

    @Override
    public String getActivateDescription(final CardView card) {
        return getInputProxy().getActivateAction(card);
    }

    @Override
    public void reorderHand(final CardView card, final int index) {
        final PlayerZone hand = player.getZone(ZoneType.Hand);
        hand.reorder(game.getCard(card), index);
        player.updateZoneForView(hand);
    }
}
