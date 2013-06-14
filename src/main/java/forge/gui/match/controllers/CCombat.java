package forge.gui.match.controllers;

import java.util.List;

import forge.Card;
import forge.Command;
import forge.GameEntity;
import forge.game.Game;
import forge.game.combat.AttackingBand;
import forge.game.phase.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
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
    
    private Game game;

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

    public final boolean hasCombatToShow() {
        PhaseHandler pH = game.getPhaseHandler();
        PhaseType ph = pH.getPhase();

        return game.getCombat().isCombat() && ph.isAfter(PhaseType.COMBAT_BEGIN) && ph.isBefore(PhaseType.END_OF_TURN);
    }
    
    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        if (hasCombatToShow()) // display combat
            VCombat.SINGLETON_INSTANCE.updateCombat(game.getCombat().getAttackers().size(), getCombatDescription(game.getCombat()));
        else
            VCombat.SINGLETON_INSTANCE.updateCombat(0, "");
    }
    
    public void setModel(Game game)
    {
        this.game = game;

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
            for(AttackingBand band : bands) {
                display.append(" BAND");
                if (band.getBlocked()) {
                    display.append(" (blocked)");
                }
                display.append("\n");
                
                for (final Card c : band.getAttackers()) {
                    display.append(" > ");
                    display.append(combatantToString(c)).append("\n");
                }

                for (final Card element : band.getBlockers()) {
                    display.append("     < ").append(combatantToString(element)).append("\n");
                }
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
