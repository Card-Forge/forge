package forge.card.spellability;


import forge.ComputerUtil;

import java.util.ArrayList;


/**
 * <p>SpellAbilityList class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class SpellAbilityList {
    private ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();

    /**
     * <p>Constructor for SpellAbilityList.</p>
     */
    public SpellAbilityList() {
    }

    /**
     * <p>Constructor for SpellAbilityList.</p>
     *
     * @param s a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbilityList(SpellAbility s) {
        add(s);
    }

    /**
     * <p>Constructor for SpellAbilityList.</p>
     *
     * @param s an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    public SpellAbilityList(SpellAbility[] s) {
        for (int i = 0; i < s.length; i++)
            add(s[i]);
    }

    /**
     * <p>remove.</p>
     *
     * @param n a int.
     */
    public void remove(int n) {
        list.remove(n);
    }

    /**
     * <p>add.</p>
     *
     * @param s a {@link forge.card.spellability.SpellAbility} object.
     */
    public void add(SpellAbility s) {
        list.add(s);
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return list.size();
    }

    /**
     * <p>get.</p>
     *
     * @param n a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility get(int n) {
        return list.get(n);
    }

    /**
     * <p>addAll.</p>
     *
     * @param s a {@link forge.card.spellability.SpellAbilityList} object.
     */
    public void addAll(SpellAbilityList s) {
        for (int i = 0; i < s.size(); i++)
            add(s.get(i));
    }

    //Move1.getMax() uses this
    /**
     * <p>execute.</p>
     */
    public void execute() {
        for (int i = 0; i < size(); i++) {
            if (!ComputerUtil.canPayCost(get(i))) throw new RuntimeException(
                    "SpellAbilityList : execute() error, cannot pay for the spell " + get(i).getSourceCard()
                            + " - " + get(i).getStackDescription());

            ComputerUtil.playNoStack(get(i));
        }
    }//execute()

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            sb.append(get(i).getSourceCard().toString());
            sb.append(" - ");
            sb.append(get(i).getStackDescription());
            sb.append("\r\n");
        }
        return sb.toString();
    }//toString()

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        return toString().equals(o.toString());
    }
}
