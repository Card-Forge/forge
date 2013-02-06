package forge.game.player;

import java.util.List;

import javax.swing.JOptionPane;

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.control.input.InputBlock;
import forge.control.input.InputCleanup;
import forge.control.input.InputPassPriority;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameState;
import forge.game.GameType;
import forge.game.phase.PhaseType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.match.CMatchUI;
import forge.item.CardPrinted;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerHuman extends PlayerController {


    private PhaseType autoPassUntil = null;

    private final Input defaultInput;
    private final Input blockInput;
    private final Input cleanupInput;
    private final HumanPlayer player;
    
    public final Input getDefaultInput() {
        return defaultInput;
    }

    public PlayerControllerHuman(GameState game0, HumanPlayer p) {
        super(game0);
        player = p;
        
        defaultInput = new InputPassPriority();
        blockInput = new InputBlock(getPlayer());
        cleanupInput = new InputCleanup(game);
    }

    public boolean mayAutoPass(PhaseType phase) {

        return phase.isBefore(autoPassUntil);
    }


    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        boolean isLocalPlayer = getPlayer().equals(Singletons.getControl().getPlayer());
        return isLocalPlayer && !CMatchUI.SINGLETON_INSTANCE.stopAtPhase(turn, phase);
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities) {
        if (abilities.size() == 0) {
            return null;
        } else if (abilities.size() == 1) {
            return abilities.get(0);
        } else {
            return GuiChoose.oneOrNone("Choose", abilities); // some day network interaction will be here
        }
    }

    /** Input to use when player has to declare blockers */
    public Input getBlockInput() {
        return blockInput;
    }

    /**
     * @return the cleanupInput
     */
    public Input getCleanupInput() {
        return cleanupInput;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param c
     */
    public void playFromSuspend(Card c) {
        c.setSuspendCast(true);
        game.getActionPlay().playCardWithoutManaCost(c, c.getOwner());
    }

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
            game.getActionPlay().playCardWithoutManaCost(cascadedCard, getPlayer());
        return result;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#mayPlaySpellAbilityForFree(forge.card.spellability.SpellAbility)
     */
    @Override
    public void mayPlaySpellAbilityForFree(SpellAbility copySA) {
        game.getActionPlay().playSpellAbilityForFree(copySA);
    }

    /**
     * @return the player
     */
    @Override
    public Player getPlayer() {
        return player;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#sideboard(forge.deck.Deck)
     */
    @Override
    public Deck sideboard(Deck deck, GameType gameType) {
        DeckSection sideboard = deck.getSideboard();

        //  + deck.getSideboard().countAll()
        int deckMinSize = Math.min(deck.getMain().countAll(), gameType.getDecksFormat().getMainRange().getMinimumInteger());
        //IntRange sbRange = gameType.getDecksFormat().getSideRange();
        int sideboardSize = sideboard.countAll();
    
        DeckSection newSb = new DeckSection();
        List<CardPrinted> newMain = null;
        
    
        while (newMain == null || newMain.size() < deckMinSize) {
            
            if ( newMain != null ) {
                String errMsg = String.format("Too few cards in your main deck (minimum %d), please make modifications to your deck again.", deckMinSize);
                JOptionPane.showMessageDialog(null, errMsg, "Invalid deck", JOptionPane.ERROR_MESSAGE);
            }
            
            newMain = GuiChoose.order("Sideboard", "Main Deck", sideboardSize, deck.getSideboard().toFlatList(), deck.getMain().toFlatList(), null, true);
        }
    
        newSb.clear();
        newSb.addAll(deck.getMain());
        newSb.addAll(deck.getSideboard());
        for(CardPrinted c : newMain)
            newSb.remove(c);
    
        Deck res = (Deck) deck.copyTo(deck.getName());
        res.getMain().clear();
        res.getMain().add(newMain);
        res.getSideboard().clear();
        res.getSideboard().addAll(newSb);
        return res;
    }



}
