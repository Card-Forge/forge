package forge.screens.match.views;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import forge.Forge;
import forge.player.AutoYieldStore;
import forge.screens.match.MatchController;
import forge.toolbox.FCheckBox;
import forge.toolbox.FChoiceList;
import forge.toolbox.FDialog;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.util.TextBounds;

public class VAutoYieldsAndTriggers extends FDialog {
    private static final String YIELD_PREFIX = "[" + Forge.getLocalizer().getMessage("lblYield") + "] ";
    private static final String ACCEPT_PREFIX = "[" + Forge.getLocalizer().getMessage("lblAlwaysYes") + "] ";
    private static final String DECLINE_PREFIX = "[" + Forge.getLocalizer().getMessage("lblAlwaysNo") + "] ";

    /** Sort by the post-tag substring so same-card entries sit adjacent regardless of tag. */
    private static final Comparator<String> ENTRY_COMPARATOR = (a, b) -> {
        Collator c = Collator.getInstance();
        int byCard = c.compare(stripTag(a), stripTag(b));
        return byCard != 0 ? byCard : c.compare(a, b);
    };

    private final FTextField filterField;
    private final FChoiceList<String> lstEntries;
    private final FCheckBox chkDisableYields;
    private final FCheckBox chkDisableTriggers;
    /** Master list, sorted, mutated by Remove. */
    private final List<String> allEntries;

    public VAutoYieldsAndTriggers() {
        super(Forge.getLocalizer().getMessage("lblAutoYieldsAndTriggers"), 2);

        allEntries = new ArrayList<>();
        for (String key : MatchController.instance.getGameController().getYieldController().getAutoYields()) {
            allEntries.add(YIELD_PREFIX + key);
        }
        for (Map.Entry<String, AutoYieldStore.TriggerDecision> e : MatchController.instance.getGameController().getYieldController().getAutoTriggers()) {
            String prefix = e.getValue() == AutoYieldStore.TriggerDecision.ACCEPT ? ACCEPT_PREFIX : DECLINE_PREFIX;
            allEntries.add(prefix + e.getKey());
        }
        Collections.sort(allEntries, ENTRY_COMPARATOR);

        filterField = add(new FTextField());
        filterField.setGhostText(Forge.getLocalizer().getMessage("lblSearch"));
        filterField.setChangedHandler(e -> applyFilter());

        lstEntries = add(new FChoiceList<String>(allEntries) {
            @Override
            protected void onCompactModeChange() {
                VAutoYieldsAndTriggers.this.revalidate();
            }

            @Override
            protected boolean allowDefaultItemWrap() {
                return true;
            }
        });
        chkDisableYields = add(new FCheckBox(Forge.getLocalizer().getMessage("lblDisableAllAutoYields"),
                MatchController.instance.getGameController().getDisableAutoYields()));
        chkDisableYields.setCommand(e -> MatchController.instance.getGameController().setDisableAutoYields(chkDisableYields.isSelected()));
        chkDisableTriggers = add(new FCheckBox(Forge.getLocalizer().getMessage("lblDisableAllAutoTriggers"),
                MatchController.instance.getGameController().getDisableAutoTriggers()));
        chkDisableTriggers.setCommand(e -> MatchController.instance.getGameController().setDisableAutoTriggers(chkDisableTriggers.isSelected()));

        initButton(0, Forge.getLocalizer().getMessage("lblOK"), e -> hide());
        initButton(1, Forge.getLocalizer().getMessage("lblRemove"), e -> {
            String selected = lstEntries.getSelectedItem();
            if (selected == null) return;
            allEntries.remove(selected);
            lstEntries.removeItem(selected);
            boolean abilityScope = MatchController.instance.getGameController().getYieldController().isAbilityScope();
            if (selected.startsWith(YIELD_PREFIX)) {
                String key = selected.substring(YIELD_PREFIX.length());
                MatchController.instance.getGameController().setShouldAutoYield(key, false, abilityScope);
            } else if (selected.startsWith(ACCEPT_PREFIX)) {
                String key = selected.substring(ACCEPT_PREFIX.length());
                MatchController.instance.getGameController().setTriggerDecision(key, AutoYieldStore.TriggerDecision.ASK, abilityScope);
            } else if (selected.startsWith(DECLINE_PREFIX)) {
                String key = selected.substring(DECLINE_PREFIX.length());
                MatchController.instance.getGameController().setTriggerDecision(key, AutoYieldStore.TriggerDecision.ASK, abilityScope);
            }
            setButtonEnabled(1, !allEntries.isEmpty());
            lstEntries.cleanUpSelections();
            VAutoYieldsAndTriggers.this.revalidate();
        });
        setButtonEnabled(1, !allEntries.isEmpty());
    }

    private void applyFilter() {
        String needle = filterField.getText();
        if (needle == null || needle.isEmpty()) {
            lstEntries.setListData(allEntries);
            return;
        }
        String lower = needle.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String e : allEntries) {
            if (e.toLowerCase().contains(lower)) filtered.add(e);
        }
        lstEntries.setListData(filtered);
    }

    private static String stripTag(String s) {
        if (s.startsWith(YIELD_PREFIX)) return s.substring(YIELD_PREFIX.length());
        if (s.startsWith(ACCEPT_PREFIX)) return s.substring(ACCEPT_PREFIX.length());
        if (s.startsWith(DECLINE_PREFIX)) return s.substring(DECLINE_PREFIX.length());
        return s;
    }

    @Override
    public void show() {
        if (!allEntries.isEmpty()) {
            super.show();
        }
        else {
            FOptionPane.showMessageDialog(
                    Forge.getLocalizer().getMessage("lblNoActiveAutoYieldOrTrigger"),
                    Forge.getLocalizer().getMessage("lblNoAutoYieldOrTrigger"),
                    FOptionPane.INFORMATION_ICON);
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        float filterHeight = FTextField.getDefaultHeight();
        TextBounds yieldChkSize = chkDisableYields.getAutoSizeBounds();
        TextBounds trigChkSize = chkDisableTriggers.getAutoSizeBounds();
        float chkHeight = Math.max(yieldChkSize.height, trigChkSize.height);

        float listHeight = lstEntries.getListItemRenderer().getItemHeight() * lstEntries.getCount();
        float maxListHeight = maxHeight - 5 * padding - 2 * chkHeight - filterHeight;
        if (listHeight > maxListHeight) {
            listHeight = maxListHeight;
        }

        filterField.setBounds(x, y, w, filterHeight);
        y += filterHeight + padding;
        lstEntries.setBounds(x, y, w, listHeight);
        y += listHeight + padding;
        chkDisableYields.setBounds(x, y, Math.min(yieldChkSize.width, w), chkHeight);
        y += chkHeight + padding;
        chkDisableTriggers.setBounds(x, y, Math.min(trigChkSize.width, w), chkHeight);
        y += chkHeight + padding;

        return y;
    }
}
