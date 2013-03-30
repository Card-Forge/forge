package forge.control.input;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import forge.Card;
import forge.CardUtil;
import forge.Constant;
import forge.FThreads;
import forge.card.MagicColor;
import forge.card.ability.ApiType;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaPool;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.views.VMessage;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class InputPayManaBase extends InputSyncronizedBase implements InputPayment {

    private static final long serialVersionUID = -9133423708688480255L;

    protected int phyLifeToLose = 0;
    
    protected final Player whoPays;
    protected final GameState game;
    protected ManaCostBeingPaid manaCost;
    protected final SpellAbility saPaidFor;
    
    boolean bPaid = false;
    
    protected InputPayManaBase(final GameState game, SpellAbility saToPayFor) {
        this.game = game;
        this.whoPays = saToPayFor.getActivatingPlayer();
        this.saPaidFor = saToPayFor;
    }
    
    /** {@inheritDoc} */
    @Override
    public void selectCard(final Card card) {
        if (card.getManaAbility().isEmpty() || card.isInZone(ZoneType.Hand)) {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
            return;
        }
        // only tap card if the mana is needed
        activateManaAbility(card, this.manaCost);
    }
    
    public void selectManaPool(String color) {
        useManaFromPool(color, this.manaCost);
    }

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
        if (am.getApi() == ApiType.ManaReflected) {
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
    protected void useManaFromPool(String color, ManaCostBeingPaid manaCost) {
        ManaPool mp = whoPays.getManaPool();
    
        // Convert Color to short String
        String manaStr = "1";
        if (!color.equalsIgnoreCase("Colorless")) {
            manaStr = CardUtil.getShortColor(color);
        }
        
        this.manaCost = mp.payManaFromPool(saPaidFor, manaCost, manaStr); 
    
        onManaAbilityPlayed(null);
        showMessage();
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
    protected void activateManaAbility(final Card card, ManaCostBeingPaid manaCost) {
        // make sure computer's lands aren't selected
        if (card.getController() != whoPays) {
            return;
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
            } else if (!m.meetsManaRestrictions(saPaidFor)) {
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
            return;
        }
    
        // Store some information about color costs to help with any mana choices
        String colorsNeeded = colorRequired.toString();
        if ("1".equals(colorsNeeded)) {  // only colorless left
            if (saPaidFor.getSourceCard() != null
                    && !saPaidFor.getSourceCard().getSVar("ManaNeededToAvoidNegativeEffect").equals("")) {
                colorsNeeded = "";
                String[] negEffects = saPaidFor.getSourceCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
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
        if (saPaidFor.getSourceCard() != null
                && saPaidFor.getSourceCard().hasKeyword("Sunburst") && saPaidFor.isSpell()) {
            colorsNeeded = "WUBRG";
            skipExpress = true;
        }
    
        if (!skipExpress) {
            // express Mana Choice
            final ArrayList<SpellAbility> colorMatches = new ArrayList<SpellAbility>();
    
            for (final SpellAbility am : abilities) {
                AbilityManaPart m = am.getManaPart();
                if (am.getApi() == ApiType.ManaReflected) {
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
    
            if (colorMatches.isEmpty()) {
                // can only match colorless just grab the first and move on.
                choice = false;
            } else if (colorMatches.size() < abilities.size()) {
                // leave behind only color matches
                abilities = colorMatches;
            }
        }
    
        final SpellAbility chosen = abilities.size() > 1 && choice ? GuiChoose.one("Choose mana ability", abilities) : abilities.get(0);
        SpellAbility subchosen = chosen;
        while(subchosen.getManaPart() == null)
        {
            subchosen = subchosen.getSubAbility();
        }
    
        // save off color needed for use by any mana and reflected mana
        subchosen.getManaPart().setExpressChoice(colorsNeeded);
        
        // System.out.println("Chosen sa=" + chosen + " of " + chosen.getSourceCard() + " to pay mana");
        Runnable proc = new Runnable() {
            @Override
            public void run() {
                final Player p = chosen.getActivatingPlayer();
                p.getGame().getActionPlay().playSpellAbility(chosen, p);
                onManaAbilityPlayed(chosen); 
            }
        };
        FThreads.invokeInNewThread(proc, true);
        // EDT that removes lockUI from input stack will call our showMessage() method
    }
    
    public void onManaAbilityPlayed(final SpellAbility saPaymentSrc) { 
        if ( saPaymentSrc != null) // null comes when they've paid from pool
            this.manaCost = whoPays.getManaPool().payManaFromAbility(saPaidFor, manaCost, saPaymentSrc);

        onManaAbilityPaid();
        if ( saPaymentSrc != null )
            whoPays.getZone(ZoneType.Battlefield).updateObservers();
    }
    
    protected final void checkIfAlredyPaid() {
        if (manaCost.isPaid()) {
            bPaid = true;
            done();
        }
    }
    
    protected void onManaAbilityPaid() {} // some inputs overload it
    protected abstract void done();

    @Override
    public String toString() {
        return String.format("PayManaBase %s (out of %s)", manaCost.toString(), manaCost.getStartingCost() );
    }

    protected void handleConvokedCards(boolean isCancelled) {
        if (saPaidFor.getTappedForConvoke() != null) {
            for (final Card c : saPaidFor.getTappedForConvoke()) {
                c.setTapped(false);
                if (!isCancelled)
                    c.tap(); // that will tap cards with all the triggers, it's no good to call this from EDT
            }
            saPaidFor.clearTappedForConvoke();
        }
    }
    
    public boolean isPaid() { return bPaid; }
}
