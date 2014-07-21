package forge.player;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import forge.LobbyPlayer;
import forge.GuiBase;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardShields;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
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
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.match.input.*;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.util.ITriggerEvent;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SGuiDialog;
import forge.util.gui.SOptionPane;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerHuman extends PlayerController {
    public PlayerControllerHuman(Game game0, Player p, LobbyPlayer lp) {
        super(game0, p, lp);
    }

    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        return !GuiBase.getInterface().stopAtPhase(turn, phase);
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        return GuiBase.getInterface().getAbilityToPlay(abilities, triggerEvent);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param c
     */
    /**public void playFromSuspend(Card c) {
        c.setSuspendCast(true);
        HumanPlay.playCardWithoutPayingManaCost(player, c);
    }**/


    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#mayPlaySpellAbilityForFree(forge.card.spellability.SpellAbility)
     */
    @Override
    public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets) {
        HumanPlay.playSaWithoutPayingManaCost(player.getGame(), copySA, mayChoseNewTargets);
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        HumanPlay.playSpellAbilityNoStack(player, effectSA, !canSetupTargets);
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

        int deckMinSize = Math.min(mainSize, gameType.getDecksFormat().getMainRange().getMinimum());
        Range<Integer> sbRange = gameType.getDecksFormat().getSideRange();
        // Limited doesn't have a sideboard max, so let the Main min take care of things.
        int sbMax = sbRange == null ? combinedDeckSize : sbRange.getMaximum();

        List<PaperCard> newMain = null;

        if (sbSize == 0 && mainSize == deckMinSize) {
            // Skip sideboard loop if there are no sideboarding opportunities
            return null;
        }
        else {
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
                    SOptionPane.showErrorDialog(errMsg, "Invalid Deck");
                }
                // Sideboard rules have changed for M14, just need to consider min maindeck and max sideboard sizes
                // No longer need 1:1 sideboarding in non-limited formats
                newMain = SGuiChoose.sideboard(sideboard.toFlatList(), main.toFlatList());
            } while (conform && (newMain.size() < deckMinSize || combinedDeckSize - newMain.size() > sbMax));
        }
        return newMain;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#assignCombatDamage()
     */
    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender, boolean overrideOrder) {
        // Attacker is a poor name here, since the creature assigning damage
        // could just as easily be the blocker.
        Map<Card, Integer> map;
        if (defender != null && assignDamageAsIfNotBlocked(attacker)) {
            map = new HashMap<Card, Integer>();
            map.put(null, damageDealt);
        }
        else {
            if ((attacker.hasKeyword("Trample") && defender != null) || (blockers.size() > 1)) {
                map = GuiBase.getInterface().getDamageToAssign(attacker, blockers, damageDealt, defender, overrideOrder);
            }
            else {
                map = new HashMap<Card, Integer>();
                map.put(blockers.get(0), damageDealt);
            }
        }
        return map;
    }

    private final boolean assignDamageAsIfNotBlocked(Card attacker) {
        return attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")
                || (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                && SGuiDialog.confirm(attacker, "Do you want to assign its combat damage as though it weren't blocked?"));
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#announceRequirements(java.lang.String)
     */
    @Override
    public Integer announceRequirements(SpellAbility ability, String announce, boolean canChooseZero) {
        int min = canChooseZero ? 0 : 1;
        return SGuiChoose.getInteger("Choose " + announce + " for " + ability.getHostCard().getName(),
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

        InputSelectCardsFromList inp = new InputSelectCardsFromList(min == 0 ? 1 : min, max, valid);
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
        GuiBase.getInterface().setPanelSelection(sa.getHostCard());
        
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
            InputSelectCardsFromList sc = new InputSelectCardsFromList(min, max, sourceList);
            sc.setMessage(title);
            sc.setCancelAllowed(isOptional);
            sc.showAndWait();
            return Lists.newArrayList(sc.getSelected());
        }

        return SGuiChoose.many(title, "Chosen Cards", min, max, sourceList, sa.getHostCard());
    }

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
            InputSelectEntitiesFromList<T> input = new InputSelectEntitiesFromList<T>(isOptional ? 0 : 1, 1, options);
            input.setCancelAllowed(isOptional);
            input.setMessage(formatMessage(title, targetedPlayer));
            input.showAndWait();
            return Iterables.getFirst(input.getSelected(), null);
        }

        return isOptional ? SGuiChoose.oneOrNone(title, options) : SGuiChoose.one(title, options);
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        final Integer[] choices = new Integer[max + 1 - min];
        for (int i = 0; i <= max - min; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        return SGuiChoose.one(title, choices).intValue();
    }
    
    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> choices, Player relatedPlayer) {
        return SGuiChoose.one(title, choices).intValue();
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(java.util.List<SpellAbility> spells, SpellAbility sa, String title) {
        // Human is supposed to read the message and understand from it what to choose
        return spells.size() < 2 ? spells.get(0) : SGuiChoose.one(title, spells);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmAction(forge.card.spellability.SpellAbility, java.lang.String, java.lang.String)
     */
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return SGuiDialog.confirm(sa.getHostCard(), message);
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife,
            String string, int bid, Player winner) {
        return SGuiDialog.confirm(sa.getHostCard(), string + " Highest Bidder " + winner);
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return SGuiDialog.confirm(hostCard, message);
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

        InputConfirm inp = new InputConfirm(buildQuestion.toString());
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public boolean getWillPlayOnFirstTurn(boolean isFirstGame) {
        String prompt = String.format("%s, you %s\n\nWould you like to play or draw?", 
                player.getName(), isFirstGame ? " have won the coin toss." : " lost the last game."); 
        InputConfirm inp = new InputConfirm(prompt, "Play", "Draw");
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public List<Card> orderBlockers(Card attacker, List<Card> blockers) {
        GuiBase.getInterface().setPanelSelection(attacker);
        return SGuiChoose.order("Choose Damage Order for " + attacker, "Damaged First", blockers, attacker);
    }
    
    @Override
    public List<Card> orderBlocker(final Card attacker, final Card blocker, final List<Card> oldBlockers) {
    	GuiBase.getInterface().setPanelSelection(attacker);
    	return SGuiChoose.insertInList("Choose blocker after which to place " + attacker + " in damage order; cancel to place it first", blocker, oldBlockers);
    }

    @Override
    public List<Card> orderAttackers(Card blocker, List<Card> attackers) {
        GuiBase.getInterface().setPanelSelection(blocker);
        return SGuiChoose.order("Choose Damage Order for " + blocker, "Damaged First", attackers, blocker);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#reveal(java.lang.String, java.util.List, forge.game.zone.ZoneType, forge.game.player.Player)
     */
    @Override
    public void reveal(Collection<Card> cards, ZoneType zone, Player owner, String message) {
        if (StringUtils.isBlank(message)) {
            message = "Looking at cards in {player's} " + zone.name();
        }
        String fm = formatMessage(message, owner);
        if (!cards.isEmpty())
            SGuiChoose.reveal(fm, cards);
        else
            SGuiDialog.message("There are no cards in the named location", fm);
    }

    @Override
    public ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN) {
        List<Card> toBottom = null;
        List<Card> toTop = null;

        if (topN.size() == 1) {
            if (willPutCardOnTop(topN.get(0))) {
                toTop = topN;
            }
            else {
                toBottom = topN;
            }
        }
        else {
            toBottom = SGuiChoose.many("Select cards to be put on the bottom of your library", "Cards to put on the bottom", -1, topN, null);
            topN.removeAll(toBottom);
            if (topN.isEmpty()) {
                toTop = null;
            }
            else if (topN.size() == 1) {
                toTop = topN;
            }
            else {
                toTop = SGuiChoose.order("Arrange cards to be put on top of your library", "Cards arranged", topN, null);
            }
        }
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        Card c1 = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(c.getName()), null);
        return SGuiDialog.confirm(c1, "Where will you put " + c1.getName() + " in your library", new String[]{"Top", "Bottom"});
    }

    @Override
    public List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone) {
        switch (destinationZone) {
            case Library:
                return SGuiChoose.order("Choose order of cards to put into the library", "Closest to top", cards, null);
            case Battlefield:
                return SGuiChoose.order("Choose order of cards to put onto the battlefield", "Put first", cards, null);
            case Graveyard:
                return SGuiChoose.order("Choose order of cards to put into the graveyard", "Closest to bottom", cards, null);
            case PlanarDeck:
                return SGuiChoose.order("Choose order of cards to put into the planar deck", "Closest to top", cards, null);
            case SchemeDeck:
                return SGuiChoose.order("Choose order of cards to put into the scheme deck", "Closest to top", cards, null);
            case Stack:
                return SGuiChoose.order("Choose order of copies to cast", "Put first", cards, null);
            default:
                System.out.println("ZoneType " + destinationZone + " - Not Ordered");
                break;
        }
        return cards;
    }

    @Override
    public List<Card> chooseCardsToDiscardFrom(Player p, SpellAbility sa, List<Card> valid, int min, int max) {
        if (p != player) {
            return SGuiChoose.many("Choose " + min + " card" + (min != 1 ? "s" : "") + " to discard",
                    "Discarded", min, min, valid, null);
        }

        InputSelectCardsFromList inp = new InputSelectCardsFromList(min, max, valid);
        inp.setMessage(sa.hasParam("AnyNumber") ? "Discard up to %d card(s)" : "Discard %d card(s)");
        inp.showAndWait();
        return Lists.newArrayList(inp.getSelected());
    }

    @Override
    public void playMiracle(SpellAbility miracle, Card card) {
        if (SGuiDialog.confirm(card, card + " - Drawn. Play for Miracle Cost?")) {
            HumanPlay.playSpellAbility(player, miracle);
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

        final Integer chosenAmount = SGuiChoose.one("Exile how many cards?", cntChoice);
        System.out.println("Delve for " + chosenAmount);

        for (int i = 0; i < chosenAmount; i++) {
            final Card nowChosen = SGuiChoose.oneOrNone("Exile which card?", grave);

            if (nowChosen == null) {
                // User canceled,abort delving.
                toExile.clear();
                break;
            }

            grave.remove(nowChosen);
            toExile.add(nowChosen);
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
        TargetSelection select = new TargetSelection(sa);
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
        final InputSelectEntitiesFromList<Card> target = new InputSelectEntitiesFromList<Card>(num, num, hand) {
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
        String chosen = SGuiChoose.one("Pay Mana from Mana Pool", options);
        String idx = TextUtil.split(chosen, '.')[0];
        return manaChoices.get(Integer.parseInt(idx)-1);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseSomeType(java.lang.String, java.lang.String, java.util.List, java.util.List, java.lang.String)
     */
    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, List<String> validTypes, List<String> invalidTypes, boolean isOptional) {
        if(isOptional)
            return SGuiChoose.oneOrNone("Choose a " + kindOfType.toLowerCase() + " type", validTypes);
        else
            return SGuiChoose.one("Choose a " + kindOfType.toLowerCase() + " type", validTypes);
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ArrayListMultimap<Object, Player> votes) {
        return SGuiChoose.one(prompt, options);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmReplacementEffect(forge.card.replacement.ReplacementEffect, forge.card.spellability.SpellAbility, java.lang.String)
     */
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question) {
        return SGuiDialog.confirm(replacementEffect.getHostCard(), question);
    }

    @Override
    public List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer) {
        final InputConfirmMulligan inp = new InputConfirmMulligan(player, firstPlayer, isCommander);
        inp.showAndWait();
        return inp.isKeepHand() ? null : isCommander ? inp.getSelectedCards() : player.getCardsIn(ZoneType.Hand);
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        // This input should not modify combat object itself, but should return user choice
        InputAttack inpAttack = new InputAttack(attacker, combat);
        inpAttack.showAndWait();
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        // This input should not modify combat object itself, but should return user choice
        InputBlock inpBlock = new InputBlock(defender, combat);
        inpBlock.showAndWait();
    }

    @Override
    public SpellAbility chooseSpellAbilityToPlay() {
        PhaseType phase = game.getPhaseHandler().getPhase();
        
        boolean maySkipPriority = mayAutoPass(phase) || isUiSetToSkipPhase(game.getPhaseHandler().getPlayerTurn(), phase);
        if (game.getStack().isEmpty() && maySkipPriority) {
            return null;
        }
        else {
            autoPassCancel(); // probably cancel, since something has happened
        }
        
        InputPassPriority defaultInput = new InputPassPriority(player);
        defaultInput.showAndWait();
        return defaultInput.getChosenSa();
    }
    
    @Override
    public void playChosenSpellAbility(SpellAbility chosenSa)
    {
        HumanPlay.playSpellAbility(player, chosenSa);
    }

    @Override
    public List<Card> chooseCardsToDiscardToMaximumHandSize(int nDiscard) {
        final int n = player.getZone(ZoneType.Hand).size();
        final int max = player.getMaxHandSize();

        InputSelectCardsFromList inp = new InputSelectCardsFromList(nDiscard, nDiscard, player.getZone(ZoneType.Hand).getCards());
        String msgFmt = "Cleanup Phase: You can only have a maximum of %d cards, you currently have %d  cards in your hand - select %d card(s) to discard";
        String message = String.format(msgFmt, max, n, nDiscard);
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
        InputSelectCardsFromList inp = new InputSelectCardsFromList(min, max, valid);
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
        
        return HumanPlay.payCostDuringAbilityResolve(player, c, cost, sa, prompt);
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
        List<Card> chosen = SGuiChoose.many("Choose cards to activate from opening hand and their order", "Activate first", -1, srcCards, null);
        for (Card c : chosen) {
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
        return SGuiDialog.confirm(sa.getHostCard(), question, defaultVal == null || defaultVal.booleanValue(), labels);
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        String[] labelsSrc = call ? new String[]{"heads", "tails"} : new String[]{"win the flip", "lose the flip"};
        String[] strResults = new String[results.length];
        for (int i = 0; i < results.length; i++) {
            strResults[i] = labelsSrc[results[i] ? 0 : 1];
        }
        return SGuiChoose.one(sa.getHostCard().getName() + " - Choose a result", strResults) == labelsSrc[0];
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap) {
        String title = entityBeingDamaged + " - select which prevention shield to use";
        return choiceMap.get(SGuiChoose.one(title, options));
    }

    @Override
    public Pair<CounterType,String> chooseAndRemoveOrPutCounter(Card cardWithCounter) {
        if (!cardWithCounter.hasCounters()) {
            System.out.println("chooseCounterType was reached with a card with no counters on it. Consider filtering this card out earlier");
            return null;
        }

        String counterChoiceTitle = "Choose a counter type on " + cardWithCounter;
        final CounterType chosen = SGuiChoose.one(counterChoiceTitle, cardWithCounter.getCounters().keySet());

        String putOrRemoveTitle = "What to do with that '" + chosen.getName() + "' counter ";
        final String putString = "Put another " + chosen.getName() + " counter on " + cardWithCounter;
        final String removeString = "Remove a " + chosen.getName() + " counter from " + cardWithCounter;
        final String addOrRemove = SGuiChoose.one(putOrRemoveTitle, new String[]{putString,removeString});

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

        List<Pair<SpellAbilityStackInstance, GameObject>> chosen = SGuiChoose.getChoices(saSpellskite.getHostCard().getName(), 1, 1, allTargets, null, fnToString);
        return Iterables.getFirst(chosen, null);
    }

    @Override
    public void notifyOfValue(SpellAbility sa, GameObject realtedTarget, String value) {
        String message = formatNotificationMessage(sa, realtedTarget, value);
        if (sa.isManaAbility()) {
            game.getGameLog().add(GameLogEntryType.LAND, message);
        } else {
            SGuiDialog.message(message, sa.getHostCard() == null ? "" : sa.getHostCard().getName());
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
                a = SGuiChoose.one(modeTitle, choices);
            }
            else {
                a = SGuiChoose.oneOrNone(modeTitle, choices);
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
        return SGuiChoose.getChoices(message, min, max, options);
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
            return MagicColor.fromName(SGuiChoose.one(message, colorNames));
        }
        int idxChosen = SGuiDialog.confirm(c, message, colorNames) ? 0 : 1;
        return MagicColor.fromName(colorNames[idxChosen]);
    }

    @Override
    public PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name) {
        Iterable<PaperCard> cardsFromDb = FModel.getMagicDb().getCommonCards().getUniqueCards();
        List<PaperCard> cards = Lists.newArrayList(Iterables.filter(cardsFromDb, cpp));
        Collections.sort(cards);
        return SGuiChoose.one(message, cards);
    }

    @Override
    public CounterType chooseCounterType(Collection<CounterType> options, SpellAbility sa, String prompt) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return SGuiChoose.one(prompt, options);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String question) {
        InputConfirm inp = new InputConfirm(question);
        inp.showAndWait();
        return inp.getResult();
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers, HashMap<String, Object> runParams) {
        if(possibleReplacers.size() == 1)
            return possibleReplacers.get(0);
        return SGuiChoose.one(prompt, possibleReplacers);
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        return SGuiChoose.one(string, choices);
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, List<Player> allPayers) {
        // if it's paid by the AI already the human can pay, but it won't change anything
        return HumanPlay.payCostDuringAbilityResolve(player, sa.getHostCard(), cost, sa, null);
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        List<SpellAbility> orderedSAs = activePlayerSAs;
        if (activePlayerSAs.size() > 1) { // give a dual list form to create instead of needing to do it one at a time
            orderedSAs = SGuiChoose.order("Select order for Simultaneous Spell Abilities", "Resolve first", activePlayerSAs, null);
        }
        int size = orderedSAs.size();
        for (int i = size - 1; i >= 0; i--) {
            SpellAbility next = orderedSAs.get(i);
            if (next.isTrigger()) {
                HumanPlay.playSpellAbility(player, next);
            } else {
                player.getGame().getStack().add(next);
            }
        }
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        HumanPlay.playSpellAbilityNoStack(player, wrapperAbility);
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        HumanPlay.playSpellAbility(player, tgtSA);
        return true;
    }

    @Override
    public Map<GameEntity, CounterType> chooseProliferation() {
        InputProliferate inp = new InputProliferate();
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return inp.getProliferationMap();
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        final TargetSelection select = new TargetSelection(currentAbility);
        return select.chooseTargets(null);
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, List<Card> pile1, List<Card> pile2, boolean faceUp) {
        if (!faceUp) {
            final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
            final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());
            final String[] possibleValues = { p1Str , p2Str };
            return SGuiDialog.confirm(sa.getHostCard(), "Choose a Pile", possibleValues);
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
                final Object o = SGuiChoose.one("Choose a pile", disp);
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
            SGuiChoose.reveal(message + " from " + Lang.getPossessedObject(mayBeYou(p), "deck"), removedAnteCards.get(p));
        }
    }

	@Override
	public CardShields chooseRegenerationShield(Card c) {
		if (c.getShield().size() < 2) {
            return Iterables.getFirst(c.getShield(), null);
		}
		return SGuiChoose.one("Choose a regeneration shield:", c.getShield());
	}

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        return SGuiChoose.many("Select cards to add to your deck", "Add these to my deck", 0, losses.size(), losses, null);
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, boolean isActivatedSa) {
        return HumanPlay.payManaCost(toPay, costPartMana, sa, player, prompt, isActivatedSa);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa, ManaCost manaCost, List<Card> untappedCreats) {
        InputSelectCardsForConvoke inp = new InputSelectCardsForConvoke(player, manaCost, untappedCreats);
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

}
