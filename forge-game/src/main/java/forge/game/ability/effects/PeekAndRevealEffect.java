package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

/** 
 * PeekAndReveal is a simplified way of handling something that could be done with Dig and NoMove$.
 * Kinship cards use this, and many other cards could have simpler scripts by just using PeekAndReveal.
 */
public class PeekAndRevealEffect extends SpellAbilityEffect {
    
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Player peeker = sa.getActivatingPlayer();
        final int numPeek = sa.hasParam("PeekAmount") ?
                AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("PeekAmount"), sa) : 1;
        final String verb = sa.hasParam("NoReveal") || sa.hasParam("RevealOptional") ? " looks at " :
                " reveals ";
        final String defined = sa.getParamOrDefault("Defined", "");
        final List<Player> libraryPlayers = getDefinedPlayersOrTargeted(sa);
        final String defString = Lang.joinHomogenous(libraryPlayers);
        String who = defined.equals("Player") && verb.equals(" reveals ") ? "Each player" :
                sa.hasParam("NoPeek") && verb.equals(" reveals ") ? defString : "";
        String whose = defined.equals("Player") && verb.equals(" looks at ") ? "each player's"
                : libraryPlayers.size() == 1 && libraryPlayers.get(0) == peeker ? "their" :
                defString + "'s";

        final StringBuilder sb = new StringBuilder();

        sb.append(who.equals("") ? peeker : who);
        sb.append(verb).append("the top ");
        sb.append(numPeek > 1 ? Lang.getNumeral(numPeek) + " cards " : "card ").append("of ").append(whose);
        sb.append(" library.");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final boolean rememberRevealed = sa.hasParam("RememberRevealed");
        final boolean imprintRevealed = sa.hasParam("ImprintRevealed");
        final boolean noPeek = sa.hasParam("NoPeek");
        String revealValid = sa.getParamOrDefault("RevealValid", "Card");
        String peekAmount = sa.getParamOrDefault("PeekAmount", "1");
        int numPeek = AbilityUtils.calculateAmount(source, peekAmount, sa);
        
        List<Player> libraryPlayers = getDefinedPlayersOrTargeted(sa);
        Player peekingPlayer = sa.getActivatingPlayer();
        
        for (Player libraryToPeek : libraryPlayers) {
            final PlayerZone library = libraryToPeek.getZone(ZoneType.Library);
            numPeek = Math.min(numPeek, library.size());

            CardCollection peekCards = new CardCollection();
            for (int i = 0; i < numPeek; i++) {
                peekCards.add(library.get(i));
            }

            CardCollectionView revealableCards = CardLists.getValidCards(peekCards, revealValid,
                    sa.getActivatingPlayer(), source, sa);
            boolean doReveal = !sa.hasParam("NoReveal") && !revealableCards.isEmpty();
            if (!noPeek) {
                peekingPlayer.getController().reveal(peekCards, ZoneType.Library, libraryToPeek,
                        CardTranslation.getTranslatedName(source.getName()) + " - " +
                                Localizer.getInstance().getMessage("lblLookingCardFrom"));
            }
            
            if (doReveal && sa.hasParam("RevealOptional"))
                doReveal = peekingPlayer.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblRevealCardToOtherPlayers"), null);
            
            if (doReveal) {
                peekingPlayer.getGame().getAction().reveal(revealableCards, ZoneType.Library, libraryToPeek, !noPeek,
                        CardTranslation.getTranslatedName(source.getName()) + " - " +
                                Localizer.getInstance().getMessage("lblRevealingCardFrom"));

                if (rememberRevealed) {
                    Map<Integer, Card> cachedMap = Maps.newHashMap();
                    for (Card c : revealableCards) {
                        source.addRemembered(CardUtil.getLKICopy(c, cachedMap));
                    }
                }
                if (imprintRevealed) {
                    Map<Integer, Card> cachedMap = Maps.newHashMap();
                    for (Card c : revealableCards) {
                        source.addImprintedCard(CardUtil.getLKICopy(c, cachedMap));
                    }
                }
            } else if (sa.hasParam("RememberPeeked")) {
                Map<Integer, Card> cachedMap = Maps.newHashMap();
                for (Card c : revealableCards) {
                    source.addRemembered(CardUtil.getLKICopy(c, cachedMap));
                }
            }
        }
    }

}
