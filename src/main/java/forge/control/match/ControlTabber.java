/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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
package forge.control.match;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import forge.AllZone;
import forge.GuiDisplayUtil;
import forge.MyObservable;
import forge.Singletons;
import forge.view.match.ViewTabber;

/**
 * Controls the vertical tabber in sidebar used for viewing gameplay data:
 * stack, combat, etc.
 * 
 */
public class ControlTabber extends MyObservable {
    private final ViewTabber view;
    private MouseAdapter maMilling, maHand, maLibrary, maUnlimited,
        maMana, maSetup, maTutor, maCounter, maTap, maUntap, maLife;

    /**
     * Controls the vertical tabber in sidebar used for viewing gameplay data:
     * stack, combat, etc.
     * 
     * @param v
     *            &emsp; The tabber Swing component
     */
    public ControlTabber(final ViewTabber v) {
        this.view = v;

        if (Singletons.getModel().getPreferences().isMillingLossCondition()) {
            this.view.getLblMilling().setEnabled(true);
        } else {
            this.view.getLblMilling().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getHandView()) {
            this.view.getLblHandView().setEnabled(true);
        } else {
            this.view.getLblHandView().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getLibraryView()) {
            this.view.getLblLibraryView().setEnabled(true);
        } else {
            this.view.getLblLibraryView().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getUnlimitedLand()) {
            this.view.getLblUnlimitedLands().setEnabled(true);
        } else {
            this.view.getLblUnlimitedLands().setEnabled(false);
        }

        // Various mouse adapters for dev buttons
        initMouseAdapters();
    }

    /** Adds observers to tabber. */
    public void addObservers() {
        // Stack
        final Observer o1 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlTabber.this.view.updateStack();
            }
        };

        //Game Log
        final Observer o2 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                ControlTabber.this.view.updateConsole();
            }
        };

        AllZone.getStack().deleteObserver(o1);
        AllZone.getGameLog().deleteObserver(o2);

        AllZone.getStack().addObserver(o1);
        AllZone.getGameLog().addObserver(o2);
    }

    /** Adds listeners to various components in tabber. */
    public void addListeners() {
        // Milling enable toggle
        this.view.getLblMilling().removeMouseListener(maMilling);
        this.view.getLblMilling().addMouseListener(maMilling);

        // View any hand toggle
        this.view.getLblHandView().removeMouseListener(maHand);
        this.view.getLblHandView().addMouseListener(maHand);

        // DevMode: View any library toggle
        this.view.getLblLibraryView().removeMouseListener(maLibrary);
        this.view.getLblLibraryView().addMouseListener(maLibrary);

        // DevMode: Play unlimited land this turn toggle
        this.view.getLblUnlimitedLands().removeMouseListener(maUnlimited);
        this.view.getLblUnlimitedLands().addMouseListener(maUnlimited);

        // DevMode: Generate mana
        this.view.getLblGenerateMana().removeMouseListener(maMana);
        this.view.getLblGenerateMana().addMouseListener(maMana);

        // DevMode: Battlefield setup
        this.view.getLblSetupGame().removeMouseListener(maSetup);
        this.view.getLblSetupGame().addMouseListener(maSetup);

        // DevMode: Tutor for card
        this.view.getLblTutor().removeMouseListener(maTutor);
        this.view.getLblTutor().addMouseListener(maTutor);

        // DevMode: Add counter to permanent
        this.view.getLblCounterPermanent().removeMouseListener(maCounter);
        this.view.getLblCounterPermanent().addMouseListener(maCounter);

        // DevMode: Tap permanent
        this.view.getLblTapPermanent().removeMouseListener(maTap);
        this.view.getLblTapPermanent().addMouseListener(maTap);

        // DevMode: Untap permanent
        this.view.getLblUntapPermanent().removeMouseListener(maUntap);
        this.view.getLblUntapPermanent().addMouseListener(maUntap);

        // DevMode: Set life
        this.view.getLblSetLife().removeMouseListener(maLife);
        this.view.getLblSetLife().addMouseListener(maLife);
    }

    /**
     * Gets the view.
     * 
     * @return ViewTabber
     */
    public ViewTabber getView() {
        return this.view;
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Dev" panel.
     */
    public void showPnlDev() {
        this.view.getVtpTabber().showTab(4);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Players"
     * panel.
     */
    public void showPnlPlayers() {
        this.view.getVtpTabber().showTab(3);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Console"
     * panel.
     */
    public void showPnlConsole() {
        this.view.getVtpTabber().showTab(2);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Combat"
     * panel.
     */
    public void showPnlCombat() {
        this.view.getVtpTabber().showTab(1);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Stack"
     * panel.
     */
    public void showPnlStack() {
        this.view.getVtpTabber().showTab(0);
    }

    /** Simple method that inits the mouse adapters for listeners,
     * here to simplify life in the constructor.
     */
    private void initMouseAdapters() {
        maMilling = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblMilling().toggleEnabled();
            }
        };

        maHand = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblHandView().toggleEnabled();
            }
        };

        maLibrary = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblLibraryView().toggleEnabled();
            }
        };

        maUnlimited = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblUnlimitedLands().toggleEnabled();
            }
        };

        maMana = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeGenerateMana();
            }
        };

        maSetup = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devSetupGameState();
            }
        };

        maTutor = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeTutor();
            }
        };

        maCounter = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeAddCounter();
            }
        };

        maTap = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeTapPerm();
            }
        };

        maUntap = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeUntapPerm();
            }
        };

        maLife = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeSetLife();
            }
        };
    }
}
