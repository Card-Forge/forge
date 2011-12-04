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

        AllZone.getStack().addObserver(o1);
        AllZone.getGameLog().addObserver(o2);
    }

    /** Adds listeners to various components in tabber. */
    public void addListeners() {
        // Milling enable toggle
        this.view.getLblMilling().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblMilling().toggleEnabled();
            }
        });

        // View any hand toggle
        this.view.getLblHandView().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblHandView().toggleEnabled();
            }
        });

        // DevMode: View any library toggle
        this.view.getLblLibraryView().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblLibraryView().toggleEnabled();
            }
        });

        // DevMode: Play unlimited land this turn toggle
        this.view.getLblUnlimitedLands().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeUnlimitedLand();

                // TODO: Enable toggle for this (e.g. Unlimited land each turn:
                // enabled)
                // Also must change enabled/disabled text in ViewTabber to
                // reflect this.
                // view.getLblUnlimitedLands().toggleEnabled();
            }
        });

        // DevMode: Generate mana
        this.view.getLblGenerateMana().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeGenerateMana();
            }
        });

        // DevMode: Battlefield setup
        this.view.getLblSetupGame().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devSetupGameState();
            }
        });

        // DevMode: Tutor for card
        this.view.getLblTutor().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeTutor();
            }
        });

        // DevMode: Add counter to permanent
        this.view.getLblCounterPermanent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeAddCounter();
            }
        });

        // DevMode: Tap permanent
        this.view.getLblTapPermanent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeTapPerm();
            }
        });

        // DevMode: Untap permanent
        this.view.getLblUntapPermanent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeUntapPerm();
            }
        });

        // DevMode: Set human life
        this.view.getLblHumanLife().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeSetLife();
            }
        });
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
}
