package forge.screens.match;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;

import forge.Singletons;
import forge.gui.UiCommand;
import forge.player.AutoYieldStore;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class VAutoTriggers extends FDialog {
    private static final int PADDING = 10;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 26;

    private static final String ACCEPT_PREFIX = "[" + Localizer.getInstance().getMessage("lblAlwaysYes") + "] ";
    private static final String DECLINE_PREFIX = "[" + Localizer.getInstance().getMessage("lblAlwaysNo") + "] ";

    private final FButton btnOk;
    private final FButton btnRemove;
    private final FList<String> lstAutoTriggers;
    private final FScrollPane listScroller;
    private final FCheckBox chkDisableAll;
    private final List<String> autoTriggers;

    public VAutoTriggers(final CMatchUI matchUI) {
        super();
        setTitle(Localizer.getInstance().getMessage("lblAutoTriggers"));

        autoTriggers = new ArrayList<>();
        for (final Map.Entry<String, AutoYieldStore.TriggerDecision> entry : matchUI.getGameController().getYieldController().getAutoTriggers()) {
            autoTriggers.add(formatEntry(entry));
        }
        lstAutoTriggers = new FList<>(new AutoTriggersListModel());

        int x = PADDING;
        int y = PADDING;
        int width = Singletons.getView().getFrame().getWidth() * 2 / 3;
        int w = width - 2 * PADDING;

        listScroller = new FScrollPane(lstAutoTriggers, true);

        chkDisableAll = new FCheckBox(Localizer.getInstance().getMessage("lblDisableAllAutoTriggers"), matchUI.getGameController().getDisableAutoTriggers());
        chkDisableAll.addChangeListener(e -> matchUI.getGameController().setDisableAutoTriggers(chkDisableAll.isSelected()));

        btnOk = new FButton(Localizer.getInstance().getMessage("lblOK"));
        btnOk.setCommand((UiCommand) () -> setVisible(false));
        btnRemove = new FButton(Localizer.getInstance().getMessage("lblRemoveTrigger"));
        btnRemove.setCommand((UiCommand) () -> {
            String selected = lstAutoTriggers.getSelectedValue();
            if (selected != null) {
                autoTriggers.remove(selected);
                btnRemove.setEnabled(autoTriggers.size() > 0);
                String key = stripPrefix(selected);
                boolean abilityScope = !forge.localinstance.properties.ForgeConstants.AUTO_TRIGGER_PER_CARD.equals(
                        forge.model.FModel.getPreferences().getPref(forge.localinstance.properties.ForgePreferences.FPref.UI_AUTO_TRIGGER_MODE));
                matchUI.getGameController().setShouldAlwaysAskTrigger(key, abilityScope);
                VAutoTriggers.this.revalidate();
                lstAutoTriggers.repaint();
            }
        });
        if (autoTriggers.size() > 0) {
            lstAutoTriggers.setSelectedIndex(0);
        }
        else {
            btnRemove.setEnabled(false);
        }

        Dimension checkBoxSize = chkDisableAll.getPreferredSize();
        int listHeight = lstAutoTriggers.getMinimumSize().height + 2 * PADDING;

        add(listScroller, x, y, w, listHeight);
        y += listHeight + PADDING;
        add(chkDisableAll, x, y, checkBoxSize.width, checkBoxSize.height);
        x = w - 2 * BUTTON_WIDTH - PADDING;
        add(btnOk, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        x += BUTTON_WIDTH + PADDING;
        add(btnRemove, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

        this.pack();
        this.setSize(width, getHeight());
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

    private class AutoTriggersListModel extends AbstractListModel<String> {
        @Override
        public int getSize() {
            return autoTriggers.size();
        }

        @Override
        public String getElementAt(final int index) {
            return autoTriggers.get(index);
        }
    }

    public void showAutoTriggers() {
        if (lstAutoTriggers.getCount() > 0) {
            setVisible(true);
            dispose();
        } else {
            FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblNoActiveAutoTrigger"), Localizer.getInstance().getMessage("lblNoAutoTrigger"), FOptionPane.INFORMATION_ICON);
        }
    }
}
