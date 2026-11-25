/*
 * Forge: Play Magic: the Gathering.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.arcane;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollBar;
import javax.swing.WindowConstants;

import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.screens.match.CMatchUI;
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FFrame;
import forge.view.arcane.util.CardPanelMouseAdapter;

/**
 * Lets the player sort cards into the top of their library and a second pile. Only the moveable cards are shown, in a
 * section for each pile. Clicking a card moves it to the other section, and dragging places it.
 */
public class ListCardArea extends FloatingCardArea {

    private static final int HEADER_HEIGHT = 30;
    private static final int DIVIDER_GAP = 14;
    private static final int DIVIDER_TEXT_GAP = 8;
    private static final int CARD_GAP = 6;

    private static Rectangle lastBounds;

    private final List<CardView> topSection = new ArrayList<>();
    private final List<CardView> bottomSection = new ArrayList<>();
    private final List<CardView> otherCards = new ArrayList<>();
    private final String topLabel;
    private final String bottomLabel;
    private final String dividerText;

    private int dividerY;

    private ListCardArea(final CMatchUI matchUI, final String title, final Iterable<CardView> cards, final Iterable<CardView> moveableCards, final ZoneType secondPile) {
        super(matchUI);
        this.title = title;
        for (final CardView card : cards) {
            (Iterables.contains(moveableCards, card) ? topSection : otherCards).add(card);
        }
        final Localizer localizer = Localizer.getInstance();
        topLabel = localizer.getMessage("lblTopOfLibrary");
        bottomLabel = localizer.getMessage(secondPile == ZoneType.Library ? "lblBottomOfLibrary" : "lblGraveyard");
        dividerText = secondPile == ZoneType.Library ? localizer.getMessage("lblOtherCardsInLibrary", otherCards.size()) : "";

        window.add(getScrollPane(), "grow, push, wrap, gap 6 6 6 6");
        window.setModal(true);
        // Only Done closes the dialog, so that a stray Esc doesn't accept the arrangement
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getScrollPane().setViewportView(this);
        final FButton doneButton = new FButton(localizer.getMessage("lblDone"));
        doneButton.addActionListener(e -> {
            lastBounds = window.getBounds();
            window.dispose();
        });
        window.add(doneButton, "align center, gapbottom 8, w 140, h 24");
        setOpaque(false);
        setDragEnabled(true);
        addCardPanelMouseListener(new CardPanelMouseAdapter() {
            @Override
            public void mouseDragged(final CardPanel dragPanel, final int dragOffsetX, final int dragOffsetY, final MouseEvent evt) {
                placeCard(dragPanel.getCard(), evt.getX(), evt.getY());
                revalidate();
            }

            @Override
            public void mouseDragEnd(final CardPanel dragPanel, final MouseEvent evt) {
                placeCard(dragPanel.getCard(), evt.getX(), evt.getY());
                refresh();
            }
        });
    }

    public static List<CardView> arrange(final CMatchUI matchUI, final String title, final Iterable<CardView> cards, final Iterable<CardView> moveableCards, final ZoneType secondPile) {
        final ListCardArea area = new ListCardArea(matchUI, title, cards, moveableCards, secondPile);
        area.showWindow();
        final List<CardView> arranged = new ArrayList<>(area.topSection);
        arranged.addAll(area.otherCards);
        arranged.addAll(area.bottomSection);
        return arranged;
    }

    @Override
    protected Iterable<CardView> getCards() {
        return Iterables.concat(topSection, bottomSection);
    }

    @Override
    protected void showWindow() {
        if (lastBounds == null) {
            final FFrame mainFrame = Singletons.getView().getFrame();
            window.setSize(mainFrame.getWidth() / 4, mainFrame.getHeight() * 2 / 3 - 50);
        } else {
            window.setBounds(lastBounds);
        }
        window.setVisible(true);
    }

    @Override
    protected void finishDoLayout() {
        final Rectangle rect = getScrollPane().getVisibleRect();
        final Insets insets = getScrollPane().getInsets();
        int width = rect.width - insets.left - insets.right;
        final JScrollBar scrollBar = getScrollPane().getVerticalScrollBar();
        if (scrollBar.isVisible()) {
            width -= scrollBar.getWidth();
        }
        final int height = rect.height - insets.top - insets.bottom;
        final int sectionHeight = (height - 2 * GUTTER_Y - 2 * DIVIDER_GAP) / 2;

        int cardWidth = getCardWidthMax();
        while (cardWidth > getCardWidthMin() && !(fits(topSection, cardWidth, width, sectionHeight) && fits(bottomSection, cardWidth, width, sectionHeight))) {
            cardWidth--;
        }

        layoutSection(topSection, cardWidth, width, GUTTER_Y);
        // The divider stays in the middle, unless the top section is too tall even with the smallest cards
        dividerY = GUTTER_Y + Math.max(sectionHeight, sectionHeight(topSection, cardWidth, width)) + DIVIDER_GAP;
        final int bottomY = dividerY + DIVIDER_GAP;
        layoutSection(bottomSection, cardWidth, width, bottomY);

        final Dimension size = new Dimension(width, Math.max(height, bottomY + sectionHeight(bottomSection, cardWidth, width) + GUTTER_Y));
        if (!size.equals(getPreferredSize())) {
            setPreferredSize(size);
            getParent().invalidate();
            getParent().validate();
        }
        repaint();
    }

    private static int cardHeight(final int cardWidth) {
        return Math.round(cardWidth * CardPanel.ASPECT_RATIO);
    }

    private static int columns(final int cardWidth, final int width) {
        return Math.max(1, (width - 2 * GUTTER_X + CARD_GAP) / (cardWidth + CARD_GAP));
    }

    private static int sectionHeight(final List<CardView> section, final int cardWidth, final int width) {
        final int columns = columns(cardWidth, width);
        final int rows = (section.size() + columns - 1) / columns;
        return HEADER_HEIGHT + rows * (cardHeight(cardWidth) + CARD_GAP);
    }

    private static boolean fits(final List<CardView> section, final int cardWidth, final int width, final int height) {
        return cardWidth <= width - 2 * GUTTER_X && sectionHeight(section, cardWidth, width) <= height;
    }

    private void layoutSection(final List<CardView> section, final int cardWidth, final int width, final int top) {
        final int columns = columns(cardWidth, width);
        final int cardHeight = cardHeight(cardWidth);
        for (int i = 0; i < section.size(); i++) {
            final int column = i % columns;
            final int rowSize = Math.min(columns, section.size() - (i - column));
            final int rowWidth = rowSize * (cardWidth + CARD_GAP) - CARD_GAP;
            getCardPanel(section.get(i).getId()).setCardBounds((width - rowWidth) / 2 + column * (cardWidth + CARD_GAP),
                    top + HEADER_HEIGHT + i / columns * (cardHeight + CARD_GAP), cardWidth, cardHeight);
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FSkin.setGraphicsFont(g2, FSkin.getBoldFont(13));
        FSkin.setGraphicsColor(g2, FSkin.getColor(FSkin.Colors.CLR_TEXT));
        drawCentered(g2, topLabel, GUTTER_Y + HEADER_HEIGHT / 2);
        drawCentered(g2, bottomLabel, dividerY + DIVIDER_GAP + HEADER_HEIGHT / 2);

        FSkin.setGraphicsFont(g2, FSkin.getFont(12));
        FSkin.setGraphicsColor(g2, FSkin.getColor(FSkin.Colors.CLR_BORDERS));
        final int lineEnd = dividerText.isEmpty() ? getWidth() / 2 : (getWidth() - g2.getFontMetrics().stringWidth(dividerText)) / 2 - DIVIDER_TEXT_GAP;
        g2.drawLine(GUTTER_X, dividerY, lineEnd, dividerY);
        g2.drawLine(getWidth() - lineEnd, dividerY, getWidth() - GUTTER_X, dividerY);
        drawCentered(g2, dividerText, dividerY);
    }

    private void drawCentered(final Graphics2D g2, final String text, final int centerY) {
        final FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, centerY + (fm.getAscent() - fm.getDescent()) / 2);
    }

    private void placeCard(final CardView card, final int x, final int y) {
        final List<CardView> section = y < dividerY ? topSection : bottomSection;
        topSection.remove(card);
        bottomSection.remove(card);
        section.add((int) section.stream().filter(other -> isBefore(getCardPanel(other.getId()), x, y)).count(), card);
    }

    private static boolean isBefore(final CardPanel panel, final int x, final int y) {
        if (y < panel.getCardY()) {
            return false;
        }
        return y > panel.getCardY() + panel.getCardHeight() || panel.getCardX() + panel.getCardWidth() / 2 < x;
    }

    @Override
    public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
        switchSection(panel.getCard());
    }

    @Override
    public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
        switchSection(panel.getCard());
    }

    private void switchSection(final CardView card) {
        if (topSection.remove(card)) {
            bottomSection.add(card);
        } else {
            bottomSection.remove(card);
            topSection.add(card);
        }
        refresh();
    }
}
