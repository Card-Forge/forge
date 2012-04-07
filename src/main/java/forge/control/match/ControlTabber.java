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
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;

import forge.AllZone;
import forge.MyObservable;
import forge.Singletons;
import forge.gui.GuiDisplayUtil;
import forge.properties.ForgePreferences.FPref;
import forge.view.match.ViewTabber;

/**
 * Controls the vertical tabber in sidebar used for viewing gameplay data:
 * stack, combat, etc.
 * 
 */
public class ControlTabber extends MyObservable {
    private final ViewTabber view;
    private final MouseListener madMilling, madUnlimited, madAddAnyCard,
        madMana, madSetup, madTutor, madCounter, madTap, madUntap, madLife;

    private Observer obsStack, obsLog;

    /** */
    public static final int STACK_PANEL = 0;
    /** */
    public static final int COMBAT_PANEL = 1;
    /** */
    public static final int LOG_PANEL = 2;
    /** */
    public static final int PLAYERS_PANEL = 3;
    /** */
    public static final int DEV_PANEL = 4;

    /**
     * Controls the vertical tabber in sidebar used for viewing gameplay data:
     * stack, combat, etc.
     * 
     * @param v
     *            &emsp; The tabber Swing component
     */
    public ControlTabber(final ViewTabber v) {
        this.view = v;

        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_MILLING_LOSS)) {
            this.view.getLblMilling().setEnabled(true);
        } else {
            this.view.getLblMilling().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND)) {
            this.view.getLblUnlimitedLands().setEnabled(true);
        } else {
            this.view.getLblUnlimitedLands().setEnabled(false);
        }

        // Observers and listeners
        obsStack = new Observer() { @Override
            public void update(final Observable a, final Object b) {
                ControlTabber.this.view.updateStack(); } };

        obsLog = new Observer() { @Override
            public void update(final Observable a, final Object b) {
                ControlTabber.this.view.updateConsole(); } };

        madMilling = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblMilling().toggleEnabled(); } };

        madUnlimited = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                ControlTabber.this.view.getLblUnlimitedLands().toggleEnabled(); } };

        madMana = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeGenerateMana(); } };

        madSetup = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devSetupGameState(); } };

        madTutor = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeTutor(); } };

        madAddAnyCard = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeAddAnyCard(); } };

        madCounter = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeAddCounter(); } };

        madTap = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeTapPerm(); } };

        madUntap = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeUntapPerm(); } };

        madLife = new MouseAdapter() { @Override
            public void mousePressed(final MouseEvent e) {
                GuiDisplayUtil.devModeSetLife(); } };

        addListeners();
        addObservers();
    }

    /** Adds observers to tabber. */
    public void addObservers() {
        AllZone.getStack().addObserver(obsStack);
        AllZone.getGameLog().addObserver(obsLog);
    }

    /** Adds listeners to various components in tabber. */
    public void addListeners() {
        // Milling enable toggle
        this.view.getLblMilling().removeMouseListener(madMilling);
        this.view.getLblMilling().addMouseListener(madMilling);

        // DevMode: Play unlimited land this turn toggle
        this.view.getLblUnlimitedLands().removeMouseListener(madUnlimited);
        this.view.getLblUnlimitedLands().addMouseListener(madUnlimited);

        // DevMode: Generate mana
        this.view.getLblGenerateMana().removeMouseListener(madMana);
        this.view.getLblGenerateMana().addMouseListener(madMana);

        // DevMode: Battlefield setup
        this.view.getLblSetupGame().removeMouseListener(madSetup);
        this.view.getLblSetupGame().addMouseListener(madSetup);

        // DevMode: Tutor for card
        this.view.getLblTutor().removeMouseListener(madTutor);
        this.view.getLblTutor().addMouseListener(madTutor);

        this.view.getAnyCard().removeMouseListener(madAddAnyCard);
        this.view.getAnyCard().addMouseListener(madAddAnyCard);

        // DevMode: Add counter to permanent
        this.view.getLblCounterPermanent().removeMouseListener(madCounter);
        this.view.getLblCounterPermanent().addMouseListener(madCounter);

        // DevMode: Tap permanent
        this.view.getLblTapPermanent().removeMouseListener(madTap);
        this.view.getLblTapPermanent().addMouseListener(madTap);

        // DevMode: Untap permanent
        this.view.getLblUntapPermanent().removeMouseListener(madUntap);
        this.view.getLblUntapPermanent().addMouseListener(madUntap);

        // DevMode: Set life
        this.view.getLblSetLife().removeMouseListener(madLife);
        this.view.getLblSetLife().addMouseListener(madLife);
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
        this.view.getVtpTabber().showTab(DEV_PANEL);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Players"
     * panel.
     */
    public void showPnlPlayers() {
        this.view.getVtpTabber().showTab(PLAYERS_PANEL);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Log"
     * panel.
     */
    public void showPnlGameLog() {
        this.view.getVtpTabber().showTab(LOG_PANEL);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Combat"
     * panel.
     */
    public void showPnlCombat() {
        this.view.getVtpTabber().showTab(COMBAT_PANEL);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Stack"
     * panel.
     */
    public void showPnlStack() {
        this.view.getVtpTabber().showTab(STACK_PANEL);
    }
}
