package forge.screens.home.rogue;

import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.PreviewReferenceType;
import forge.gamemodes.rogue.TextHelper;
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.localinstance.skin.FSkinProp;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingConstants;

/**
 * Shared helper for Rogue buttons.
 */
public final class RogueButtonHelper {

  private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*|\\{\\{(.+?)}}");
  private static final Color EMPHASIS_TEXT_COLOR = new Color(0xE0, 0xB4, 0x37);
  private static final Color KEYWORD_TEXT_COLOR = new Color(0xEE, 0x8C, 0xDD);
  private static final int MIN_BUTTON_WIDTH = 240;
  private static final int DEFAULT_WRAP_WIDTH = 420;
  private static final int HORIZONTAL_TEXT_PADDING = 22;
  private static final int VERTICAL_TEXT_PADDING = 8;
  private static final int CONTENT_VERTICAL_NUDGE = 2;
  private static final int TITLE_DESCRIPTION_GAP = 3;
  private static final float DESCRIPTION_FONT_SIZE = 12f;
  private static final int COMPACT_BUTTON_EXTRA_WIDTH = 8;
  private RogueButtonHelper() {
  }

  public static FButton createChoiceButton(String label, String description) {
    return createChoiceButton(label, description, List.of());
  }

  public static FButton createChoiceButton(String label, String description, List<PreviewReference> references) {
    return createButton(label, description, references, true);
  }

  public static FButton createNavigationButton(String label) {
    return createButton(label, "", List.of(), false);
  }

  private static FButton createButton(String label, String description, List<PreviewReference> references,
      boolean playSound) {
    String highlightedDescription = applyAutomaticKeywordHighlights(
        applyAutomaticCardHighlights(description == null ? "" : description, references), references);
    FButton button = new RogueChoiceButton(label, highlightedDescription);
    if (playSound) {
      button.addActionListener(e -> SoundSystem.instance.play(
          SoundEffectType.Token, SoundEffectType.Token.isSynced()));
    }
    return button;
  }

  static void setChoiceButtonSizeHint(FButton button, int buttonWidth) {
    if (button instanceof RogueChoiceButton choiceButton) {
      choiceButton.setPreferredButtonWidth(buttonWidth);
    }
  }

  public static FButton createViewDeckButtonForHistory(Runnable onClick) {
    FButton button = new FButton("View Deck");
    button.setIcon(FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE));
    button.setIconTextGap(6);
    button.setHorizontalTextPosition(SwingConstants.RIGHT);
    button.addActionListener(e -> onClick.run());
    return button;
  }

  public static Dimension getCompactButtonSize(FButton button, int minWidth, int minHeight) {
    Dimension preferredSize = button.getPreferredSize();
    int width = Math.max(minWidth, preferredSize.width + COMPACT_BUTTON_EXTRA_WIDTH);
    int height = Math.max(minHeight, preferredSize.height);
    return new Dimension(width, height);
  }

  private static String applyAutomaticCardHighlights(String description, List<PreviewReference> references) {
    return applyAutomaticHighlights(description, references, PreviewReferenceType.CARD, "**", "**", false);
  }

  private static String applyAutomaticKeywordHighlights(String description, List<PreviewReference> references) {
    return applyAutomaticHighlights(description, references, PreviewReferenceType.KEYWORD, "{{", "}}", true);
  }

  private static String applyAutomaticHighlights(String description, List<PreviewReference> references,
      PreviewReferenceType type, String prefix, String suffix, boolean allowSimplePlural) {
    if (references == null || references.isEmpty()) {
      return description;
    }

    List<String> tokens = new ArrayList<>();
    for (PreviewReference reference : references) {
      if (reference.type() != type) {
        continue;
      }
      String token = extractHighlightToken(reference, type);
      if (token.isEmpty()
          || description.contains(prefix + token + suffix)
          || description.contains("**" + token + "**")) {
        continue;
      }
      tokens.add(token);
    }

    tokens.sort(Comparator.comparingInt(String::length).reversed());

    String highlightedDescription = description;
    for (String token : tokens) {
      highlightedDescription = highlightWholeWordOutsideExistingHighlights(
          highlightedDescription, token, prefix, suffix, allowSimplePlural);
    }
    return highlightedDescription;
  }

  private static String highlightWholeWordOutsideExistingHighlights(String text, String token, String prefix,
      String suffix, boolean allowSimplePlural) {
    Matcher matcher = HIGHLIGHT_PATTERN.matcher(text);
    StringBuilder sb = new StringBuilder();
    int lastIndex = 0;

    while (matcher.find()) {
      sb.append(highlightWholeWord(text.substring(lastIndex, matcher.start()), token, prefix, suffix,
          allowSimplePlural));
      sb.append(matcher.group());
      lastIndex = matcher.end();
    }

    sb.append(highlightWholeWord(text.substring(lastIndex), token, prefix, suffix, allowSimplePlural));
    return sb.toString();
  }

  private static String highlightWholeWord(String text, String token, String prefix, String suffix,
      boolean allowSimplePlural) {
    if (token.isEmpty()) {
      return text;
    }

    String pluralSuffix = allowSimplePlural && !token.endsWith("s") ? "s?" : "";
    Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}_])(" + Pattern.quote(token) + pluralSuffix
        + ")(?![\\p{L}\\p{N}_])");
    Matcher matcher = pattern.matcher(text);
    return matcher.replaceAll(Matcher.quoteReplacement(prefix) + "$1" + Matcher.quoteReplacement(suffix));
  }

  private static List<TextRun> parseDescriptionRuns(String description) {
    if (description == null || description.isEmpty()) {
      return List.of();
    }

    Matcher matcher = HIGHLIGHT_PATTERN.matcher(description);
    List<TextRun> runs = new ArrayList<>();
    int lastIndex = 0;

    while (matcher.find()) {
      addRun(runs, description.substring(lastIndex, matcher.start()), HighlightStyle.NORMAL);
      if (matcher.group(1) != null) {
        addRun(runs, matcher.group(1), HighlightStyle.EMPHASIS);
      } else {
        addRun(runs, matcher.group(2), HighlightStyle.KEYWORD);
      }
      lastIndex = matcher.end();
    }
    addRun(runs, description.substring(lastIndex), HighlightStyle.NORMAL);
    return runs;
  }

  private static void addRun(List<TextRun> runs, String text, HighlightStyle style) {
    if (text.isEmpty()) {
      return;
    }
    runs.add(new TextRun(text, style));
  }

  private static List<LayoutToken> tokenizeRuns(List<TextRun> runs) {
    List<LayoutToken> tokens = new ArrayList<>();
    for (TextRun run : runs) {
      int index = 0;
      while (index < run.text().length()) {
        char c = run.text().charAt(index);
        if (c == '\r' || c == '\n') {
          if (c == '\r' && index + 1 < run.text().length() && run.text().charAt(index + 1) == '\n') {
            index++;
          }
          tokens.add(new LayoutToken("", HighlightStyle.NORMAL, false, true));
          index++;
          continue;
        }

        int start = index;
        if (Character.isWhitespace(c)) {
          while (index < run.text().length()) {
            char current = run.text().charAt(index);
            if (current == '\r' || current == '\n' || !Character.isWhitespace(current)) {
              break;
            }
            index++;
          }
          tokens.add(new LayoutToken(normalizeWhitespace(run.text().substring(start, index)),
              run.style(), true, false));
          continue;
        }

        while (index < run.text().length()) {
          char current = run.text().charAt(index);
          if (current == '\r' || current == '\n' || Character.isWhitespace(current)) {
            break;
          }
          index++;
        }
        tokens.add(new LayoutToken(run.text().substring(start, index), run.style(), false, false));
      }
    }
    return tokens;
  }

  private static String normalizeWhitespace(String text) {
    return text.replace('\t', ' ');
  }

  private static String extractHighlightToken(PreviewReference reference, PreviewReferenceType type) {
    if (type == PreviewReferenceType.CARD) {
      return TextHelper.extractCardNameFromReference(reference.token());
    }
    return reference.token() == null ? "" : reference.token().trim();
  }

  private enum HighlightStyle {
    NORMAL,
    EMPHASIS,
    KEYWORD
  }

  private record TextRun(String text, HighlightStyle style) {}

  private record LayoutToken(String text, HighlightStyle style, boolean whitespace, boolean newline) {}

  private record LayoutLine(List<TextRun> runs, int width) {}

  private static final class RogueChoiceButton extends FButton {
    private final String choiceLabel;
    private final List<TextRun> descriptionRuns;
    private int preferredButtonWidth;

    private RogueChoiceButton(String label, String description) {
      super("");
      choiceLabel = label == null ? "" : label;
      descriptionRuns = parseDescriptionRuns(description);
    }

    private void setPreferredButtonWidth(int buttonWidth) {
      preferredButtonWidth = buttonWidth;
    }

    @Override
    public Dimension getPreferredSize() {
      Font titleFont = getFont();
      Font descriptionFont = getDescriptionFont();
      FontMetrics titleMetrics = getFontMetrics(titleFont);
      FontMetrics descriptionMetrics = getFontMetrics(descriptionFont);
      int availableWidth = preferredButtonWidth > 0
          ? Math.max(1, preferredButtonWidth - 2 * HORIZONTAL_TEXT_PADDING)
          : DEFAULT_WRAP_WIDTH;
      List<LayoutLine> lines = layoutDescriptionLines(descriptionMetrics, availableWidth);

      int height = getContentHeight(titleMetrics, descriptionMetrics, lines) + 2 * VERTICAL_TEXT_PADDING;

      Dimension baseSize = super.getPreferredSize();
      int width = Math.max(baseSize.width, MIN_BUTTON_WIDTH);
      if (preferredButtonWidth > 0) {
        width = Math.max(width, preferredButtonWidth);
      }
      return new Dimension(width, Math.max(baseSize.height, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      Font titleFont = getFont();
      Font descriptionFont = getDescriptionFont();
      FontMetrics titleMetrics = g2.getFontMetrics(titleFont);
      FontMetrics descriptionMetrics = g2.getFontMetrics(descriptionFont);
      int availableWidth = Math.max(1, getWidth() - 2 * HORIZONTAL_TEXT_PADDING);
      List<LayoutLine> lines = layoutDescriptionLines(descriptionMetrics, availableWidth);

      g2.setFont(titleFont);
      g2.setColor(getForeground());
      int contentHeight = getContentHeight(titleMetrics, descriptionMetrics, lines);
      int contentTop = Math.max(VERTICAL_TEXT_PADDING,
          (getHeight() - contentHeight) / 2 + CONTENT_VERTICAL_NUDGE);
      int titleBaseline = contentTop + titleMetrics.getAscent();
      int titleX = Math.max(HORIZONTAL_TEXT_PADDING,
          (getWidth() - titleMetrics.stringWidth(choiceLabel)) / 2);
      g2.drawString(choiceLabel, titleX, titleBaseline);

      if (!lines.isEmpty()) {
        int baseline = contentTop + titleMetrics.getHeight() + TITLE_DESCRIPTION_GAP + descriptionMetrics.getAscent();
        g2.setFont(descriptionFont);
        for (LayoutLine line : lines) {
          int x = Math.max(HORIZONTAL_TEXT_PADDING, (getWidth() - line.width()) / 2);
          for (TextRun run : line.runs()) {
            g2.setColor(getRunColor(run.style()));
            g2.drawString(run.text(), x, baseline);
            x += descriptionMetrics.stringWidth(run.text());
          }
          baseline += descriptionMetrics.getHeight();
        }
      }

      g2.dispose();
    }

    private Font getDescriptionFont() {
      return getFont().deriveFont(Font.BOLD, DESCRIPTION_FONT_SIZE);
    }

    private Color getRunColor(HighlightStyle style) {
      return switch (style) {
        case EMPHASIS -> EMPHASIS_TEXT_COLOR;
        case KEYWORD -> KEYWORD_TEXT_COLOR;
        default -> getForeground();
      };
    }

    private int getContentHeight(FontMetrics titleMetrics, FontMetrics descriptionMetrics, List<LayoutLine> lines) {
      int height = titleMetrics.getHeight();
      if (!lines.isEmpty()) {
        height += TITLE_DESCRIPTION_GAP + lines.size() * descriptionMetrics.getHeight();
      }
      return height;
    }

    private List<LayoutLine> layoutDescriptionLines(FontMetrics metrics, int maxWidth) {
      if (descriptionRuns.isEmpty()) {
        return List.of();
      }

      List<LayoutLine> lines = new ArrayList<>();
      List<TextRun> currentLine = new ArrayList<>();
      int currentWidth = 0;

      for (LayoutToken token : tokenizeRuns(descriptionRuns)) {
        if (token.newline()) {
          currentWidth = commitLine(lines, currentLine, currentWidth);
          continue;
        }

        TextRun textRun = new TextRun(token.text(), token.style());
        if (token.whitespace()) {
          if (currentLine.isEmpty()) {
            continue;
          }
          currentLine.add(textRun);
          currentWidth += metrics.stringWidth(token.text());
          continue;
        }

        int tokenWidth = metrics.stringWidth(token.text());
        if (!currentLine.isEmpty() && currentWidth + tokenWidth > maxWidth) {
          currentWidth = commitLine(lines, currentLine, currentWidth);
        }

        currentLine.add(textRun);
        currentWidth += tokenWidth;
      }

      commitLine(lines, currentLine, currentWidth);
      return lines;
    }

    private int commitLine(List<LayoutLine> lines, List<TextRun> currentLine, int currentWidth) {
      while (!currentLine.isEmpty() && currentLine.get(currentLine.size() - 1).text().isBlank()) {
        TextRun trailing = currentLine.remove(currentLine.size() - 1);
        currentWidth -= getFontMetrics(getDescriptionFont()).stringWidth(trailing.text());
      }

      if (!currentLine.isEmpty()) {
        lines.add(new LayoutLine(mergeAdjacentRuns(currentLine), currentWidth));
        currentLine.clear();
      }
      return 0;
    }

    private List<TextRun> mergeAdjacentRuns(List<TextRun> runs) {
      List<TextRun> merged = new ArrayList<>();
      for (TextRun run : runs) {
        if (merged.isEmpty()) {
          merged.add(run);
          continue;
        }

        TextRun previous = merged.get(merged.size() - 1);
        if (previous.style() == run.style()) {
          merged.set(merged.size() - 1, new TextRun(previous.text() + run.text(), previous.style()));
        } else {
          merged.add(run);
        }
      }
      return List.copyOf(merged);
    }
  }
}
