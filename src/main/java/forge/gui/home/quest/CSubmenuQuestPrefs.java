package forge.gui.home.quest;

import javax.swing.SwingUtilities;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.ICDoc;
import forge.gui.home.quest.VSubmenuQuestPrefs.PrefInput;
import forge.quest.data.QuestPreferences;

/** 
 * Controls the quest preferences submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuQuestPrefs implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

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
     * Checks validity of values entered into prefInputs.
     * @param i0 &emsp; a PrefInput object
     */
    public static void validateAndSave(PrefInput i0) {
        if (i0.getText().equals(i0.getPreviousText())) { return; }
        final QuestPreferences prefs = Singletons.getModel().getQuestPreferences();

        int val = Integer.parseInt(i0.getText());
        resetErrors();

        String validationError = QuestPreferencesHandler.validatePreference(i0.getQPref(), val, prefs);
        if( null != validationError)
        {
            showError(i0, validationError);
            return;
        }

        prefs.setPreference(i0.getQPref(), i0.getText());
        prefs.save();
        i0.setPreviousText(i0.getText());
    }

    private static void showError(PrefInput i0, String s0) {
        final VSubmenuQuestPrefs view = VSubmenuQuestPrefs.SINGLETON_INSTANCE;
        String s = "Save failed: " + s0;
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
