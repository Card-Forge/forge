package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.gamemodes.rogue.TextHelper;
import forge.gamemodes.rogue.effect.SanctumContext;
import forge.gamemodes.rogue.flow.RogueDecisions.SanctumAction;
import forge.gamemodes.rogue.flow.RogueDecisions.SanctumAnswer;
import forge.gamemodes.rogue.flow.RogueDecisions.SanctumOption;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.screens.home.rogue.RogueUIHelper.PreviewTarget;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog for Sanctum node interaction. Allows player to rest or craft a carry item.
 */
public class SanctumDialog {

  private static final int DIALOG_WIDTH = 600;
  private static final int MIN_DIALOG_HEIGHT = 390;
  private static final int PANEL_INSETS = 20;
  private static final int CHOICE_RESULT = 1;

  private final MainPanel panel;
  private final List<PreviewTarget> previewTargets = new ArrayList<>();
  private FOptionPane optionPane;
  private RoguePreviewPopup previewPopup;
  private SanctumAction choice = SanctumAction.SKIP;
  private SanctumContext.SanctumChoice customChoice;

  /**
   * Create a Sanctum dialog.
   *
   * @param options The choices, in the order they are shown
   */
  public SanctumDialog(List<SanctumOption> options) {
    panel = new MainPanel();

    FLabel lblTitle = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueSanctum"))
        .fontSize(20)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.CENTER)
        .build();

    FLabel lblDescription = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueChooseAction"))
        .fontSize(14)
        .fontAlign(SwingConstants.CENTER)
        .build();
    int choiceButtonWidth = (DIALOG_WIDTH - 2 * PANEL_INSETS) * 4 / 5;

    int desiredHeight = PANEL_INSETS;

    panel.add(lblTitle, "w 100%!, h 60px!, ax center, gap 0 0 20px 10px, wrap");
    desiredHeight += 60 + 20 + 10;
    panel.add(lblDescription, "w 100%!, h 30px!, ax center, gap 0 0 10px 20px, wrap");
    desiredHeight += 30 + 10 + 20;
    for (SanctumOption option : options) {
      FButton btnOption = RogueButtonHelper.createChoiceButton(
          option.label(), TextHelper.stripPreviewMarkers(option.description()),
          TextHelper.extractPreviewReferences(option.description()));
      RogueButtonHelper.setChoiceButtonSizeHint(btnOption, choiceButtonWidth);
      btnOption.addActionListener(e -> {
        hidePreview();
        choice = option.action();
        customChoice = option.custom();
        optionPane.setResult(CHOICE_RESULT);
        optionPane.setVisible(false);
      });
      btnOption.setEnabled(option.enabled());
      if (!option.enabled() && option.disabledReason() != null && !option.disabledReason().isEmpty()) {
        btnOption.setToolTipText(option.disabledReason());
      }
      previewTargets.add(new PreviewTarget(btnOption, TextHelper.extractPreviewReferences(option.description())));
      panel.add(btnOption, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
      desiredHeight += btnOption.getPreferredSize().height + 10 + 10;
    }

    int dialogHeight = Math.min(Math.max(desiredHeight + PANEL_INSETS, MIN_DIALOG_HEIGHT),
        RogueUIHelper.getMaxChoiceDialogHeight());
    Dimension dialogSize = new Dimension(DIALOG_WIDTH, dialogHeight);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
  }

  /**
   * Show the dialog and return the player's choice.
   *
   * @return The selected action, with the choice if it is CUSTOM
   */
  public SanctumAnswer show() {
    SoundSystem.instance.play(SoundEffectType.Enchantment,
        SoundEffectType.Enchantment.isSynced());
    optionPane = new FOptionPane(
        null,
        Localizer.getInstance().getMessage("lblRogueSanctum"),
        null,
        panel,
        List.of(Localizer.getInstance().getMessage("lblSkip")),
        0
    );
    previewPopup = RogueUIHelper.prepareChoiceDialogWithPreviews(optionPane, panel, previewTargets);

    optionPane.setVisible(true);
    hidePreview();
    optionPane.dispose();

    if (optionPane.getResult() == 0) {
      choice = SanctumAction.SKIP;
    }
    return new SanctumAnswer(choice, customChoice);
  }

  private void hidePreview() {
    if (previewPopup != null) {
      previewPopup.hide();
    }
  }

  private static class MainPanel extends SkinnedPanel {

    private MainPanel() {
      super(new MigLayout("insets 20, gap 0, wrap, align center", "[grow, center]", ""));
      setOpaque(false);
    }
  }
}
