package forge.toolbox.special;

import forge.game.phase.PhaseType;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PhaseIndicator extends JPanel {
    private static final long serialVersionUID = -863730022835609252L;
    
    // Phase labels
    private PhaseLabel lblUpkeep = new PhaseLabel("UP");
    private PhaseLabel lblDraw = new PhaseLabel("DR");
    private PhaseLabel lblMain1 = new PhaseLabel("M1");
    private PhaseLabel lblBeginCombat = new PhaseLabel("BC");
    private PhaseLabel lblDeclareAttackers = new PhaseLabel("DA");
    private PhaseLabel lblDeclareBlockers = new PhaseLabel("DB");
    private PhaseLabel lblFirstStrike = new PhaseLabel("FS");
    private PhaseLabel lblCombatDamage = new PhaseLabel("CD");
    private PhaseLabel lblEndCombat = new PhaseLabel("EC");
    private PhaseLabel lblMain2 = new PhaseLabel("M2");
    private PhaseLabel lblEndTurn = new PhaseLabel("ET");
    private PhaseLabel lblCleanup = new PhaseLabel("CL");
    
    
    public PhaseIndicator() { 
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0 0 1% 0, gap 0, wrap"));
        populatePhase();
    }
    
    /** Adds phase indicator labels to phase area JPanel container. */
    private void populatePhase() {
        // Constraints string, set once
        final String constraints = "w 94%!, h 7.2%, gaptop 1%, gapleft 3%";

        lblUpkeep.setToolTipText("<html>Phase: Upkeep<br>Click to toggle.</html>");
        this.add(lblUpkeep, constraints);

        lblDraw.setToolTipText("<html>Phase: Draw<br>Click to toggle.</html>");
        this.add(lblDraw, constraints);

        lblMain1.setToolTipText("<html>Phase: Main 1<br>Click to toggle.</html>");
        this.add(lblMain1, constraints);

        lblBeginCombat.setToolTipText("<html>Phase: Begin Combat<br>Click to toggle.</html>");
        this.add(lblBeginCombat, constraints);

        lblDeclareAttackers.setToolTipText("<html>Phase: Declare Attackers<br>Click to toggle.</html>");
        this.add(lblDeclareAttackers, constraints);

        lblDeclareBlockers.setToolTipText("<html>Phase: Declare Blockers<br>Click to toggle.</html>");
        this.add(lblDeclareBlockers, constraints);

        lblFirstStrike.setToolTipText("<html>Phase: First Strike Damage<br>Click to toggle.</html>");
        this.add(lblFirstStrike, constraints);

        lblCombatDamage.setToolTipText("<html>Phase: Combat Damage<br>Click to toggle.</html>");
        this.add(lblCombatDamage, constraints);

        lblEndCombat.setToolTipText("<html>Phase: End Combat<br>Click to toggle.</html>");
        this.add(lblEndCombat, constraints);

        lblMain2.setToolTipText("<html>Phase: Main 2<br>Click to toggle.</html>");
        this.add(lblMain2, constraints);

        lblEndTurn.setToolTipText("<html>Phase: End Turn<br>Click to toggle.</html>");
        this.add(lblEndTurn, constraints);

        lblCleanup.setToolTipText("<html>Phase: Cleanup<br>Click to toggle.</html>");
        this.add(lblCleanup, constraints);
    }
    

    //========== Custom class handling
    public PhaseLabel getLabelFor(final PhaseType s) {
        switch (s) {
        case UPKEEP:
            return this.getLblUpkeep();
        case DRAW:
            return this.getLblDraw();
        case MAIN1:
            return this.getLblMain1();
        case COMBAT_BEGIN:
            return this.getLblBeginCombat();
        case COMBAT_DECLARE_ATTACKERS:
            return this.getLblDeclareAttackers();
        case COMBAT_DECLARE_BLOCKERS:
            return this.getLblDeclareBlockers();
        case COMBAT_DAMAGE:
            return this.getLblCombatDamage();
        case COMBAT_FIRST_STRIKE_DAMAGE:
            return this.getLblFirstStrike();
        case COMBAT_END:
            return this.getLblEndCombat();
        case MAIN2:
            return this.getLblMain2();
        case END_OF_TURN:
            return this.getLblEndTurn();
        case CLEANUP:
            return this.getLblCleanup();
        default:
            return null;
        }
    }

    /**
     * Resets all phase buttons to "inactive", so highlight won't be drawn on
     * them. "Enabled" state remains the same.
     */
    public void resetPhaseButtons() {
        getLblUpkeep().setActive(false);
        getLblDraw().setActive(false);
        getLblMain1().setActive(false);
        getLblBeginCombat().setActive(false);
        getLblDeclareAttackers().setActive(false);
        getLblDeclareBlockers().setActive(false);
        getLblFirstStrike().setActive(false);
        getLblCombatDamage().setActive(false);
        getLblEndCombat().setActive(false);
        getLblMain2().setActive(false);
        getLblEndTurn().setActive(false);
        getLblCleanup().setActive(false);
    }

    // Phases
    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblUpkeep() {
        return this.lblUpkeep;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblDraw() {
        return this.lblDraw;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblMain1() {
        return this.lblMain1;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblBeginCombat() {
        return this.lblBeginCombat;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblDeclareAttackers() {
        return this.lblDeclareAttackers;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblDeclareBlockers() {
        return this.lblDeclareBlockers;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblCombatDamage() {
        return this.lblCombatDamage;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblFirstStrike() {
        return this.lblFirstStrike;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblEndCombat() {
        return this.lblEndCombat;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblMain2() {
        return this.lblMain2;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblEndTurn() {
        return this.lblEndTurn;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblCleanup() {
        return this.lblCleanup;
    }
}