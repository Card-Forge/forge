package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.deckchooser.FDeckViewer;
import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
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
 * Dialog for Chest node interaction. Shows reward choices plus an optional skip action.
 */
public class ChestDialog {

  private static final int DIALOG_WIDTH = 400;
  private static final int MIN_DIALOG_HEIGHT = 400;
  private static final int PANEL_INSETS = 20;
  private static final int VIEW_DECK_OPTION = 0;
  private static final int VIEW_MAP_OPTION = 1;
  private static final int REROLL_OPTION = 2;
  private static final int LOOT_CHOICE_RESULT = 3;

  private final MainPanel panel;
  private final RogueRun run;
  private final ChoiceRerollContext rerollCtx;
  private final List<PreviewTarget> previewTargets = new ArrayList<>();
  private FOptionPane optionPane;
  private RoguePreviewPopup previewPopup;
  private ChestEffect selectedLoot;

  public ChestDialog(List<ChestEffect> lootChoices, RogueRun run, ChoiceRerollContext rerollCtx) {
    this.run = run;
    this.rerollCtx = rerollCtx;
    panel = new MainPanel();

    FLabel lblTitle = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueChooseYourLoot"))
        .fontSize(20).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build();

    int choiceButtonWidth = (DIALOG_WIDTH - 2 * PANEL_INSETS) * 4 / 5;

    int desiredHeight = PANEL_INSETS;

    panel.add(lblTitle, "w 100%!, h 60px!, ax center, gap 0 0 20px 10px, wrap");
    desiredHeight += 60 + 20 + 10;

    for (ChestEffect loot : lootChoices) {
      List<PreviewReference> previewReferences = loot.getPreviewReferences();
      FButton btnReward = RogueButtonHelper.createChoiceButton(
          loot.getDisplayName(), loot.getDescription(), previewReferences);
      RogueButtonHelper.setChoiceButtonSizeHint(btnReward, choiceButtonWidth);
      btnReward.addActionListener(e -> {
        selectedLoot = loot;
        hidePreview();
        optionPane.setResult(LOOT_CHOICE_RESULT);
        optionPane.setVisible(false);
      });
      previewTargets.add(new PreviewTarget(btnReward, previewReferences));
      panel.add(btnReward, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
      desiredHeight += btnReward.getPreferredSize().height + 10 + 10;
    }

    FButton btnSkip = RogueButtonHelper.createChoiceButton("Skip Loot", "");
    RogueButtonHelper.setChoiceButtonSizeHint(btnSkip, choiceButtonWidth);
    btnSkip.addActionListener(e -> {
      selectedLoot = null;
      hidePreview();
      optionPane.setResult(LOOT_CHOICE_RESULT);
      optionPane.setVisible(false);
    });
    panel.add(btnSkip, "w 80%!, ax center, gap 0 0 10px 10px, wrap");
    desiredHeight += btnSkip.getPreferredSize().height + 10 + 10;

    int dialogHeight = Math.min(Math.max(desiredHeight + PANEL_INSETS, MIN_DIALOG_HEIGHT), getMaxDialogHeight());
    Dimension dialogSize = new Dimension(DIALOG_WIDTH, dialogHeight);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
  }

  public DialogResult show() {
    selectedLoot = null;
    boolean hasRerolls = rerollCtx.remainingRerolls > 0;
    previewPopup = new RoguePreviewPopup();
    previewTargets.forEach(target ->
        previewPopup.attachTo(target.component(), target.references()));
    int result;
    do {
      List<String> buttons = new ArrayList<>();
      buttons.add("View Deck");
      buttons.add("View Map");
      if (hasRerolls) {
        buttons.add("Reroll (" + rerollCtx.remainingRerolls + " left)");
      }
      optionPane = new FOptionPane(null, "Chest", null, panel,
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
      optionPane.setVisible(true);
      result = optionPane.getResult();
      hidePreview();
      optionPane.dispose();

      if (result == VIEW_DECK_OPTION) {
        showCurrentDeck();
      } else if (result == VIEW_MAP_OPTION) {
        RogueMapDialog.show(run);
      }
    } while (result == VIEW_DECK_OPTION || result == VIEW_MAP_OPTION);

    return new DialogResult(hasRerolls && result == REROLL_OPTION, selectedLoot);
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

  public record DialogResult(boolean rerollRequested, ChestEffect choice) {}

  private static class MainPanel extends SkinnedPanel {
    private MainPanel() {
      super(new MigLayout("insets 20, gap 0, wrap, align center", "[grow, center]", ""));
      setOpaque(false);
    }
  }
}
