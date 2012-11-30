package forge.gui.home.quest;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import forge.gui.toolbox.ExperimentalLabel;

/** Dictates methods required for a panel with stats/pet display. */

public interface IVQuestStats {
    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    ExperimentalLabel getBtnBazaar();

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    ExperimentalLabel getBtnSpellShop();

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    ExperimentalLabel getBtnUnlock();

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
    JLabel getLblCurrentDeck();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblWinStreak();

    /** @return {@link javax.swing.JComboBox} */
    JComboBox getCbxPet();

    /** @return {@link javax.swing.JCheckBox} */
    JCheckBox getCbPlant();

    /** @return {@link javax.swing.JLabel} */
    JLabel getLblZep();
}
