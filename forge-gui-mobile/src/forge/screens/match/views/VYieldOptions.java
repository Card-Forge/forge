package forge.screens.match.views;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDialog;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.TextBounds;
import forge.util.Utils;

public class VYieldOptions extends FDialog {

    private static final String[] STACK_SCOPE_VALUES = { "never", "always", "stack", "turn" };
    private static final String[] NO_ACTIONS_SCOPE_VALUES = { "never", "always", "turn" };

    private static final FSkinFont DESC_FONT = FSkinFont.get(10);
    private static final FSkinColor DESC_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.55f);

    private final FScrollPane scroller;

    private final FLabel hdrInterrupts;
    private final FCheckBox chkInterruptAttackers;
    private final FCheckBox chkInterruptTargeting;
    private final FCheckBox chkInterruptMassRemoval;
    private final FCheckBox chkInterruptOpponentSpell;
    private final FCheckBox chkInterruptTriggers;
    private final FCheckBox chkInterruptReveal;
    private final FCheckBox chkAutoPassRespectsInterrupts;

    private final FLabel hdrSuggestions;
    private final FLabel lblStackScope;
    private final FComboBox<String> cboStackScope;
    private final FLabel lblNoActionsScope;
    private final FComboBox<String> cboNoActionsScope;

    private final FCheckBox chkSuppressOwnTurn;
    private final FCheckBox chkSuppressAfterYield;

    private final FLabel hdrSpeed;
    private final FLabel lblBudget;
    private final FLabel descBudget;
    private final FTextField txtBudgetMs;
    private final FCheckBox chkSkipPhaseDelay;
    private final FCheckBox chkSkipResolveDelay;

    public VYieldOptions() {
        super(Forge.getLocalizer().getMessage("lblYieldSettings"), 1);
        final IGameController ctrl = MatchController.instance.getGameController();
        final ForgePreferences prefs = FModel.getPreferences();

        scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                return layoutScrollerContent(visibleWidth);
            }
        });

        hdrInterrupts = scroller.add(headerLabel("lblInterruptSettings"));
        chkInterruptAttackers     = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnAttackers"),     ctrl.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_ATTACKERS)));
        chkInterruptTargeting     = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnTargeting"),     ctrl.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_TARGETING)));
        chkInterruptMassRemoval   = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnMassRemoval"),   ctrl.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)));
        chkInterruptOpponentSpell = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnOpponentSpell"), ctrl.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)));
        chkInterruptTriggers      = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnTriggers"),      ctrl.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_TRIGGERS)));
        chkInterruptReveal        = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnReveal"),        ctrl.getYieldInterruptPref(FPref.YIELD_INTERRUPT_ON_REVEAL)));
        chkAutoPassRespectsInterrupts = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblAutoPassRespectsInterrupts"), ctrl.getYieldInterruptPref(FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS)));

        chkInterruptAttackers.setCommand(e ->     persistInterrupt(ctrl, FPref.YIELD_INTERRUPT_ON_ATTACKERS,     chkInterruptAttackers.isSelected()));
        chkInterruptTargeting.setCommand(e ->     persistInterrupt(ctrl, FPref.YIELD_INTERRUPT_ON_TARGETING,     chkInterruptTargeting.isSelected()));
        chkInterruptMassRemoval.setCommand(e ->   persistInterrupt(ctrl, FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL,  chkInterruptMassRemoval.isSelected()));
        chkInterruptOpponentSpell.setCommand(e -> persistInterrupt(ctrl, FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL, chkInterruptOpponentSpell.isSelected()));
        chkInterruptTriggers.setCommand(e ->      persistInterrupt(ctrl, FPref.YIELD_INTERRUPT_ON_TRIGGERS,      chkInterruptTriggers.isSelected()));
        chkInterruptReveal.setCommand(e ->        persistInterrupt(ctrl, FPref.YIELD_INTERRUPT_ON_REVEAL,        chkInterruptReveal.isSelected()));
        chkAutoPassRespectsInterrupts.setCommand(e -> persistInterrupt(ctrl, FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS, chkAutoPassRespectsInterrupts.isSelected()));

        hdrSuggestions = scroller.add(headerLabel("lblAutomaticSuggestions"));
        lblStackScope = scroller.add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblSuggestStackYield"))
                .align(Align.left)
                .build());
        cboStackScope = scroller.add(new FComboBox<String>());
        cboStackScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeNever"));
        cboStackScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeAlways"));
        cboStackScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeStack"));
        cboStackScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeTurn"));
        cboStackScope.setSelectedIndex(indexOf(STACK_SCOPE_VALUES, prefs.getPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD)));
        cboStackScope.setDropDownChangeHandler(e -> persistScope(prefs, FPref.YIELD_DECLINE_SCOPE_STACK_YIELD, STACK_SCOPE_VALUES, cboStackScope.getSelectedIndex()));

        lblNoActionsScope = scroller.add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblSuggestNoActions"))
                .align(Align.left)
                .build());
        cboNoActionsScope = scroller.add(new FComboBox<String>());
        cboNoActionsScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeNever"));
        cboNoActionsScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeAlways"));
        cboNoActionsScope.addItem(Forge.getLocalizer().getMessage("lblDeclScopeTurn"));
        cboNoActionsScope.setSelectedIndex(indexOf(NO_ACTIONS_SCOPE_VALUES, prefs.getPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS)));
        cboNoActionsScope.setDropDownChangeHandler(e -> persistScope(prefs, FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, NO_ACTIONS_SCOPE_VALUES, cboNoActionsScope.getSelectedIndex()));

        chkSuppressOwnTurn    = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblSuppressOnOwnTurn"),    prefs.getPrefBoolean(FPref.YIELD_SUPPRESS_ON_OWN_TURN)));
        chkSuppressAfterYield = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblSuppressAfterYield"),   prefs.getPrefBoolean(FPref.YIELD_SUPPRESS_AFTER_END)));
        chkSuppressOwnTurn.setCommand(e -> persistBool(prefs, FPref.YIELD_SUPPRESS_ON_OWN_TURN,    chkSuppressOwnTurn.isSelected()));
        chkSuppressAfterYield.setCommand(e -> persistBool(prefs, FPref.YIELD_SUPPRESS_AFTER_END,   chkSuppressAfterYield.isSelected()));

        hdrSpeed = scroller.add(headerLabel("lblSpeedSettings"));
        lblBudget = scroller.add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblAutoPassBudgetLabel"))
                .align(Align.left)
                .build());
        descBudget = scroller.add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblAutoPassBudgetDesc"))
                .font(DESC_FONT)
                .textColor(DESC_COLOR)
                .align(Align.left)
                .build());
        int currentBudget = prefs.getPrefInt(FPref.YIELD_AVAILABLE_ACTIONS_BUDGET_MS);
        txtBudgetMs = scroller.add(new FTextField(currentBudget > 0 ? String.valueOf(currentBudget) : ""));
        txtBudgetMs.setGhostText(Forge.getLocalizer().getMessage("lblDynamic"));
        txtBudgetMs.setIsNumeric(true);

        chkSkipPhaseDelay   = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblSkipPhaseDelay"),   prefs.getPrefBoolean(FPref.YIELD_SKIP_PHASE_DELAY)));
        chkSkipResolveDelay = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblSkipResolveDelay"), prefs.getPrefBoolean(FPref.YIELD_SKIP_RESOLVE_DELAY)));
        chkSkipPhaseDelay.setCommand(e -> persistBool(prefs, FPref.YIELD_SKIP_PHASE_DELAY,   chkSkipPhaseDelay.isSelected()));
        chkSkipResolveDelay.setCommand(e -> persistBool(prefs, FPref.YIELD_SKIP_RESOLVE_DELAY, chkSkipResolveDelay.isSelected()));

        initButton(0, Forge.getLocalizer().getMessage("lblOK"), e -> {
            String txt = txtBudgetMs.getText().trim();
            if (txt.isEmpty()) {
                txt = "0";
            }
            try {
                int v = Integer.parseInt(txt);
                if (v < 0) {
                    v = 0;
                }
                prefs.setPref(FPref.YIELD_AVAILABLE_ACTIONS_BUDGET_MS, String.valueOf(v));
                prefs.save();
            } catch (NumberFormatException nfe) {
                FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblInvalidBudget"));
                return;
            }
            hide();
        });
    }

    private static FLabel headerLabel(String localizerKey) {
        // ButtonBuilder gives the gradient bar background used by SettingsScreen group headers.
        return new FLabel.ButtonBuilder()
                .text(Forge.getLocalizer().getMessage(localizerKey))
                .font(FSkinFont.get(14))
                .align(Align.center)
                .build();
    }

    private FScrollPane.ScrollBounds layoutScrollerContent(float visibleWidth) {
        float padding = FOptionPane.PADDING;
        float rowGap = Utils.scale(6);
        float sectionGap = Utils.scale(14);

        float x = padding;
        float w = visibleWidth - 2 * padding;
        float y = padding;

        TextBounds rowBounds = chkInterruptAttackers.getAutoSizeBounds();
        float rowH = Math.max(rowBounds.height, Utils.scale(28));
        // Match SettingsScreen.GROUP_HEADER_HEIGHT ratio so the gradient bar reads as a section.
        float headerH = Math.round(Utils.AVG_FINGER_HEIGHT * 0.6f);

        hdrInterrupts.setBounds(x, y, w, headerH);
        y += headerH;
        FCheckBox[] interrupts = {
                chkInterruptAttackers, chkInterruptTargeting, chkInterruptMassRemoval,
                chkInterruptOpponentSpell, chkInterruptTriggers, chkInterruptReveal,
                chkAutoPassRespectsInterrupts
        };
        for (FCheckBox cb : interrupts) {
            cb.setBounds(x, y, w, rowH);
            y += rowH + rowGap;
        }
        y += sectionGap - rowGap;

        hdrSuggestions.setBounds(x, y, w, headerH);
        y += headerH;

        float dropdownW = Math.min(visibleWidth * 0.45f, Utils.scale(160));
        float scopeLabelW = w - dropdownW - padding;
        float scopeRowH = Math.max(rowH, FTextField.getDefaultHeight());

        lblStackScope.setBounds(x, y, scopeLabelW, scopeRowH);
        cboStackScope.setBounds(x + w - dropdownW, y, dropdownW, scopeRowH);
        y += scopeRowH + rowGap;

        lblNoActionsScope.setBounds(x, y, scopeLabelW, scopeRowH);
        cboNoActionsScope.setBounds(x + w - dropdownW, y, dropdownW, scopeRowH);
        y += scopeRowH + rowGap;

        chkSuppressOwnTurn.setBounds(x, y, w, rowH);
        y += rowH + rowGap;
        chkSuppressAfterYield.setBounds(x, y, w, rowH);
        y += rowH + sectionGap;

        hdrSpeed.setBounds(x, y, w, headerH);
        y += headerH;

        float fieldH = FTextField.getDefaultHeight();
        float fieldW = Math.min(visibleWidth * 0.45f, Utils.scale(160));
        float lblW = w - fieldW - padding;
        lblBudget.setBounds(x, y, lblW, fieldH);
        txtBudgetMs.setBounds(x + w - fieldW, y, fieldW, fieldH);
        y += fieldH + Utils.scale(2);
        descBudget.setBounds(x, y, w, DESC_FONT.getCapHeight() * 2.2f);
        y += DESC_FONT.getCapHeight() * 2.2f + rowGap;

        chkSkipPhaseDelay.setBounds(x, y, w, rowH);
        y += rowH + rowGap;
        chkSkipResolveDelay.setBounds(x, y, w, rowH);
        y += rowH + padding;

        return new FScrollPane.ScrollBounds(visibleWidth, y);
    }

    private static int indexOf(String[] options, String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private static void persistInterrupt(IGameController ctrl, FPref pref, boolean value) {
        FModel.getPreferences().setPref(pref, value);
        FModel.getPreferences().save();
        ctrl.setYieldInterruptPref(pref, value);
    }

    private static void persistBool(ForgePreferences prefs, FPref pref, boolean value) {
        prefs.setPref(pref, value);
        prefs.save();
    }

    private static void persistScope(ForgePreferences prefs, FPref pref, String[] values, int index) {
        if (index < 0 || index >= values.length) {
            return;
        }
        prefs.setPref(pref, values[index]);
        prefs.save();
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        scroller.setBounds(0, 0, width, maxHeight);
        scroller.scrollToTop();
        return maxHeight;
    }
}
