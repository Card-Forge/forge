package forge.card.spellability;

import java.util.ArrayList;

import forge.ComputerUtil;

/**
 * <p>
 * SpellAbilityList class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityList {
    private final ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();

    /**
     * <p>
     * Constructor for SpellAbilityList.
     * </p>
     */
    public SpellAbilityList() {
    }

    /**
     * <p>
     * Constructor for SpellAbilityList.
     * </p>
     * 
     * @param s
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbilityList(final SpellAbility s) {
        this.add(s);
    }

    /**
     * <p>
     * Constructor for SpellAbilityList.
     * </p>
     * 
     * @param s
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     */
    public SpellAbilityList(final SpellAbility[] s) {
        for (final SpellAbility element : s) {
            this.add(element);
        }
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void remove(final int n) {
        this.list.remove(n);
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param s
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void add(final SpellAbility s) {
        this.list.add(s);
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    public final int size() {
        return this.list.size();
    }

    /**
     * <p>
     * get.
     * </p>
     * 
     * @param n
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility get(final int n) {
        return this.list.get(n);
    }

    /**
     * <p>
     * addAll.
     * </p>
     * 
     * @param s
     *            a {@link forge.card.spellability.SpellAbilityList} object.
     */
    public final void addAll(final SpellAbilityList s) {
        for (int i = 0; i < s.size(); i++) {
            this.add(s.get(i));
        }
    }

    // Move1.getMax() uses this
    /**
     * <p>
     * execute.
     * </p>
     */
    public final void execute() {
        for (int i = 0; i < this.size(); i++) {
            if (!ComputerUtil.canPayCost(this.get(i))) {
                throw new RuntimeException("SpellAbilityList : execute() error, cannot pay for the spell "
                        + this.get(i).getSourceCard() + " - " + this.get(i).getStackDescription());
            }

            ComputerUtil.playNoStack(this.get(i));
        }
    } // execute()

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.size(); i++) {
            sb.append(this.get(i).getSourceCard().toString());
            sb.append(" - ");
            sb.append(this.get(i).getStackDescription());
            sb.append("\r\n");
        }
        return sb.toString();
    } // toString()

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        return this.toString().equals(o.toString());
    }
}
