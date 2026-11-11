package forge.screens.home.rogue;

import forge.game.card.CardView;
import forge.gamemodes.rogue.RogueDeck;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Panel for displaying a commander card in the start view. Shows the commander card and allows
 * single selection. For locked commanders, shows a card back with a lock indicator.
 */
public class CommanderCardPanel extends SkinnedPanel {
  private final transient RogueDeck commander;
  private final PaperCard commanderCard;
  private final CardPicturePanel cardPicture;
  private boolean selected;
  private boolean highlighted;  // For locked commanders when clicked
  private boolean hovered;      // For hover effect
  private boolean dimmed;
  private final boolean locked;
  private transient Consumer<CommanderCardPanel> selectionCallback;

  public CommanderCardPanel(RogueDeck commander, VSubmenuRogueStart view) {
    super(null);
    this.commander = commander;
    // Get the commander card from the start deck
    this.commanderCard = commander.getStartDeck().getCommanders().get(0);
    this.selected = false;
    this.locked = !commander.isUnlocked();
    this.cardPicture = new CardPicturePanel();

    setOpaque(false);
    setLayout(null); // Manual layout

    // Display commander card or card back for locked commanders
    if (locked) {
      cardPicture.setCard(CardView.getCardForUi(commanderCard).getCurrentState(), false);
    } else {
      cardPicture.setItem(commanderCard);
    }
    cardPicture.setOpaque(false);
    add(cardPicture);

    // Add mouse listener for selection
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (selectionCallback != null) {
          selectionCallback.accept(CommanderCardPanel.this);
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        hovered = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hovered = false;
        setCursor(Cursor.getDefaultCursor());
        repaint();
      }
    });

    // Add mouse wheel listener for card zoom (only for unlocked commanders)
    addMouseWheelListener(e -> {
      if (!locked && e.getWheelRotation() < 0 && view.getZoomUtil() != null) { // Scroll up to zoom
        view.getZoomUtil().showZoom(commanderCard);
      }
    });
  }

  public void setSelectionCallback(Consumer<CommanderCardPanel> callback) {
    this.selectionCallback = callback;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
    repaint();
  }

  public boolean isSelected() {
    return selected;
  }

  public void setDimmed(boolean dimmed) {
    if (this.dimmed != dimmed) {
      this.dimmed = dimmed;
      repaint();
    }
  }

  public void setHighlighted(boolean highlighted) {
    this.highlighted = highlighted;
    repaint();
  }

  public boolean isHighlighted() {
    return highlighted;
  }

  public RogueDeck getCommander() {
    return commander;
  }

  public boolean isLocked() {
    return locked;
  }

  public void refreshHiddenCardImage() {
    if (locked) {
      cardPicture.onImageFetched();
    }
  }

  @Override
  public void setVisible(boolean visible) {
    if (!visible) {
      hovered = false;
      setCursor(Cursor.getDefaultCursor());
    }
    super.setVisible(visible);
  }

  @Override
  public void doLayout() {
    // Make card picture fill the panel
    cardPicture.setBounds(0, 0, getWidth(), getHeight());
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();
    int height = getHeight();

    // Draw selection indicators if selected (unlocked commanders)
    if (selected) {
      CardUtil.drawSelectionHighlight(g2d, width, height);
    }
    // Draw yellow/gold border if highlighted (locked commanders when clicked)
    else if (highlighted) {
      g2d.setColor(new Color(255, 215, 0));  // Gold color
      g2d.setStroke(new BasicStroke(6));
      g2d.drawRect(3, 3, width - 6, height - 6);
    }
    // Draw hover border (yellow/gold) when not selected and not highlighted
    else if (hovered) {
      g2d.setColor(new Color(255, 215, 0));  // Gold color
      g2d.setStroke(new BasicStroke(4));
      g2d.drawRect(3, 3, width - 6, height - 6);
    }

    if (dimmed) {
      g2d.setColor(new Color(70, 70, 70, 145));
      g2d.fillRect(0, 0, width, height);
    }
  }
}
