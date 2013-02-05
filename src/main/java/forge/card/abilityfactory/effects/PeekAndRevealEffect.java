package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;

/** 
 * PeeakAndReveal is a simplified why of handling something that could
 * be done with dig and NoMove$ . All of the Kinship cards are going to use this
 * And there's probably a bunch of existing cards that would have simpler scripts
 * by just using PeekAndReveal
 *
 */
public class PeekAndRevealEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        //RevealValid$ Card.sharesCreatureTypeWith | RevealOptional$ True | RememberRevealed$ True
        Card source = sa.getSourceCard();
        boolean revealOptional = sa.hasParam("RevealOptional");
        boolean rememberRevealed = sa.hasParam("RememberRevealed");
        String revealValid = sa.hasParam("RevealValid") ? sa.getParam("RevealValid") : "Card";
        String peekAmount = sa.hasParam("PeekAmount") ? sa.getParam("PeekAmount") : "1";
        int numPeek = AbilityFactory.calculateAmount(sa.getSourceCard(), peekAmount, sa);
        
        // Right now, this is only used on your own library.
        Player libraryToPeek = sa.getActivatingPlayer();
        Player peekingPlayer = sa.getActivatingPlayer();
        
        final PlayerZone library = libraryToPeek.getZone(ZoneType.Library);
        numPeek = Math.min(numPeek, library.size());
        
        List<Card> peekCards = new ArrayList<Card>();
        for(int i = 0; i < numPeek; i++) {
            peekCards.add(library.get(i));
        }
        
        List<Card> revealableCards = CardLists.getValidCards(peekCards, revealValid, sa.getActivatingPlayer(), sa.getSourceCard());
        boolean doReveal = !revealableCards.isEmpty();
        
        //peekingPlayer.showCards(peekCards)
        if (peekingPlayer.isHuman()) {
            GuiChoose.one(source + "Revealing cards from library", peekCards);
            if (doReveal && revealOptional) {
                StringBuilder question = new StringBuilder();
                question.append("Reveal cards to other players?");
                doReveal = GuiDialog.confirm(source, question.toString());
            }
        } else {
            if (doReveal && revealOptional) {
                // If 
                AbilitySub subAb = sa.getSubAbility();
                doReveal = subAb != null && subAb.chkAIDrawback((AIPlayer)peekingPlayer);
            }
        }
        
        if (doReveal) {
            if (!peekingPlayer.isHuman()) {
                GuiChoose.one(source + "Revealing cards from library", revealableCards);
            }
            // Singletons.getModel().getGameAction().revealCardsToOtherPlayers(peekingPlayer, revealableCards);
            if (rememberRevealed) {
                for(Card c : revealableCards) {
                    source.addRemembered(c);
                }
            }
        }
    }

}
