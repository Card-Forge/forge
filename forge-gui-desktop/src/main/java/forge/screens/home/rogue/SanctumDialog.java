package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.TextHelper;
import forge.gamemodes.rogue.effect.SanctumContext;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Dialog for Sanctum node interaction. Allows player to rest or craft a carry item.
 */
public class SanctumDialog {

  private static final int DIALOG_WIDTH = 600;
  private static final int MIN_DIALOG_HEIGHT = 390;
  private static final int PANEL_INSETS = 20;
  private static final int CHOICE_RESULT = 1;

  public enum SanctumChoice {
    HEAL,
    COOK,
    REFLECT,
    CUSTOM,
    SKIP
  }

  private final MainPanel panel;
  private final List<PreviewTarget> previewTargets = new ArrayList<>();
  private FOptionPane optionPane;
  private RoguePreviewPopup previewPopup;
  private SanctumChoice choice = SanctumChoice.SKIP;
  private SanctumContext.SanctumChoice customChoice;

  /**
   * Create a Sanctum dialog.
   *
   * @param restDescription Text shown on the Rest choice
   * @param restEnabled Whether REST should be selectable
   * @param restDisabledReason Tooltip shown when REST is disabled
   */
  public SanctumDialog(String restDescription, boolean restEnabled, String restDisabledReason,
                       SanctumContext sanctumCtx) {
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

    FButton btnRest = RogueButtonHelper.createChoiceButton(
        Localizer.getInstance().getMessage("lblRogueRest"), TextHelper.stripPreviewMarkers(restDescription),
        TextHelper.extractPreviewReferences(restDescription));
    RogueButtonHelper.setChoiceButtonSizeHint(btnRest, choiceButtonWidth);
    btnRest.addActionListener(e -> {
      hidePreview();
      choice = SanctumChoice.HEAL;
      optionPane.setResult(CHOICE_RESULT);
      optionPane.setVisible(false);
    });
    btnRest.setEnabled(restEnabled);
    if (!restEnabled && restDisabledReason != null && !restDisabledReason.isEmpty()) {
      btnRest.setToolTipText(restDisabledReason);
    }
    previewTargets.add(new PreviewTarget(btnRest, TextHelper.extractPreviewReferences(restDescription)));

    String cookDescription = Localizer.getInstance().getMessage("lblRogueCookDescription", "{{Item}}");
    FButton btnCook = RogueButtonHelper.createChoiceButton(
        Localizer.getInstance().getMessage("lblRogueCook"), TextHelper.stripPreviewMarkers(cookDescription),
        TextHelper.extractPreviewReferences(cookDescription));
    RogueButtonHelper.setChoiceButtonSizeHint(btnCook, choiceButtonWidth);
    btnCook.addActionListener(e -> {
      hidePreview();
      choice = SanctumChoice.COOK;
      optionPane.setResult(CHOICE_RESULT);
      optionPane.setVisible(false);
    });
    previewTargets.add(new PreviewTarget(btnCook, TextHelper.extractPreviewReferences(cookDescription)));

    String reflectDescription = Localizer.getInstance().getMessage("lblRogueReflectDescription", "{{Removal Credits}}");
    FButton btnReflect = RogueButtonHelper.createChoiceButton(
        Localizer.getInstance().getMessage("lblRogueReflect"), TextHelper.stripPreviewMarkers(reflectDescription),
        TextHelper.extractPreviewReferences(reflectDescription));
    RogueButtonHelper.setChoiceButtonSizeHint(btnReflect, choiceButtonWidth);
    btnReflect.addActionListener(e -> {
      hidePreview();
      choice = SanctumChoice.REFLECT;
      optionPane.setResult(CHOICE_RESULT);
      optionPane.setVisible(false);
    });
    previewTargets.add(new PreviewTarget(btnReflect, TextHelper.extractPreviewReferences(reflectDescription)));

    int desiredHeight = PANEL_INSETS;

    panel.add(lblTitle, "w 100%!, h 60px!, ax center, gap 0 0 20px 10px, wrap");
    desiredHeight += 60 + 20 + 10;
    panel.add(lblDescription, "w 100%!, h 30px!, ax center, gap 0 0 10px 20px, wrap");
    desiredHeight += 30 + 10 + 20;
    panel.add(btnRest, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
    desiredHeight += btnRest.getPreferredSize().height + 10 + 10;
    panel.add(btnCook, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
    desiredHeight += btnCook.getPreferredSize().height + 10 + 10;
    panel.add(btnReflect, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
    desiredHeight += btnReflect.getPreferredSize().height + 10 + 10;
    for (SanctumContext.SanctumChoice extraChoice : sanctumCtx.extraChoices) {
      FButton btnExtraChoice = RogueButtonHelper.createChoiceButton(
          extraChoice.label(), TextHelper.stripPreviewMarkers(extraChoice.description()),
          TextHelper.extractPreviewReferences(extraChoice.description()));
      RogueButtonHelper.setChoiceButtonSizeHint(btnExtraChoice, choiceButtonWidth);
      btnExtraChoice.addActionListener(e -> {
        hidePreview();
        choice = SanctumChoice.CUSTOM;
        customChoice = extraChoice;
        optionPane.setResult(CHOICE_RESULT);
        optionPane.setVisible(false);
      });
      previewTargets.add(new PreviewTarget(btnExtraChoice,
          TextHelper.extractPreviewReferences(extraChoice.description())));
      panel.add(btnExtraChoice, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
      desiredHeight += btnExtraChoice.getPreferredSize().height + 10 + 10;
    }

    int dialogHeight = Math.min(Math.max(desiredHeight + PANEL_INSETS, MIN_DIALOG_HEIGHT), getMaxDialogHeight());
    Dimension dialogSize = new Dimension(DIALOG_WIDTH, dialogHeight);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
  }

  /**
   * Show the dialog and return the player's choice.
   *
   * @return The selected choice (HEAL, COOK, REFLECT, or SKIP)
   */
  public SanctumChoice show() {
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
    optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    RogueUIHelper.disableSpacebarButtonActivation(optionPane.getContentPane());
    optionPane.getTitleBar().setVisible(false);
    previewPopup = new RoguePreviewPopup();
    previewTargets.forEach(target -> previewPopup.attachTo(target.component(), target.references()));

    panel.revalidate();
    panel.repaint();

    optionPane.setVisible(true);
    hidePreview();
    optionPane.dispose();

    if (optionPane.getResult() == 0) {
      choice = SanctumChoice.SKIP;
    }
    return choice;
  }

  public SanctumContext.SanctumChoice getCustomChoice() {
    return customChoice;
  }

  private void hidePreview() {
    if (previewPopup != null) {
      previewPopup.hide();
    }
  }

  private static int getMaxDialogHeight() {
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;
    return (int) (usableHeight * 0.9) - 80;
  }

  private record PreviewTarget(JComponent component, List<PreviewReference> references) {}

  private static class MainPanel extends SkinnedPanel {

    private MainPanel() {
      super(new MigLayout("insets 20, gap 0, wrap, align center", "[grow, center]", ""));
      setOpaque(false);
    }
  }
}
