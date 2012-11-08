package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.GameNew;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class RestartGameEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        GameState game = Singletons.getModel().getGame();
        List<Player> players = game.getPlayers();
        Map<Player, List<Card>> playerLibraries = new HashMap<Player, List<Card>>();
        
        // Don't grab Ante Zones
        List<ZoneType> restartZones = new ArrayList<ZoneType>(Arrays.asList(ZoneType.Battlefield,
                ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile, ZoneType.Command));
        
        ZoneType leaveZone = ZoneType.smartValueOf(sa.hasParam("RestrictFromZone") ? sa.getParam("RestrictFromZone") : null);
        restartZones.remove(leaveZone);
        String leaveRestriction = sa.hasParam("RestrictFromValid") ? sa.getParam("RestrictFromValid") : "Card";
        
        for(Player p : players) {
            List<Card> newLibrary = new ArrayList<Card>(p.getCardsIn(restartZones));
            List<Card> filteredCards = null;
            if (leaveZone != null) {
                filteredCards = CardLists.filter(p.getCardsIn(leaveZone), 
                        CardPredicates.restriction(leaveRestriction.split(","), p, sa.getSourceCard()));
            }
            
            newLibrary.addAll(filteredCards);
            playerLibraries.put(p, newLibrary);
        }
        
        GameNew.restartGame(game, sa.getActivatingPlayer(), playerLibraries);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public String getStackDescription(SpellAbility sa) {
        String desc = sa.getParam("SpellDescription");
        
        if (desc == null) {
            desc = "Restart the game.";
        }
        
        return desc.replace("CARDNAME", sa.getSourceCard().getName());
    } 
}
