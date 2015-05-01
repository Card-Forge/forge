package forge.screens.home.quest;

import javax.swing.SwingUtilities;

import com.google.common.primitives.Ints;

import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.quest.data.QuestPreferences;
import forge.screens.home.quest.VSubmenuQuestPrefs.PrefInput;

/**
 * Controls the quest preferences submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuQuestPrefs implements ICDoc {
    SINGLETON_INSTANCE;

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { VSubmenuQuestPrefs.SINGLETON_INSTANCE.focusFirstTextbox(); }
        });
    }

    /**
     * Checks validity of values entered into quest preference input text
     * fields.
     *
     * @param i0
     *            the input.
     */
    public static void validateAndSave(final PrefInput i0) {
        if (i0.getText().equals(i0.getPreviousText())) { return; }
        final QuestPreferences prefs = FModel.getQuestPreferences();

        final Integer val = Ints.tryParse(i0.getText());
        resetErrors();

        final String validationError = val == null ? "Enter a number" : prefs.validatePreference(i0.getQPref(), val.intValue());
        if (validationError != null) {
            showError(i0, validationError);
            return;
        }

        prefs.setPref(i0.getQPref(), i0.getText());
        prefs.save();
        i0.setPreviousText(i0.getText());
    }

    private static void showError(final PrefInput i0, final String s0) {
        final VSubmenuQuestPrefs view = VSubmenuQuestPrefs.SINGLETON_INSTANCE;
        final String s = "Save failed: " + s0;
        switch(i0.getErrType()) {
        case BOOSTER:
            view.getLblErrBooster().setVisible(true);
            view.getLblErrBooster().setText(s);
            break;
        case DIFFICULTY:
            view.getLblErrDifficulty().setVisible(true);
            view.getLblErrDifficulty().setText(s);
            break;
        case REWARDS:
            view.getLblErrRewards().setVisible(true);
            view.getLblErrRewards().setText(s);
            break;
        case SHOP:
            view.getLblErrShop().setVisible(true);
            view.getLblErrShop().setText(s);
            break;
        default:
            break;
        }

        i0.setText(i0.getPreviousText());
    }

    /** */
    public static void resetErrors() {
        final VSubmenuQuestPrefs view = VSubmenuQuestPrefs.SINGLETON_INSTANCE;

        view.getLblErrBooster().setVisible(false);
        view.getLblErrDifficulty().setVisible(false);
        view.getLblErrRewards().setVisible(false);
        view.getLblErrShop().setVisible(false);
    }

}
