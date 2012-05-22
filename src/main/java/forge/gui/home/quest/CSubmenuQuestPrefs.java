package forge.gui.home.quest;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.ICDoc;
import forge.gui.home.ICSubmenu;
import forge.gui.home.quest.VSubmenuQuestPrefs.PrefInput;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * Controls the quest preferences submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuQuestPrefs implements ICSubmenu, ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuQuestPrefs.SINGLETON_INSTANCE.populate();
        CSubmenuQuestPrefs.SINGLETON_INSTANCE.update();
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {

    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /**
     * Checks validity of values entered into prefInputs.
     * @param i0 &emsp; a PrefInput object
     */
    public static void validateAndSave(PrefInput i0) {
        if (i0.getText().equals(i0.getPreviousText())) { return; }
        final QuestPreferences prefs = Singletons.getModel().getQuestPreferences();
        int temp1, temp2;

        int val = Integer.parseInt(i0.getText());
        resetErrors();

        switch (i0.getQPref()) {
            case STARTING_CREDITS_EASY: case STARTING_CREDITS_MEDIUM:
            case STARTING_CREDITS_HARD: case STARTING_CREDITS_EXPERT:
            case REWARDS_MILLED: case REWARDS_MULLIGAN0:
            case REWARDS_ALTERNATIVE: case REWARDS_TURN5:
                if (val > 500) {
                    showError(i0, "Value too large (maximum 500).");
                    return;
                }
                break;
            case BOOSTER_COMMONS:
                temp1 = prefs.getPreferenceInt(QPref.BOOSTER_UNCOMMONS);
                temp2 = prefs.getPreferenceInt(QPref.BOOSTER_RARES);

                if (temp1 + temp2 + val > 15) {
                    showError(i0, "Booster packs must have maximum 15 cards.");
                    return;
                }
                break;
            case BOOSTER_UNCOMMONS:
                temp1 = prefs.getPreferenceInt(QPref.BOOSTER_COMMONS);
                temp2 = prefs.getPreferenceInt(QPref.BOOSTER_RARES);

                if (temp1 + temp2 + val > 15) {
                    showError(i0, "Booster packs must have maximum 15 cards.");
                    return;
                }
                break;
            case BOOSTER_RARES:
                temp1 = prefs.getPreferenceInt(QPref.BOOSTER_COMMONS);
                temp2 = prefs.getPreferenceInt(QPref.BOOSTER_UNCOMMONS);

                if (temp1 + temp2 + val > 15) {
                    showError(i0, "Booster packs must have maximum 15 cards.");
                    return;
                }
                break;
            case REWARDS_TURN1:
                if (val > 2000) {
                    showError(i0, "Value too large (maximum 2000).");
                    return;
                }
                break;
            case SHOP_STARTING_PACKS:
            case SHOP_SINGLES_COMMON: case SHOP_SINGLES_UNCOMMON: case SHOP_SINGLES_RARE:
                if (val < 0) {
                    showError(i0, "Value too small (minimum 0).");
                    return;
                } else if (val > 15) {
                    showError(i0, "Value too large (maximum 15).");
                    return;
                }
                break;
            case SHOP_WINS_FOR_ADDITIONAL_PACK: case SHOP_MAX_PACKS:
                if (val < 1) {
                    showError(i0, "Value too small (minimum 1).");
                    return;
                } else if (val > 25) {
                    showError(i0, "Value too large (maximum 25).");
                    return;
                }
                break;
            default:
                if (val > 100) {
                    showError(i0, "Value too large (maximum 100).");
                    return;
                }
                break;
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
