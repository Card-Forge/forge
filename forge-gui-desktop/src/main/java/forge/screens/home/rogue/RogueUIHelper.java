package forge.screens.home.rogue;

import forge.toolbox.FSkin;
import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gui.GuiChoose;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;
import org.tinylog.Logger;

/**
 * Shared helper for Rogue UI widgets.
 */
public final class RogueUIHelper {

  static int getAvailableAvatarIndex(final int avatarIndex) {
    // The upstream avatar tiles end with the question mark at index 125.
    if (avatarIndex < 126 && FSkin.getAvatars().containsKey(avatarIndex)) {
      return avatarIndex;
    }
    return 0;
  }


  /** Shared texts are plain, with line breaks; Swing labels need HTML to break lines. */
  public static String html(String plain, boolean centered) {
    if (plain == null || plain.startsWith("<html>")) {
      return plain;
    }
    String body = plain.replace("\n", "<br>");
    return centered ? "<html><center>" + body + "</center></html>" : "<html>" + body + "</html>";
  }

  private static final int TYPEWRITER_DELAY_MS = 15;
  private static final int TYPEWRITER_CHARS_PER_TICK = 2;

  private RogueUIHelper() {
  }

  static boolean progressReadable() {
    try {
      RogueMetaProgress.getInstance();
      return true;
    } catch (UncheckedIOException e) {
      Logger.error(e, "Could not read Rogue meta progress");
      FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblRogueProgressUnreadable"));
      return false;
    }
  }

  static Dimension getUsableDefaultScreenSize() {
    GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle bounds = config.getBounds();
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
    return new Dimension(bounds.width - insets.left - insets.right,
        bounds.height - insets.top - insets.bottom);
  }

  static int getMaxChoiceDialogHeight() {
    return (int) (getUsableDefaultScreenSize().height * 0.9) - 80;
  }

  static void layoutActionButtons(JPanel panel, int availableWidth) {
    if (availableWidth <= 0) {
      return;
    }

    final int minimumButtonWidth = 150;
    final int buttonGap = 10;
    List<Component> buttons = Arrays.stream(panel.getComponents()).filter(Component::isVisible).toList();
    int[] widths = buttons.stream()
        .mapToInt(button -> Math.min(availableWidth, Math.max(minimumButtonWidth, button.getPreferredSize().width)))
        .toArray();

    int[] buttonColumns = new int[widths.length];
    for (int rows = 1; rows <= widths.length; rows++) {
      int buttonsPerRow = widths.length / rows;
      int extraButtons = widths.length % rows;
      int[] columnWidths = new int[buttonsPerRow + (extraButtons == 0 ? 0 : 1)];
      int index = 0;
      for (int row = 0; row < rows; row++) {
        int rowSize = buttonsPerRow + (row < extraButtons ? 1 : 0);
        for (int column = 0; column < rowSize; column++) {
          buttonColumns[index] = column;
          columnWidths[column] = Math.max(columnWidths[column], widths[index]);
          index++;
        }
      }
      if (Arrays.stream(columnWidths).sum() + (columnWidths.length - 1) * buttonGap <= availableWidth) {
        break;
      }
    }

    MigLayout layout = (MigLayout) panel.getLayout();
    String grid = "insets 0, gap " + buttonGap + "px, fillx, hidemode 3";
    if (!grid.equals(layout.getLayoutConstraints())) {
      layout.setLayoutConstraints(grid);
    }
    for (int i = 0; i < buttons.size(); i++) {
      Component button = buttons.get(i);
      String size = "wmin " + widths[i] + "px, growx, h 40px!";
      if (i + 1 < buttonColumns.length && buttonColumns[i + 1] == 0) {
        size += ", wrap";
      }
      if (!size.equals(layout.getComponentConstraints(button))) {
        layout.setComponentConstraints(button, size);
      }
    }
    panel.revalidate();
    panel.repaint();
  }

  static void prepareChoiceDialog(FOptionPane optionPane, JComponent panel, int viewDeckOption,
                                  int viewMapOption) {
    configureChoicePane(optionPane);
    optionPane.getButton(viewDeckOption).setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
    optionPane.getButton(viewDeckOption).setHorizontalTextPosition(SwingConstants.RIGHT);
    optionPane.getButton(viewMapOption).setIcon(
        FSkin.getImage(FSkinProp.ICO_QUEST_MAP).resize(24, 24).getIcon());
    optionPane.getButton(viewMapOption).setHorizontalTextPosition(SwingConstants.RIGHT);
    panel.revalidate();
    panel.repaint();
  }

  record PreviewTarget(JComponent component, List<PreviewReference> references) {}

  static RoguePreviewPopup prepareChoiceDialogWithPreviews(FOptionPane optionPane, JComponent panel,
                                                           List<PreviewTarget> previewTargets) {
    configureChoicePane(optionPane);
    RoguePreviewPopup previewPopup = new RoguePreviewPopup();
    for (PreviewTarget target : previewTargets) {
      previewPopup.attachTo(target.component(), target.references());
    }
    panel.revalidate();
    panel.repaint();
    return previewPopup;
  }

  private static void configureChoicePane(FOptionPane optionPane) {
    optionPane.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    disableSpacebarButtonActivation(optionPane.getContentPane());
    optionPane.getTitleBar().setVisible(false);
  }

  static <T> T chooseDevOverride(String title, List<T> choices, T currentChoice) {
    List<T> preselected = currentChoice == null ? List.of() : List.of(currentChoice);
    List<T> selected = GuiChoose.getChoices(title, 0, 1, choices, preselected, null);
    return selected == null || selected.isEmpty() ? currentChoice : selected.get(0);
  }

  static <T> List<T> chooseDevEffects(String title, String heading,
      String selectionMessageKey, List<T> choices, List<T> preselected, int requiredSelections) {
    SkinnedPanel checkBoxPanel = new SkinnedPanel();
    checkBoxPanel.setOpaque(false);
    checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
    Map<FCheckBox, T> effectsByCheckBox = new LinkedHashMap<>();
    for (T effect : choices) {
      FCheckBox checkBox = new FCheckBox(((RogueEffect) effect).getDisplayName(), preselected.contains(effect));
      effectsByCheckBox.put(checkBox, effect);
      checkBoxPanel.add(checkBox);
    }

    FLabel infoLabel = new FLabel.Builder()
        .text("")
        .fontSize(14)
        .fontAlign(SwingConstants.CENTER)
        .build();
    FOptionPane optionPane = createChoiceOverridePane(title, heading, infoLabel, checkBoxPanel);
    Localizer localizer = Localizer.getInstance();
    Runnable updateSelection = () -> {
      long selectedCount = effectsByCheckBox.keySet().stream().filter(FCheckBox::isSelected).count();
      infoLabel.setText(localizer.getMessage(selectionMessageKey, requiredSelections, selectedCount));
      optionPane.setButtonEnabled(0, selectedCount == requiredSelections);
    };
    effectsByCheckBox.keySet().forEach(checkBox -> checkBox.addActionListener(e -> updateSelection.run()));
    updateSelection.run();

    try {
      optionPane.setVisible(true);
      if (optionPane.getResult() != 0) {
        return List.of();
      }

      List<T> selected = new ArrayList<>();
      effectsByCheckBox.forEach((checkBox, effect) -> {
        if (checkBox.isSelected()) {
          selected.add(effect);
        }
      });
      return selected;
    } finally {
      optionPane.dispose();
    }
  }

  private static FOptionPane createChoiceOverridePane(String title, String heading, FLabel infoLabel,
                                                      JComponent choices) {
    SkinnedPanel wrapper = new SkinnedPanel(new BorderLayout(0, 10));
    wrapper.setOpaque(false);

    FLabel headingLabel = new FLabel.Builder()
        .text(heading)
        .fontSize(16)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.CENTER)
        .build();

    SkinnedPanel header = new SkinnedPanel(new GridLayout(2, 1));
    header.setOpaque(false);
    header.add(headingLabel);
    header.add(infoLabel);

    wrapper.add(header, BorderLayout.NORTH);
    wrapper.add(new FScrollPane(choices, true), BorderLayout.CENTER);
    Dimension size = new Dimension(420, 420);
    wrapper.setPreferredSize(size);
    wrapper.setMinimumSize(size);
    Localizer localizer = Localizer.getInstance();
    return new FOptionPane(null, title, null, wrapper,
        List.of(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")), 0);
  }

  public static void disableSpacebarButtonActivation(Component component) {
    if (component instanceof AbstractButton button) {
      String ignoreSpace = "rogueIgnoreSpace";
      button.getActionMap().put(ignoreSpace, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // Consume Space without activating the focused decision button.
        }
      });
      InputMap inputMap = button.getInputMap(JComponent.WHEN_FOCUSED);
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), ignoreSpace);
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), ignoreSpace);
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        disableSpacebarButtonActivation(child);
      }
    }
  }

  public static JPanel createEffectPanel() {
    JPanel panel = new JPanel(new MigLayout("insets 0, gap 8 1"));
    panel.setOpaque(false);
    return panel;
  }

  static void resizeEffectScroller(FScrollPane scroller, JPanel panel) {
    Dimension effectSize = panel.getPreferredSize();
    int effectCount = panel.getComponentCount();
    int visibleRows = Math.min(3, effectCount);
    int rowHeight = visibleRows == 0 ? 0 : panel.getComponent(0).getPreferredSize().height;
    int viewportHeight = Math.min(effectSize.height,
        visibleRows * rowHeight + Math.max(0, visibleRows - 1));
    boolean needsScrollbar = effectCount > 6;
    scroller.setVerticalScrollBarPolicy(needsScrollbar
        ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    int viewportWidth = effectSize.width + 16;
    scroller.getViewport().setPreferredSize(new Dimension(viewportWidth, viewportHeight));
    int scrollbarWidth = needsScrollbar ? scroller.getVerticalScrollBar().getPreferredSize().width : 0;
    scroller.setPreferredSize(new Dimension(viewportWidth + scrollbarWidth, viewportHeight));
  }

  public static void populateEffectPanel(JPanel panel, List<RogueEffect> effects, RogueRun run) {
    panel.removeAll();
    for (int i = 0; i < effects.size(); i++) {
      RogueEffect effect = effects.get(i);
      int col = (i / 3) % 2;
      int row = (i / 6) * 3 + i % 3;
      addEffectLabel(panel, effect.getUIDisplayText(), effect.getActiveDescription(run), col, row);
    }
    panel.revalidate();
    panel.repaint();
  }

  public static void populateEffectPanelFromSnapshots(JPanel panel,
      List<RogueRunHistoryEntry.EffectSnapshot> effects) {
    panel.removeAll();
    for (int i = 0; i < effects.size(); i++) {
      RogueRunHistoryEntry.EffectSnapshot effect = effects.get(i);
      int col = (i / 3) % 2;
      int row = (i / 6) * 3 + i % 3;
      addEffectLabel(panel, effect.getDisplayText(), effect.getTooltipText(), col, row);
    }
    panel.revalidate();
    panel.repaint();
  }

  private static void addEffectLabel(JPanel panel, String displayText, String tooltipText, int col, int row) {
    FLabel effectLabel = new FLabel.Builder()
        .text(displayText)
        .fontSize(11)
        .build();
    effectLabel.setToolTipText(html(tooltipText, false));
    panel.add(effectLabel, "cell " + col + " " + row);
  }

  public static TypewriterText prepareTypewriterText(FTextArea textArea, JComponent skipComponent,
      List<String> fullTexts, int width) {
    TypewriterText typewriterText = new TypewriterText(textArea);
    List<String> texts = fullTexts == null || fullTexts.isEmpty() ? List.of("") : fullTexts;
    int textHeight = 0;
    for (String fullText : texts) {
      textArea.setText(fullText == null ? "" : fullText);
      textArea.setSize(width, Short.MAX_VALUE);
      textHeight = Math.max(textHeight, textArea.getPreferredSize().height);
    }
    Dimension textSize = new Dimension(width, textHeight);
    textArea.setPreferredSize(textSize);
    textArea.setMinimumSize(textSize);
    typewriterText.setFullText(texts.get(0));

    MouseAdapter skipRevealListener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        typewriterText.finish();
      }
    };
    textArea.addMouseListener(skipRevealListener);
    if (skipComponent != null && skipComponent != textArea) {
      skipComponent.addMouseListener(skipRevealListener);
    }

    return typewriterText;
  }

  public static final class TypewriterText {
    private final FTextArea textArea;
    private String fullText;
    private Timer timer;
    private int revealedCharacters;
    private boolean fullyRevealed;

    private TypewriterText(FTextArea textArea) {
      this.textArea = textArea;
      fullText = "";
    }

    public void setFullText(String fullText) {
      stop();
      this.fullText = fullText == null ? "" : fullText;
      revealedCharacters = 0;
      fullyRevealed = this.fullText.isEmpty();
      textArea.setText(fullyRevealed ? this.fullText : "");
    }

    public void start() {
      stop();
      revealedCharacters = 0;
      fullyRevealed = fullText.isEmpty();
      textArea.setText(fullyRevealed ? fullText : "");
      if (fullyRevealed) {
        return;
      }

      timer = new Timer(TYPEWRITER_DELAY_MS, e -> revealNextTextChunk());
      timer.start();
    }

    public void finish() {
      if (fullyRevealed) {
        return;
      }
      fullyRevealed = true;
      stop();
      textArea.setText(fullText);
      textArea.revalidate();
      textArea.repaint();
    }

    public void stop() {
      if (timer != null) {
        timer.stop();
        timer = null;
      }
    }

    private void revealNextTextChunk() {
      if (revealedCharacters >= fullText.length()) {
        finish();
        return;
      }

      revealedCharacters = Math.min(
          fullText.length(), revealedCharacters + TYPEWRITER_CHARS_PER_TICK);
      textArea.setText(fullText.substring(0, revealedCharacters));
      if (revealedCharacters >= fullText.length()) {
        finish();
      }
    }
  }
}
