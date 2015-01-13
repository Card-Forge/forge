/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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

package forge.toolbox.special;

import forge.ImageKeys;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;
import forge.assets.FSkinProp;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.gui.SOverlayUtils;
import forge.match.MatchUtil;
import forge.toolbox.FOverlay;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import forge.util.ImageUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** 
 * Displays card image at its original size and correct orientation.
 * <p>
 * Supports split, flip and double-sided cards as well as cards that
 * can be played face-down (eg. morph).
 *
 * @version $Id: CardZoomer.java 24769 2014-02-09 13:56:04Z Hellfish $
 * 
 */
public enum CardZoomer {
    SINGLETON_INSTANCE;

    // Gui controls
    private final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
    private JPanel pnlMain;
    private FImagePanel imagePanel;
    private SkinnedLabel lblFlipcard = new SkinnedLabel();    

    // Details about the current card being displayed.
    private CardView thisCard;
    private boolean isInAltState;

    // The zoomer is in button mode when it is activated by holding down the
    // middle mouse button or left and right mouse buttons simultaneously.
    private boolean isButtonMode = false;    
    private boolean isOpen = false;
    private long lastClosedTime;

    // Used to ignore mouse wheel rotation for a short period of time.
    private Timer mouseWheelCoolDownTimer;
    private boolean isMouseWheelEnabled = false;    

    // ctr
    private CardZoomer() {
        lblFlipcard.setIcon(FSkin.getIcon(FSkinProp.ICO_FLIPCARD));
        setMouseButtonListener();
        setMouseWheelListener();
        setKeyListeners();
    }

    /**
     * Creates listener for keys that are recognised by zoomer.
     * <p><ul>
     * <li>ESC will close zoomer in mouse-wheel mode only.
     * <li>CTRL will flip or transform card in either mode if applicable.
     */
    private void setKeyListeners() {
        overlay.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isButtonMode) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        closeZoomer();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    toggleCardImage();
                }
            }        
        });
    }

    /**
     * Creates listener for mouse button events.
     * <p>
     * NOTE: Needed even if ButtonMode to prevent Zoom getting stuck open on
     * certain systems.
     */
    private void setMouseButtonListener() {
        overlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                closeZoomer();
            }
        });
    }

    /**
     * Creates listener for mouse wheel events.
     * <p>
     * If the zoomer is opened using the mouse wheel then additional actions can
     * be performed dependent on the card type -
     * <p>
     * <ul>
     * <li>If mouse wheel is rotated back then close zoomer.
     * <li>If mouse wheel is rotated forward and...
     * <ul>
     * <li>if image is a flip card then rotate 180 degrees.
     * <li>if image is a double-sided card then show other side.
     */
    private void setMouseWheelListener() {
        overlay.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!isButtonMode) {
                    if (isMouseWheelEnabled) {
                        isMouseWheelEnabled = false;
                        if (e.getWheelRotation() > 0) {
                            closeZoomer();
                        } else {
                            toggleCardImage();
                            startMouseWheelCoolDownTimer(250);
                        }
                    }
                }
            }
        });
    }

    /**
     * Opens zoomer in mouse wheel mode and displays the image associated with
     * the given card based.
     * <p>
     * This method should be called if the zoomer is activated by rotating the
     * mouse wheel.
     */
    public void doMouseWheelZoom(final CardView card) {
        isButtonMode = false;
        displayCard(card);
        startMouseWheelCoolDownTimer(200);
    }

    /**
     * Opens zoomer in mouse button mode and displays the image associated with
     * the given card.
     * <p>
     * This method should be called if the zoomer is activated by holding down
     * the middle mouse button or left and right mouse buttons simultaneously.
     */    
    public void doMouseButtonZoom(final CardView newCard) {
        // don't display zoom if already zoomed or just closed zoom 
        // (handles mouse wheeling while middle clicking)
        if (isOpen || System.currentTimeMillis() - lastClosedTime < 250) {
            return;
        }

        isButtonMode = true;        
        displayCard(newCard);        
    }

    public boolean isZoomerOpen() {
        return isOpen;
    }

    private void displayCard(final CardView newCard) {
        isMouseWheelEnabled = false;
        thisCard = newCard;
        isInAltState = false;
        setLayout();
        setImage();
        SOverlayUtils.showOverlay();
        isOpen = true;
    }

    /**
     * Displays a graphical indicator that shows whether the current card can be flipped or transformed.
     */
    private void setFlipIndicator() {
        if (MatchUtil.canCardBeFlipped(thisCard)) {
            imagePanel.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));        
            imagePanel.add(lblFlipcard, "pos (100% - 100px) 0");
        }
    }

    /**
     * Needs to be called whenever the source image changes.
     */
    private void setImage() {
        imagePanel = new FImagePanel();

        BufferedImage xlhqImage = getImageXlhq();
        imagePanel.setImage(xlhqImage == null ? FImageUtil.getImage(getState()) : xlhqImage, getInitialRotation(), AutoSizeImageMode.SOURCE);

        pnlMain.removeAll();
        pnlMain.add(imagePanel, "w 80%!, h 80%!");
        pnlMain.validate();
        setFlipIndicator();
    }

    private BufferedImage getImageXlhq() {
        String key = MatchUtil.getCardImageKey(getState());

        if (key.indexOf(ImageKeys.CARD_PREFIX) != 0) {
            return null;
        }

        boolean altState = key.endsWith(ImageKeys.BACKFACE_POSTFIX);
        String imageKey = ImageUtil.getImageKey(ImageUtil.getPaperCardFromImageKey(key.substring(2)), altState, true);
        if(altState) {
            imageKey = imageKey.substring(0, imageKey.length() - ImageKeys.BACKFACE_POSTFIX.length());
            imageKey += "full.jpg";
        }

        File file = ImageKeys.getImageFile(imageKey);

        if (file != null) {
            Path path = file.toPath();
            String modPath = path.getRoot().toString() + path.subpath(0, path.getNameCount()-2).toString() + File.separator + "XLHQ" + File.separator + path.subpath(path.getNameCount()-2, path.getNameCount());
            File xlhqFile = new File(modPath.replace(".full.jpg", ".xlhq.jpg"));

            if (xlhqFile != null && xlhqFile.exists()) {
                try {
                    return ImageIO.read(xlhqFile);
                } catch (IOException ex) {
                    System.err.println("IO exception caught when trying to open a XLHQ image: " + xlhqFile.getName());
                }
            }
        }

        return null;
    }
    
    private int getInitialRotation() {
        return (thisCard.isSplitCard() || thisCard.getCurrentState().getType().isPlane() || thisCard.getCurrentState().getType().isPhenomenon() ? 90 : 0);
    }   

    private void setLayout() {
        overlay.removeAll();
        pnlMain = new JPanel();
        pnlMain.setOpaque(false);
        overlay.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));
        pnlMain.setLayout(new MigLayout("insets 0, wrap, align center"));
        overlay.add(pnlMain, "w 100%!, h 100%!");
    }

    public void closeZoomer() {
        if (!isOpen) { return; }
        stopMouseWheelCoolDownTimer();        
        isOpen = false;
        SOverlayUtils.hideOverlay();
        lastClosedTime = System.currentTimeMillis();
    }
    
    /**
     * If the zoomer is ativated using the mouse wheel then ignore
     * wheel for a short period of time after opening. This will 
     * prevent flip and double side cards from immediately flipping.
     */
    private void startMouseWheelCoolDownTimer(int millisecsDelay) {        
        isMouseWheelEnabled = false;
        createMouseWheelCoolDownTimer(millisecsDelay);
        mouseWheelCoolDownTimer.setInitialDelay(millisecsDelay);
        mouseWheelCoolDownTimer.restart();           
    }
    
    /**
     * Used to ignore mouse wheel rotation for {@code millisecsDelay} milliseconds.
     */
    private void createMouseWheelCoolDownTimer(int millisecsDelay) {
        if (mouseWheelCoolDownTimer == null) {           
            mouseWheelCoolDownTimer = new Timer(millisecsDelay, new ActionListener() {                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isMouseWheelEnabled = true;                
                }
            });
        }
    }

    private void stopMouseWheelCoolDownTimer() {
        if (mouseWheelCoolDownTimer != null && mouseWheelCoolDownTimer.isRunning()) {
            mouseWheelCoolDownTimer.stop();
        }        
    }

    /**
     * Toggles between primary and alternate image associated with card if applicable.
     */
    private void toggleCardImage() {
        if (MatchUtil.canCardBeFlipped(thisCard)) {
            toggleFlipCard();
        }
    }

    /**
     * Flips image by rotating 180 degrees each time.
     * <p>
     * No need to get the alternate card image from cache.
     * Can simply rotate current card image in situ to get same effect.
     */
    private void toggleFlipCard() {
        isInAltState = !isInAltState;
        imagePanel.setRotation(thisCard.isFlipCard() && isInAltState ? 180 : 0);
        setImage();
    }

    private CardStateView getState() {
        return thisCard.getState(isInAltState);
    }
}
