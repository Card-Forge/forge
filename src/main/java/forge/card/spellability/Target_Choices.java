package forge.card.spellability;

import java.util.ArrayList;

import forge.Card;
import forge.Player;

/**
 * <p>
 * Target_Choices class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Target_Choices {
    private int numTargeted = 0;

    /**
     * <p>
     * Getter for the field <code>numTargeted</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumTargeted() {
        return numTargeted;
    }

    // Card or Player are legal targets.
    private ArrayList<Card> targetCards = new ArrayList<Card>();
    private ArrayList<Player> targetPlayers = new ArrayList<Player>();
    private ArrayList<SpellAbility> targetSAs = new ArrayList<SpellAbility>();

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @return a boolean.
     */
    public final boolean addTarget(final Object o) {
        if (o instanceof Player) {
            return addTarget((Player) o);
        } else if (o instanceof Card) {
            return addTarget((Card) o);
        } else if (o instanceof SpellAbility) {
            return addTarget((SpellAbility) o);
        }

        return false;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean addTarget(final Card c) {
        if (!targetCards.contains(c)) {
            targetCards.add(c);
            numTargeted++;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public final boolean addTarget(final Player p) {
        if (!targetPlayers.contains(p)) {
            targetPlayers.add(p);
            numTargeted++;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean addTarget(final SpellAbility sa) {
        if (!targetSAs.contains(sa)) {
            targetSAs.add(sa);
            numTargeted++;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>targetCards</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getTargetCards() {
        return targetCards;
    }

    /**
     * <p>
     * Getter for the field <code>targetPlayers</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Player> getTargetPlayers() {
        return targetPlayers;
    }

    /**
     * <p>
     * Getter for the field <code>targetSAs</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SpellAbility> getTargetSAs() {
        return targetSAs;
    }

    /**
     * <p>
     * getTargets.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Object> getTargets() {
        ArrayList<Object> tgts = new ArrayList<Object>();
        tgts.addAll(targetPlayers);
        tgts.addAll(targetCards);
        tgts.addAll(targetSAs);

        return tgts;
    }

    /**
     * <p>
     * getTargetedString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getTargetedString() {
        ArrayList<Object> tgts = getTargets();
        StringBuilder sb = new StringBuilder("");
        for (Object o : tgts) {
            if (o instanceof Player) {
                Player p = (Player) o;
                sb.append(p.getName());
            }
            if (o instanceof Card) {
                Card c = (Card) o;
                sb.append(c);
            }
            if (o instanceof SpellAbility) {
                SpellAbility sa = (SpellAbility) o;
                sb.append(sa);
            }
            sb.append(" ");
        }

        return sb.toString();
    }
}
