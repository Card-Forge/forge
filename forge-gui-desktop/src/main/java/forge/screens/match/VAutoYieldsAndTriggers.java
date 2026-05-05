package forge.screens.match;

import java.awt.Dimension;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.Singletons;
import forge.gui.UiCommand;
import forge.player.AutoYieldStore;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Localizer;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class VAutoYieldsAndTriggers extends FDialog {
    private static final int PADDING = 10;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 26;

    private static final String YIELD_PREFIX = "[" + Localizer.getInstance().getMessage("lblYield") + "] ";
    private static final String ACCEPT_PREFIX = "[" + Localizer.getInstance().getMessage("lblAlwaysYes") + "] ";
    private static final String DECLINE_PREFIX = "[" + Localizer.getInstance().getMessage("lblAlwaysNo") + "] ";

    /** Sort by the post-tag substring so same-card entries sit adjacent regardless of tag. */
    private static final Comparator<String> ENTRY_COMPARATOR = (a, b) -> {
        Collator c = Collator.getInstance();
        int byCard = c.compare(stripTag(a), stripTag(b));
        return byCard != 0 ? byCard : c.compare(a, b);
    };

    private final FButton btnOk;
    private final FButton btnRemove;
    private final FTextField filterField;
    private final FList<String> lstEntries;
    private final FScrollPane listScroller;
    private final FCheckBox chkDisableYields;
    private final FCheckBox chkDisableTriggers;
    /** Master list, sorted, mutated by Remove. */
    private final List<String> allEntries;
    /** Currently displayed subset (filtered view of allEntries). */
    private final List<String> visibleEntries;
    private final EntriesListModel listModel;

    public VAutoYieldsAndTriggers(final CMatchUI matchUI) {
        super();
        setTitle(Localizer.getInstance().getMessage("lblAutoYieldsAndTriggers"));

        allEntries = new ArrayList<>();
        for (final String key : matchUI.getGameController().getYieldController().getAutoYields()) {
            allEntries.add(YIELD_PREFIX + key);
        }
        for (final Map.Entry<String, AutoYieldStore.TriggerDecision> e : matchUI.getGameController().getYieldController().getAutoTriggers()) {
            String prefix = e.getValue() == AutoYieldStore.TriggerDecision.ACCEPT ? ACCEPT_PREFIX : DECLINE_PREFIX;
            allEntries.add(prefix + e.getKey());
        }
        Collections.sort(allEntries, ENTRY_COMPARATOR);
        visibleEntries = new ArrayList<>(allEntries);

        listModel = new EntriesListModel();
        lstEntries = new FList<>(listModel);

        filterField = new FTextField.Builder()
                .ghostText(Localizer.getInstance().getMessage("lblSearch"))
                .showGhostTextWithFocus()
                .build();
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        int x = PADDING;
        int y = PADDING;
        int width = Singletons.getView().getFrame().getWidth() * 2 / 3;
        int w = width - 2 * PADDING;

        listScroller = new FScrollPane(lstEntries, true);

        chkDisableYields = new FCheckBox(Localizer.getInstance().getMessage("lblDisableAllAutoYields"),
                matchUI.getGameController().getDisableAutoYields());
        chkDisableYields.addChangeListener(e -> matchUI.getGameController().setDisableAutoYields(chkDisableYields.isSelected()));

        chkDisableTriggers = new FCheckBox(Localizer.getInstance().getMessage("lblDisableAllAutoTriggers"),
                matchUI.getGameController().getDisableAutoTriggers());
        chkDisableTriggers.addChangeListener(e -> matchUI.getGameController().setDisableAutoTriggers(chkDisableTriggers.isSelected()));

        btnOk = new FButton(Localizer.getInstance().getMessage("lblOK"));
        btnOk.setCommand((UiCommand) () -> setVisible(false));
        btnRemove = new FButton(Localizer.getInstance().getMessage("lblRemove"));
        btnRemove.setCommand((UiCommand) () -> {
            String selected = lstEntries.getSelectedValue();
            if (selected == null) return;
            allEntries.remove(selected);
            visibleEntries.remove(selected);
            listModel.refresh();
            btnRemove.setEnabled(!allEntries.isEmpty());
            boolean abilityScope = matchUI.getGameController().getYieldController().isAbilityScope();
            if (selected.startsWith(YIELD_PREFIX)) {
                String key = selected.substring(YIELD_PREFIX.length());
                matchUI.getGameController().setShouldAutoYield(key, false, abilityScope);
            } else if (selected.startsWith(ACCEPT_PREFIX)) {
                String key = selected.substring(ACCEPT_PREFIX.length());
                matchUI.getGameController().setTriggerDecision(key, AutoYieldStore.TriggerDecision.ASK, abilityScope);
            } else if (selected.startsWith(DECLINE_PREFIX)) {
                String key = selected.substring(DECLINE_PREFIX.length());
                matchUI.getGameController().setTriggerDecision(key, AutoYieldStore.TriggerDecision.ASK, abilityScope);
            }
            VAutoYieldsAndTriggers.this.revalidate();
            lstEntries.repaint();
        });
        if (!allEntries.isEmpty()) {
            lstEntries.setSelectedIndex(0);
        }
        else {
            btnRemove.setEnabled(false);
        }

        Dimension yieldChkSize = chkDisableYields.getPreferredSize();
        Dimension trigChkSize = chkDisableTriggers.getPreferredSize();
        int filterHeight = FTextField.HEIGHT;
        int listHeight = lstEntries.getMinimumSize().height + 2 * PADDING;
        int chkHeight = Math.max(yieldChkSize.height, trigChkSize.height);

        add(filterField, x, y, w, filterHeight);
        y += filterHeight + PADDING;
        add(listScroller, x, y, w, listHeight);
        y += listHeight + PADDING;
        add(chkDisableYields, x, y, yieldChkSize.width, chkHeight);
        add(chkDisableTriggers, x + yieldChkSize.width + PADDING, y, trigChkSize.width, chkHeight);
        y += chkHeight + PADDING;
        x = w - 2 * BUTTON_WIDTH - PADDING;
        add(btnOk, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        x += BUTTON_WIDTH + PADDING;
        add(btnRemove, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

        this.pack();
        this.setSize(width, getHeight());
    }

    private void applyFilter() {
        String needle = filterField.getText();
        visibleEntries.clear();
        if (needle == null || needle.isEmpty()) {
            visibleEntries.addAll(allEntries);
        } else {
            String lower = needle.toLowerCase();
            for (String e : allEntries) {
                if (e.toLowerCase().contains(lower)) visibleEntries.add(e);
            }
        }
        listModel.refresh();
    }

    private static String stripTag(String s) {
        if (s.startsWith(YIELD_PREFIX)) return s.substring(YIELD_PREFIX.length());
        if (s.startsWith(ACCEPT_PREFIX)) return s.substring(ACCEPT_PREFIX.length());
        if (s.startsWith(DECLINE_PREFIX)) return s.substring(DECLINE_PREFIX.length());
        return s;
    }

    private class EntriesListModel extends AbstractListModel<String> {
        @Override
        public int getSize() {
            return visibleEntries.size();
        }

        @Override
        public String getElementAt(final int index) {
            return visibleEntries.get(index);
        }

        void refresh() {
            fireContentsChanged(this, 0, Math.max(0, visibleEntries.size() - 1));
        }
    }

    public void showDialog() {
        if (!allEntries.isEmpty()) {
            setVisible(true);
            dispose();
        } else {
            FOptionPane.showMessageDialog(
                    Localizer.getInstance().getMessage("lblNoActiveAutoYieldOrTrigger"),
                    Localizer.getInstance().getMessage("lblNoAutoYieldOrTrigger"),
                    FOptionPane.INFORMATION_ICON);
        }
    }
}
