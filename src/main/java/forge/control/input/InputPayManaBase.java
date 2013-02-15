package forge.control.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardUtil;
import forge.Constant;
import forge.Singletons;
import forge.card.MagicColor;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaPool;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.gui.GuiChoose;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class InputPayManaBase extends Input {

    private static final long serialVersionUID = -9133423708688480255L;

    protected int phyLifeToLose = 0;
    
    protected final Player whoPays;
    protected final GameState game;
    protected ManaCostBeingPaid manaCost;
    
    protected InputPayManaBase(final GameState game) {
        this.game = game;
        this.whoPays = Singletons.getControl().getPlayer();
    }
    
    /**
     * <p>
     * selectManaPool.
     * </p>
     * @param color a String that represents the Color the mana is coming from
     */
    public abstract void selectManaPool(String color);

    /**
     * <p>
     * canMake.  color is like "G", returns "Green".
     * </p>
     * 
     * @param am
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static boolean canMake(final SpellAbility am, final String mana) {
        if (mana.contains("1")) {
            return true;
        }
        AbilityManaPart m = am.getManaPart();
        if (mana.contains("S") && m.isSnow()) {
            return true;
        }
        if (m.isAnyMana()) {
            return true;
        }
        if (m.isReflectedMana()) {
            final Iterable<String> reflectableColors = CardUtil.getReflectableManaColors(am, am, new HashSet<String>(), new ArrayList<Card>());
            for (final String color : reflectableColors) {
                if (mana.contains(MagicColor.toShortString(color))) {
                    return true;
                }
            }
        } else {
            String[] colorsProduced;
            if (m.isComboMana()) {
                colorsProduced = m.getComboColors().split(" ");
            }
            else {
                colorsProduced = m.getOrigProduced().split(" ");
            }
            for (final String color : colorsProduced) {
                if (mana.contains(color)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * activateManaAbility.
     * </p>
     * @param color a String that represents the Color the mana is coming from
     * @param saBeingPaidFor a SpellAbility that is being paid for
     * @param manaCost the amount of mana remaining to be paid
     * 
     * @return ManaCost the amount of mana remaining to be paid after the mana is activated
     */
    protected static ManaCostBeingPaid activateManaAbility(String color, final SpellAbility saBeingPaidFor, ManaCostBeingPaid manaCost) {
        ManaPool mp = Singletons.getControl().getPlayer().getManaPool();
    
        // Convert Color to short String
        String manaStr = "1";
        if (!color.equalsIgnoreCase("Colorless")) {
            manaStr = CardUtil.getShortColor(color);
        }
    
        return mp.payManaFromPool(saBeingPaidFor, manaCost, manaStr);
    }

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
     *            a {@link forge.card.mana.ManaCostBeingPaid} object.
     * @return a {@link forge.card.mana.ManaCostBeingPaid} object.
     */
    protected ManaCostBeingPaid activateManaAbility(final SpellAbility sa, final Card card, ManaCostBeingPaid manaCost) {
        // make sure computer's lands aren't selected
        if (card.getController() != whoPays) {
            return manaCost;
        }
        
    
        final StringBuilder cneeded = new StringBuilder();
        final StringBuilder colorRequired = new StringBuilder();
        boolean choice = true;
        boolean skipExpress = false;
    
        for (final String color : Constant.Color.MANA_COLORS) {
            String shortColor = MagicColor.toShortString(color);
            if (manaCost.isNeeded(color)) {
                cneeded.append(shortColor);
            }
            if (manaCost.isColor(shortColor)) {
                colorRequired.append(shortColor);
            }
        }
    
        List<SpellAbility> abilities = new ArrayList<SpellAbility>();
        // you can't remove unneeded abilities inside a for(am:abilities) loop :(
    
        for (SpellAbility ma : card.getManaAbility()) {
            ma.setActivatingPlayer(whoPays);
            AbilityManaPart m = null;
            SpellAbility tail = ma;
            while(m == null && tail != null)
            {
                m = tail.getManaPart();
                tail = tail.getSubAbility();
            }
            if(m == null) {
                continue;
            } else if (!ma.canPlay()) {
                continue;
            } else if (!InputPayManaBase.canMake(ma, cneeded.toString())) {
                continue;
            } else if (ma.isAbility() && ma.getRestrictions().isInstantSpeed()) {
                continue;
            } else if (!m.meetsManaRestrictions(sa)) {
                continue;
            }
    
            abilities.add(ma);
    
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
    
        // Store some information about color costs to help with any mana choices
        String colorsNeeded = colorRequired.toString();
        if ("1".equals(colorsNeeded)) {  // only colorless left
            if (sa.getSourceCard() != null
                    && !sa.getSourceCard().getSVar("ManaNeededToAvoidNegativeEffect").equals("")) {
                colorsNeeded = "";
                String[] negEffects = sa.getSourceCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
                for (String negColor : negEffects) {
                    // convert long color strings to short color strings
                    if (negColor.length() > 1) {
                        negColor = MagicColor.toShortString(negColor);
                    }
                    if (!colorsNeeded.contains(negColor)) {
                      colorsNeeded = colorsNeeded.concat(negColor);
                    }
                }
            }
            else {
                colorsNeeded = "W";
            }
        }
        else {
            // remove colorless from colors needed
            colorsNeeded = colorsNeeded.replace("1", "");
        }
    
        // If the card has sunburst or any other ability that tracks mana spent,
        // skip express Mana choice
        if (sa.getSourceCard() != null
                && sa.getSourceCard().hasKeyword("Sunburst") && sa.isSpell()) {
            colorsNeeded = "WUBRG";
            skipExpress = true;
        }
    
        if (!skipExpress) {
            // express Mana Choice
            final ArrayList<SpellAbility> colorMatches = new ArrayList<SpellAbility>();
    
            for (final SpellAbility am : abilities) {
                AbilityManaPart m = am.getManaPart();
                if (m.isReflectedMana()) {
                    final Iterable<String> reflectableColors = CardUtil.getReflectableManaColors(am, am, new HashSet<String>(), new ArrayList<Card>());
                    for (final String color : reflectableColors) {
                        if (manaCost.isColor(color)) {
                            // checking if color
                            colorMatches.add(am);
                        }
                    }
                } else if (m.isAnyMana()) {
                        colorMatches.add(am);
                } else {
                    String[] colorsProduced;
                    if (m.isComboMana()) {
                        colorsProduced = m.getComboColors().split(" ");
                    }
                    else {
                        colorsProduced = m.getOrigProduced().split(" ");
                    }
                    for (final String color : colorsProduced) {
                        if (manaCost.isColor(color)) {
                            // checking if color
                            colorMatches.add(am);
                        }
                    }
                }
            }
    
            if ((colorMatches.size() == 0)) {
                // can only match colorless just grab the first and move on.
                choice = false;
            } else if (colorMatches.size() < abilities.size()) {
                // leave behind only color matches
                abilities = colorMatches;
            }
        }
    
        SpellAbility chosen = abilities.get(0);
        if ((1 < abilities.size()) && choice) {
            final Map<String, SpellAbility> ability = new HashMap<String, SpellAbility>();
            for (final SpellAbility am : abilities) {
                ability.put(am.toString(), am);
            }
            chosen = GuiChoose.one("Choose mana ability", abilities);
        }
        
        SpellAbility subchosen = chosen;
        while(subchosen.getManaPart() == null)
        {
            subchosen = subchosen.getSubAbility();
        }
    
        // save off color needed for use by any mana and reflected mana
        subchosen.getManaPart().setExpressChoice(colorsNeeded);
        
        Player p = chosen.getActivatingPlayer();
        Singletons.getModel().getGame().getActionPlay().playSpellAbility(chosen, p);
    
        manaCost = p.getManaPool().payManaFromAbility(sa, manaCost, chosen);
    
        //AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();
        // DO NOT REMOVE THIS, otherwise the cards don't always tap (copied)
        return manaCost;
    
    }
}
