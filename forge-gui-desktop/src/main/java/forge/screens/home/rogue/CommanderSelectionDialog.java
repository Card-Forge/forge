package forge.screens.home.rogue;

import forge.ImageCache;
import forge.gamemodes.rogue.RogueCustomDeckGenerator;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerContainer;
import forge.itemmanager.ItemManagerModel;
import forge.itemmanager.views.ImageView;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Searchable image browser for choosing a generated run's Commander. */
public class CommanderSelectionDialog {
  public PaperCard show() {
    List<PaperCard> commanders = RogueCustomDeckGenerator.getEligibleCommanders();
    if (commanders.isEmpty()) {
      FOptionPane.showMessageDialog("No legal Commanders are available.", "Choose any Commander");
      return null;
    }

    CardManager cardManager = new CardManager(null, false, false, false) {
      @Override
      protected ImageView<PaperCard> createImageView(ItemManagerModel<PaperCard> model) {
        return new ImageView<PaperCard>(this, model, false) {
          private final Set<String> requestedImages = new HashSet<>();
          private boolean visibleImagesQueued;

          {
            getScroller().getViewport().addChangeListener(e -> queueVisibleImages());
          }

          @Override
          protected void onRefresh() {
            model.getOrderedList().sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(
                left.getKey().getName(), right.getKey().getName()));
            super.onRefresh();
            queueVisibleImages();
          }

          @Override
          protected void showHoveredItem(PaperCard card) {
            // Card images and the built-in zoom are sufficient in this picker.
          }

          private void queueVisibleImages() {
            if (visibleImagesQueued) {
              return;
            }
            visibleImagesQueued = true;
            SwingUtilities.invokeLater(() -> {
              visibleImagesQueued = false;
              Rectangle visible = getScroller().getViewport().getViewRect();
              if (visible.isEmpty()) {
                return;
              }
              int step = Math.max(16, visible.width / (Math.max(1, getColumnCount()) * 2));
              for (int y = visible.y + step / 2; y < visible.y + visible.height; y += step) {
                for (int x = visible.x + step / 2; x < visible.x + visible.width; x += step) {
                  PaperCard card = getItemAtIndex(getIndexAtPoint(new Point(x, y)));
                  if (card == null) {
                    continue;
                  }
                  String imageKey = card.getImageKey(false);
                  if (requestedImages.add(imageKey)
                      && ImageCache.getCardOriginalImageInfo(imageKey, true).getRight()) {
                    GuiBase.getInterface().getImageFetcher().fetchImage(imageKey,
                        () -> getComponent().repaint());
                  }
                }
              }
            });
          }
        };
      }
    };
    cardManager.setAllowMultipleSelections(false);
    JPanel content = new FSkin.SkinnedPanel(new BorderLayout());
    content.setOpaque(false);
    content.add(new ItemManagerContainer(cardManager), BorderLayout.CENTER);
    cardManager.setPool(commanders);
    cardManager.setViewIndex(1);

    GraphicsConfiguration gc = JOptionPane.getRootFrame().getGraphicsConfiguration();
    Rectangle bounds = gc.getBounds();
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int usableWidth = bounds.width - insets.left - insets.right;
    int usableHeight = bounds.height - insets.top - insets.bottom;
    int width = Math.min(usableWidth - 40, Math.max(900, (int) (usableWidth * 0.6)));
    Dimension dialogSize = new Dimension(Math.max(1, width),
        Math.max(1, (int) (usableHeight * 0.75)));
    content.setPreferredSize(dialogSize);
    content.setMinimumSize(dialogSize);
    content.setSize(dialogSize);

    FOptionPane pane = new FOptionPane(null, "Choose any Commander", null, content,
        List.of("Choose Commander", "Cancel"), 0);
    pane.setButtonEnabled(0, cardManager.getSelectedItem() != null);
    cardManager.addSelectionListener(e ->
        pane.setButtonEnabled(0, cardManager.getSelectedItem() != null));
    cardManager.setItemActivateCommand(() -> {
      if (cardManager.getSelectedItem() != null) {
        pane.setResult(0);
      }
    });
    SwingUtilities.invokeLater(cardManager::focusSearch);
    pane.setVisible(true);
    PaperCard selected = pane.getResult() == 0 ? cardManager.getSelectedItem() : null;
    pane.dispose();
    return selected;
  }
}
