package forge.gui.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.Constant;
import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactoryMana;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;

/**
 * <p>
 * Input_PayManaCostUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Input_PayManaCostUtil {
    // all mana abilities start with this and typical look like "tap: add G"
    // mana abilities are Strings and are retrieved by calling card.getKeyword()
    // taps any card that has mana ability, not just land
    /**
     * <p>
     * activateManaAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param card
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link forge.card.mana.ManaCost} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public static ManaCost activateManaAbility(final SpellAbility sa, final Card card, ManaCost manaCost) {
        // make sure computer's lands aren't selected
        if (card.getController().isComputer()) {
            return manaCost;
        }

        if (card instanceof ManaPool) {
            return ((ManaPool) card).subtractMana(sa, manaCost);
        }

        ArrayList<Ability_Mana> abilities = Input_PayManaCostUtil.getManaAbilities(card);
        final StringBuilder cneeded = new StringBuilder();
        boolean choice = true;
        boolean skipExpress = false;

        for (final String color : Constant.Color.MANA_COLORS) {
            if (manaCost.isNeeded(color)) {
                cneeded.append(Input_PayManaCostUtil.getShortColorString(color));
            }
        }

        final Iterator<Ability_Mana> it = abilities.iterator(); // you can't
                                                                // remove
        // unneeded abilities
        // inside a
        // for(am:abilities)
        // loop :(
        while (it.hasNext()) {
            final Ability_Mana ma = it.next();
            ma.setActivatingPlayer(AllZone.getHumanPlayer());
            if (!ma.canPlay()) {
                it.remove();
            } else if (!Input_PayManaCostUtil.canMake(ma, cneeded.toString())) {
                it.remove();
            }

            if (!skipExpress) {
                // skip express mana if the ability is not undoable
                if (!ma.isUndoable()) {
                    skipExpress = true;
                    continue;
                }
            }
        }
        if (abilities.isEmpty()) {
            return manaCost;
        }

        // TODO when implementing sunburst
        // If the card has sunburst or any other ability that tracks mana spent,
        // skip express Mana choice
        // if (card.getTrackManaPaid()) skipExpress = true;

        if (!skipExpress) {
            // express Mana Choice
            final ArrayList<Ability_Mana> colorMatches = new ArrayList<Ability_Mana>();

            for (final Ability_Mana am : abilities) {
                if (am.isReflectedMana()) {
                    final ArrayList<String> reflectableColors = AbilityFactoryMana.reflectableMana(am,
                            am.getAbilityFactory(), new ArrayList<String>(), new ArrayList<Card>());
                    for (final String color : reflectableColors) {
                        if (manaCost.isColor(color)) {
                            // checking if color
                            colorMatches.add(am);
                        }
                    }
                } else {
                    final String[] m = ManaPool.formatMana(am);
                    for (final String color : m) {
                        if (manaCost.isColor(color)) {
                            // checking if color
                            colorMatches.add(am);
                        }
                    }
                }
            }

            if ((colorMatches.size() == 0) || (colorMatches.size() == abilities.size())) {
                // can only match colorless just grab the first and move on.
                choice = false;
            } else if (colorMatches.size() < abilities.size()) {
                // leave behind only color matches
                abilities = colorMatches;
            }
        }

        Ability_Mana chosen = abilities.get(0);
        if ((1 < abilities.size()) && choice) {
            final HashMap<String, Ability_Mana> ability = new HashMap<String, Ability_Mana>();
            for (final Ability_Mana am : abilities) {
                ability.put(am.toString(), am);
            }
            chosen = (Ability_Mana) GuiUtils.getChoice("Choose mana ability", abilities.toArray());
        }

        AllZone.getGameAction().playSpellAbility(chosen);

        manaCost = AllZone.getHumanPlayer().getManaPool().subtractMana(sa, manaCost, chosen);

        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        // DO NOT REMOVE THIS, otherwise the cards don't always tap (copied)
        return manaCost;

    }

    /**
     * <p>
     * getManaAbilities.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Ability_Mana> getManaAbilities(final Card card) {
        return card.getManaAbility();
    }

    // color is like "G", returns "Green"
    /**
     * <p>
     * canMake.
     * </p>
     * 
     * @param am
     *            a {@link forge.card.spellability.Ability_Mana} object.
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean canMake(final Ability_Mana am, final String mana) {
        if (mana.contains("1")) {
            return true;
        }
        if (mana.contains("S") && am.isSnow()) {
            return true;
        }

        if (am.isReflectedMana()) {
            final ArrayList<String> reflectableColors = AbilityFactoryMana.reflectableMana(am, am.getAbilityFactory(),
                    new ArrayList<String>(), new ArrayList<Card>());
            for (final String color : reflectableColors) {
                if (mana.contains(Input_PayManaCostUtil.getShortColorString(color))) {
                    return true;
                }
            }
        } else {
            for (final String color : ManaPool.formatMana(am)) {
                if (mana.contains(color)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * getLongColorString.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLongColorString(final String color) {
        final Map<String, String> m = new HashMap<String, String>();
        m.put("G", Constant.Color.GREEN);
        m.put("R", Constant.Color.RED);
        m.put("U", Constant.Color.BLUE);
        m.put("B", Constant.Color.BLACK);
        m.put("W", Constant.Color.WHITE);
        m.put("S", Constant.Color.SNOW);

        Object o = m.get(color);

        if (o == null) {
            o = Constant.Color.COLORLESS;
        }

        return o.toString();
    }

    /**
     * <p>
     * getShortColorString.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getShortColorString(final String color) {
        final Map<String, String> m = new HashMap<String, String>();
        m.put(Constant.Color.GREEN, "G");
        m.put(Constant.Color.RED, "R");
        m.put(Constant.Color.BLUE, "U");
        m.put(Constant.Color.BLACK, "B");
        m.put(Constant.Color.WHITE, "W");
        m.put(Constant.Color.COLORLESS, "1");
        m.put(Constant.Color.SNOW, "S");

        final Object o = m.get(color);

        return o.toString();
    }

}
