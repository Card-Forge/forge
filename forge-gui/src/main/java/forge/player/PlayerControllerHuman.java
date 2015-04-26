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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
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
import forge.util.FCollection;
import forge.util.FCollectionView;
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
    public void setDisableAutoYields(boolean disableAutoYields0) {
        disableAutoYields = disableAutoYields0;
    }

    @Override
    public boolean mayLookAtAllCards() {
        return mayLookAtAllCards;
    }

    private final HashSet<Card> tempShownCards = new HashSet<Card>();
    public <T> void tempShow(final Iterable<T> objects) {
        for (final T t : objects) {
            if (t instanceof Card) {
                // assume you may see any card passed through here
                tempShowCard((Card) t);
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
    public SpellAbility getAbilityToPlay(final Card hostCard, final List<SpellAbility> abilities, final ITriggerEvent triggerEvent) {
        final SpellAbilityView resultView = getGui().getAbilityToPlay(CardView.get(hostCard), SpellAbilityView.getCollection(abilities), triggerEvent);
        return getGame().getSpellAbility(resultView);
    }

    @Override
    public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets) {
        HumanPlay.playSaWithoutPayingManaCost(this, player.getGame(), copySA, mayChoseNewTargets);
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        HumanPlay.playSpellAbilityNoStack(this, player, effectSA, !canSetupTargets);
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType) {
        CardPool sideboard = deck.get(DeckSection.Sideboard);
        if (sideboard == null) {
            // Use an empty cardpool instead of null for 75/0 sideboarding scenario.
            sideboard = new CardPool();
        }

        CardPool main = deck.get(DeckSection.Main);

        int mainSize = main.countAll();
        int sbSize = sideboard.countAll();
        int combinedDeckSize = mainSize + sbSize;

        int deckMinSize = Math.min(mainSize, gameType.getDeckFormat().getMainRange().getMinimum());
        Range<Integer> sbRange = gameType.getDeckFormat().getSideRange();
        // Limited doesn't have a sideboard max, so let the Main min take care of things.
        int sbMax = sbRange == null ? combinedDeckSize : sbRange.getMaximum();

        List<PaperCard> newMain = null;

        //Skip sideboard loop if there are no sideboarding opportunities
        if (sbSize == 0 && mainSize == deckMinSize) { return null; }

        // conformance should not be checked here
        boolean conform = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);
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
    public Integer announceRequirements(SpellAbility ability, String announce, boolean canChooseZero) {
        int min = canChooseZero ? 0 : 1;
        return getGui().getInteger("Choose " + announce + " for " + ability.getHostCard().getName(),
                min, Integer.MAX_VALUE, min + 9);
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView valid, String message) {
        return choosePermanentsTo(min, max, valid, message, "sacrifice");
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView valid, String message) {
        return choosePermanentsTo(min, max, valid, message, "destroy");
    }

    private CardCollectionView choosePermanentsTo(int min, int max, CardCollectionView valid, String message, String action) {
        max = Math.min(max, valid.size());
        if (max <= 0) {
            return CardCollection.EMPTY;
        }

        StringBuilder builder = new StringBuilder("Select ");
        if (min == 0) {
            builder.append("up to ");
        }
        builder.append("%d " + message + "(s) to " + action + ".");

        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage(builder.toString());
        inp.setCancelAllowed(min == 0);
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional) {
        // If only one card to choose, use a dialog box.
        // Otherwise, use the order dialog to be able to grab multiple cards in one shot

        if (max == 1) {
            Card singleChosen = chooseSingleEntityForEffect(sourceList, sa, title, isOptional);
            return singleChosen == null ? CardCollection.EMPTY : new CardCollection(singleChosen);
        }

        getGui().setPanelSelection(CardView.get(sa.getHostCard()));

        // try to use InputSelectCardsFromList when possible 
        boolean cardsAreInMyHandOrBattlefield = true;
        for (Card c : sourceList) {
            Zone z = c.getZone();
            if (z != null && (z.is(ZoneType.Battlefield) || z.is(ZoneType.Hand, player))) {
                continue;
            }
            cardsAreInMyHandOrBattlefield = false;
            break;
        }

        if (cardsAreInMyHandOrBattlefield) {
            InputSelectCardsFromList sc = new InputSelectCardsFromList(this, min, max, sourceList);
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
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer) {
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
        for (GameEntity c : optionList) {
            if (c instanceof Player) {
                continue;
            }
            Zone cz = ((Card)c).getZone(); 
            // can point at cards in own hand and anyone's battlefield
            boolean canUiPointAtCards = cz != null && (cz.is(ZoneType.Hand) && cz.getPlayer() == player || cz.is(ZoneType.Battlefield));
            if (!canUiPointAtCards) {
                canUseSelectCardsInput = false;
                break;
            }
        }

        if (canUseSelectCardsInput) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
            }
            InputSelectEntitiesFromList<T> input = new InputSelectEntitiesFromList<T>(this, isOptional ? 0 : 1, 1, optionList);
            input.setCancelAllowed(isOptional);
            input.setMessage(MessageUtil.formatMessage(title, player, targetedPlayer));
            input.showAndWait();
            return Iterables.getFirst(input.getSelected(), null);
        }

        tempShow(optionList);
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
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        if (min >= max) {
            return min;
        }
        final Integer[] choices = new Integer[max + 1 - min];
        for (int i = 0; i <= max - min; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        return getGui().one(title, choices).intValue();
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> choices, Player relatedPlayer) {
        return getGui().one(title, choices).intValue();
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(java.util.List<SpellAbility> spells, SpellAbility sa, String title) {
        if (spells.size() < 2) {
            return spells.get(0);
        }

        // Human is supposed to read the message and understand from it what to choose
        return getGui().one(title, spells);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmAction(forge.card.spellability.SpellAbility, java.lang.String, java.lang.String)
     */
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return getGui().confirm(CardView.get(sa.getHostCard()), message);
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife,
            String string, int bid, Player winner) {
        return getGui().confirm(CardView.get(sa.getHostCard()), string + " Highest Bidder " + winner);
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return getGui().confirm(CardView.get(hostCard), message);
    }

    @Override
    public boolean confirmTrigger(SpellAbility sa, Trigger regtrig, Map<String, String> triggerParams, boolean isMandatory) {
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
        HashMap<String, Object> tos = sa.getTriggeringObjects();
        if (tos.containsKey("Attacker")) {
            buildQuestion.append("\nAttacker: " + tos.get("Attacker"));
        }
        if (tos.containsKey("Card")) {
            Card card = (Card) tos.get("Card");
            if (card != null && (card.getController() == player || game.getZoneOf(card) == null
                    || game.getZoneOf(card).getZoneType().isKnown())) {
                buildQuestion.append("\nTriggered by: " + tos.get("Card"));
            }
        }

        InputConfirm inp = new InputConfirm(this, buildQuestion.toString());
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
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
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String message) {
        reveal(CardView.getCollection(cards), zone, PlayerView.get(owner), message);
    }

    @Override
    public void reveal(Collection<CardView> cards, ZoneType zone, PlayerView owner, String message) {
        if (StringUtils.isBlank(message)) {
            message = "Looking at cards in {player's} " + zone.name().toLowerCase();
        } else {
            message += "{player's} " + zone.name().toLowerCase();
        }
        String fm = MessageUtil.formatMessage(message, getLocalPlayerView(), owner);
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
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
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
        final boolean result = getGui().confirm(view, "Put " + view + " on the top or bottom of your library?", new String[]{"Top", "Bottom"});
        endTempShowCards();
        
        return result;
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone) {
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
    public CardCollectionView chooseCardsToDiscardFrom(Player p, SpellAbility sa, CardCollection valid, int min, int max) {
        if (p != player) {
            tempShowCards(valid);
            final CardCollection choices = game.getCardList(getGui().many("Choose " + min + " card" + (min != 1 ? "s" : "") + " to discard",
                    "Discarded", min, min, CardView.getCollection(valid), null));
            endTempShowCards();
            return choices;
        }

        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
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
    public CardCollectionView chooseCardsToDelve(final int colorLessAmount, final CardCollection grave) {
        final int cardsInGrave = Math.min(colorLessAmount, grave.size());
        if (cardsInGrave == 0) {
            return CardCollection.EMPTY;
        }
        final CardCollection toExile = new CardCollection();
        final Integer[] cntChoice = new Integer[cardsInGrave + 1];
        for (int i = 0; i <= cardsInGrave; i++) {
            cntChoice[i] = Integer.valueOf(i);
        }

        final Integer chosenAmount = getGui().one("Delve how many cards?", cntChoice);
        for (int i = 0; i < chosenAmount; i++) {
            final CardView nowChosen = getGui().oneOrNone("Exile which card?", CardView.getCollection(grave));

            if (nowChosen == null) {
                // User canceled,abort delving.
                toExile.clear();
                break;
            }

            Card card = game.getCard(nowChosen);
            grave.remove(card);
            toExile.add(card);
        }
        return toExile;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseTargets(forge.card.spellability.SpellAbility, forge.card.spellability.SpellAbilityStackInstance)
     */
    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability) {
        SpellAbility sa = ability.isWrapper() ? ((WrappedAbility) ability).getWrappedAbility() : ability;
        if (sa.getTargetRestrictions() == null) {
            return null;
        }
        TargetChoices oldTarget = sa.getTargets();
        TargetSelection select = new TargetSelection(this, sa);
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
    public CardCollectionView chooseCardsToDiscardUnlessType(int num, CardCollectionView hand, final String uType, SpellAbility sa) {
        final InputSelectEntitiesFromList<Card> target = new InputSelectEntitiesFromList<Card>(this, num, num, hand) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasAllTargets() {
                for (Card c : selected) {
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
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        List<String> options = new ArrayList<String>();
        for (int i = 0; i < manaChoices.size(); i++) {
            Mana m = manaChoices.get(i);
            options.add(String.format("%d. %s mana from %s", 1+i, MagicColor.toLongString(m.getColor()), m.getSourceCard()));
        }
        String chosen = getGui().one("Pay Mana from Mana Pool", options);
        String idx = TextUtil.split(chosen, '.')[0];
        return manaChoices.get(Integer.parseInt(idx)-1);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseSomeType(java.lang.String, java.lang.String, java.util.List, java.util.List, java.lang.String)
     */
    @Override
    public String chooseSomeType(final String kindOfType, final SpellAbility sa, final List<String> validTypes,  List<String> invalidTypes, final boolean isOptional) {
        final List<String> types = Lists.newArrayList(validTypes);
        if (invalidTypes != null && !invalidTypes.isEmpty()) {
            Iterables.removeAll(types, invalidTypes);
        }
        if (isOptional) {
            return getGui().oneOrNone("Choose a " + kindOfType.toLowerCase() + " type", types);
        }
        return getGui().one("Choose a " + kindOfType.toLowerCase() + " type", types);
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ArrayListMultimap<Object, Player> votes) {
        return getGui().one(prompt, options);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmReplacementEffect(forge.card.replacement.ReplacementEffect, forge.card.spellability.SpellAbility, java.lang.String)
     */
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question) {
        return getGui().confirm(CardView.get(replacementEffect.getHostCard()), question);
    }

    @Override
    public CardCollectionView getCardsToMulligan(boolean isCommander, Player firstPlayer) {
        final InputConfirmMulligan inp = new InputConfirmMulligan(this, player, firstPlayer, isCommander);
        inp.showAndWait();
        return inp.isKeepHand() ? null : isCommander ? inp.getSelectedCards() : player.getCardsIn(ZoneType.Hand);
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
    public void declareBlockers(Player defender, Combat combat) {
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
                catch (InterruptedException e) {
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
                catch (InterruptedException e) {
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
    public void playChosenSpellAbility(SpellAbility chosenSa) {
        HumanPlay.playSpellAbility(this, player, chosenSa);
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int nDiscard) {
        final int max = player.getMaxHandSize();

        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, nDiscard, nDiscard, player.getZone(ZoneType.Hand).getCards());
        String message = "Cleanup Phase\nSelect " + nDiscard + " card" + (nDiscard > 1 ? "s" : "") + 
                " to discard to bring your hand down to the maximum of " + max + " cards.";
        inp.setMessage(message);
        inp.setCancelAllowed(false);
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        max = Math.min(max, valid.size());
        min = Math.min(min, max);
        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage("Choose Which Cards to Reveal");
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public boolean payManaOptional(Card c, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose) {
        if (sa == null && cost.isOnlyManaCost() && cost.getTotalMana().isZero() 
                && !FModel.getPreferences().getPrefBoolean(FPref.MATCHPREF_PROMPT_FREE_BLOCKS)) {
            return true;
        }
        return HumanPlay.payCostDuringAbilityResolve(this, player, c, cost, sa, prompt);
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        CardCollection srcCards = new CardCollection();
        for (SpellAbility sa : usableFromOpeningHand) {
            srcCards.add(sa.getHostCard());
        }
        List<SpellAbility> result = new ArrayList<SpellAbility>();
        if (srcCards.isEmpty()) {
            return result;
        }
        final List<CardView> chosen = getGui().many("Choose cards to activate from opening hand and their order", "Activate first", -1, CardView.getCollection(srcCards), null);
        for (final CardView view : chosen) {
            final Card c = game.getCard(view);
            for (SpellAbility sa : usableFromOpeningHand) {
                if (sa.getHostCard() == c) {
                    result.add(sa);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        String[] labels = new String[]{"Option1", "Option2"};
        switch (kindOfChoice) {
            case HeadsOrTails:  labels = new String[]{"Heads", "Tails"}; break;
            case TapOrUntap:    labels = new String[]{"Tap", "Untap"}; break;
            case OddsOrEvens:   labels = new String[]{"Odds", "Evens"}; break;
            case UntapOrLeaveTapped:    labels = new String[]{"Untap", "Leave tapped"}; break;
            case UntapTimeVault: labels = new String[]{"Untap (and skip this turn)", "Leave tapped"}; break;
            case PlayOrDraw:    labels = new String[]{"Play", "Draw"}; break;
            default:            labels = kindOfChoice.toString().split("Or");
        }
        return getGui().confirm(CardView.get(sa.getHostCard()), question, defaultVal == null || defaultVal.booleanValue(), labels);
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        String[] labelsSrc = call ? new String[]{"heads", "tails"} : new String[]{"win the flip", "lose the flip"};
        String[] strResults = new String[results.length];
        for (int i = 0; i < results.length; i++) {
            strResults[i] = labelsSrc[results[i] ? 0 : 1];
        }
        return getGui().one(sa.getHostCard().getName() + " - Choose a result", strResults).equals(labelsSrc[0]);
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap) {
        String title = entityBeingDamaged + " - select which prevention shield to use";
        return choiceMap.get(getGui().one(title, options));
    }

    @Override
    public Pair<CounterType,String> chooseAndRemoveOrPutCounter(Card cardWithCounter) {
        if (!cardWithCounter.hasCounters()) {
            System.out.println("chooseCounterType was reached with a card with no counters on it. Consider filtering this card out earlier");
            return null;
        }

        String counterChoiceTitle = "Choose a counter type on " + cardWithCounter;
        final CounterType chosen = getGui().one(counterChoiceTitle, cardWithCounter.getCounters().keySet());

        String putOrRemoveTitle = "What to do with that '" + chosen.getName() + "' counter ";
        final String putString = "Put another " + chosen.getName() + " counter on " + cardWithCounter;
        final String removeString = "Remove a " + chosen.getName() + " counter from " + cardWithCounter;
        final String addOrRemove = getGui().one(putOrRemoveTitle, new String[]{putString,removeString});

        return new ImmutablePair<CounterType,String>(chosen,addOrRemove);
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility saSpellskite, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
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
    public void notifyOfValue(SpellAbility sa, GameObject realtedTarget, String value) {
        String message = MessageUtil.formatNotificationMessage(sa, player, realtedTarget, value);
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
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num) {
        List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
        String modeTitle = String.format("%s activated %s - Choose a mode", sa.getActivatingPlayer(), sa.getHostCard());
        List<AbilitySub> chosen = Lists.newArrayListWithCapacity(num);
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

            choices.remove(a);
            chosen.add(a);
        }
        return chosen;
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        return getGui().getChoices(message, min, max, options);
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        int cntColors = colors.countColors();
        switch (cntColors) {
            case 0: return 0;
            case 1: return colors.getColor();
            default: return chooseColorCommon(message, sa == null ? null : sa.getHostCard(), colors, false);
        }
    }
    
    @Override
    public byte chooseColorAllowColorless(String message, Card c, ColorSet colors) {
        int cntColors = 1 + colors.countColors();
        switch (cntColors) {
            case 1: return 0;
            default: return chooseColorCommon(message, c, colors, true);
        }
    }
    
    private byte chooseColorCommon(String message, Card c, ColorSet colors, boolean withColorless) {
        int cntColors = colors.countColors();
        if(withColorless) cntColors++;
        String[] colorNames = new String[cntColors];
        int i = 0;
        if (withColorless) {
            colorNames[i++] = MagicColor.toLongString((byte)0);
        }
        for (byte b : colors) {
            colorNames[i++] = MagicColor.toLongString(b);
        }
        if (colorNames.length > 2) {
            return MagicColor.fromName(getGui().one(message, colorNames));
        }
        int idxChosen = getGui().confirm(CardView.get(c), message, colorNames) ? 0 : 1;
        return MagicColor.fromName(colorNames[idxChosen]);
    }

    @Override
    public PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name) {
        Iterable<PaperCard> cardsFromDb = FModel.getMagicDb().getCommonCards().getUniqueCards();
        List<PaperCard> cards = Lists.newArrayList(Iterables.filter(cardsFromDb, cpp));
        Collections.sort(cards);
        return getGui().one(message, cards);
    }

    @Override
    public CounterType chooseCounterType(Collection<CounterType> options, SpellAbility sa, String prompt) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return getGui().one(prompt, options);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String question) {
        InputConfirm inp = new InputConfirm(this, question);
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers, HashMap<String, Object> runParams) {
        ReplacementEffect first = possibleReplacers.get(0);
        if (possibleReplacers.size() == 1) {
            return first;
        }
        String firstStr = first.toString();
        for (int i = 1; i < possibleReplacers.size(); i++) {
            if (!possibleReplacers.get(i).toString().equals(firstStr)) {
                return getGui().one(prompt, possibleReplacers); //prompt user if there are multiple different options
            }
        }
        return first; //return first option without prompting if all options are the same
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        return getGui().one(string, choices);
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
        // if it's paid by the AI already the human can pay, but it won't change anything
        return HumanPlay.payCostDuringAbilityResolve(this, player, sa.getHostCard(), cost, sa, null);
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        List<SpellAbility> orderedSAs = activePlayerSAs;
        if (activePlayerSAs.size() > 1) { // give a dual list form to create instead of needing to do it one at a time
            String firstStr = orderedSAs.get(0).toString();
            for (int i = 1; i < orderedSAs.size(); i++) { //don't prompt user if all options are the same
                if (!orderedSAs.get(i).toString().equals(firstStr)) {
                    orderedSAs = getGui().order("Select order for simultaneous abilities", "Resolve first", activePlayerSAs, null);
                    break;
                }
            }
        }
        int size = orderedSAs.size();
        for (int i = size - 1; i >= 0; i--) {
            SpellAbility next = orderedSAs.get(i);
            if (next.isTrigger()) {
                HumanPlay.playSpellAbility(this, player, next);
            }
            else {
                player.getGame().getStack().add(next);
            }
        }
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        HumanPlay.playSpellAbilityNoStack(this, player, wrapperAbility);
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        HumanPlay.playSpellAbility(this, player, tgtSA);
        return true;
    }

    @Override
    public Map<GameEntity, CounterType> chooseProliferation() {
        InputProliferate inp = new InputProliferate(this);
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return inp.getProliferationMap();
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        final TargetSelection select = new TargetSelection(this, currentAbility);
        return select.chooseTargets(null);
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, boolean faceUp) {
        if (!faceUp) {
            final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
            final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());
            final String[] possibleValues = { p1Str , p2Str };
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
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        for (Player p : removedAnteCards.keySet()) {
            getGui().reveal(message + " from " + Lang.getPossessedObject(MessageUtil.mayBeYou(player, p), "deck"), removedAnteCards.get(p));
        }
    }

    @Override
    public CardShields chooseRegenerationShield(Card c) {
        if (c.getShieldCount() < 2) {
            return Iterables.getFirst(c.getShields(), null);
        }
        ArrayList<CardShields> shields = new ArrayList<CardShields>();
        for (CardShields shield : c.getShields()) {
            shields.add(shield);
        }
        return getGui().one("Choose a regeneration shield:", shields);
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        return getGui().many("Select cards to add to your deck", "Add these to my deck", 0, losses.size(), losses, null);
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, boolean isActivatedSa) {
        return HumanPlay.payManaCost(this, toPay, costPartMana, sa, player, prompt, isActivatedSa);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCreats) {
        InputSelectCardsForConvoke inp = new InputSelectCardsForConvoke(this, player, manaCost, untappedCreats);
        inp.showAndWait();
        return inp.getConvokeMap();
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<PaperCard> cpp, String valid, String message) {
        while (true) {
            PaperCard cp = chooseSinglePaperCard(sa, message, cpp, sa.getHostCard().getName());
            Card instanceForPlayer = Card.fromPaperCard(cp, player); // the Card instance for test needs a game to be tested
            if (instanceForPlayer.isValid(valid, sa.getHostCard().getController(), sa.getHostCard())) {
                return cp.getName();
            }
        }
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider) {
        return chooseSingleEntityForEffect(fetchList, delayedReveal, sa, selectPrompt, isOptional, decider);
    }

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

    public void selectButtonOk() {
        inputProxy.selectButtonOK();
    }

    public void selectButtonCancel() {
        inputProxy.selectButtonCancel();
    }

    public void confirm() {
        if (inputQueue.getInput() instanceof InputConfirm) {
            selectButtonOk();
        }
    }

    public void passPriority() {
        passPriority(false);
    }
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
                    getGui().message("Cannot pass priority at this time.");
                }
            });
        }
    }

    public void useMana(final byte mana) {
        final Input input = inputQueue.getInput();
        if (input instanceof InputPayMana) {
            ((InputPayMana) input).useManaFromPool(mana);
        }
    }

    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        inputProxy.selectPlayer(playerView, triggerEvent);
    }

    public boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect, final ITriggerEvent triggerEvent) {
        return inputProxy.selectCard(cardView, otherCardViewsToSelect, triggerEvent);
    }

    public void selectAbility(final SpellAbilityView sa) {
        inputProxy.selectAbility(getGame().getSpellAbility(sa));
    }

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
        public void setCanPlayUnlimitedLands(boolean canPlayUnlimitedLands0) {
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
            Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message("No player has priority at the moment, so mana cannot be added to their pool.");
                return;
            }

            final Card dummy = new Card(-777777, game);
            dummy.setOwner(pPriority);
            Map<String, String> produced = new HashMap<String, String>();
            produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
            final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
            game.getAction().invoke(new Runnable() {
                @Override public void run() { abMana.produceMana(null); }
            });
        }

        private GameState createGameStateObject() {
            return new GameState() {
                @Override
                public IPaperCard getPaperCard(String cardName) {
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
                File f = GuiBase.getInterface().getSaveFile(new File(ForgeConstants.USER_GAMES_DIR, "state.txt"));
                if (f != null && (!f.exists() || getGui().showConfirmDialog("Overwrite existing file?", "File exists!"))) {
                    final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                    bw.write(state.toString());
                    bw.close();
                }
            } catch (Exception e) {
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
            File gamesDir = new File(ForgeConstants.USER_GAMES_DIR);
            if (!gamesDir.exists()) { // if the directory does not exist, try to create it
                gamesDir.mkdir();
            }
            
            String filename = GuiBase.getInterface().showFileDialog("Select Game State File", ForgeConstants.USER_GAMES_DIR);
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

            Player pPriority = game.getPhaseHandler().getPriorityPlayer();
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
            Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message("No player has priority at the moment, so their deck can't be tutored from.");
                return;
            }

            final CardCollection lib = (CardCollection)pPriority.getCardsIn(ZoneType.Library);
            final List<ZoneType> origin = new ArrayList<ZoneType>();
            origin.add(ZoneType.Library);
            SpellAbility sa = new SpellAbility.EmptySa(new Card(-1, game));
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

            final CounterType counter = getGui().oneOrNone("Which type of counter?", CounterType.values());
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
                    InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0, Integer.MAX_VALUE, untapped);
                    inp.setCancelAllowed(true);
                    inp.setMessage("Choose permanents to tap");
                    inp.showAndWait();
                    if (!inp.hasCancelled()) {
                        for (Card c : inp.getSelected()) {
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
                    InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0, Integer.MAX_VALUE, tapped);
                    inp.setCancelAllowed(true);
                    inp.setMessage("Choose permanents to untap");
                    inp.showAndWait();
                    if (!inp.hasCancelled()) {
                        for (Card c : inp.getSelected()) {
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
            Input input = inputQueue.getInput();
            if (!(input instanceof InputPassPriority)) {
                getGui().message("You must have priority to use this feature.", "Win Game");
                return;
            }

            //set life of all other players to 0
            final LobbyPlayer guiPlayer = getLobbyPlayer();
            final FCollectionView<Player> players = game.getPlayers();
            for (Player player : players) {
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

            final PlanarDice res = getGui().oneOrNone("Choose result", PlanarDice.values());
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
            for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
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
                @Override
                public void run() {
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
    public void awaitNextInput() {
        getGui().awaitNextInput();
    }
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
    public void reorderHand(CardView card, int index) {
        PlayerZone hand = player.getZone(ZoneType.Hand);
        hand.reorder(game.getCard(card), index);
        player.updateZoneForView(hand);
    }
}
