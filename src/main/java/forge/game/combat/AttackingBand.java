package forge.game.combat;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.GameEntity;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AttackingBand implements Comparable<AttackingBand> {
    
    private List<Card> attackers = new ArrayList<Card>();
    private List<Card> blockers = new ArrayList<Card>();
    private GameEntity defender = null;
    private Boolean blocked = null;
    
    public AttackingBand(List<Card> band, GameEntity def) {
        attackers.addAll(band);
        this.defender = def;
    }
    
    public AttackingBand(Card card, GameEntity def) {
        attackers.add(card);
        this.defender = def;
    }
    
    public List<Card> getAttackers() { return this.attackers; }
    public List<Card> getBlockers() { return this.blockers; }
    public GameEntity getDefender() { return this.defender; }
    
    public void addAttacker(Card card) { attackers.add(card); }
    public void removeAttacker(Card card) { attackers.remove(card); }
    
    public void addBlocker(Card card) { blockers.add(card); }
    public void removeBlocker(Card card) { blockers.remove(card); }
    public void setBlockers(List<Card> blockers) { this.blockers = blockers; }
    
    public void setDefender(GameEntity def) { this.defender = def; }

    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public boolean getBlocked() { return this.blocked != null && this.blocked.booleanValue();  }
    
    public void calculateBlockedState() { this.blocked = !this.blockers.isEmpty(); }

    public static boolean isValidBand(List<Card> band, boolean shareDamage) {
        if (band.isEmpty()) {
            // An empty band is not a valid band
            return false;
        }
        
        int bandingCreatures = CardLists.getKeyword(band, "Banding").size();
        int neededBandingCreatures = shareDamage ? 1 : band.size() - 1;
        if (neededBandingCreatures <= bandingCreatures) {
            // For starting a band, only one can be non-Banding
            // For sharing damage, only one needs to be Banding
            return true;
        }
        
        // Legends lands, Master of the Hunt, Old Fogey (just in case)
        // Since Bands With Other is a dead keyword, no major reason to make this more generic
        // But if someone is super motivated, feel free to do it. Just make sure you update Tolaria and Shelkie Brownie
        String[] bandsWithString = { "Bands with Other Legendary Creatures", "Bands with Other Creatures named Wolves of the Hunt", 
        "Bands with Other Dinosaurs" };
        String[] validString = { "Legendary.Creature", "Creature.namedWolves of the Hunt", "Dinosaur" }; 
        
        Card source = band.get(0);
        for(int i = 0; i < bandsWithString.length; i++) {
            String keyword = bandsWithString[i];
            String valid = validString[i];
            
            // Check if a bands with other keyword exists in band, and each creature in the band fits the valid quality
            if (!CardLists.getKeyword(band, keyword).isEmpty() &&
                    CardLists.getValidCards(band, valid, source.getController(), source).size() == band.size()) {
                return true;
            }
        }

        return false;
    }
    
    public boolean canJoinBand(Card card) {
        // Trying to join an existing band, attackers should be non-empty and card should exist
        List<Card> newBand = new ArrayList<Card>(attackers);
        if (card != null) {
            newBand.add(card);
        }
        
        return isValidBand(newBand, false);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(AttackingBand o) {
        if (o == null) {
            return -1;
        }
        
        List<Card> compareAttackers = o.getAttackers();
        
        int sizeDiff = this.attackers.size() - compareAttackers.size();
        if (sizeDiff > 0) {
            return 1;
        } else if (sizeDiff < 0) {
            return -1;
        } else if (sizeDiff == 0 && this.attackers.isEmpty()) {
            return 0;
        }
        
        return this.attackers.get(0).compareTo(compareAttackers.get(0));
    }
}
