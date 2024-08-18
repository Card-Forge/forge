package forge.game.combat;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;

import java.util.List;

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

    public static boolean isValidBand(CardCollectionView band, boolean shareDamage) {
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

        for (Card c : CardLists.getKeyword(band, Keyword.BANDSWITH)) {
            for (KeywordInterface kw : c.getKeywords(Keyword.BANDSWITH)) {
                String o = kw.getOriginal();
                String m[] = o.split(":");

                if (CardLists.getValidCards(band, m[1], c.getController(), c, null).size() == band.size()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canJoinBand(Card card) {
        // Trying to join an existing band, attackers should be non-empty and card should exist
        CardCollection newBand = new CardCollection(attackers);
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
        return String.format("%s %s", attackers.toString(), blocked == null ? " ? " : blocked ? ">||" : ">>>" );
    }

}
