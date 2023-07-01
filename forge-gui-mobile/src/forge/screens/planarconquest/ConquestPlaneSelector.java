package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.card.CardRenderer;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTimer;
import forge.toolbox.GuiDialog;
import forge.util.Utils;
import forge.util.collect.FCollectionView;

public class ConquestPlaneSelector extends FDisplayObject {
    private static final FSkinFont PLANE_NAME_FONT = FSkinFont.get(30);
    private static final Color BACK_COLOR = FSkinColor.fromRGB(1, 2, 2);
    private static final float MONITOR_TOP_MULTIPLIER = 15f / 315f;
    private static final float MONITOR_BOTTOM_MULTIPLIER = 23f / 315f;
    private static final float MONITOR_LEFT_MULTIPLIER = 19f / 443f;
    private static final float ARROW_THICKNESS = Utils.scale(3);

    private static List<ConquestPlane> planes = ImmutableList.copyOf(Iterables.filter(FModel.getPlanes(), new Predicate<ConquestPlane>() {
        @Override
        public boolean apply(ConquestPlane plane) {
            return !plane.isUnreachable(); //filter out unreachable planes
        }
    }));

    private final FTimer timer = new FTimer(2.5f) {
        @Override
        protected void tick() {
            FCollectionView<PaperCard> planeCards = getSelectedPlane().getPlaneCards();
            if (++artIndex >= planeCards.size()) {
                artIndex = 0;
            }
            currentArt = CardRenderer.getCardArt(planeCards.get(artIndex));
        }
    };
    private int selectedIndex, artIndex;
    private int currentPlaneIndex = -1;
    private FImage currentArt;
    private Rectangle leftArrowBounds, rightArrowBounds, currentArtBounds;

    public ConquestPlaneSelector() {
        reset();
    }

    public ConquestPlane getSelectedPlane() {
        return planes.get(selectedIndex);
    }
    public void setSelectedPlane(ConquestPlane plane0) {
        setSelectedIndex(planes.indexOf(plane0));
    }

    public void setCurrentPlane(ConquestPlane plane0) {
        currentPlaneIndex = planes.indexOf(plane0);
        setSelectedIndex((currentPlaneIndex + 1) % planes.size());
    }

    public void activate() {
        timer.start();
    }

    public void deactivate() {
        timer.stop();
    }

    public void reset() {
        setSelectedIndex(0);
        timer.stop();
    }

    private void setSelectedIndex(int selectedIndex0) {
        if (currentPlaneIndex == selectedIndex0) { return; } //can't select current plane
        selectedIndex = selectedIndex0;
        artIndex = 0;
        currentArt = CardRenderer.getCardArt(getSelectedPlane().getPlaneCards().get(artIndex));
        timer.restart();
        onSelectionChange();
    }

    protected void onSelectionChange() {
    }

    private void incrementSelectedIndex(int dir) {
        int newIndex = selectedIndex + dir;
        if (newIndex == currentPlaneIndex && currentPlaneIndex != -1) {
            newIndex += dir; //skip hidden current plane
        }
        if (newIndex < 0) {
            newIndex = planes.size() - 1;
            if (newIndex == currentPlaneIndex) {
                newIndex--;
            }
        }
        else if (newIndex >= planes.size()) {
            newIndex = 0;
            if (newIndex == currentPlaneIndex) {
                newIndex++;
            }
        }
        setSelectedIndex(newIndex);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (leftArrowBounds.contains(x, y)) {
            incrementSelectedIndex(-1);
            return true;
        }
        if (rightArrowBounds.contains(x, y)) {
            incrementSelectedIndex(1);
            return true;
        }
        if (currentArtBounds != null && currentArtBounds.contains(x, y)) {
            ConquestPlane plane = getSelectedPlane();
            String desc = plane.getDescription();
            if (!desc.isEmpty()) {
                GuiDialog.message(plane.getDescription().replace("\\n", "\n"), plane.getName().replace("_", " "));
            } else {
                GuiDialog.message(Forge.getLocalizer().getMessage("lblThisPlaneHasNoDesc"), plane.getName());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            incrementSelectedIndex(velocityX > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics g) {
        float hmod = Forge.getHeightModifier();
        float w = getWidth();
        float h = getHeight();

        //draw background
        FImage background = FSkinTexture.BG_SPACE;
        float backgroundHeight = w * background.getHeight() / background.getWidth();
        g.fillRect(BACK_COLOR, 0, 0, w, h);
        g.drawImage(background, 0, h - backgroundHeight, w, backgroundHeight); //retain aspect ratio, remaining area will be covered by back color

        //determine monitor position
        FImage monitor = FSkinImage.PLANE_MONITOR;
        float monitorLeft = FOptionPane.PADDING / 2;
        float monitorWidth = w - 2 * monitorLeft;
        float monitorHeight = (monitorWidth * monitor.getHeight() / monitor.getWidth()) - hmod;
        float monitorLeftOffset = monitorWidth * MONITOR_LEFT_MULTIPLIER;
        float monitorTopOffset = monitorHeight * MONITOR_TOP_MULTIPLIER;
        float monitorBottomOffset = monitorHeight * MONITOR_BOTTOM_MULTIPLIER;
        float monitorTop = monitorLeft + monitorLeftOffset - monitorTopOffset;

        //draw plane art inside monitor
        if (currentArt != null) {
            float x = monitorLeft + monitorLeftOffset - 1; //-1 to account for rounding error
            float y = monitorTop + monitorTopOffset - 1;
            float artWidth = monitorWidth - 2 * monitorLeftOffset + 2;
            float artHeight = monitorHeight - monitorTopOffset - monitorBottomOffset + 2;

            //scale up art to fill height of monitor while retaining aspect ratio
            float fullArtWidth = artHeight * currentArt.getWidth() / currentArt.getHeight();
            
            float artHeightClipMod = 0f;
            if (fullArtWidth < monitorWidth) {
                //if the card art is too narrow, widen it to fully cover the monitor size               
                float scaledArtHeight = monitorWidth * (artHeight / fullArtWidth);
                fullArtWidth = monitorWidth;
                artHeightClipMod = scaledArtHeight - artHeight;
                artHeight = scaledArtHeight;
            }
            
            g.startClip(x, y, artWidth, artHeight - artHeightClipMod);
            g.drawImage(currentArt, x + (monitorWidth - fullArtWidth) / 2, y, fullArtWidth, artHeight);
            g.endClip();

            currentArtBounds = new Rectangle(x, y, artWidth, artHeight);
        }

        //draw monitor so plane art remains within it
        g.drawImage(monitor, monitorLeft, monitorTop, monitorWidth, monitorHeight);

        //draw plane name
        float arrowOffsetLeft = monitorLeft * 2.5f;
        float arrowSize = PLANE_NAME_FONT.getCapHeight();
        float textLeft = arrowSize + 1.5f * arrowOffsetLeft;
        float monitorBottom = monitorTop + monitorHeight;
        float remainingHeight = h - monitorBottom;
        ConquestPlane plane = getSelectedPlane();
        g.drawText(plane.getName().replace("_", " " ), PLANE_NAME_FONT, Color.WHITE, textLeft, monitorBottom, w - 2 * textLeft, remainingHeight, false, Align.center, true);

        //draw left/right arrows
        float yMid = monitorBottom + remainingHeight / 2;
        float offsetX = arrowSize / 4;
        float offsetY = arrowSize / 2;
        float midOffsetX = arrowSize * 0.4f;

        float xMid = arrowOffsetLeft + midOffsetX;
        g.drawLine(ARROW_THICKNESS, Color.WHITE, xMid + offsetX, yMid - offsetY, xMid - offsetX, yMid + 1);
        g.drawLine(ARROW_THICKNESS, Color.WHITE, xMid - offsetX, yMid - 1, xMid + offsetX, yMid + offsetY);
        leftArrowBounds = new Rectangle(0, monitorBottom, textLeft + arrowSize, remainingHeight);

        xMid = w - arrowOffsetLeft - midOffsetX;
        g.drawLine(ARROW_THICKNESS, Color.WHITE, xMid - offsetX, yMid - offsetY, xMid + offsetX, yMid + 1);
        g.drawLine(ARROW_THICKNESS, Color.WHITE, xMid + offsetX, yMid - 1, xMid - offsetX, yMid + offsetY);
        rightArrowBounds = new Rectangle(w - leftArrowBounds.width, monitorBottom, leftArrowBounds.width, remainingHeight);
    }

    public void updateReachablePlanes() {
        planes = ImmutableList.copyOf(Iterables.filter(FModel.getPlanes(), new Predicate<ConquestPlane>() {
            @Override
            public boolean apply(ConquestPlane plane) {
                return !plane.isUnreachable();
            }
        }));
    }
}
