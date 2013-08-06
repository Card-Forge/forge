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

package forge.gui.toolbox.special;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.gui.CardPicturePanel;
import forge.gui.SOverlayUtils;
import forge.gui.toolbox.FOverlay;

/** 
 * Displays card image BIG.
 *
 * @version $Id$
 * 
 */
public enum CardZoomer {
    SINGLETON_INSTANCE;    

    private final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
    private Card thisCard;
    private JPanel pnlMain;
    private boolean temporary, zoomed;
    private long lastClosedTime;
    
    private CardZoomer() {        
        setupMouseListeners();
        setupKeyListeners();
    }
    
    private void setupKeyListeners() {
        overlay.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!temporary && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeZoomer();                   
                }                
            }        
        });
    }

    private void setupMouseListeners() {
        overlay.addMouseListener(new MouseAdapter() {            
            @Override
            public void mouseReleased(MouseEvent e) {
                closeZoomer(); //NOTE: Needed even if temporary to prevent Zoom getting stuck open on certain systems
            }
        });

        overlay.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!temporary && e.getWheelRotation() > 0) {
                    closeZoomer();
                } 
            }
        });
    }

    public boolean isZoomed() {
        return zoomed;
    }

    public void displayZoomedCard(Card card) {
        displayZoomedCard(card, false);
    }

    public void displayZoomedCard(Card card, boolean temp) {
        if (zoomed || System.currentTimeMillis() - lastClosedTime < 250) {
            return; //don't display zoom if already zoomed or just closed zoom (handles mouse wheeling while middle clicking)
        }
        thisCard = card;
        temporary = temp;
        setLayout();

        CardPicturePanel picturePanel = new CardPicturePanel(); 
        picturePanel.setCard(thisCard);        
        picturePanel.setOpaque(false);
        pnlMain.add(picturePanel, "w 80%!, h 80%!");        

        SOverlayUtils.showOverlay();
        zoomed = true;
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
        if (!zoomed) { return; }
        zoomed = false;
        SOverlayUtils.hideOverlay();
        lastClosedTime = System.currentTimeMillis();
    }
}
