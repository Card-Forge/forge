package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Forge;
import forge.player.AutoYieldStore;
import forge.screens.match.MatchController;
import forge.toolbox.FCheckBox;
import forge.toolbox.FChoiceList;
import forge.toolbox.FDialog;
import forge.toolbox.FOptionPane;
import forge.util.TextBounds;

public class VAutoTriggers extends FDialog {
    private static final String ACCEPT_PREFIX = "[" + Forge.getLocalizer().getMessage("lblAlwaysYes") + "] ";
    private static final String DECLINE_PREFIX = "[" + Forge.getLocalizer().getMessage("lblAlwaysNo") + "] ";

    private final FChoiceList<String> lstAutoTriggers;
    private final FCheckBox chkDisableAll;

    public VAutoTriggers() {
        super(Forge.getLocalizer().getMessage("lblAutoTriggers"), 2);
        List<String> autoTriggers = new ArrayList<>();
        for (Map.Entry<String, AutoYieldStore.TriggerDecision> entry : MatchController.instance.getGameController().getYieldController().getAutoTriggers()) {
            autoTriggers.add(formatEntry(entry));
        }
        lstAutoTriggers = add(new FChoiceList<String>(autoTriggers) {
            @Override
            protected void onCompactModeChange() {
                VAutoTriggers.this.revalidate(); //revalidate entire dialog so height updated
            }

            @Override
            protected boolean allowDefaultItemWrap() {
                return true;
            }
        });
        chkDisableAll = add(new FCheckBox(Forge.getLocalizer().getMessage("lblDisableAllAutoTriggers"), MatchController.instance.getGameController().getDisableAutoTriggers()));
        chkDisableAll.setCommand(e -> MatchController.instance.getGameController().setDisableAutoTriggers(chkDisableAll.isSelected()));
        initButton(0, Forge.getLocalizer().getMessage("lblOK"), e -> hide());
        initButton(1, Forge.getLocalizer().getMessage("lblRemoveTrigger"), e -> {
            String selected = lstAutoTriggers.getSelectedItem();
            if (selected != null) {
                lstAutoTriggers.removeItem(selected);
                String key = stripPrefix(selected);
                boolean abilityScope = !forge.localinstance.properties.ForgeConstants.AUTO_TRIGGER_PER_CARD.equals(
                        forge.model.FModel.getPreferences().getPref(forge.localinstance.properties.ForgePreferences.FPref.UI_AUTO_TRIGGER_MODE));
                MatchController.instance.getGameController().setShouldAlwaysAskTrigger(key, abilityScope);
                setButtonEnabled(1, lstAutoTriggers.getCount() > 0);
                lstAutoTriggers.cleanUpSelections();
                VAutoTriggers.this.revalidate();
            }
        });
        setButtonEnabled(1, autoTriggers.size() > 0);
    }

    private static String formatEntry(final Map.Entry<String, AutoYieldStore.TriggerDecision> entry) {
        String prefix = entry.getValue() == AutoYieldStore.TriggerDecision.ACCEPT ? ACCEPT_PREFIX : DECLINE_PREFIX;
        return prefix + entry.getKey();
    }

    private static String stripPrefix(final String display) {
        if (display.startsWith(ACCEPT_PREFIX)) return display.substring(ACCEPT_PREFIX.length());
        if (display.startsWith(DECLINE_PREFIX)) return display.substring(DECLINE_PREFIX.length());
        return display;
    }

    @Override
    public void show() {
        if (lstAutoTriggers.getCount() > 0) {
            super.show();
        }
        else {
            FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblNoActiveAutoTrigger"), Forge.getLocalizer().getMessage("lblNoAutoTrigger"), FOptionPane.INFORMATION_ICON);
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        TextBounds checkBoxSize = chkDisableAll.getAutoSizeBounds();

        float listHeight = lstAutoTriggers.getListItemRenderer().getItemHeight() * lstAutoTriggers.getCount();
        float maxListHeight = maxHeight - 3 * padding - checkBoxSize.height;
        if (listHeight > maxListHeight) {
            listHeight = maxListHeight;
        }

        lstAutoTriggers.setBounds(x, y, w, listHeight);
        y += listHeight + padding;
        chkDisableAll.setBounds(x, y, Math.min(checkBoxSize.width, w), checkBoxSize.height);
        y += checkBoxSize.height + padding;

        return y;
    }
}
