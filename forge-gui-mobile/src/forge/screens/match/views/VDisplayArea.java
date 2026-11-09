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

    /**
     * Keeps controller selection valid after the cards changed: follows the selected card to its new position, or,
     * when it left this area (played, discarded...), moves the highlight to the card that took its place.
     */
    protected void syncControllerSelection() {
        if (selectedChild == null) {
            return;
        }
        int index = indexOfChild(this, selectedChild);
        if (index >= 0) {
            selectedIndex = index;
            return;
        }
        boolean active = selectedChild.isHovered();
        selectedChild.setHovered(false);
        selectedChild = null;
        if (!active || getChildCount() == 0) {
            selectedIndex = -1;
            if (active) {
                MatchScreen.nullPotentialListener();
            }
            return;
        }
        selectedIndex = Math.min(Math.max(selectedIndex, 0), getChildCount() - 1);
        selectedChild = getChildAt(selectedIndex);
        selectedChild.setHovered(true);
        MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
    }

    static int indexOfChild(FScrollPane area, FDisplayObject child) {
        for (int i = 0; i < area.getChildCount(); i++) {
            if (area.getChildAt(i) == child) {
                return i;
            }
        }
        return -1;
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
    /** Highlights the current card for controller navigation (the first one if none was selected yet). */
    public void selectCurrent() {
        if (selectedChild != null && selectedIndex >= 0 && selectedIndex < getChildCount() && getChildAt(selectedIndex) == selectedChild) {
            selectedChild.setHovered(true);
            scrollIntoView(selectedChild);
            MatchScreen.setPotentialListener(Arrays.asList(selectedChild));
        } else {
            selectedIndex = -1;
            selectedChild = null;
            setNextSelected(1);
        }
    }

    /** Whether the controller highlight is on the first card (or nothing is highlighted). */
    public boolean isSelectionAtStart() {
        return selectedIndex <= 0;
    }

    /** Removes the controller highlight. */
    public void unselectCurrent() {
        if (selectedChild != null) {
            selectedChild.setHovered(false);
            MatchScreen.nullPotentialListener();
        }
    }

    public void tapChild() {
        if (selectedChild instanceof FCardPanel)
            VCardDisplayArea.CardAreaPanel.get(((FCardPanel) selectedChild).getCard()).controllerTap();
        else if (selectedChild instanceof VManaPool.ManaLabel)
            ((VManaPool.ManaLabel) selectedChild).activate();
    }
    public void showZoom() {
        if (selectedChild instanceof FCardPanel)
            VCardDisplayArea.CardAreaPanel.get(((FCardPanel) selectedChild).getCard()).showZoom();
    }
}
