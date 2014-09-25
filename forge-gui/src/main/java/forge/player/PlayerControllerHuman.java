package forge.player;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import forge.FThreads;
import forge.LobbyPlayer;
import forge.achievement.AchievementCollection;
import forge.card.CardCharacteristicName;
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
import forge.game.PlanarDice;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
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
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.interfaces.IGuiBase;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.match.MatchUtil;
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
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;
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
    private final GameView gameView;
    /**
     * Cards this player may look at right now, for example when searching a
     * library.
     */
    private final Set<Card> mayLookAt = Sets.newHashSet();
    private boolean mayLookAtAllCards = false;

    public PlayerControllerHuman(Game game0, Player p, LobbyPlayer lp, IGuiBase gui) {
        super(game0, p, lp);
        if (p.getController() == null || p.getLobbyPlayer() == lp) {
            gameView = new GameView(gui, game0);

            // aggressively cache a view for each player (also caches cards)
            for (final Player player : game.getRegisteredPlayers()) {
                gameView.getPlayerView(player);
            }
        }
        else { //handle the case of one player controlling another
            for (Player p0 : game.getPlayers()) {
                if (p0.getLobbyPlayer() == lp) {
                    p = p0;
                    break;
                }
            }
            gameView = (GameView)MatchUtil.getGameView(p);
        }
    }

    public IGuiBase getGui() {
        return gameView.getGui();
    }

    public InputProxy getInputProxy() {
        return gameView.getInputProxy();
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
        return !MatchUtil.getController().stopAtPhase(gameView.getPlayerView(turn), phase);
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */ 
    public SpellAbility getAbilityToPlay(final List<SpellAbility> abilities, final ITriggerEvent triggerEvent) {
        final int choice = MatchUtil.getController().getAbilityToPlay(gameView.getSpellAbilityViews(abilities), triggerEvent);
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
                final Map<CardView, Integer> result = MatchUtil.getDamageToAssign(vAttacker, vBlockers, damageDealt, vDefender, overrideOrder);
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
        return choosePermanentsTo(min, max, valid, message, "sacrifice");
    }

    @Override
    public List<Card> choosePermanentsToDestroy(SpellAbility sa, int min, int max, List<Card> valid, String message) {
        return choosePermanentsTo(min, max, valid, message, "destroy");
    }

    private List<Card> choosePermanentsTo(int min, int max, List<Card> valid, String message, String action) {
        max = Math.min(max, valid.size());
        if (max <= 0) {
            return new ArrayList<Card>();
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

        MatchUtil.getController().setPanelSelection(gameView.getCardView(sa.getHostCard()));

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
        }
        else {
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
        MatchUtil.getController().setPanelSelection(vAttacker);
        final List<CardView> choices = SGuiChoose.order(getGui(), "Choose Damage Order for " + vAttacker, "Damaged First", gameView.getCardViews(blockers), vAttacker);
        return gameView.getCards(choices);
    }

    @Override
    public List<Card> orderBlocker(final Card attacker, final Card blocker, final List<Card> oldBlockers) {
        final CardView vAttacker = gameView.getCardView(attacker);
        MatchUtil.getController().setPanelSelection(vAttacker);
        final List<CardView> choices = SGuiChoose.insertInList(getGui(), "Choose blocker after which to place " + vAttacker + " in damage order; cancel to place it first", gameView.getCardView(blocker), gameView.getCardViews(oldBlockers));
        return gameView.getCards(choices);
    }

    @Override
    public List<Card> orderAttackers(final Card blocker, final List<Card> attackers) {
        final CardView vBlocker = gameView.getCardView(blocker);
        MatchUtil.getController().setPanelSelection(vBlocker);
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
        List<CardView> choices;
        mayLookAt.addAll(cards);
        switch (destinationZone) {
            case Library:
                choices = SGuiChoose.order(getGui(), "Choose order of cards to put into the library", "Closest to top", getCardViews(cards), null);
                break;
            case Battlefield:
                choices = SGuiChoose.order(getGui(), "Choose order of cards to put onto the battlefield", "Put first", getCardViews(cards), null);
                break;
            case Graveyard:
                choices = SGuiChoose.order(getGui(), "Choose order of cards to put into the graveyard", "Closest to bottom", getCardViews(cards), null);
                break;
            case PlanarDeck:
                choices = SGuiChoose.order(getGui(), "Choose order of cards to put into the planar deck", "Closest to top", getCardViews(cards), null);
                break;
            case SchemeDeck:
                choices = SGuiChoose.order(getGui(), "Choose order of cards to put into the scheme deck", "Closest to top", getCardViews(cards), null);
                break;
            case Stack:
                choices = SGuiChoose.order(getGui(), "Choose order of copies to cast", "Put first", getCardViews(cards), null);
                break;
            default:
                System.out.println("ZoneType " + destinationZone + " - Not Ordered");
                mayLookAt.clear();
                return cards;
        }
        mayLookAt.clear();
        return getCards(choices);
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
        int cardsInGrave = Math.min(colorLessAmount, grave.size());
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

            grave.remove(gameView.getCard(nowChosen));
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
        if (isOptional) {
            return SGuiChoose.oneOrNone(getGui(), "Choose a " + kindOfType.toLowerCase() + " type", types);
        }
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
                    MatchUtil.fireEvent(new UiEventAttackerDeclared(gameView.getCardView(attacker.getLeft()), gameView.getGameEntityView(attacker.getRight())));
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
            PlayerView playerView = gameView.getLocalPlayerView();
            MatchUtil.getController().showPromptMessage(playerView, "Yielding until " + (phase == PhaseType.CLEANUP ? "end of turn" : phase.nameForUi.toString()) +
                    ".\nYou may cancel this yield to take an action.");
            ButtonUtil.update(playerView, false, true, false);
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
        PlayerView playerView = gameView.getLocalPlayerView();
        MatchUtil.getController().showPromptMessage(playerView, "");
        ButtonUtil.update(playerView, false, false, false);
        InputBase.awaitNextInput(gameView);
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
                && !FModel.getPreferences().getPrefBoolean(FPref.MATCHPREF_PROMPT_FREE_BLOCKS)) {
            return true;
        }
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
        switch (kindOfChoice) {
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
        }
        else {
            SGuiDialog.message(getGui(), message, sa.getHostCard() == null ? "" : getCardView(sa.getHostCard()).toString());
        }
    }

    private String formatMessage(String message, Object related) {
        if (related instanceof Player && message.indexOf("{player") >= 0) {
            message = message.replace("{player}", mayBeYou(related)).replace("{player's}", Lang.getPossesive(mayBeYou(related)));
        }
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
        if (withColorless) {
            colorNames[i++] = MagicColor.toLongString((byte)0);
        }
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
        if (possibleReplacers.size() == 1) {
            return possibleReplacers.get(0);
        }
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
    public boolean chooseCardsPile(SpellAbility sa, List<Card> pile1, List<Card> pile2, boolean faceUp) {
        if (!faceUp) {
            final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
            final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());
            final String[] possibleValues = { p1Str , p2Str };
            return SGuiDialog.confirm(getGui(), gameView.getCardView(sa.getHostCard()), "Choose a Pile", possibleValues);
        }

        mayLookAt.addAll(pile1);
        mayLookAt.addAll(pile2);

        final int idPile1 = Integer.MIN_VALUE, idPile2 = Integer.MIN_VALUE + 1;
        final List<CardView> cards = Lists.newArrayListWithCapacity(pile1.size() + pile2.size() + 2);
        final CardView pileView1 = new CardView(true) {
            @Override
            public String toString() {
                return "--- Pile 1 ---";
            }
        };
        pileView1.setId(idPile1);
        cards.add(pileView1);
        cards.addAll(getCardViews(pile1));

        final CardView pileView2 = new CardView(true) {
            @Override
            public String toString() {
                return "--- Pile 2 ---";
            }
        };
        pileView2.setId(idPile2);
        cards.add(pileView2);
        cards.addAll(getCardViews(pile2));

        // make sure Pile 1 or Pile 2 is clicked on
        boolean result;
        while (true) {
            final CardView chosen = SGuiChoose.one(getGui(), "Choose a pile", cards);
            if (chosen.equals(pileView1)) {
                result = true;
                break;
            } else if (chosen.equals(pileView2)) {
                result = false;
                break;
            }
        }

        mayLookAt.clear();
        return result;
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
        while (true) {
            PaperCard cp = chooseSinglePaperCard(sa, message, cpp, sa.getHostCard().getName());
            Card instanceForPlayer = Card.fromPaperCard(cp, player); // the Card instance for test needs a game to be tested
            if (instanceForPlayer.isValid(valid, sa.getHostCard().getController(), sa.getHostCard())) {
                return cp.getName();
            }
        }
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, List<Card> fetchList, String selectPrompt, boolean isOptional, Player decider) {
        return chooseSingleEntityForEffect(fetchList, sa, selectPrompt, isOptional, decider);
    }

    public boolean isGuiPlayer() {
        return lobbyPlayer == GamePlayerUtil.getGuiPlayer();
    }

    /*
     * What follows are the View methods.
     */
    private class GameView extends LocalGameView {
        public GameView(IGuiBase gui0, Game game0) {
            super(gui0, game0);
        }

        @Override
        public GameOutcome.AnteResult getAnteResult() {
            return game.getOutcome().anteResult.get(player);
        }

        @Override
        public void updateAchievements() {
            AchievementCollection.updateAll(PlayerControllerHuman.this);
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
            getInputProxy().selectButtonOK();
        }

        @Override
        public void selectButtonCancel() {
            getInputProxy().selectButtonCancel();
        }

        @Override
        public void confirm() {
            if (inputQueue.getInput() instanceof InputConfirm) {
                selectButtonOk();
            }
        }

        @Override
        public boolean passPriority() {
            return passPriority(false);
        }
        @Override
        public boolean passPriorityUntilEndOfTurn() {
            return passPriority(true);
        }
        private boolean passPriority(final boolean passUntilEndOfTurn) {
            final Input inp = gameView.getInputProxy().getInput();
            if (inp instanceof InputPassPriority) {
                if (passUntilEndOfTurn) {
                    autoPassUntilEndOfTurn();
                }
                inp.selectButtonOK();
                return true;
            }

            FThreads.invokeInEdtNowOrLater(getGui(), new Runnable() {
                @Override
                public void run() {
                    SOptionPane.showMessageDialog(getGui(), "Cannot pass priority at this time.");
                }
            });
            return false;
        }

        @Override
        public void useMana(final byte mana) {
            final Input input = inputQueue.getInput();
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
            getInputProxy().alphaStrike();
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
        public boolean mayShowCard(final Card c) {
            if (mayLookAtAllCards()) {
                return true;
            }
            return c == null || mayLookAt.contains(c) || c.canBeShownTo(player);
        }

        @Override
        public boolean mayShowCardFace(final Card c) {
            if (mayLookAtAllCards()) {
                return true;
            }
            return c == null || !c.isFaceDown() || c.canCardFaceBeShownTo(player);
        }

        @Override
        public Iterable<String> getAutoYields() {
            return PlayerControllerHuman.this.getAutoYields();
        }

        @Override
        public boolean shouldAutoYield(final String key) {
            return PlayerControllerHuman.this.shouldAutoYield(key);
        }

        @Override
        public void setShouldAutoYield(String key, boolean autoYield) {
            PlayerControllerHuman.this.setShouldAutoYield(key, autoYield);
        }

        @Override
        public boolean getDisableAutoYields() {
            return game.getDisableAutoYields();
        }
        @Override
        public void setDisableAutoYields(final boolean b) {
            game.setDisableAutoYields(b);
        }

        @Override
        public boolean shouldAlwaysAcceptTrigger(final Integer trigger) {
            return PlayerControllerHuman.this.shouldAlwaysAcceptTrigger(trigger);
        }

        @Override
        public boolean shouldAlwaysDeclineTrigger(final Integer trigger) {
            return PlayerControllerHuman.this.shouldAlwaysDeclineTrigger(trigger);
        }

        public boolean shouldAlwaysAskTrigger(final Integer trigger) {
            return PlayerControllerHuman.this.shouldAlwaysAskTrigger(trigger);
        }

        @Override
        public void setShouldAlwaysAcceptTrigger(final Integer trigger) {
            PlayerControllerHuman.this.setShouldAlwaysAcceptTrigger(trigger);
        }

        @Override
        public void setShouldAlwaysDeclineTrigger(final Integer trigger) {
            PlayerControllerHuman.this.setShouldAlwaysDeclineTrigger(trigger);
        }

        @Override
        public void setShouldAlwaysAskTrigger(final Integer trigger) {
            PlayerControllerHuman.this.setShouldAlwaysAskTrigger(trigger);
        }

        @Override
        public void autoPassCancel() {
            PlayerControllerHuman.this.autoPassCancel();
        }

        @Override
        public boolean canPlayUnlimitedLands() {
            return PlayerControllerHuman.this.canPlayUnlimitedLands();
        }

        @Override
        public boolean canViewAllCards() {
            return PlayerControllerHuman.this.mayLookAtAllCards;
        }

        @Override
        public DevModeCheats cheat() {
            return PlayerControllerHuman.this.cheat();
        }
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

    public PlayerView getPlayerView() {
        return gameView.getPlayerView(getPlayer());
    }
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
    public final List<CardView> getCardViews(final Iterable<Card> cards) {
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
    public final List<SpellAbility> getSpellAbilities(List<SpellAbilityView> cards) {
        return gameView.getSpellAbilities(cards);
    }

    public boolean canUndoLastAction() {
        return gameView.canUndoLastAction();
    }

    public boolean tryUndoLastAction() {
        return gameView.tryUndoLastAction();
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
    private DevModeCheats cheats;
    public DevModeCheats cheat() {
        if (cheats == null) {
            cheats = new DevModeCheats();
            //TODO: In Network game, inform other players that this player is cheating
        }
        return cheats;
    }
    public boolean hasCheated() {
        return cheats != null;
    }
    public class DevModeCheats {
        private DevModeCheats() {
        }

        public void setCanPlayUnlimitedLands(boolean canPlayUnlimitedLands0) {
            canPlayUnlimitedLands = canPlayUnlimitedLands0;
        }

        public void setViewAllCards(final boolean canViewAll) {
            mayLookAtAllCards = canViewAll;
            for (final Player p : game.getPlayers()) {
                MatchUtil.updateCards(getCardViews(p.getAllCards()));
            }
        }

        public void generateMana() {
            Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                SGuiDialog.message(getGui(), "No player has priority at the moment, so mana cannot be added to their pool.");
                return;
            }

            final Card dummy = new Card(-777777);
            dummy.setOwner(pPriority);
            Map<String, String> produced = new HashMap<String, String>();
            produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
            final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
            game.getAction().invoke(new Runnable() {
                @Override public void run() { abMana.produceMana(null); }
            });
        }

        public void setupGameState() {
            int humanLife = -1;
            int computerLife = -1;

            final Map<ZoneType, String> humanCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);
            final Map<ZoneType, String> aiCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);

            String tChangePlayer = "NONE";
            String tChangePhase = "NONE";

            File gamesDir = new File(ForgeConstants.USER_GAMES_DIR);
            if (!gamesDir.exists()) { // if the directory does not exist, try to create it
                gamesDir.mkdir();
            }
            
            String filename = getGui().showFileDialog("Select Game State File", ForgeConstants.USER_GAMES_DIR);
            if (filename == null) {
                return;
            }

            try {
                final FileInputStream fstream = new FileInputStream(filename);
                final DataInputStream in = new DataInputStream(fstream);
                final BufferedReader br = new BufferedReader(new InputStreamReader(in));

                String temp = "";

                while ((temp = br.readLine()) != null) {

                    final String[] tempData = temp.split("=");
                    if (tempData.length < 2 || temp.charAt(0) == '#') {
                        continue;
                    }

                    final String categoryName = tempData[0].toLowerCase();
                    final String categoryValue = tempData[1];

                    if (categoryName.equals("humanlife"))                   humanLife = Integer.parseInt(categoryValue);
                    else if (categoryName.equals("ailife"))                 computerLife = Integer.parseInt(categoryValue);

                    else if (categoryName.equals("activeplayer"))           tChangePlayer = categoryValue.trim().toLowerCase();
                    else if (categoryName.equals("activephase"))            tChangePhase = categoryValue;

                    else if (categoryName.equals("humancardsinplay"))       humanCardTexts.put(ZoneType.Battlefield, categoryValue);
                    else if (categoryName.equals("aicardsinplay"))          aiCardTexts.put(ZoneType.Battlefield, categoryValue);
                    else if (categoryName.equals("humancardsinhand"))       humanCardTexts.put(ZoneType.Hand, categoryValue);
                    else if (categoryName.equals("aicardsinhand"))          aiCardTexts.put(ZoneType.Hand, categoryValue);
                    else if (categoryName.equals("humancardsingraveyard"))  humanCardTexts.put(ZoneType.Graveyard, categoryValue);
                    else if (categoryName.equals("aicardsingraveyard"))     aiCardTexts.put(ZoneType.Graveyard, categoryValue);
                    else if (categoryName.equals("humancardsinlibrary"))    humanCardTexts.put(ZoneType.Library, categoryValue);
                    else if (categoryName.equals("aicardsinlibrary"))       aiCardTexts.put(ZoneType.Library, categoryValue);
                    else if (categoryName.equals("humancardsinexile"))      humanCardTexts.put(ZoneType.Exile, categoryValue);
                    else if (categoryName.equals("aicardsinexile"))         aiCardTexts.put(ZoneType.Exile, categoryValue);
                }

                in.close();
            }
            catch (final FileNotFoundException fnfe) {
                SOptionPane.showErrorDialog(getGui(), "File not found: " + filename);
            }
            catch (final Exception e) {
                SOptionPane.showErrorDialog(getGui(), "Error loading battle setup file!");
                return;
            }

            setupGameState(humanLife, computerLife, humanCardTexts, aiCardTexts, tChangePlayer, tChangePhase);
        }

        private void setupGameState(final int humanLife, final int computerLife, final Map<ZoneType, String> humanCardTexts,
                final Map<ZoneType, String> aiCardTexts, final String tChangePlayer, final String tChangePhase) {

            Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                SGuiDialog.message(getGui(), "No player has priority at the moment, so game state cannot be setup.");
                return;
            }
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    final Player human = game.getPlayers().get(0);
                    final Player ai = game.getPlayers().get(1);

                    Player newPlayerTurn = tChangePlayer.equals("human") ? newPlayerTurn = human : tChangePlayer.equals("ai") ? newPlayerTurn = ai : null;
                    PhaseType newPhase = tChangePhase.trim().equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tChangePhase);

                    game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn);

                    game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);

                    setupPlayerState(humanLife, humanCardTexts, human);
                    setupPlayerState(computerLife, aiCardTexts, ai);

                    game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

                    game.getAction().checkStaticAbilities(true);
                }
            });
        }

        private void setupPlayerState(int life, Map<ZoneType, String> cardTexts, final Player p) {
            Map<ZoneType, List<Card>> humanCards = new EnumMap<ZoneType, List<Card>>(ZoneType.class);
            for(Entry<ZoneType, String> kv : cardTexts.entrySet()) {
                humanCards.put(kv.getKey(), processCardsForZone(kv.getValue().split(";"), p));
            }

            if (life > 0) p.setLife(life, null);
            for (Entry<ZoneType, List<Card>> kv : humanCards.entrySet()) {
                if (kv.getKey() == ZoneType.Battlefield) {
                    for (final Card c : kv.getValue()) {
                        p.getZone(ZoneType.Hand).add(c);
                        p.getGame().getAction().moveToPlay(c);
                        c.setSickness(false);
                    }
                } else {
                    p.getZone(kv.getKey()).setCards(kv.getValue());
                }
            }
        }

        /**
         * <p>
         * processCardsForZone.
         * </p>
         * 
         * @param data
         *            an array of {@link java.lang.String} objects.
         * @param player
         *            a {@link forge.game.player.Player} object.
         * @return a {@link forge.CardList} object.
         */
        private List<Card> processCardsForZone(final String[] data, final Player player) {
            final List<Card> cl = new ArrayList<Card>();
            for (final String element : data) {
                final String[] cardinfo = element.trim().split("\\|");

                final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(cardinfo[0]), player);

                boolean hasSetCurSet = false;
                for (final String info : cardinfo) {
                    if (info.startsWith("Set:")) {
                        c.setCurSetCode(info.substring(info.indexOf(':') + 1));
                        hasSetCurSet = true;
                    } else if (info.equalsIgnoreCase("Tapped:True")) {
                        c.tap();
                    } else if (info.startsWith("Counters:")) {
                        final String[] counterStrings = info.substring(info.indexOf(':') + 1).split(",");
                        for (final String counter : counterStrings) {
                            c.addCounter(CounterType.valueOf(counter), 1, true);
                        }
                    } else if (info.equalsIgnoreCase("SummonSick:True")) {
                        c.setSickness(true);
                    } else if (info.equalsIgnoreCase("FaceDown:True")) {
                        c.setState(CardCharacteristicName.FaceDown);
                    }
                }

                if (!hasSetCurSet) {
                    c.setCurSetCode(c.getMostRecentSet());
                }

                cl.add(c);
            }
            return cl;
        }

        /**
         * <p>
         * Tutor.
         * </p>
         * 
         * @since 1.0.15
         */
        public void tutorForCard() {
            Player pPriority = game.getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                SGuiDialog.message(getGui(), "No player has priority at the moment, so their deck can't be tutored from.");
                return;
            }

            final List<Card> lib = pPriority.getCardsIn(ZoneType.Library);
            final List<ZoneType> origin = new ArrayList<ZoneType>();
            origin.add(ZoneType.Library);
            SpellAbility sa = new SpellAbility.EmptySa(new Card(-1));
            final Card card = chooseSingleCardForZoneChange(ZoneType.Hand, origin, sa, lib, "Choose a card", true, pPriority);
            if (card == null) { return; }

            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    game.getAction().moveToHand(card);
                }
            });
        }

        /**
         * <p>
         * AddCounter.
         * </p>
         * 
         * @since 1.0.15
         */
        public void addCountersToPermanent() {
            final List<Card> cards = game.getCardsIn(ZoneType.Battlefield);
            final CardView cardView = SGuiChoose.oneOrNone(getGui(), "Add counters to which card?", getCardViews(cards));
            final Card card = getCard(cardView);
            if (card == null) { return; }

            final CounterType counter = SGuiChoose.oneOrNone(getGui(), "Which type of counter?", CounterType.values());
            if (counter == null) { return; }

            final Integer count = SGuiChoose.getInteger(getGui(), "How many counters?", 1, Integer.MAX_VALUE, 10);
            if (count == null) { return; }
            
            card.addCounter(counter, count, false);
        }

        public void tapPermanents() {
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    final List<Card> untapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Predicates.not(CardPredicates.Presets.TAPPED));
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

        public void untapPermanents() {
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    final List<Card> tapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.TAPPED);
                    InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0, Integer.MAX_VALUE, tapped);
                    inp.setCancelAllowed(true);
                    inp.setMessage("Choose permanents to untap");
                    inp.showAndWait();
                    if( !inp.hasCancelled() )
                        for(Card c : inp.getSelected())
                            c.untap();
                }
            });
        }

        public void setPlayerLife() {
            final List<Player> players = game.getPlayers();
            final PlayerView playerView = SGuiChoose.oneOrNone(getGui(), "Set life for which player?", getPlayerViews(players));
            final Player player = getPlayer(playerView);
            if (player == null) { return; }

            final Integer life = SGuiChoose.getInteger(getGui(), "Set life to what?", 0);
            if (life == null) { return; }

            player.setLife(life, null);
        }

        public void winGame() {
            Input input = gameView.getInputQueue().getInput();
            if (!(input instanceof InputPassPriority)) {
                SOptionPane.showMessageDialog(getGui(), "You must have priority to use this feature.", "Win Game", SOptionPane.INFORMATION_ICON);
                return;
            }

            //set life of all other players to 0
            final LobbyPlayer guiPlayer = getLobbyPlayer();
            final List<Player> players = game.getPlayers();
            for (Player player : players) {
                if (player.getLobbyPlayer() != guiPlayer) {
                    player.setLife(0, null);
                }
            }

            //pass priority so that causes gui player to win
            input.selectButtonOK();
        }

        public void addCardToHand() {
            final List<Player> players = game.getPlayers();
            final PlayerView pView = SGuiChoose.oneOrNone(getGui(), "Put card in hand for which player?", getPlayerViews(players));
            final Player p = getPlayer(pView);
            if (null == p) {
                return;
            }

            final List<PaperCard> cards =  Lists.newArrayList(FModel.getMagicDb().getCommonCards().getUniqueCards());
            Collections.sort(cards);

            // use standard forge's list selection dialog
            final IPaperCard c = SGuiChoose.oneOrNone(getGui(), "Name the card", cards);
            if (c == null) {
                return;
            }

            game.getAction().invoke(new Runnable() { @Override public void run() {
                game.getAction().moveToHand(Card.fromPaperCard(c, p));
            }});
        }

        public void addCardToBattlefield() {
            final List<Player> players = game.getPlayers();
            final PlayerView pView = SGuiChoose.oneOrNone(getGui(), "Put card in play for which player?", getPlayerViews(players));
            final Player p = getPlayer(pView);
            if (null == p) {
                return;
            }

            final List<PaperCard> cards =  Lists.newArrayList(FModel.getMagicDb().getCommonCards().getUniqueCards());
            Collections.sort(cards);

            // use standard forge's list selection dialog
            final IPaperCard c = SGuiChoose.oneOrNone(getGui(), "Name the card", cards);
            if (c == null) {
                return;
            }

            game.getAction().invoke(new Runnable() {
                @Override public void run() {
                    final Card forgeCard = Card.fromPaperCard(c, p);

                    if (c.getRules().getType().isLand()) {
                        game.getAction().moveToPlay(forgeCard);
                    } else {
                        final List<SpellAbility> choices = forgeCard.getBasicSpells();
                        if (choices.isEmpty()) {
                            return; // when would it happen?
                        }

                        final SpellAbility sa;
                        if (choices.size() == 1) {
                            sa = choices.iterator().next();
                        } else {
                            final SpellAbilityView saView = SGuiChoose.oneOrNone(getGui(), "Choose", getSpellAbilityViews(choices));
                            sa = getSpellAbility(saView);
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

        public void riggedPlanarRoll() {
            final List<Player> players = game.getPlayers();
            final PlayerView playerView = SGuiChoose.oneOrNone(getGui(), "Which player should roll?", getPlayerViews(players));
            final Player player = getPlayer(playerView);
            if (player == null) { return; }

            final PlanarDice res = SGuiChoose.oneOrNone(getGui(), "Choose result", PlanarDice.values());
            if (res == null) { return; }

            System.out.println("Rigging planar dice roll: " + res.toString());

            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    PlanarDice.roll(player, res);
                }
            });
        }

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
            final IPaperCard c = SGuiChoose.oneOrNone(getGui(), "Name the card", allPlanars);
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
}
