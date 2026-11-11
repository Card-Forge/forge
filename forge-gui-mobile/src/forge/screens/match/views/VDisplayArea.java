package forge.screens.match.views;

import forge.Graphics;
import forge.screens.match.MatchScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;

import java.util.Arrays;

public abstract class VDisplayArea extends FScrollPane {
    private FDisplayObject selectedChild;
    private int selectedIndex = -1;
    public VDisplayArea() {
        setVisible(false); //hide by default
    }
    @Override
    protected void drawBackground(Graphics g) {
        //compact zone tabs: the zone opens over the battlefield, so it paints its own backing above the field's cards
        if (VPlayerPanel.isZoneOverlay()) {
            //only as wide as the cards themselves, so the battlefield background stays visible around them
            float left = Float.MAX_VALUE, right = 0;
            for (FDisplayObject child : getChildren()) { left = Math.min(left, child.getLeft()); right = Math.max(right, child.getRight()); }
            left = Math.max(0, left);
            right = Math.min(right, getWidth());
            if (right <= left) { left = 0; right = getWidth(); } //empty zone: the whole (one card wide) box
            VPlayerPanel.fillOverlayZone(g, left, right - left, getHeight());
        }
    }

    public abstract int getCount();
    public abstract void update();
    public void setNextSelected(int val) {
        if (getChildCount() < 1) {
            selectedIndex = -1;
            return;
        }
        if (selectedIndex == -1) {
            selectedIndex++;
            if (selectedChild != null)
                selectedChild.setHovered(false);
            selectedChild = getChildAt(selectedIndex);
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
            return;
        }
        if (selectedIndex+val < getChildCount()) {
            selectedIndex+=val;
            if (selectedChild != null)
                selectedChild.setHovered(false);
            selectedChild = getChildAt(selectedIndex);
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
        }
    }
    public void setPreviousSelected(int val) {
        if (getChildCount() < 1) {
            selectedIndex = -1;
            return;
        }
        if (selectedIndex-val > -1) {
            selectedIndex-=val;
            if (selectedChild != null)
                selectedChild.setHovered(false);
            selectedChild = getChildAt(selectedIndex);
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
        }
    }
    public void tapChild() {
        if (selectedChild instanceof FCardPanel)
            VCardDisplayArea.CardAreaPanel.get(((FCardPanel) selectedChild).getCard()).selectCard(false);
        else if (selectedChild instanceof VManaPool.ManaLabel)
            ((VManaPool.ManaLabel) selectedChild).activate();
    }
    public void showZoom() {
        if (selectedChild instanceof FCardPanel)
            VCardDisplayArea.CardAreaPanel.get(((FCardPanel) selectedChild).getCard()).showZoom();
    }
}
