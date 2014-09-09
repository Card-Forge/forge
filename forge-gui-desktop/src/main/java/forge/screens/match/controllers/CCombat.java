package forge.screens.match.controllers;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.screens.match.views.VCombat;
import forge.util.Lang;
import forge.view.CardView;
import forge.view.CardView.CardStateView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.PlayerView;

/** 
 * Controls the combat panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCombat implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    
    private CombatView combat;

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
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
        final CombatView localCombat = this.combat; // noone will re-assign this from other thread. 
        if (localCombat != null) {
            VCombat.SINGLETON_INSTANCE.updateCombat(localCombat.getNumAttackers(), getCombatDescription(localCombat));
        } else {
            VCombat.SINGLETON_INSTANCE.updateCombat(0, "");
        }
    }

    public void setModel(final CombatView combat) {
        this.combat = combat;
    }

    private static String getCombatDescription(final CombatView localCombat) {
        final StringBuilder display = new StringBuilder();

        for (final GameEntityView defender : localCombat.getDefenders()) {
            display.append(getCombatDescription(localCombat, defender));
        }
        return display.toString().trim();
    }

    private static String getCombatDescription(final CombatView localCombat, final GameEntityView defender) {
        final StringBuilder display = new StringBuilder();

        Iterable<Iterable<CardView>> bands = localCombat.getAttackingBandsOf(defender);
        if (bands == null || Iterables.isEmpty(bands)) {
            return StringUtils.EMPTY;
        }

        display.append("\n");

        if (defender instanceof CardView) {
            final PlayerView controller = ((CardView) defender).getController();
            display.append(Lang.getPossesive(controller.getName())).append(" ");
        }

        display.append(defender).append(" is attacked by:\n");

        // Associate Bands, Attackers Blockers
        boolean previousBand = false;
        for (final Iterable<CardView> band : bands) {
            final int bandSize = Iterables.size(band);
            if (bandSize == 0) {
                continue;
            }

            // Space out band blocks from non-band blocks
            if (previousBand) {
                display.append("\n");
            }

            final Iterable<CardView> blockers = localCombat.getBlockers(band); 
            final boolean blocked = (blockers != null);
            final boolean isBand = bandSize > 1;
            if (isBand) {
                // Only print Band data if it's actually a band
                display.append(" > BAND");
                display.append(blocked ? " (blocked)" : " >>>");
                display.append("\n");
            }

            for (final CardView attacker : band) {
                display.append(" > ");
                display.append(combatantToString(attacker)).append("\n");
            }

            if (!isBand) {
                if (blockers != null && Iterables.isEmpty(blockers)) {
                    // if single creature is blocked, but no longer has blockers, tell the user!
                    display.append("     (blocked)\n");
                } else {
                    display.append("     >>>\n");
                }
            }

            if (blockers != null) {
                for (final CardView blocker : blockers) {
                    display.append("     < ")
                           .append(combatantToString(blocker))
                           .append("\n");
                }
            }
            previousBand = isBand;
        }

        return display.toString();
    }

    /**
     * <p>
     * combatantToString.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.lang.String} object.
     */
    private static String combatantToString(final CardView c) {
        final StringBuilder sb = new StringBuilder();
        final CardStateView state = c.getOriginal();

        final String name = state.getName();

        sb.append("( ").append(state.getPower()).append(" / ").append(state.getToughness()).append(" ) ... ");
        sb.append(name);
        sb.append(" [").append(c.getId()).append("] ");

        return sb.toString();
    }
}
