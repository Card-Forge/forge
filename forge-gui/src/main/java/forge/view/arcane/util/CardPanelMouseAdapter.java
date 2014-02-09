package forge.view.arcane.util;

import forge.view.arcane.CardPanel;

import java.awt.event.MouseEvent;

public abstract class CardPanelMouseAdapter implements CardPanelMouseListener {
    @Override
    public void mouseOver(CardPanel panel, MouseEvent evt) {
    }

    @Override
    public void mouseOut(CardPanel panel, MouseEvent evt) {
    }

    @Override
    public void mouseLeftClicked(CardPanel panel, MouseEvent evt) {
    }

    @Override
    public void mouseRightClicked(CardPanel panel, MouseEvent evt) {
    }

    @Override
    public void mouseDragStart(CardPanel dragPanel, MouseEvent evt) {
    }

    @Override
    public void mouseDragged(CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {
    }

    @Override
    public void mouseDragEnd(CardPanel dragPanel, MouseEvent evt) {
    }
}
