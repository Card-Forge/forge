package forge.screens.home.rogue;

import forge.deckchooser.FDeckViewer;
import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueEvent.EventChoice;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.localinstance.skin.FSkinProp;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
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
 * Dialog for Event node interaction. Shows event description and choice buttons.
 */
public class EventDialog {

  private static final int DIALOG_WIDTH = 600;
  private static final int MIN_DIALOG_HEIGHT = 400;
  private static final int PANEL_INSETS = 20;
  private static final int FULL_WIDTH = DIALOG_WIDTH - 2 * PANEL_INSETS;
  private static final int VIEW_DECK_OPTION = 0;
  private static final int VIEW_MAP_OPTION = 1;
  private static final int REROLL_OPTION = 2;
  private static final int CHOICE_RESULT = 3;

  private final MainPanel panel;
  private final RogueRun run;
  private final ChoiceRerollContext rerollCtx;
  private final List<PreviewTarget> previewTargets = new ArrayList<>();
  private final RogueUIHelper.TypewriterText typewriterText;
  private FOptionPane optionPane;
  private RoguePreviewPopup previewPopup;
  private EventChoice selectedChoice;

  public EventDialog(RogueEvent event, RogueRun run, ChoiceRerollContext rerollCtx) {
    this.run = run;
    this.rerollCtx = rerollCtx;
    panel = new MainPanel();

    FLabel lblTitle = new FLabel.Builder()
        .text(event.getDisplayName())
        .fontSize(20).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build();

    FTextArea txtDescription = new FTextArea(event.getDescription());
    txtDescription.setFont(txtDescription.getFont().deriveFont(15f));
    typewriterText = RogueUIHelper.prepareTypewriterText(txtDescription, panel,
        List.of(event.getDescription()), FULL_WIDTH);
    previewTargets.add(new PreviewTarget(txtDescription, event.getPreviewReferences()));
    int choiceButtonWidth = FULL_WIDTH * 4 / 5;

    int desiredHeight = PANEL_INSETS;

    panel.add(lblTitle, "w 100%!, h 60px!, ax center, gap 0 0 20px 10px, wrap");
    desiredHeight += 60 + 20 + 10;
    panel.add(txtDescription, "w 100%!, ax center, gap 0 0 10px 20px, wrap");
    desiredHeight += txtDescription.getPreferredSize().height + 10 + 20;

    for (EventChoice choice : event.getChoices()) {
      FButton btn = RogueButtonHelper.createChoiceButton(choice.label(), choice.effect().getDescription(),
          choice.effect().getPreviewReferences());
      RogueButtonHelper.setChoiceButtonSizeHint(btn, choiceButtonWidth);
      btn.addActionListener(e -> {
        hidePreview();
        selectedChoice = choice;
        optionPane.setResult(CHOICE_RESULT);
        optionPane.setVisible(false);
      });
      boolean enabled = choice.effect().isChoiceAvailable(run);
      btn.setEnabled(enabled);
      if (!enabled) {
        btn.setToolTipText(choice.effect().getUnavailableReason(run));
      }
      previewTargets.add(new PreviewTarget(btn, choice.effect().getPreviewReferences()));
      panel.add(btn, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
      desiredHeight += btn.getPreferredSize().height + 10 + 10;
    }

    int dialogHeight = Math.min(Math.max(desiredHeight + PANEL_INSETS, MIN_DIALOG_HEIGHT), getMaxDialogHeight());
    Dimension dialogSize = new Dimension(DIALOG_WIDTH, dialogHeight);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
  }

  /** Show dialog and return the selected action. */
  public DialogResult show() {
    selectedChoice = null;
    SoundSystem.instance.play(SoundEffectType.Sorcery,
        SoundEffectType.Sorcery.isSynced());
    boolean hasRerolls = rerollCtx.remainingRerolls > 0;
    previewPopup = new RoguePreviewPopup();
    previewTargets.forEach(target -> previewPopup.attachTo(target.component(), target.references()));
    int result;
    do {
      List<String> buttons = new ArrayList<>();
      buttons.add("View Deck");
      buttons.add("View Map");
      if (hasRerolls) {
        buttons.add("Reroll (" + rerollCtx.remainingRerolls + " left)");
      }
      optionPane = new FOptionPane(null, "Event", null, panel,
          buttons, VIEW_DECK_OPTION);
      optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      RogueUIHelper.disableSpacebarButtonActivation(optionPane.getContentPane());
      optionPane.getTitleBar().setVisible(false);
      optionPane.getButton(VIEW_DECK_OPTION).setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
      optionPane.getButton(VIEW_DECK_OPTION).setHorizontalTextPosition(SwingConstants.RIGHT);
      optionPane.getButton(VIEW_MAP_OPTION).setIcon(
          FSkin.getImage(FSkinProp.ICO_QUEST_MAP).resize(24, 24).getIcon());
      optionPane.getButton(VIEW_MAP_OPTION).setHorizontalTextPosition(SwingConstants.RIGHT);
      panel.revalidate();
      panel.repaint();
      typewriterText.start();
      optionPane.setVisible(true);
      result = optionPane.getResult();
      typewriterText.stop();
      hidePreview();
      optionPane.dispose();

      if (result == VIEW_DECK_OPTION) {
        showCurrentDeck();
      } else if (result == VIEW_MAP_OPTION) {
        RogueMapDialog.show(run);
      }
    } while (result == VIEW_DECK_OPTION || result == VIEW_MAP_OPTION);

    return new DialogResult(hasRerolls && result == REROLL_OPTION, selectedChoice);
  }

  private void hidePreview() {
    if (previewPopup != null) {
      previewPopup.hide();
    }
  }

  private void showCurrentDeck() {
    hidePreview();
    if (run != null && run.getCurrentDeck() != null) {
      FDeckViewer.show(run.getCurrentDeck());
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

  public record DialogResult(boolean rerollRequested, EventChoice choice) {}

  private static class MainPanel extends SkinnedPanel {
    private MainPanel() {
      super(new MigLayout("insets 20, gap 0, wrap, align center", "[grow, center]", ""));
      setOpaque(false);
    }
  }
}
