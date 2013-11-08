package forge.gui.match.controllers;

import java.util.List;

import forge.Card;
import forge.Command;
import forge.GameEntity;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VCombat;
import forge.util.Lang;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCombat implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    
    private Combat combat;

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        Combat localCombat = this.combat; // noone will re-assign this from other thread. 
        if (localCombat != null )
            VCombat.SINGLETON_INSTANCE.updateCombat(localCombat.getAttackers().size(), getCombatDescription(localCombat));
        else
            VCombat.SINGLETON_INSTANCE.updateCombat(0, "");
    }
    
    public void setModel(Combat combat)
    {
        this.combat = combat;

    }
    
    private static String getCombatDescription(Combat combat) {
        final StringBuilder display = new StringBuilder();

        // Not a big fan of the triple nested loop here
        for (GameEntity defender : combat.getDefenders()) {
            List<AttackingBand> bands = combat.getAttackingBandsOf(defender);
            if (bands == null || bands.isEmpty()) {
                continue;
            }

            if (display.length() > 0) {
                display.append("\n");
            }

            if (defender instanceof Card) {
                Player controller = ((Card) defender).getController();
                display.append(Lang.getPossesive(controller.getName())).append(" ");
            }

            display.append(defender.getName()).append(" is attacked by:\n");

            // Associate Bands, Attackers Blockers
            boolean previousBand = false;
            for(AttackingBand band : bands) {
                if (band.isEmpty())
                    continue;
                
                // Space out band blocks from non-band blocks
                if (previousBand) {
                    display.append("\n");
                }
                
                Boolean blocked = band.isBlocked();
                boolean isBand = band.getAttackers().size() > 1;
                if (isBand) {
                    // Only print Band data if it's actually a band
                    display.append(" > BAND");
                    
                    if( blocked != null )
                        display.append(blocked.booleanValue() ? " (blocked)" : " >>>");
                    
                    display.append("\n");
                }
                
                for (final Card c : band.getAttackers()) {
                    display.append(" > ");
                    display.append(combatantToString(c)).append("\n");
                }

                List<Card> blockers = combat.getBlockers(band);
                if (!isBand && blockers.isEmpty()) {
                    // if single creature is blocked, but no longer has blockers, tell the user!
                    if (blocked != null)
                        display.append(blocked.booleanValue() ? "     (blocked)\n" : "     >>>\n");
                }

                for (final Card element : blockers) {
                    display.append("     < ").append(combatantToString(element)).append("\n");
                }
                previousBand = isBand;
            }
        }
        return display.toString().trim();
    }
    
    /**
     * <p>
     * combatantToString.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    private static String combatantToString(final Card c) {
        final StringBuilder sb = new StringBuilder();

        final String name = (c.isFaceDown()) ? "Morph" : c.getName();
        
        sb.append("( ").append(c.getNetAttack()).append(" / ").append(c.getNetDefense()).append(" ) ... ");
        sb.append(name);
        sb.append(" [").append(c.getUniqueNumber()).append("] ");
        

        return sb.toString();
    }
}
