package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/** 
 * PeeakAndReveal is a simplified why of handling something that could
 * be done with dig and NoMove$ . All of the Kinship cards are going to use this
 * And there's probably a bunch of existing cards that would have simpler scripts
 * by just using PeekAndReveal
 *
 */
public class PeekAndRevealEffect extends SpellAbilityEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        //RevealValid$ Card.sharesCreatureTypeWith | RevealOptional$ True | RememberRevealed$ True
        Card source = sa.getSourceCard();
        final boolean rememberRevealed = sa.hasParam("RememberRevealed");
        final boolean imprintRevealed = sa.hasParam("ImprintRevealed");
        String revealValid = sa.hasParam("RevealValid") ? sa.getParam("RevealValid") : "Card";
        String peekAmount = sa.hasParam("PeekAmount") ? sa.getParam("PeekAmount") : "1";
        int numPeek = AbilityUtils.calculateAmount(sa.getSourceCard(), peekAmount, sa);
        
        // Right now, this is only used on your own library.
        List<Player> libraryPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        Player peekingPlayer = sa.getActivatingPlayer();
        
        for(Player libraryToPeek : libraryPlayers) {
            final PlayerZone library = libraryToPeek.getZone(ZoneType.Library);
            numPeek = Math.min(numPeek, library.size());
            
            List<Card> peekCards = new ArrayList<Card>();
            for(int i = 0; i < numPeek; i++) {
                peekCards.add(library.get(i));
            }
            
            List<Card> revealableCards = CardLists.getValidCards(peekCards, revealValid, sa.getActivatingPlayer(), sa.getSourceCard());
            boolean doReveal = !sa.hasParam("NoReveal") && !revealableCards.isEmpty();
            if (!sa.hasParam("NoPeek")) {
                peekingPlayer.getController().reveal(source + "Revealing cards from library", peekCards, ZoneType.Library, peekingPlayer);
            }
            
            if( doReveal && sa.hasParam("RevealOptional") )
                doReveal = peekingPlayer.getController().confirmAction(sa, null, "Reveal cards to other players?");
            
            if (doReveal) {
                peekingPlayer.getGame().getAction().reveal(revealableCards, peekingPlayer);

                // Singletons.getModel().getGameAction().revealCardsToOtherPlayers(peekingPlayer, revealableCards);
                if (rememberRevealed) {
                    for(Card c : revealableCards) {
                        source.addRemembered(CardUtil.getLKICopy(c));
                    }
                }
                if (imprintRevealed) {
                    for(Card c : revealableCards) {
                        source.addImprinted(CardUtil.getLKICopy(c));
                    }
                }
            } else if (sa.hasParam("RememberPeeked")) {
                for(Card c : revealableCards) {
                    source.addRemembered(CardUtil.getLKICopy(c));
                }
            }
        }
    }

}
