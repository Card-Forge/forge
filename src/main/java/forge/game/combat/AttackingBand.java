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

    public boolean canJoinBand(Card card) {
        // If this card has banding it can definitely join
        if (card.hasKeyword("Banding")) {
            return true;
        }
        
        // If all of the cards in the Band have banding, it can definitely join
        if (attackers.size() == CardLists.getKeyword(attackers, "Banding").size()) {
            return true;
        }
        
        // TODO add checks for bands with other
        //List<Card> bandsWithOther = CardLists.getKeyword(attackers, "Bands with Other");
        
        return false;
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
