package forge.screens.match.views;

import forge.screens.match.MatchScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;

import java.util.Arrays;
import java.util.List;

public abstract class VDisplayArea extends FScrollPane {
    private FDisplayObject selectedChild;
    private int selectedIndex = -1;
    public VDisplayArea() {
        setVisible(false); //hide by default
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
