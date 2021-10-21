package forge.game.combat;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;

public class AttackingBand {
    private CardCollection attackers = new CardCollection();
    private Boolean blocked = null; // even if all blockers were killed before FS or CD, band remains blocked

    public AttackingBand(final List<Card> band) {
        attackers.addAll(band);
    }

    public AttackingBand(final Card card) {
        attackers.add(card);
    }

    public CardCollectionView getAttackers() { return attackers; }

    public void addAttacker(Card card) { attackers.add(card); }
    public void removeAttacker(Card card) { attackers.remove(card); }

    public static boolean isValidBand(List<Card> band, boolean shareDamage) {
        if (band.isEmpty()) {
            // An empty band is not a valid band
            return false;
        }

        int bandingCreatures = CardLists.getKeyword(band, Keyword.BANDING).size();
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
        for (int i = 0; i < bandsWithString.length; i++) {
            String keyword = bandsWithString[i];
            String valid = validString[i];

            // Check if a bands with other keyword exists in band, and each creature in the band fits the valid quality
            if (!CardLists.getKeyword(band, keyword).isEmpty() &&
                    CardLists.getValidCards(band, valid, source.getController(), source, null).size() == band.size()) {
                return true;
            }
        }

        return false;
    }
    
    public boolean canJoinBand(Card card) {
        // Trying to join an existing band, attackers should be non-empty and card should exist
        List<Card> newBand = new ArrayList<>(attackers);
        if (card != null) {
            newBand.add(card);
        }

        return isValidBand(newBand, false);
    }

    public boolean contains(Card c) {
        return attackers.contains(c);
    }

    public Boolean isBlocked() { return blocked; }
    public void setBlocked(boolean value) { blocked = value; }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return attackers.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s %s", attackers.toString(), blocked == null ? " ? " : blocked.booleanValue() ? ">||" : ">>>" );
    }

}
