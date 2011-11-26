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
 * Controls the vertical tabber in sidebar used for
 * viewing gameplay data: stack, combat, etc.
 *
 */
public class ControlTabber extends MyObservable {
    private ViewTabber view;

    /** 
     * Controls the vertical tabber in sidebar used for
     * viewing gameplay data: stack, combat, etc.
     *
     * @param v &emsp; The tabber Swing component
     */
    public ControlTabber(ViewTabber v) {
        view = v;
        if (Singletons.getModel().getPreferences().isMillingLossCondition()) {
            view.getLblMilling().setEnabled(true);
        }
        else {
            view.getLblMilling().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getHandView()) {
            view.getLblHandView().setEnabled(true);
        }
        else {
            view.getLblHandView().setEnabled(false);
        }

        if (Singletons.getModel().getPreferences().getLibraryView()) {
            view.getLblLibraryView().setEnabled(true);
        }
        else {
            view.getLblLibraryView().setEnabled(false);
        }
    }

    /** Adds observers to tabber. */
    public void addObservers() {
        // Stack
        Observer o1 = new Observer() {
            public void update(final Observable a, final Object b) {
                view.updateStack();
            }
        };

        AllZone.getStack().addObserver(o1);
    }

    /** Adds listeners to various components in tabber. */
    public void addListeners() {
        // Milling enable toggle
        view.getLblMilling().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                view.getLblMilling().toggleEnabled();
            }
        });

        // View any hand toggle
        view.getLblHandView().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                view.getLblHandView().toggleEnabled();
            }
        });

        // DevMode: View any library toggle
        view.getLblLibraryView().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                view.getLblLibraryView().toggleEnabled();
            }
        });

        // DevMode: Play unlimited land this turn toggle
        view.getLblUnlimitedLands().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeUnlimitedLand();

                // TODO: Enable toggle for this (e.g. Unlimited land each turn: enabled)
                // Also must change enabled/disabled text in ViewTabber to reflect this.
                //view.getLblUnlimitedLands().toggleEnabled();
            }
        });

        // DevMode: Generate mana
        view.getLblGenerateMana().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeGenerateMana();
            }
        });

        // DevMode: Battlefield setup
        view.getLblSetupGame().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devSetupGameState();
            }
        });

        // DevMode: Tutor for card
        view.getLblTutor().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeTutor();
            }
        });

        // DevMode: Add counter to permanent
        view.getLblCounterPermanent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeAddCounter();
            }
        });

        // DevMode: Tap permanent
        view.getLblTapPermanent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeTapPerm();
            }
        });

        // DevMode: Untap permanent
        view.getLblUntapPermanent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeUntapPerm();
            }
        });

        // DevMode: Set human life
        view.getLblHumanLife().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GuiDisplayUtil.devModeSetLife();
            }
        });
    }

    /** @return ViewTabber */
    public ViewTabber getView() {
        return view;
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Dev" panel.
     */
    public void showPnlDev() {
        view.getVtpTabber().showTab(4);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Players" panel.
     */
    public void showPnlPlayers() {
        view.getVtpTabber().showTab(3);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Console" panel.
     */
    public void showPnlConsole() {
        view.getVtpTabber().showTab(2);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Combat" panel.
     */
    public void showPnlCombat() {
        view.getVtpTabber().showTab(1);
    }

    /**
     * Programatically forces card layout of sidebar tabber to show "Stack" panel.
     */
    public void showPnlStack() {
        view.getVtpTabber().showTab(0);
    }
}
