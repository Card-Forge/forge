package forge.gui.home.quest;

import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FComboBoxWrapper;
import forge.gui.toolbox.FLabel;

/** Dictates methods required for a panel with stats/pet display. */

public interface IVQuestStats {
    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getBtnBazaar();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getBtnSpellShop();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getBtnUnlock();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getBtnTravel();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblCredits();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblLife();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblWorld();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblWins();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblLosses();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblNextChallengeInWins();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblCurrentDeck();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblWinStreak();

    /** @return {@link javax.swing.FComboBoxWrapper} */
    FComboBoxWrapper<String> getCbxPet();

    /** @return {@link forge.gui.toolbox.FCheckBox} */
    FCheckBox getCbPlant();

    /** @return {@link forge.gui.toolbox.FCheckBox} */ 
    FCheckBox getCbCharm();

    /** @return {@link forge.gui.toolbox.FLabel} */
    FLabel getLblZep();
}
