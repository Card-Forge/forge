package forge.gui.home.quest;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import forge.gui.toolbox.FLabel;

/** Dictates methods required for a panel with stats/pet display. */

public interface IStatsAndPet {
    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getBtnBazaar();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getBtnSpellShop();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblCredits();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblLife();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblWins();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblLosses();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblNextChallengeInWins();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblWinStreak();

    /** @return {@link javax.swing.JComboBox} */
    JComboBox getCbxPet();

    /** @return {@link javax.swing.JCheckBox} */
    JCheckBox getCbPlant();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblZep();

    /** */
    void updateCurrentDeckStatus();
}
