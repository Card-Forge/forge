package forge.screens.home.rogue;

import forge.toolbox.FSkin;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
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
import javax.swing.Timer;
import net.miginfocom.swing.MigLayout;

/**
 * Shared helper for Rogue UI widgets.
 */
public final class RogueUIHelper {

  static int getAvailableAvatarIndex(final int avatarIndex) {
    if (FSkin.getAvatars().containsKey(avatarIndex)) {
      return avatarIndex;
    }
    return FSkin.getAvatars().containsKey(125) ? 125 : 0;
  }


  private static final int TYPEWRITER_DELAY_MS = 15;
  private static final int TYPEWRITER_CHARS_PER_TICK = 2;

  private RogueUIHelper() {
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
