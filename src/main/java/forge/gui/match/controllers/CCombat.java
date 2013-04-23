package forge.gui.match.controllers;

import java.util.List;

import forge.Card;
import forge.Command;
import forge.GameEntity;
import forge.game.GameState;
import forge.game.phase.Combat;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VCombat;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCombat implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    
    private GameState game;

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
        if (!game.getPhaseHandler().inCombat())
            VCombat.SINGLETON_INSTANCE.updateCombat(0, "");
        else
            VCombat.SINGLETON_INSTANCE.updateCombat(game.getCombat().getAttackers().size(), getCombatDescription(game.getCombat()));
    }
    
    public void setModel(GameState game)
    {
        this.game = game;

    }
    
    private static String getCombatDescription(Combat combat) {
        final StringBuilder display = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final List<GameEntity> defenders = combat.getDefenders();
        final List<List<Card>> attackers = combat.sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            List<Card> atk = attackers.get(def);
            if ((atk == null) || (atk.size() == 0)) {
                continue;
            }

            if (def > 0) {
                display.append("\n");
            }

            display.append("Defender - ");
            display.append(defenders.get(def).toString());
            display.append("\n");

            for (final Card c : atk) {
                // loop through attackers
                display.append("-> ");
                display.append(combatantToString(c)).append("\n");

                List<Card> blockers = combat.getBlockers(c);

                // loop through blockers
                for (final Card element : blockers) {
                    display.append(" [ ");
                    display.append(combatantToString(element)).append("\n");
                }
            } // loop through attackers
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

        sb.append(name);
        sb.append(" (").append(c.getUniqueNumber()).append(") ");
        sb.append(c.getNetAttack()).append("/").append(c.getNetDefense());

        return sb.toString();
    }
}
