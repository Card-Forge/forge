/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

import forge.Singletons;
import forge.game.card.CardView;
import forge.gui.framework.SDisplayUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.MouseTriggerEvent;
import forge.view.FDialog;
import forge.view.FFrame;

//import forge.util.collect.FCollectionView;

// show some cards in a new window
public abstract class FloatingCardArea extends CardArea {

    protected static final String COORD_DELIM = ","; 
    protected static final ForgePreferences prefs = FModel.getPreferences();

    protected String title;
    protected FPref locPref;
    protected boolean hasBeenShown, locLoaded;

    protected abstract Iterable<CardView> getCards();

    protected FloatingCardArea(final CMatchUI matchUI) {
        this(matchUI, new FScrollPane(false, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
    }
    protected FloatingCardArea(final CMatchUI matchUI, final FScrollPane scrollPane) {
        super(matchUI, scrollPane);
    }

    protected void showWindow() {
        onShow();
        getWindow().setFocusableWindowState(false); // should probably do this earlier
        getWindow().setVisible(true);
    }

    protected void hideWindow() {
        onShow();
        getWindow().setFocusableWindowState(false); // should probably do this earlier
        getWindow().setVisible(false);
        getWindow().dispose(); //pfps so that old content does not show up
    }

    protected void showOrHideWindow() {
        if (getWindow().isVisible()) {
            hideWindow();
        } else {
            showWindow();
        }
    }
    protected void onShow() {
        if (!hasBeenShown) {
            loadLocation();
            getWindow().getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override public final void onLeftDoubleClick(final MouseEvent e) {
                    getWindow().setVisible(false); //hide window if titlebar double-clicked
                }
            });
        }
    }

    @SuppressWarnings("serial")
    protected final FDialog window = new FDialog(false, true, "0") {
        @Override
        public void setLocationRelativeTo(Component c) {
            if (hasBeenShown || locLoaded) { return; }
            super.setLocationRelativeTo(c);
        }

        @Override
        public void setVisible(boolean b0) {
            if (isVisible() == b0) {
                return;
            }
            if (!b0 && hasBeenShown && locPref != null) {
                //update preference before hiding window, as otherwise its location will be 0,0
                prefs.setPref(locPref,
                        getX() + COORD_DELIM + getY() + COORD_DELIM +
                                getWidth() + COORD_DELIM + getHeight());
                //don't call prefs.save(), instead allowing them to be saved when match ends
            }
            if (b0) {
                doRefresh();  // force a refresh before showing to pick up any changes when hidden
                hasBeenShown = true;
            }
            super.setVisible(b0);
        }
    };

    public boolean isVisible() {
        return window.isVisible();
    }

    protected FDialog getWindow() {
        return window;
    }

    protected void loadLocation() {
        if (locPref != null) {
            String value = prefs.getPref(locPref);
            if (value.length() > 0) {
                String[] coords = value.split(COORD_DELIM);
                if (coords.length == 4) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int w = Integer.parseInt(coords[2]);
                        int h = Integer.parseInt(coords[3]);
    
                        //ensure the window is accessible
                        int centerX = x + w / 2;
                        int centerY = y + h / 2;
                        Rectangle screenBounds = SDisplayUtil.getScreenBoundsForPoint(new Point(centerX, centerY)); 
                        if (centerX < screenBounds.x) {
                            x = screenBounds.x;
                        }
                        else if (centerX > screenBounds.x + screenBounds.width) {
                            x = screenBounds.x + screenBounds.width - w;
                            if (x < screenBounds.x) {
                                x = screenBounds.x;
                            }
                        }
                        if (centerY < screenBounds.y) {
                            y = screenBounds.y;
                        }
                        else if (centerY > screenBounds.y + screenBounds.height) {
                            y = screenBounds.y + screenBounds.height - h;
                            if (y < screenBounds.y) {
                                y = screenBounds.y;
                            }
                        }
                        getWindow().setBounds(x, y, w, h);
                        locLoaded = true;
                        return;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                prefs.setPref(locPref, ""); //clear value if invalid
                prefs.save();
            }
        }
        //fallback default size
        FFrame mainFrame = Singletons.getView().getFrame();
        getWindow().setSize(mainFrame.getWidth() / 5, mainFrame.getHeight() / 2);
    }

    protected void refresh() {
        if (!getWindow().isVisible()) { return; } //don't refresh while window hidden
	doRefresh();
    }

    protected void doRefresh() {
        List<CardPanel> cardPanels = new ArrayList<>();
        Iterable<CardView> cards = getCards();
        if (cards != null) {
            for (final CardView card : cards) {
                CardPanel cardPanel = getCardPanel(card.getId());
                if (cardPanel == null) {
                    cardPanel = new CardPanel(getMatchUI(), card);
                    cardPanel.setDisplayEnabled(true);
                }
                else {
                    cardPanel.setCard(card); //ensure card view updated
                }
                cardPanels.add(cardPanel);
            }
        }

        boolean hadCardPanels = getCardPanels().size() > 0;
        setCardPanels(cardPanels);
        getWindow().setTitle(String.format(title, cardPanels.size()));

	//pfps - rather suspect, so commented out for now
	//        //if window had cards and now doesn't, hide window
	//        //(e.g. cast final card from Flashback zone)
	//        if (hadCardPanels && cardPanels.size() == 0) {
	//            getWindow().setVisible(false);
	//        }
    }

    @Override
    public void doLayout() {
        if (getWindow().isResizing()) {
            //delay layout slightly to reduce flicker during window resize
            layoutTimer.restart();
        } else {
            finishDoLayout();
        }
    }

    protected final Timer layoutTimer = new Timer(250, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            layoutTimer.stop();
            finishDoLayout();
        }
    });

    protected void finishDoLayout() {
        super.doLayout();
    }

    @Override
    public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
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
