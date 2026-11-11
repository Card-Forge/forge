package forge.screens.home.rogue;

import forge.toolbox.FSkin;
import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.localinstance.skin.FSkinProp;
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
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Shared helper for Rogue UI widgets.
 */
public final class RogueUIHelper {

  static int getAvailableAvatarIndex(final int avatarIndex) {
    // The default sheet's question mark is the first tile without a portrait.
    if (avatarIndex < 125 && FSkin.getAvatars().containsKey(avatarIndex)) {
      return avatarIndex;
    }
    return 0;
  }


  private static final int TYPEWRITER_DELAY_MS = 15;
  private static final int TYPEWRITER_CHARS_PER_TICK = 2;

  private RogueUIHelper() {
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

  static FOptionPane createChoiceOverridePane(String title, String heading, FLabel infoLabel,
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
    return new FOptionPane(null, title, null, wrapper,
        List.of("OK", Localizer.getInstance().getMessage("lblCancel")), 0);
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
    effectLabel.setToolTipText(tooltipText);
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
