package forge.screens.match.views;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CZone;
import forge.toolbox.FScrollPane;
import forge.toolbox.MouseTriggerEvent;
import forge.view.arcane.CardArea;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;
import forge.view.arcane.FloatingZone;
import net.miginfocom.swing.MigLayout;

/**
 * Dockable zone tab view. Displays zone cards inside a DragCell tab,
 * as an alternative to the floating FDialog window.
 */
public class VZone implements IVDoc<CZone> {
    private final CZone control;
    private final EDocID docID;
    private final DragTab tab;
    private DragCell parentCell;

    private final FScrollPane scroller = new FScrollPane(false);
    private final ZoneCardArea cardArea;

    private final CMatchUI matchUI;
    private final PlayerView player;
    private final ZoneType zone;
    private boolean sortedByName = false;

    public VZone(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        this.matchUI = matchUI;
        this.player = player;
        this.zone = zone;
        this.docID = EDocID.fromZoneType(zone);
        this.cardArea = new ZoneCardArea(matchUI, scroller);
        this.cardArea.setVertical(true);
        this.cardArea.setOpaque(false);
        this.tab = new DragTab(capitalizedName());

        scroller.setViewportView(cardArea);

        control = new CZone(this);

        // Right-click context menu on tab
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final JPopupMenu menu = new JPopupMenu();

                    final Localizer localizer = Localizer.getInstance();
                    final JMenuItem undockItem = new JMenuItem(localizer.getMessage("lblUndock"));
                    undockItem.addActionListener(ev -> FloatingZone.undockZone(VZone.this));
                    menu.add(undockItem);

                    final JMenuItem sortItem = new JMenuItem(sortedByName ? localizer.getMessage("lblUnsort") : localizer.getMessage("lblSortByName"));
                    sortItem.addActionListener(ev -> toggleSorted());
                    menu.add(sortItem);

                    final JMenuItem closeItem = new JMenuItem(Localizer.getInstance().getMessage("lblClose"));
                    closeItem.addActionListener(ev -> FloatingZone.showOrHide(matchUI, player, zone));
                    menu.add(closeItem);

                    menu.show(tab, e.getX(), e.getY());
                }
            }
        });
    }

    /** Refresh card panels from zone data. */
    public void refresh() {
        final List<CardPanel> cardPanels = new ArrayList<>();
        final Iterable<CardView> cards = player.getCards(zone);
        if (cards != null) {
            final List<CardView> cardList = new ArrayList<>();
            for (final CardView card : cards) {
                cardList.add(card);
            }
            if (sortedByName) {
                cardList.sort(Comparator.comparing(CardView::getName));
            } else if (zone == ZoneType.Flashback) {
                cardList.sort(FloatingZone.ZONE_ORDER_COMPARATOR);
            }
            for (final CardView card : cardList) {
                CardPanel cardPanel = cardArea.getCardPanel(card.getId());
                if (cardPanel == null) {
                    cardPanel = new CardPanel(matchUI, card);
                    cardPanel.setDisplayEnabled(true);
                } else {
                    cardPanel.setCard(card);
                }
                if (zone == ZoneType.Flashback) {
                    final ZoneType cardZone = card.getZone();
                    if (cardZone != null) {
                        cardPanel.setZoneBanner(cardZone.getTranslatedName().toUpperCase(), cardZone);
                    }
                }
                cardPanels.add(cardPanel);
            }
        }
        cardArea.setCardPanels(cardPanels);
        updateTabLabel(cardPanels.size());
    }

    private void updateTabLabel(final int count) {
        final String countStr = String.valueOf(count);
        String label;
        if (matchUI.isLocalPlayer(player)) {
            label = capitalizedName() + " (" + countStr + ")";
        } else {
            label = Localizer.getInstance().getMessage("lblPlayerZoneN", player.getName(), capitalizedName(), countStr);
        }
        tab.setText(label);
        tab.setToolTipText(tab.getText());
    }

    private String capitalizedName() {
        final String name = zone.getTranslatedName();
        if (name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private void toggleSorted() {
        sortedByName = !sortedByName;
        refresh();
    }

    public CMatchUI getMatchUI() { return matchUI; }
    public PlayerView getPlayer() { return player; }
    public ZoneType getZone() { return zone; }

    public CardPanel getCardPanel(final CardView card) {
        return cardArea.getCardPanel(card.getId());
    }

    // IVDoc implementation

    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.add(scroller, "w 100%, h 100%!");
    }

    @Override
    public EDocID getDocumentID() { return docID; }

    @Override
    public void setParentCell(final DragCell cell0) { this.parentCell = cell0; }

    @Override
    public DragCell getParentCell() { return this.parentCell; }

    @Override
    public DragTab getTabLabel() { return tab; }

    @Override
    public CZone getLayoutControl() { return control; }

    /**
     * Inner CardArea subclass that routes mouse events to CMatchUI,
     * identical to how FloatingCardArea handles them.
     */
    private static class ZoneCardArea extends CardArea {
        ZoneCardArea(final CMatchUI matchUI, final FScrollPane scrollPane) {
            super(matchUI, scrollPane);
        }

        @Override
        public void mouseOver(final CardPanel panel, final MouseEvent evt) {
            getMatchUI().setCard(panel.getCard(), evt.isShiftDown());
            super.mouseOver(panel, evt);
        }

        @Override
        public void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
            getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt));
            super.mouseLeftClicked(panel, evt);
        }

        @Override
        public void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
            getMatchUI().getGameController().selectCard(panel.getCard(), null, new MouseTriggerEvent(evt));
            super.mouseRightClicked(panel, evt);
        }
    }
}
