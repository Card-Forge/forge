package forge.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.control.FControlGamePlayback;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.events.UiEventAttackerDeclared;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.GameObject;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardShields;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.match.input.ButtonUtil;
import forge.match.input.Input;
import forge.match.input.InputAttack;
import forge.match.input.InputBase;
import forge.match.input.InputBlock;
import forge.match.input.InputConfirm;
import forge.match.input.InputConfirmMulligan;
import forge.match.input.InputPassPriority;
import forge.match.input.InputPayMana;
import forge.match.input.InputProliferate;
import forge.match.input.InputProxy;
import forge.match.input.InputSelectCardsForConvoke;
import forge.match.input.InputSelectCardsFromList;
import forge.match.input.InputSelectEntitiesFromList;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.util.DevModeUtil;
import forge.util.ITriggerEvent;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SGuiDialog;
import forge.util.gui.SOptionPane;
import forge.view.CardView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.LocalGameView;
import forge.view.PlayerView;
import forge.view.SpellAbilityView;
import forge.view.StackItemView;

/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerHuman extends PlayerController {

    private IGuiBase gui;
    private final InputProxy inputProxy;
    private final GameView gameView;
    /**
     * Cards this player may look at right now, for example when searching a
     * library.
     */
    private final Set<Card> mayLookAt = Sets.newHashSet();
    private boolean mayLookAtAllCards = false;

    public PlayerControllerHuman(final Game game0, final Player p, final LobbyPlayer lp, final IGuiBase gui) {
        super(game0, p, lp);
        this.gui = gui;
        this.inputProxy = new InputProxy(this, game0);
        this.gameView = new GameView(game0);

        // aggressively cache a view for each player (also caches cards)
        for (final Player player : game.getRegisteredPlayers()) {
            gameView.getPlayerView(player);
        }
    }

    public IGuiBase getGui() {
        return gui;
    }

    public InputProxy getInputProxy() {
        return inputProxy;
    }

    public LocalGameView getGameView() {
        return gameView;
    }

    /**
     * @return the mayLookAtAllCards
     */
    public boolean mayLookAtAllCards() {
        return mayLookAtAllCards;
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

    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        return !getGui().stopAtPhase(gameView.getPlayerView(turn), phase);
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */ 
    public SpellAbility getAbilityToPlay(final List<SpellAbility> abilities, final ITriggerEvent triggerEvent) {
        final SpellAbilityView choice = getGui().getAbilityToPlay(gameView.getSpellAbilityViews(abilities), triggerEvent);
        return gameView.getSpellAbility(choice);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#mayPlaySpellAbilityForFree(forge.card.spellability.SpellAbility)
     */
    @Override
    public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets) {
        HumanPlay.playSaWithoutPayingManaCost(this, player.getGame(), copySA, mayChoseNewTargets);
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        HumanPlay.playSpellAbilityNoStack(this, player, effectSA, !canSetupTargets);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#sideboard(forge.deck.Deck)
     */
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
                SOptionPane.showErrorDialog(getGui(), errMsg, "Invalid Deck");
            }
            // Sideboard rules have changed for M14, just need to consider min maindeck and max sideboard sizes
            // No longer need 1:1 sideboarding in non-limited formats
            newMain = getGui().sideboard(sideboard, main);
        } while (conform && (newMain.size() < deckMinSize || combinedDeckSize - newMain.size() > sbMax));

        return newMain;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#assignCombatDamage()
     */
    @Override
    public Map<Card, Integer> assignCombatDamage(final Card attacker,
            final List<Card> blockers, final int damageDealt,
            final GameEntity defender, final boolean overrideOrder) {
        // Attacker is a poor name here, since the creature assigning damage
        // could just as easily be the blocker.
        final Map<Card, Integer> map = Maps.newHashMap();
        if (defender != null && assignDamageAsIfNotBlocked(attacker)) {
            map.put(null, damageDealt);
        } else {
            final List<CardView> vBlockers = gameView.getCardViews(blockers);
            if ((attacker.hasKeyword("Trample") && defender != null) || (blockers.size() > 1)) {
                final CardView vAttacker = gameView.getCardView(attacker);
                final GameEntityView vDefender = gameView.getGameEntityView(defender);
                final Map<CardView, Integer> result = getGui().getDamageToAssign(vAttacker, vBlockers, damageDealt, vDefender, overrideOrder);
                for (final Entry<CardView, Integer> e : result.entrySet()) {
                    map.put(gameView.getCard(e.getKey()), e.getValue());
                }
            } else {
                map.put(blockers.get(0), damageDealt);
            }
        }
        return map;
    }

    private final boolean assignDamageAsIfNotBlocked(final Card attacker) {
        return attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")
                || (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                && SGuiDialog.confirm(getGui(), gameView.getCardView(attacker), "Do you want to assign its combat damage as though it weren't blocked?"));
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#announceRequirements(java.lang.String)
     */
    @Override
    public Integer announceRequirements(SpellAbility ability, String announce, boolean canChooseZero) {
        int min = canChooseZero ? 0 : 1;
        return SGuiChoose.getInteger(getGui(), "Choose " + announce + " for " + ability.getHostCard().getName(),
                min, Integer.MAX_VALUE, min + 9);
    }

    @Override
    public List<Card> choosePermanentsToSacrifice(SpellAbility sa, int min, int max, List<Card> valid, String message) {
        String outerMessage = "Select %d " + message + "(s) to sacrifice";
        return choosePermanentsTo(min, max, valid, outerMessage);
    }

    @Override
    public List<Card> choosePermanentsToDestroy(SpellAbility sa, int min, int max, List<Card> valid, String message) {
        String outerMessage = "Select %d " + message + "(s) to be destroyed";
        return choosePermanentsTo(min, max, valid, outerMessage);
    }

    private List<Card> choosePermanentsTo(int min, int max, List<Card> valid, String outerMessage) {
        max = Math.min(max, valid.size());
        if (max <= 0) {
            return new ArrayList<Card>();
        }

        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min == 0 ? 1 : min, max, valid);
        inp.setMessage(outerMessage);
        inp.setCancelAllowed(min == 0);
        inp.showAndWait();
        return Lists.newArrayList(inp.getSelected());
    }


    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseCardsForEffect(java.util.Collection, forge.card.spellability.SpellAbility, java.lang.String, int, boolean)
     */
    @Override
    public List<Card> chooseCardsForEffect(List<Card> sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional) {
        // If only one card to choose, use a dialog box.
        // Otherwise, use the order dialog to be able to grab multiple cards in one shot
        if (max == 1) {
            Card singleChosen = chooseSingleEntityForEffect(sourceList, sa, title, isOptional);
            return singleChosen == null ?  Lists.<Card>newArrayList() : Lists.newArrayList(singleChosen);
        }

        getGui().setPanelSelection(gameView.getCardView(sa.getHostCard()));

        // try to use InputSelectCardsFromList when possible 
        boolean cardsAreInMyHandOrBattlefield = true;
        for(Card c : sourceList) {
            Zone z = c.getZone();
            if (z != null && (z.is(ZoneType.Battlefield) || z.is(ZoneType.Hand, player)))
                continue;
            cardsAreInMyHandOrBattlefield = false;
            break;
        }
        
        if(cardsAreInMyHandOrBattlefield) {
            InputSelectCardsFromList sc = new InputSelectCardsFromList(this, min, max, sourceList);
            sc.setMessage(title);
            sc.setCancelAllowed(isOptional);
            sc.showAndWait();
            return Lists.newArrayList(sc.getSelected());
        }

        final List<CardView> choices = SGuiChoose.many(getGui(), title, "Chosen Cards", min, max, gameView.getCardViews(sourceList), gameView.getCardView(sa.getHostCard()));
        return getCards(choices);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(Collection<T> options, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer) {
        // Human is supposed to read the message and understand from it what to choose
        if (options.isEmpty()) {
            return null;
        }
        if (!isOptional && options.size() == 1) {
            return Iterables.getFirst(options, null);
        }

        boolean canUseSelectCardsInput = true;
        for (GameEntity c : options) {
            if (c instanceof Player) 
                continue;
            Zone cz = ((Card)c).getZone(); 
            // can point at cards in own hand and anyone's battlefield
            boolean canUiPointAtCards = cz != null && (cz.is(ZoneType.Hand) && cz.getPlayer() == player || cz.is(ZoneType.Battlefield));
            if (!canUiPointAtCards) {
                canUseSelectCardsInput = false;
                break;
            }
        }

        if (canUseSelectCardsInput) {
            InputSelectEntitiesFromList<T> input = new InputSelectEntitiesFromList<T>(this, isOptional ? 0 : 1, 1, options);
            input.setCancelAllowed(isOptional);
            input.setMessage(formatMessage(title, targetedPlayer));
            input.showAndWait();
            return Iterables.getFirst(input.getSelected(), null);
        }

        for (final T t : options) {
            if (t instanceof Card) {
                // assume you may see any card passed through here
                mayLookAt.add((Card) t);
            }
        }
        final GameEntityView result = isOptional ? SGuiChoose.oneOrNone(getGui(), title, gameView.getGameEntityViews((Iterable<GameEntity>) options)) : SGuiChoose.one(getGui(), title, gameView.getGameEntityViews((Iterable<GameEntity>) options));
        mayLookAt.clear();
        return (T) gameView.getGameEntity(result);
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        final Integer[] choices = new Integer[max + 1 - min];
        for (int i = 0; i <= max - min; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        return SGuiChoose.one(getGui(), title, choices).intValue();
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> choices, Player relatedPlayer) {
        return SGuiChoose.one(getGui(), title, choices).intValue();
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(java.util.List<SpellAbility> spells, SpellAbility sa, String title) {
        if (spells.size() < 2) {
            return spells.get(0);
        }

        // Human is supposed to read the message and understand from it what to choose
        final SpellAbilityView choice = SGuiChoose.one(getGui(), title, gameView.getSpellAbilityViews(spells));
        return gameView.getSpellAbility(choice);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmAction(forge.card.spellability.SpellAbility, java.lang.String, java.lang.String)
     */
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return SGuiDialog.confirm(getGui(), gameView.getCardView(sa.getHostCard()), message);
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife,
            String string, int bid, Player winner) {
        return SGuiDialog.confirm(getGui(), gameView.getCardView(sa.getHostCard()), string + " Highest Bidder " + winner);
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return SGuiDialog.confirm(getGui(), gameView.getCardView(hostCard), message);
    }

    @Override
    public boolean confirmTrigger(SpellAbility sa, Trigger regtrig, Map<String, String> triggerParams, boolean isMandatory) {
        if (this.shouldAlwaysAcceptTrigger(regtrig.getId())) {
            return true;
        }
        if (this.shouldAlwaysDeclineTrigger(regtrig.getId())) {
            return false;
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
        } else {
            final String prompt = String.format("%s, you %s\n\nWho would you like to start this game?", 
                    player.getName(), isFirstGame ? " have won the coin toss." : " lost the last game.");
            final InputSelectEntitiesFromList<Player> input = new InputSelectEntitiesFromList<>(this, 1, 1, game.getPlayersInTurnOrder());
            input.setMessage(prompt);
            input.showAndWait();
            return input.getFirstSelected();
        }
    }

    @Override
    public List<Card> orderBlockers(final Card attacker, final List<Card> blockers) {
        final CardView vAttacker = gameView.getCardView(attacker);
        getGui().setPanelSelection(vAttacker);
        final List<CardView> choices = SGuiChoose.order(getGui(), "Choose Damage Order for " + vAttacker, "Damaged First", gameView.getCardViews(blockers), vAttacker);
        return gameView.getCards(choices);
    }

    @Override
    public List<Card> orderBlocker(final Card attacker, final Card blocker, final List<Card> oldBlockers) {
        final CardView vAttacker = gameView.getCardView(attacker);
        getGui().setPanelSelection(vAttacker);
        final List<CardView> choices = SGuiChoose.insertInList(getGui(), "Choose blocker after which to place " + vAttacker + " in damage order; cancel to place it first", gameView.getCardView(blocker), gameView.getCardViews(oldBlockers));
        return gameView.getCards(choices);
    }

    @Override
    public List<Card> orderAttackers(final Card blocker, final List<Card> attackers) {
        final CardView vBlocker = gameView.getCardView(blocker);
        getGui().setPanelSelection(vBlocker);
        final List<CardView> choices = SGuiChoose.order(getGui(), "Choose Damage Order for " + vBlocker, "Damaged First", gameView.getCardViews(attackers), vBlocker);
        return gameView.getCards(choices);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#reveal(java.lang.String, java.util.List, forge.game.zone.ZoneType, forge.game.player.Player)
     */
    @Override
    public void reveal(Collection<Card> cards, ZoneType zone, Player owner, String message) {
        if (StringUtils.isBlank(message)) {
            message = "Looking at cards in {player's} " + zone.name().toLowerCase();
        }
        else {
            message += "{player's} " + zone.name().toLowerCase();
        }
        String fm = formatMessage(message, owner);
        if (!cards.isEmpty()) {
            mayLookAt.addAll(cards);
            SGuiChoose.reveal(getGui(), fm, gameView.getCardViews(cards));
            mayLookAt.clear();
        }
        else {
            SGuiDialog.message(getGui(), formatMessage("There are no cards in {player's} " +
                    zone.name().toLowerCase(), owner), fm);
        }
    }

    @Override
    public ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN) {
        List<Card> toBottom = null;
        List<Card> toTop = null;

        mayLookAt.addAll(topN);
        if (topN.size() == 1) {
            if (willPutCardOnTop(topN.get(0))) {
                toTop = topN;
            }
            else {
                toBottom = topN;
            }
        }
        else {
            final List<CardView> toBottomViews = SGuiChoose.many(getGui(), "Select cards to be put on the bottom of your library", "Cards to put on the bottom", -1, gameView.getCardViews(topN), null); 
            toBottom = gameView.getCards(toBottomViews);
            topN.removeAll(toBottom);
            if (topN.isEmpty()) {
                toTop = null;
            }
            else if (topN.size() == 1) {
                toTop = topN;
            }
            else {
                final List<CardView> toTopViews = SGuiChoose.order(getGui(), "Arrange cards to be put on top of your library", "Cards arranged", gameView.getCardViews(topN), null); 
                toTop = gameView.getCards(toTopViews);
            }
        }
        mayLookAt.clear();
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public boolean willPutCardOnTop(final Card c) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(c.getName());
        final Card c1 = (pc != null ? Card.fromPaperCard(pc, null) : c);
        final CardView view = gameView.getCardView(c1);
        return SGuiDialog.confirm(getGui(), view, "Put " + view + " on the top or bottom of your library?", new String[]{"Top", "Bottom"});
    }

    @Override
    public List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone) {
        switch (destinationZone) {
            case Library:
                return SGuiChoose.order(getGui(), "Choose order of cards to put into the library", "Closest to top", cards, null);
            case Battlefield:
                return SGuiChoose.order(getGui(), "Choose order of cards to put onto the battlefield", "Put first", cards, null);
            case Graveyard:
                return SGuiChoose.order(getGui(), "Choose order of cards to put into the graveyard", "Closest to bottom", cards, null);
            case PlanarDeck:
                return SGuiChoose.order(getGui(), "Choose order of cards to put into the planar deck", "Closest to top", cards, null);
            case SchemeDeck:
                return SGuiChoose.order(getGui(), "Choose order of cards to put into the scheme deck", "Closest to top", cards, null);
            case Stack:
                return SGuiChoose.order(getGui(), "Choose order of copies to cast", "Put first", cards, null);
            default:
                System.out.println("ZoneType " + destinationZone + " - Not Ordered");
                break;
        }
        return cards;
    }

    @Override
    public List<Card> chooseCardsToDiscardFrom(Player p, SpellAbility sa, List<Card> valid, int min, int max) {
        if (p != player) {
            mayLookAt.addAll(valid);
            final List<CardView> choices = SGuiChoose.many(getGui(), "Choose " + min + " card" + (min != 1 ? "s" : "") + " to discard",
                    "Discarded", min, min, gameView.getCardViews(valid), null);
            mayLookAt.clear();
            return getCards(choices);
        }

        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage(sa.hasParam("AnyNumber") ? "Discard up to %d card(s)" : "Discard %d card(s)");
        inp.showAndWait();
        return Lists.newArrayList(inp.getSelected());
    }

    @Override
    public void playMiracle(final SpellAbility miracle, final Card card) {
        final CardView view = gameView.getCardView(card);
        if (SGuiDialog.confirm(getGui(), view, view + " - Drawn. Play for Miracle Cost?")) {
            HumanPlay.playSpellAbility(this, player, miracle);
        }
    }

    @Override
    public List<Card> chooseCardsToDelve(int colorLessAmount, List<Card> grave) {
        List<Card> toExile = new ArrayList<Card>();
        int cardsInGrave = grave.size();
        final Integer[] cntChoice = new Integer[cardsInGrave + 1];
        for (int i = 0; i <= cardsInGrave; i++) {
            cntChoice[i] = Integer.valueOf(i);
        }

        final Integer chosenAmount = SGuiChoose.one(getGui(), "Exile how many cards?", cntChoice);
        System.out.println("Delve for " + chosenAmount);

        for (int i = 0; i < chosenAmount; i++) {
            final CardView nowChosen = SGuiChoose.oneOrNone(getGui(), "Exile which card?", gameView.getCardViews(grave));

            if (nowChosen == null) {
                // User canceled,abort delving.
                toExile.clear();
                break;
            }

            grave.remove(nowChosen);
            toExile.add(gameView.getCard(nowChosen));
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
    public List<Card> chooseCardsToDiscardUnlessType(int num, List<Card> hand, final String uType, SpellAbility sa) {
        final InputSelectEntitiesFromList<Card> target = new InputSelectEntitiesFromList<Card>(this, num, num, hand) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasAllTargets() {
                for (Card c : selected) {
                    if (c.isType(uType)) {
                        return true;
                    }
                }
                return super.hasAllTargets();
            }
        };
        target.setMessage("Select %d card(s) to discard, unless you discard a " + uType + ".");
        target.showAndWait();
        return Lists.newArrayList(target.getSelected());
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
        String chosen = SGuiChoose.one(getGui(), "Pay Mana from Mana Pool", options);
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
        if(isOptional)
            return SGuiChoose.oneOrNone(getGui(), "Choose a " + kindOfType.toLowerCase() + " type", types);
        else
            return SGuiChoose.one(getGui(), "Choose a " + kindOfType.toLowerCase() + " type", types);
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ArrayListMultimap<Object, Player> votes) {
        return SGuiChoose.one(getGui(), prompt, options);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmReplacementEffect(forge.card.replacement.ReplacementEffect, forge.card.spellability.SpellAbility, java.lang.String)
     */
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question) {
        return SGuiDialog.confirm(getGui(), gameView.getCardView(replacementEffect.getHostCard()), question);
    }

    @Override
    public List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer) {
        final InputConfirmMulligan inp = new InputConfirmMulligan(this, player, firstPlayer, isCommander);
        inp.showAndWait();
        return inp.isKeepHand() ? null : isCommander ? inp.getSelectedCards() : player.getCardsIn(ZoneType.Hand);
    }

    @Override
    public void declareAttackers(Player attackingPlayer, Combat combat) {
        if (mayAutoPass()) {
            List<Pair<Card, GameEntity>> mandatoryAttackers = CombatUtil.getMandatoryAttackers(attackingPlayer, combat, combat.getDefenders());
            if (!mandatoryAttackers.isEmpty()) {
                //even if auto-passing attack phase, if there are any mandatory attackers,
                //ensure they're declared and then delay slightly so user can see as much
                for (Pair<Card, GameEntity> attacker : mandatoryAttackers) {
                    combat.addAttacker(attacker.getLeft(), attacker.getRight());
                    getGui().fireEvent(new UiEventAttackerDeclared(gameView.getCardView(attacker.getLeft()), gameView.getGameEntityView(attacker.getRight())));
                }
                try {
                    Thread.sleep(FControlGamePlayback.combatDelay);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return; //don't prompt to declare attackers if user chose to end the turn
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
        updateAutoPassPrompt();
    }

    public void updateAutoPassPrompt() {
        if (mayAutoPass()) {
            //allow user to cancel auto-pass
            InputBase.cancelAwaitNextInput(); //don't overwrite prompt with awaiting opponent
            PhaseType phase = getAutoPassUntilPhase();
            getGui().showPromptMessage("Yielding until " + (phase == PhaseType.CLEANUP ? "end of turn" : phase.nameForUi.toString()) +
                    ".\nYou may cancel this yield to take an action.");
            ButtonUtil.update(getGui(), false, true, false);
        }
    }

    @Override
    public void autoPassUntilEndOfTurn() {
        super.autoPassUntilEndOfTurn();
        updateAutoPassPrompt();
    }

    @Override
    public void autoPassCancel() {
        if (getAutoPassUntilPhase() == null) { return; }
        super.autoPassCancel();

        //prevent prompt getting stuck on yielding message while actually waiting for next input opportunity
        getGui().showPromptMessage("");
        ButtonUtil.update(getGui(), false, false, false);
        InputBase.awaitNextInput(getGui());
    }

    @Override
    public SpellAbility chooseSpellAbilityToPlay() {
        MagicStack stack = game.getStack();

        if (mayAutoPass()) {
            //avoid prompting for input if current phase is set to be auto-passed
            //instead posing a short delay if needed to prevent the game jumping ahead too quick
            int delay = 0;
            if (stack.isEmpty()) {
                //make sure to briefly pause at phases you're not set up to skip
                if (!isUiSetToSkipPhase(game.getPhaseHandler().getPlayerTurn(), game.getPhaseHandler().getPhase())) {
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
            if (isUiSetToSkipPhase(game.getPhaseHandler().getPlayerTurn(), game.getPhaseHandler().getPhase())) {
                return null; //avoid prompt for input if stack is empty and player is set to skip the current phase
            }
        }
        else if (!game.getDisableAutoYields()) {
            SpellAbility ability = stack.peekAbility();
            if (ability != null && ability.isAbility() && shouldAutoYield(ability.toUnsuppressedString())) {
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

        InputPassPriority defaultInput = new InputPassPriority(this, player);
        defaultInput.showAndWait();
        return defaultInput.getChosenSa();
    }

    @Override
    public void playChosenSpellAbility(SpellAbility chosenSa) {
        HumanPlay.playSpellAbility(this, player, chosenSa);
    }

    @Override
    public List<Card> chooseCardsToDiscardToMaximumHandSize(int nDiscard) {
        final int max = player.getMaxHandSize();

        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, nDiscard, nDiscard, player.getZone(ZoneType.Hand).getCards());
        String message = "Cleanup Phase\nSelect " + nDiscard + " card" + (nDiscard > 1 ? "s" : "") + 
                " to discard to bring your hand down to the maximum of " + max + " cards.";
        inp.setMessage(message);
        inp.setCancelAllowed(false);
        inp.showAndWait();
        return Lists.newArrayList(inp.getSelected());
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseCardsToRevealFromHand(int, int, java.util.List)
     */
    @Override
    public List<Card> chooseCardsToRevealFromHand(int min, int max, List<Card> valid) {
        max = Math.min(max, valid.size());
        min = Math.min(min, max);
        InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage("Choose Which Cards to Reveal");
        inp.showAndWait();
        return Lists.newArrayList(inp.getSelected());
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#payManaOptional(forge.Card, forge.card.cost.Cost)
     */
    @Override
    public boolean payManaOptional(Card c, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose) {
        if (sa == null && cost.isOnlyManaCost() && cost.getTotalMana().isZero() 
                && !FModel.getPreferences().getPrefBoolean(FPref.MATCHPREF_PROMPT_FREE_BLOCKS))
            return true;
        
        return HumanPlay.payCostDuringAbilityResolve(this, player, c, cost, sa, prompt);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseSaToActivateFromOpeningHand(java.util.List)
     */
    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        List<Card> srcCards = new ArrayList<Card>();
        for (SpellAbility sa : usableFromOpeningHand) {
            srcCards.add(sa.getHostCard());
        }
        List<SpellAbility> result = new ArrayList<SpellAbility>();
        if (srcCards.isEmpty()) {
            return result;
        }
        final List<CardView> chosen = SGuiChoose.many(getGui(), "Choose cards to activate from opening hand and their order", "Activate first", -1, gameView.getCardViews(srcCards), null);
        for (final CardView view : chosen) {
            final Card c = getCard(view);
            for (SpellAbility sa : usableFromOpeningHand) {
                if (sa.getHostCard() == c) {
                    result.add(sa);
                    break;
                }
            }
        }
        return result;
    }

    // end of not related candidates for move.

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseBinary(java.lang.String, boolean)
     */
    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        String[] labels = new String[]{"Option1", "Option2"};
        switch(kindOfChoice) {
            case HeadsOrTails:  labels = new String[]{"Heads", "Tails"}; break;
            case TapOrUntap:    labels = new String[]{"Tap", "Untap"}; break;
            case OddsOrEvens:   labels = new String[]{"Odds", "Evens"}; break;
            case UntapOrLeaveTapped:    labels = new String[]{"Untap", "Leave tapped"}; break;
            case UntapTimeVault: labels = new String[]{"Untap (and skip this turn)", "Leave tapped"}; break;
            case PlayOrDraw:    labels = new String[]{"Play", "Draw"}; break;
            default:            labels = kindOfChoice.toString().split("Or");

        }
        return SGuiDialog.confirm(getGui(), gameView.getCardView(sa.getHostCard()), question, defaultVal == null || defaultVal.booleanValue(), labels);
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        String[] labelsSrc = call ? new String[]{"heads", "tails"} : new String[]{"win the flip", "lose the flip"};
        String[] strResults = new String[results.length];
        for (int i = 0; i < results.length; i++) {
            strResults[i] = labelsSrc[results[i] ? 0 : 1];
        }
        return SGuiChoose.one(getGui(), sa.getHostCard().getName() + " - Choose a result", strResults) == labelsSrc[0];
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap) {
        String title = entityBeingDamaged + " - select which prevention shield to use";
        return choiceMap.get(SGuiChoose.one(getGui(), title, options));
    }

    @Override
    public Pair<CounterType,String> chooseAndRemoveOrPutCounter(Card cardWithCounter) {
        if (!cardWithCounter.hasCounters()) {
            System.out.println("chooseCounterType was reached with a card with no counters on it. Consider filtering this card out earlier");
            return null;
        }

        String counterChoiceTitle = "Choose a counter type on " + cardWithCounter;
        final CounterType chosen = SGuiChoose.one(getGui(), counterChoiceTitle, cardWithCounter.getCounters().keySet());

        String putOrRemoveTitle = "What to do with that '" + chosen.getName() + "' counter ";
        final String putString = "Put another " + chosen.getName() + " counter on " + cardWithCounter;
        final String removeString = "Remove a " + chosen.getName() + " counter from " + cardWithCounter;
        final String addOrRemove = SGuiChoose.one(getGui(), putOrRemoveTitle, new String[]{putString,removeString});

        return new ImmutablePair<CounterType,String>(chosen,addOrRemove);
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility saSpellskite, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        if (allTargets.size() < 2) {
            return Iterables.getFirst(allTargets, null);
        }

        final Function<Pair<SpellAbilityStackInstance, GameObject>, String> fnToString = new Function<Pair<SpellAbilityStackInstance, GameObject>, String>() {
            @Override
            public String apply(Pair<SpellAbilityStackInstance, GameObject> targ) {
                return targ.getRight().toString() + " - " + targ.getLeft().getStackDescription();
            }
        };

        List<Pair<SpellAbilityStackInstance, GameObject>> chosen = SGuiChoose.getChoices(getGui(), saSpellskite.getHostCard().getName(), 1, 1, allTargets, null, fnToString);
        return Iterables.getFirst(chosen, null);
    }

    @Override
    public void notifyOfValue(SpellAbility sa, GameObject realtedTarget, String value) {
        String message = formatNotificationMessage(sa, realtedTarget, value);
        if (sa.isManaAbility()) {
            game.getGameLog().add(GameLogEntryType.LAND, message);
        } else {
            SGuiDialog.message(getGui(), message, sa.getHostCard() == null ? "" : getCardView(sa.getHostCard()).toString());
        }
    }

    private String formatMessage(String message, Object related) {
        if(related instanceof Player && message.indexOf("{player") >= 0)
            message = message.replace("{player}", mayBeYou(related)).replace("{player's}", Lang.getPossesive(mayBeYou(related)));
        
        return message;
    }

    // These are not much related to PlayerController
    private String formatNotificationMessage(SpellAbility sa, GameObject target, String value) {
        if (sa.getApi() == null || sa.getHostCard() == null) {
            return ("Result: " + value);
        }
        switch(sa.getApi()) {
            case ChooseDirection:
                return value;
            case ChooseNumber:
                if (sa.hasParam("SecretlyChoose")) {
                    return value;
                }
                final boolean random = sa.hasParam("Random");
                return String.format(random ? "Randomly chosen number for %s is %s" : "%s choses number: %s", mayBeYou(target), value);
            case FlipACoin:
                String flipper = StringUtils.capitalize(mayBeYou(target));
                return sa.hasParam("NoCall")
                        ? String.format("%s flip comes up %s", Lang.getPossesive(flipper), value)
                        : String.format("%s %s the flip", flipper, Lang.joinVerb(flipper, value));
            case Protection:
                String choser = StringUtils.capitalize(mayBeYou(target));
                return String.format("%s %s protection from %s", choser, Lang.joinVerb(choser, "choose"), value);
            case Vote:
                String chooser = StringUtils.capitalize(mayBeYou(target));
                return String.format("%s %s %s", chooser, Lang.joinVerb(chooser, "vote"), value);
            default:
                return String.format("%s effect's value for %s is %s", sa.getHostCard().getName(), mayBeYou(target), value);
        }
    }

    private String mayBeYou(Object what) {
        return what == null ? "(null)" : what == player ? "you" : what.toString();
    }

    // end of not related candidates for move.

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseModeForAbility(forge.card.spellability.SpellAbility, java.util.List, int, int)
     */
    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num) {
        List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
        String modeTitle = String.format("%s activated %s - Choose a mode", sa.getActivatingPlayer(), sa.getHostCard());
        List<AbilitySub> chosen = new ArrayList<AbilitySub>();
        for (int i = 0; i < num; i++) {
            AbilitySub a;
            if (i < min) {
                a = SGuiChoose.one(getGui(), modeTitle, choices);
            }
            else {
                a = SGuiChoose.oneOrNone(getGui(), modeTitle, choices);
            }
            if (null == a) {
                break;
            }

            choices.remove(a);
            chosen.add(a);
        }
        return chosen;
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        return SGuiChoose.getChoices(getGui(), message, min, max, options);
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
        if(withColorless)
            colorNames[i++] = MagicColor.toLongString((byte)0);
        for (byte b : colors) {
            colorNames[i++] = MagicColor.toLongString(b);
        }
        if (colorNames.length > 2) {
            return MagicColor.fromName(SGuiChoose.one(getGui(), message, colorNames));
        }
        int idxChosen = SGuiDialog.confirm(getGui(), gameView.getCardView(c), message, colorNames) ? 0 : 1;
        return MagicColor.fromName(colorNames[idxChosen]);
    }

    @Override
    public PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name) {
        Iterable<PaperCard> cardsFromDb = FModel.getMagicDb().getCommonCards().getUniqueCards();
        List<PaperCard> cards = Lists.newArrayList(Iterables.filter(cardsFromDb, cpp));
        Collections.sort(cards);
        return SGuiChoose.one(getGui(), message, cards);
    }

    @Override
    public CounterType chooseCounterType(Collection<CounterType> options, SpellAbility sa, String prompt) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return SGuiChoose.one(getGui(), prompt, options);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String question) {
        InputConfirm inp = new InputConfirm(this, question);
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers, HashMap<String, Object> runParams) {
        if(possibleReplacers.size() == 1)
            return possibleReplacers.get(0);
        return SGuiChoose.one(getGui(), prompt, possibleReplacers);
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        return SGuiChoose.one(getGui(), string, choices);
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, List<Player> allPayers) {
        // if it's paid by the AI already the human can pay, but it won't change anything
        return HumanPlay.payCostDuringAbilityResolve(this, player, sa.getHostCard(), cost, sa, null);
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        List<SpellAbility> orderedSAs = activePlayerSAs;
        if (activePlayerSAs.size() > 1) { // give a dual list form to create instead of needing to do it one at a time
            final List<SpellAbilityView> orderedSAViews = SGuiChoose.order(getGui(), "Select order for Simultaneous Spell Abilities", "Resolve first", gameView.getSpellAbilityViews(activePlayerSAs), null);
            orderedSAs = getSpellAbilities(orderedSAViews);
        }
        int size = orderedSAs.size();
        for (int i = size - 1; i >= 0; i--) {
            SpellAbility next = orderedSAs.get(i);
            if (next.isTrigger()) {
                HumanPlay.playSpellAbility(this, player, next);
            } else {
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
    public boolean chooseCardsPile(SpellAbility sa, List<Card> pile1, List<Card> pile2, boolean faceUp) {
        if (!faceUp) {
            final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
            final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());
            final String[] possibleValues = { p1Str , p2Str };
            return SGuiDialog.confirm(getGui(), gameView.getCardView(sa.getHostCard()), "Choose a Pile", possibleValues);
        } else {
            final Card[] disp = new Card[pile1.size() + pile2.size() + 2];
            disp[0] = new Card(-1);
            disp[0].setName("Pile 1");
            for (int i = 0; i < pile1.size(); i++) {
                disp[1 + i] = pile1.get(i);
            }
            disp[pile1.size() + 1] = new Card(-2);
            disp[pile1.size() + 1].setName("Pile 2");
            for (int i = 0; i < pile2.size(); i++) {
                disp[pile1.size() + i + 2] = pile2.get(i);
            }

            // make sure Pile 1 or Pile 2 is clicked on
            while (true) {
                final Object o = SGuiChoose.one(getGui(), "Choose a pile", disp);
                final Card c = (Card) o;
                String name = c.getName();

                if (!(name.equals("Pile 1") || name.equals("Pile 2"))) {
                    continue;
                }

                return name.equals("Pile 1");
            }
        }
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        for (Player p : removedAnteCards.keySet()) {
            SGuiChoose.reveal(getGui(), message + " from " + Lang.getPossessedObject(mayBeYou(p), "deck"), removedAnteCards.get(p));
        }
    }

    @Override
    public CardShields chooseRegenerationShield(Card c) {
        if (c.getShield().size() < 2) {
            return Iterables.getFirst(c.getShield(), null);
        }
        return SGuiChoose.one(getGui(), "Choose a regeneration shield:", c.getShield());
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        return SGuiChoose.many(getGui(), "Select cards to add to your deck", "Add these to my deck", 0, losses.size(), losses, null);
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, boolean isActivatedSa) {
        return HumanPlay.payManaCost(this, toPay, costPartMana, sa, player, prompt, isActivatedSa);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa, ManaCost manaCost, List<Card> untappedCreats) {
        InputSelectCardsForConvoke inp = new InputSelectCardsForConvoke(this, player, manaCost, untappedCreats);
        inp.showAndWait();
        return inp.getConvokeMap();
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<PaperCard> cpp, String valid, String message) {
        PaperCard cp = null;
        while(true) {
            cp = chooseSinglePaperCard(sa, message, cpp, sa.getHostCard().getName());
            Card instanceForPlayer = Card.fromPaperCard(cp, player); // the Card instance for test needs a game to be tested
            if (instanceForPlayer.isValid(valid, sa.getHostCard().getController(), sa.getHostCard()))
                return cp.getName();
        }
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, List<Card> fetchList, String selectPrompt, boolean b, Player decider) {
        return chooseSingleEntityForEffect(fetchList, sa, selectPrompt, b, decider);
    }

    public boolean isGuiPlayer() {
        return lobbyPlayer == getGui().getGuiPlayer();
    }

    /*
     * What follows are the View methods.
     */

    private class GameView extends LocalGameView {

        public GameView(Game game) {
            super(game);
        }

        @Override
        public GameOutcome.AnteResult getAnteResult() {
            return this.getGame().getOutcome().anteResult.get(player);
        }

        @Override
        public boolean canUndoLastAction() {
            if (!game.stack.canUndo()) {
                return false;
            }
            final Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
            if (priorityPlayer == null || priorityPlayer != player) {
                return false;
            }
            return true;
        }

        @Override
        public boolean tryUndoLastAction() {
            if (!canUndoLastAction()) {
                return false;
            }

            if (game.getStack().undo()) {//canUndoLastAction(gui) && GuiBase.getInterface().getGame().stack.undo()) {
                final Input currentInput = getGui().getInputQueue().getInput();
                if (currentInput instanceof InputPassPriority) {
                    currentInput.showMessageInitial(); //ensure prompt updated if needed
                }
                return true;
            }
            return false;
        }

        @Override
        public void selectButtonOk() {
            getInputProxy().selectButtonOK();
        }

        @Override
        public void selectButtonCancel() {
            getInputProxy().selectButtonCancel();
        }

        @Override
        public void confirm() {
            if (getGui().getInputQueue().getInput() instanceof InputConfirm) {
                selectButtonOk();
            }
        }

        @Override
        public boolean passPriority() {
            return getInputProxy().passPriority();
        }

        @Override
        public boolean passPriorityUntilEndOfTurn() {
            return getInputProxy().passPriorityUntilEndOfTurn();
        }

        @Override
        public void useMana(final byte mana) {
            final Input input = getGui().getInputQueue().getInput();
            if (input instanceof InputPayMana) {
                ((InputPayMana) input).useManaFromPool(mana);
            }
        }

        @Override
        public void selectPlayer(final PlayerView player, final ITriggerEvent triggerEvent) {
            getInputProxy().selectPlayer(player, triggerEvent);
        }

        @Override
        public boolean selectCard(final CardView card, final ITriggerEvent triggerEvent) {
            return getInputProxy().selectCard(card, triggerEvent);
        }

        @Override
        public void selectAbility(final SpellAbilityView sa) {
            getInputProxy().selectAbility(sa);
        }

        @Override
        public void alphaStrike() {
            final Combat combat = game.getCombat();
            if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || combat == null
                    || game.getPhaseHandler().getPlayerTurn() != player
                    || !(getGui().getInputQueue().getInput() instanceof InputAttack)) {
                return;
            }

            final List<Player> defenders = player.getOpponents();

            for (final Card c : CardLists.filter(player.getCardsIn(ZoneType.Battlefield), Presets.CREATURES)) {
                if (combat.isAttacking(c))
                    continue;

                for (final Player defender : defenders) {
                    if (CombatUtil.canAttack(c, defender, combat)) {
                        combat.addAttacker(c, defender);
                        break;
                    }
                }
            }
        }

        /**
         * Check whether a card may be shown. If {@code mayLookAtAllCards} is
         * {@code true}, any card may be shown.
         * 
         * @param c a card.
         * @return whether the card may be shown.
         * @see GameView#mayShowCardNoRedirect(CardView)
         */
        @Override
        public boolean mayShowCard(final CardView c) {
            if (mayLookAtAllCards()) {
                return true;
            }
            final Card card = getCard(c);
            return card == null || mayLookAt.contains(card) || card.canBeShownTo(player);
        }

        @Override
        public boolean mayShowCardFace(final CardView c) {
            if (mayLookAtAllCards()) {
                return true;
            }
            final Card card = getCard(c);
            return card == null || !c.isFaceDown() || card.canCardFaceBeShownTo(player);
        }

        @Override
        public boolean getDisableAutoYields() {
            return this.getGame().getDisableAutoYields();
        }
        @Override
        public void setDisableAutoYields(final boolean b) {
            this.getGame().setDisableAutoYields(b);
        }

        // Dev mode functions
        @Override
        public void devTogglePlayManyLands(final boolean b) {
            player.canCheatPlayUnlimitedLands = b;
        }
        @Override
        public void devGenerateMana() {
            DevModeUtil.devModeGenerateMana(game, PlayerControllerHuman.this);
        }
        @Override
        public void devSetupGameState() {
            DevModeUtil.devSetupGameState(game, PlayerControllerHuman.this);
        }
        @Override
        public void devTutorForCard() {
            DevModeUtil.devModeTutor(game, PlayerControllerHuman.this);
        }
        @Override
        public void devAddCardToHand() {
            DevModeUtil.devModeCardToHand(game, PlayerControllerHuman.this);
        }
        @Override
        public void devAddCounterToPermanent() {
            DevModeUtil.devModeAddCounter(game, PlayerControllerHuman.this);
        }
        @Override
        public void devTapPermanent() {
            DevModeUtil.devModeTapPerm(game, PlayerControllerHuman.this);
        }
        @Override
        public void devUntapPermanent() {
            DevModeUtil.devModeUntapPerm(game, PlayerControllerHuman.this);
        }
        @Override
        public void devSetPlayerLife() {
            DevModeUtil.devModeSetLife(game, PlayerControllerHuman.this);
        }
        @Override
        public void devWinGame() {
            DevModeUtil.devModeWinGame(game, PlayerControllerHuman.this);
        }
        @Override
        public void devAddCardToBattlefield() {
            DevModeUtil.devModeCardToBattlefield(game, PlayerControllerHuman.this);
        }
        @Override
        public void devRiggedPlanerRoll() {
            DevModeUtil.devModeRiggedPlanarRoll(game, PlayerControllerHuman.this);
        }
        @Override
        public void devPlaneswalkTo() {
            DevModeUtil.devModePlaneswalkTo(game, PlayerControllerHuman.this);
        }

    }

    /**
     * @return
     * @see forge.view.LocalGameView#getPlayers()
     */
    public List<PlayerView> getPlayers() {
        return gameView.getPlayers();
    }

    /**
     * @return
     * @see forge.view.LocalGameView#getPlayerTurn()
     */
    public PlayerView getPlayerTurn() {
        return gameView.getPlayerTurn();
    }

    /**
     * @return
     * @see forge.view.LocalGameView#getCombat()
     */
    public CombatView getCombat() {
        return gameView.getCombat();
    }

    /**
     * @param c
     * @return
     * @see forge.view.LocalGameView#getCombat(forge.game.combat.Combat)
     */
    public CombatView getCombat(Combat c) {
        return gameView.getCombat(c);
    }

    /**
     * @param si
     * @return
     * @see forge.view.LocalGameView#getStackItemView(forge.game.spellability.SpellAbilityStackInstance)
     */
    public StackItemView getStackItemView(SpellAbilityStackInstance si) {
        return gameView.getStackItemView(si);
    }

    /**
     * @param view
     * @return
     * @see forge.view.LocalGameView#getStackItem(forge.view.StackItemView)
     */
    public SpellAbilityStackInstance getStackItem(StackItemView view) {
        return gameView.getStackItem(view);
    }

    /**
     * @param e
     * @return
     * @see forge.view.LocalGameView#getGameEntityView(forge.game.GameEntity)
     */
    public final GameEntityView getGameEntityView(GameEntity e) {
        return gameView.getGameEntityView(e);
    }

    /**
     * @param players
     * @return
     * @see forge.view.LocalGameView#getPlayerViews(java.lang.Iterable)
     */
    public final List<PlayerView> getPlayerViews(Iterable<Player> players) {
        return gameView.getPlayerViews(players);
    }

    /**
     * @param p
     * @return
     * @see forge.view.LocalGameView#getPlayerView(forge.game.player.Player)
     */
    public PlayerView getPlayerView(Player p) {
        return gameView.getPlayerView(p);
    }

    /**
     * @param p
     * @return
     * @see forge.view.LocalGameView#getPlayer(forge.view.PlayerView)
     */
    public Player getPlayer(PlayerView p) {
        return gameView.getPlayer(p);
    }

    /**
     * @param c
     * @return
     * @see forge.view.LocalGameView#getCardView(forge.game.card.Card)
     */
    public CardView getCardView(Card c) {
        return gameView.getCardView(c);
    }

    /**
     * @param cards
     * @return
     * @see forge.view.LocalGameView#getCardViews(java.util.List)
     */
    public final List<CardView> getCardViews(List<Card> cards) {
        return gameView.getCardViews(cards);
    }

    /**
     * @param cards
     * @return
     * @see forge.view.LocalGameView#getCardViews(java.lang.Iterable)
     */
    public final List<CardView> getCardViews(Iterable<Card> cards) {
        return gameView.getCardViews(cards);
    }

    /**
     * @param c
     * @return
     * @see forge.view.LocalGameView#getCard(forge.view.CardView)
     */
    public Card getCard(CardView c) {
        return gameView.getCard(c);
    }

    /**
     * @param cards
     * @return
     * @see forge.view.LocalGameView#getCards(java.util.List)
     */
    public final List<Card> getCards(List<CardView> cards) {
        return gameView.getCards(cards);
    }

    /**
     * @param sa
     * @return
     * @see forge.view.LocalGameView#getSpellAbilityView(forge.game.spellability.SpellAbility)
     */
    public SpellAbilityView getSpellAbilityView(SpellAbility sa) {
        return gameView.getSpellAbilityView(sa);
    }

    /**
     * @param cards
     * @return
     * @see forge.view.LocalGameView#getSpellAbilityViews(java.util.List)
     */
    public final List<SpellAbilityView> getSpellAbilityViews(
            List<SpellAbility> cards) {
        return gameView.getSpellAbilityViews(cards);
    }

    /**
     * @param c
     * @return
     * @see forge.view.LocalGameView#getSpellAbility(forge.view.SpellAbilityView)
     */
    public SpellAbility getSpellAbility(SpellAbilityView c) {
        return gameView.getSpellAbility(c);
    }

    /**
     * @param cards
     * @return
     * @see forge.view.LocalGameView#getSpellAbilities(java.util.List)
     */
    public final List<SpellAbility> getSpellAbilities(
            List<SpellAbilityView> cards) {
        return gameView.getSpellAbilities(cards);
    }

}
