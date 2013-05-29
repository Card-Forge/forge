package forge.game.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.Card;
import forge.GameEntity;
import forge.Singletons;
import forge.card.mana.Mana;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.GuiUtils;
import forge.gui.input.InputAttack;
import forge.gui.input.InputBlock;
import forge.gui.input.InputConfirmMulligan;
import forge.gui.input.InputPassPriority;
import forge.gui.input.InputPlayOrDraw;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;
import forge.gui.input.InputSynchronized;
import forge.gui.match.CMatchUI;
import forge.item.CardPrinted;
import forge.util.TextUtil;



/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerHuman extends PlayerController {


    public PlayerControllerHuman(Game game0, Player p, LobbyPlayer lp) {
        super(game0, p, lp);
    }


    @Override
    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        return !CMatchUI.SINGLETON_INSTANCE.stopAtPhase(turn, phase);
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities) {
        if (abilities.isEmpty()) {
            return null;
        } else if (abilities.size() == 1) {
            return abilities.get(0);
        } else {
            return GuiChoose.oneOrNone("Choose ability to play", abilities);
        }
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
     * @see forge.game.player.PlayerController#playCascade(java.util.List, forge.Card)
     */
    @Override
    public boolean playCascade(Card cascadedCard, Card sourceCard) {

        final StringBuilder title = new StringBuilder();
        title.append(sourceCard.getName()).append(" - Cascade Ability");
        final StringBuilder question = new StringBuilder();
        question.append("Cast ").append(cascadedCard.getName());
        question.append(" without paying its mana cost?");


        boolean result = GuiDialog.confirm(cascadedCard, question.toString());
        if ( result )
            HumanPlay.playCardWithoutPayingManaCost(player, cascadedCard);
        return result;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#mayPlaySpellAbilityForFree(forge.card.spellability.SpellAbility)
     */
    @Override
    public void playSpellAbilityForFree(SpellAbility copySA) {
        HumanPlay.playSaWithoutPayingManaCost(player.getGame(), copySA);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#sideboard(forge.deck.Deck)
     */
    @Override
    public Deck sideboard(Deck deck, GameType gameType) {
        CardPool sideboard = deck.get(DeckSection.Sideboard);
        CardPool main = deck.get(DeckSection.Main);

        int deckMinSize = Math.min(main.countAll(), gameType.getDecksFormat().getMainRange().getMinimumInteger());
    
        CardPool newSb = new CardPool();
        List<CardPrinted> newMain = null;
        
        while (newMain == null || newMain.size() < deckMinSize) {
            if (newMain != null) {
                String errMsg = String.format("Too few cards in your main deck (minimum %d), please make modifications to your deck again.", deckMinSize);
                JOptionPane.showMessageDialog(null, errMsg, "Invalid deck", JOptionPane.ERROR_MESSAGE);
            }
            
            boolean isLimited = (gameType == GameType.Draft || gameType == GameType.Sealed);
            newMain = GuiChoose.sideboard(sideboard.toFlatList(), main.toFlatList(), isLimited);
        }
    
        newSb.clear();
        newSb.addAll(main);
        newSb.addAll(sideboard);
        for(CardPrinted c : newMain) {
            newSb.remove(c);
        }
    
        Deck res = (Deck)deck.copyTo(deck.getName());
        res.getMain().clear();
        res.getMain().add(newMain);
        CardPool resSb = res.getOrCreate(DeckSection.Sideboard);
        resSb.clear();
        resSb.addAll(newSb);
        return res;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#assignCombatDamage()
     */
    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender) {
        // Attacker is a poor name here, since the creature assigning damage
        // could just as easily be the blocker. 
        Map<Card, Integer> map;
        if (defender != null && assignDamageAsIfNotBlocked(attacker)) {
            map = new HashMap<Card, Integer>();
            map.put(null, damageDealt);
        } else {
            if ((attacker.hasKeyword("Trample") && defender != null) || (blockers.size() > 1)) {
                map = CMatchUI.SINGLETON_INSTANCE.getDamageToAssign(attacker, blockers, damageDealt, defender);
            } else {
                map = new HashMap<Card, Integer>();
                map.put(blockers.get(0), damageDealt);
            }
        }
        return map;
    }
    
    private final boolean assignDamageAsIfNotBlocked(Card attacker) {
        return attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")
                || (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                && GuiDialog.confirm(attacker, "Do you want to assign its combat damage as though it weren't blocked?"));
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#announceRequirements(java.lang.String)
     */
    @Override
    public Integer announceRequirements(SpellAbility ability, String announce, boolean canChooseZero) {
        List<Object> options = new ArrayList<Object>();
        for(int i = canChooseZero ? 0 : 1; i < 10; i++)
            options.add(Integer.valueOf(i));
        options.add("Other amount");
        
        
        Object chosen = GuiChoose.oneOrNone("Choose " + announce + " for " + ability.getSourceCard().getName(), options);
        if (chosen instanceof Integer || chosen == null)
            return (Integer)chosen;

        String message = String.format("How much will you announce for %s?%s", announce, canChooseZero ? "" : " (X cannot be 0)");
        while(true){
            String str = JOptionPane.showInputDialog(null, message, ability.getSourceCard().getName(), JOptionPane.QUESTION_MESSAGE);
            if (null == str) return null; // that is 'cancel'
            
            if(StringUtils.isNumeric(str)) {
                Integer val = Integer.valueOf(str);
                if (val == 0 && canChooseZero || val > 0) 
                    return val;
            }
            JOptionPane.showMessageDialog(null, "You have to enter a valid number", "Announce value", JOptionPane.WARNING_MESSAGE);
        }
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#choosePermanentsToSacrifice(java.util.List, int, forge.card.spellability.SpellAbility, boolean, boolean)
     */
    @Override
    public List<Card> choosePermanentsToSacrifice(List<Card> validTargets, String validMessage, int amount, SpellAbility sa, boolean destroy, boolean isOptional) {
        int max = Math.min(amount, validTargets.size());
        if (max <= 0)
            return new ArrayList<Card>();
        
        int min = isOptional ? 0 : amount;
        if (min > max) {
            min = max;
        }

        InputSelectCards inp = new InputSelectCardsFromList(min, max, validTargets);
        // TODO: Either compose a message here, or pass it as parameter from caller. 
        inp.setMessage("Select %d " + validMessage + "(s) to sacrifice");
        
        Singletons.getControl().getInputQueue().setInputAndWait(inp);
        if( inp.hasCancelled() )
            return new ArrayList<Card>();
        else return inp.getSelected(); 
    }

    @Override
    public Card chooseSingleCardForEffect(List<Card> options, SpellAbility sa, String title, boolean isOptional) {
        // Human is supposed to read the message and understand from it what to choose
        if ( isOptional )
            return GuiChoose.oneOrNone(title, options);
        else 
            return GuiChoose.one(title, options);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmAction(forge.card.spellability.SpellAbility, java.lang.String, java.lang.String)
     */
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return GuiDialog.confirm(sa.getSourceCard(), message);
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return GuiDialog.confirm(hostCard, message);
    }

    @Override
    public boolean getWillPlayOnFirstTurn(boolean isFirstGame) {
        InputPlayOrDraw inp = new InputPlayOrDraw(player, isFirstGame);
        Singletons.getControl().getInputQueue().setInputAndWait(inp);
        return inp.isPlayingFirst();
    }

    @Override
    public List<Card> orderBlockers(Card attacker, List<Card> blockers) {
        GuiUtils.setPanelSelection(attacker);
        return GuiChoose.order("Choose Damage Order for " + attacker, "Damaged First", 0, blockers, null, attacker);
    }

    @Override
    public List<Card> orderAttackers(Card blocker, List<Card> attackers) {
        GuiUtils.setPanelSelection(blocker);
        return GuiChoose.order("Choose Damage Order for " + blocker, "Damaged First", 0, attackers, null, blocker);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#reveal(java.lang.String, java.util.List, forge.game.zone.ZoneType, forge.game.player.Player)
     */
    @Override
    public void reveal(String string, Collection<Card> cards, ZoneType zone, Player owner) {
        String message = string;
        if ( StringUtils.isBlank(message) ) 
            message = String.format("Looking at %s's %s", owner, zone);
        GuiChoose.oneOrNone(message, cards);
    }

    @Override
    public ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN) {
        List<Card> toBottom = null;
        List<Card> toTop = null;
        
        if (topN.size() == 1) {
            if (willPutCardOnTop(topN.get(0)))
                toTop = topN;
            else 
                toBottom = topN;
        } else { 
            toBottom = GuiChoose.order("Select cards to be put on the bottom of your library", "Cards to put on the bottom", -1, topN, null, null);
            topN.removeAll(toBottom);
            if ( topN.isEmpty() )
                toTop = null;
            else if ( topN.size() == 1 )
                toTop = topN;
            else
                toTop = GuiChoose.order("Arrange cards to be put on top of your library", "Cards arranged", 0, topN, null, null);
        }
        return ImmutablePair.of(toTop, toBottom);
    }


    @Override
    public boolean willPutCardOnTop(Card c) {
        return GuiDialog.confirm(c, "Where will you put " + c.getName() + " in your library", new String[]{"Top", "Bottom"} );
    }

    @Override
    public List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone) {
        if (destinationZone == ZoneType.Library) {
            return GuiChoose.order("Choose order of cards to put into the library", "Closest to top", 0, cards, null, null);
        } else if (destinationZone == ZoneType.Battlefield) {
            return GuiChoose.order("Choose order of cards to put onto the battlefield", "Put first", 0, cards, null, null);
        } else if (destinationZone == ZoneType.Graveyard) {
            return GuiChoose.order("Choose order of cards to put into the graveyard", "Closest to top", 0, cards, null, null);
        }
        return cards;
    }

    @Override
    public List<Card> chooseCardsToDiscardFrom(Player p, SpellAbility sa, List<Card> valid, int min, int max) {
        if ( p != player ) {
            int cntToKeepInHand =  min == 0 ? -1 : valid.size() - min;
            return GuiChoose.order("Choose cards to Discard", "Discarded", cntToKeepInHand, valid, null, null);
        }

        InputSelectCards inp = new InputSelectCardsFromList(min, max, valid);
        inp.setMessage("Discard %d cards");
        Singletons.getControl().getInputQueue().setInputAndWait(inp);
        return inp.getSelected();
    }

    @Override
    public Card chooseCardToDredge(List<Card> dredgers) {
        if (GuiDialog.confirm(null, "Do you want to dredge?", false)) {
            return GuiChoose.oneOrNone("Select card to dredge", dredgers);
        }
        return null;
    }

    @Override
    public void playMiracle(SpellAbility miracle, Card card) {
        if (GuiDialog.confirm(card, card + " - Drawn. Play for Miracle Cost?")) {
            HumanPlay.playSpellAbility(player, miracle);
        }
    }

    @Override
    public void playMadness(SpellAbility madness) {
        if (GuiDialog.confirm(madness.getSourceCard(), madness.getSourceCard() + " - Discarded. Pay Madness Cost?")) {
           HumanPlay.playSpellAbility(player, madness);
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

        final Integer chosenAmount = GuiChoose.one("Exile how many cards?", cntChoice);
        System.out.println("Delve for " + chosenAmount);

        for (int i = 0; i < chosenAmount; i++) {
            final Card nowChosen = GuiChoose.oneOrNone("Exile which card?", grave);

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
    public Target chooseTargets(SpellAbility ability) {
        if (ability.getTarget() == null) {
            return null;
        }
        Target oldTarget = new Target(ability.getTarget());
        TargetSelection select = new TargetSelection(ability);
        ability.getTarget().resetTargets();
        if (select.chooseTargets()) {
            return ability.getTarget();
        } else {
            // Return old target, since we had to reset them above
            return oldTarget;
        }
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseCardsToDiscardUnlessType(int, java.lang.String, forge.card.spellability.SpellAbility)
     */
    @Override
    public List<Card> chooseCardsToDiscardUnlessType(int num, List<Card> hand, final String uType, SpellAbility sa) {
        final InputSelectCards target = new InputSelectCardsFromList(num, num, hand) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasAllTargets() {
                for(Card c : selected) {
                    if (c.isType(uType))
                        return true;
                }
                return super.hasAllTargets();
            }
        };
        target.setMessage("Select %d cards to discard, unless you discard a " + uType + ".");
        Singletons.getControl().getInputQueue().setInputAndWait(target);
        return target.getSelected();
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseManaFromPool(java.util.List)
     */
    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        List<String> options = new ArrayList<String>();
        for(int i = 0; i < manaChoices.size(); i++) {
            Mana m = manaChoices.get(i);
            options.add(String.format("%d. %s mana from %s", 1+i, m.getColor(), m.getSourceCard() ));
        }
        String chosen = GuiChoose.one("Pay Mana from Mana Pool", options);
        String idx = TextUtil.split(chosen, '.')[0];
        return manaChoices.get(Integer.parseInt(idx)-1);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseSomeType(java.lang.String, java.lang.String, java.util.List, java.util.List, java.lang.String)
     */
    @Override
    public String chooseSomeType(String kindOfType, String aiLogic, List<String> validTypes, List<String> invalidTypes) {
        return GuiChoose.one("Choose a " + kindOfType.toLowerCase() + " type", validTypes);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#confirmReplacementEffect(forge.card.replacement.ReplacementEffect, forge.card.spellability.SpellAbility, java.lang.String)
     */
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question) {
        return GuiDialog.confirm(replacementEffect.getHostCard(), question);
    }


    @Override
    public List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer) {
        final InputConfirmMulligan inp = new InputConfirmMulligan(player, firstPlayer, isCommander);
        Singletons.getControl().getInputQueue().setInputAndWait(inp);
        return inp.isKeepHand() ? null : isCommander ? inp.getSelectedCards() : player.getCardsIn(ZoneType.Hand);
    }


    private void showDefaultInput() {
        SpellAbility chosenSa = null;
        do {
            if (chosenSa != null) {
                HumanPlay.playSpellAbility(player, chosenSa);
            }
            InputPassPriority defaultInput = new InputPassPriority(player);
            Singletons.getControl().getInputQueue().setInputAndWait(defaultInput);
            chosenSa = defaultInput.getChosenSa();
        } while( chosenSa != null );
    }
    
    
    @Override
    public void takePriority() {
        PhaseType phase = game.getPhaseHandler().getPhase();

        boolean maySkipPriority = mayAutoPass(phase) || isUiSetToSkipPhase(game.getPhaseHandler().getPlayerTurn(), phase);
        if (game.getStack().isEmpty() && maySkipPriority) {
            return;
        } else
            autoPassCancel(); // probably cancel, since something has happened
        
        switch(phase) {

            case COMBAT_DECLARE_ATTACKERS:
                game.getCombat().initiatePossibleDefenders(player.getOpponents());
                InputSynchronized inpAttack = new InputAttack(player);
                Singletons.getControl().getInputQueue().setInputAndWait(inpAttack);
                return;
    
            case COMBAT_DECLARE_BLOCKERS:
                InputSynchronized inpBlock = new InputBlock(player, game.getCombat());
                Singletons.getControl().getInputQueue().setInputAndWait(inpBlock);
                return;

            default:
                showDefaultInput();
        }
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
        Singletons.getControl().getInputQueue().setInputAndWait(inp);
        return inp.getSelected();
    }
}
