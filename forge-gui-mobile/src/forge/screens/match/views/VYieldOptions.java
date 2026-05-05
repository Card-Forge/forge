package forge.screens.match.views;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.gamemodes.match.DeclineScope;
import forge.gamemodes.match.SuggestionType;
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


    private static final FSkinFont DESC_FONT = FSkinFont.get(12);
    private static final FSkinFont BODY_FONT = FSkinFont.get(11);
    private static final FSkinColor DESC_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.55f);

    private final FScrollPane scroller;

    private final FLabel hdrInterrupts;
    private final FLabel descInterrupts;
    private final FCheckBox chkInterruptAttackers;
    private final FCheckBox chkInterruptTargeting;
    private final FCheckBox chkInterruptMassRemoval;
    private final FCheckBox chkInterruptOpponentSpell;
    private final FCheckBox chkInterruptTriggers;
    private final FCheckBox chkInterruptReveal;
    private final FCheckBox chkAutoPassRespectsInterrupts;

    private final FLabel hdrSuggestions;
    private final FLabel descSuggestions;
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
        descInterrupts = scroller.add(descriptionLabel("lblInterruptSettingsDesc"));
        chkInterruptAttackers     = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnAttackers"),     ctrl.getYieldController().getBoolPref(FPref.YIELD_INTERRUPT_ON_ATTACKERS)));
        chkInterruptTargeting     = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnTargeting"),     ctrl.getYieldController().getBoolPref(FPref.YIELD_INTERRUPT_ON_TARGETING)));
        chkInterruptMassRemoval   = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnMassRemoval"),   ctrl.getYieldController().getBoolPref(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)));
        chkInterruptOpponentSpell = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnOpponentSpell"), ctrl.getYieldController().getBoolPref(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)));
        chkInterruptTriggers      = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnTriggers"),      ctrl.getYieldController().getBoolPref(FPref.YIELD_INTERRUPT_ON_TRIGGERS)));
        chkInterruptReveal        = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblInterruptOnReveal"),        ctrl.getYieldController().getBoolPref(FPref.YIELD_INTERRUPT_ON_REVEAL)));
        chkAutoPassRespectsInterrupts = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblAutoPassRespectsInterrupts"), ctrl.getYieldController().getBoolPref(FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS)));

        chkInterruptAttackers.setCommand(e ->     persistBool(ctrl, FPref.YIELD_INTERRUPT_ON_ATTACKERS,     chkInterruptAttackers.isSelected()));
        chkInterruptTargeting.setCommand(e ->     persistBool(ctrl, FPref.YIELD_INTERRUPT_ON_TARGETING,     chkInterruptTargeting.isSelected()));
        chkInterruptMassRemoval.setCommand(e ->   persistBool(ctrl, FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL,  chkInterruptMassRemoval.isSelected()));
        chkInterruptOpponentSpell.setCommand(e -> persistBool(ctrl, FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL, chkInterruptOpponentSpell.isSelected()));
        chkInterruptTriggers.setCommand(e ->      persistBool(ctrl, FPref.YIELD_INTERRUPT_ON_TRIGGERS,      chkInterruptTriggers.isSelected()));
        chkInterruptReveal.setCommand(e ->        persistBool(ctrl, FPref.YIELD_INTERRUPT_ON_REVEAL,        chkInterruptReveal.isSelected()));
        chkAutoPassRespectsInterrupts.setCommand(e -> persistBool(ctrl, FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS, chkAutoPassRespectsInterrupts.isSelected()));

        hdrSuggestions = scroller.add(headerLabel("lblAutomaticSuggestions"));
        descSuggestions = scroller.add(descriptionLabel("lblAutomaticSuggestionsDesc"));
        lblStackScope = scroller.add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblSuggestStackYield"))
                .align(Align.left)
                .build());
        cboStackScope = scroller.add(new FComboBox<String>());
        DeclineScope[] stackScopes = SuggestionType.STACK_YIELD.allowedScopes().toArray(new DeclineScope[0]);
        for (DeclineScope s : stackScopes) cboStackScope.addItem(Forge.getLocalizer().getMessage(s.labelKey()));
        cboStackScope.setSelectedIndex(scopeIndex(stackScopes, prefs.getPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD)));
        cboStackScope.setDropDownChangeHandler(e -> persistScope(ctrl, FPref.YIELD_DECLINE_SCOPE_STACK_YIELD, stackScopes, cboStackScope.getSelectedIndex()));

        lblNoActionsScope = scroller.add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblSuggestNoActions"))
                .align(Align.left)
                .build());
        cboNoActionsScope = scroller.add(new FComboBox<String>());
        DeclineScope[] noActionsScopes = SuggestionType.NO_ACTIONS.allowedScopes().toArray(new DeclineScope[0]);
        for (DeclineScope s : noActionsScopes) cboNoActionsScope.addItem(Forge.getLocalizer().getMessage(s.labelKey()));
        cboNoActionsScope.setSelectedIndex(scopeIndex(noActionsScopes, prefs.getPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS)));
        cboNoActionsScope.setDropDownChangeHandler(e -> persistScope(ctrl, FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, noActionsScopes, cboNoActionsScope.getSelectedIndex()));

        chkSuppressOwnTurn    = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblSuppressOnOwnTurn"),    prefs.getPrefBoolean(FPref.YIELD_SUPPRESS_ON_OWN_TURN)));
        chkSuppressAfterYield = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblSuppressAfterYield"),   prefs.getPrefBoolean(FPref.YIELD_SUPPRESS_AFTER_END)));
        chkSuppressOwnTurn.setCommand(e -> persistBool(ctrl, FPref.YIELD_SUPPRESS_ON_OWN_TURN,    chkSuppressOwnTurn.isSelected()));
        chkSuppressAfterYield.setCommand(e -> persistBool(ctrl, FPref.YIELD_SUPPRESS_AFTER_END,   chkSuppressAfterYield.isSelected()));

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
        chkSkipPhaseDelay.setCommand(e -> persistBool(ctrl, FPref.YIELD_SKIP_PHASE_DELAY,   chkSkipPhaseDelay.isSelected()));
        chkSkipResolveDelay.setCommand(e -> persistBool(ctrl, FPref.YIELD_SKIP_RESOLVE_DELAY, chkSkipResolveDelay.isSelected()));

        FLabel[] bodyLabels = {
                chkInterruptAttackers, chkInterruptTargeting, chkInterruptMassRemoval,
                chkInterruptOpponentSpell, chkInterruptTriggers, chkInterruptReveal,
                chkAutoPassRespectsInterrupts,
                lblStackScope, lblNoActionsScope,
                chkSuppressOwnTurn, chkSuppressAfterYield,
                lblBudget,
                chkSkipPhaseDelay, chkSkipResolveDelay
        };
        for (FLabel l : bodyLabels) l.setFont(BODY_FONT);
        cboStackScope.setFont(BODY_FONT);
        cboNoActionsScope.setFont(BODY_FONT);
        txtBudgetMs.setFont(BODY_FONT);

        initButton(0, Forge.getLocalizer().getMessage("lblOK"), e -> {
            String txt = txtBudgetMs.getText().trim();
            if (txt.isEmpty()) {
                txt = "0";
            }
            try {
                int v = Integer.parseInt(txt);
                if (v < 0) v = 0;
                persistString(ctrl, FPref.YIELD_AVAILABLE_ACTIONS_BUDGET_MS, String.valueOf(v));
            } catch (NumberFormatException nfe) {
                FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblInvalidBudget"));
                return;
            }
            hide();
        });
    }

    private static FLabel headerLabel(String localizerKey) {
        return new FLabel.ButtonBuilder()
                .text(Forge.getLocalizer().getMessage(localizerKey))
                .font(FSkinFont.get(14))
                .align(Align.center)
                .build();
    }

    private static FLabel descriptionLabel(String localizerKey) {
        return new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage(localizerKey))
                .font(DESC_FONT)
                .textColor(DESC_COLOR)
                .align(Align.left)
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
        float headerH = Math.round(Utils.AVG_FINGER_HEIGHT * 0.6f);

        hdrInterrupts.setBounds(x, y, w, headerH);
        y += headerH;
        float descH = DESC_FONT.getCapHeight() * 2.2f;
        descInterrupts.setBounds(x, y, w, descH);
        y += descH + Utils.scale(2);
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
        descSuggestions.setBounds(x, y, w, descH);
        y += descH + Utils.scale(2);

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

    private static int scopeIndex(DeclineScope[] options, String prefValue) {
        DeclineScope current = DeclineScope.fromPref(prefValue);
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) return i;
        }
        return 0;
    }

    private static void persistBool(IGameController ctrl, FPref pref, boolean value) {
        FModel.getPreferences().setPref(pref, value);
        FModel.getPreferences().save();
        if (ctrl != null) ctrl.setYieldBoolPref(pref, value);
    }

    private static void persistScope(IGameController ctrl, FPref pref, DeclineScope[] options, int index) {
        if (index < 0 || index >= options.length) return;
        String value = options[index].name();
        FModel.getPreferences().setPref(pref, value);
        FModel.getPreferences().save();
        if (ctrl != null) ctrl.setYieldStringPref(pref, value);
    }

    private static void persistString(IGameController ctrl, FPref pref, String value) {
        FModel.getPreferences().setPref(pref, value);
        FModel.getPreferences().save();
        if (ctrl != null) ctrl.setYieldStringPref(pref, value);
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        scroller.setBounds(0, 0, width, maxHeight);
        scroller.scrollToTop();
        return maxHeight;
    }
}
