package forge.control.match;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.view.match.ViewDock;
import forge.view.match.ViewDock.KeyboardShortcutField;

/** 
 * Child controller, handles dock button operations.
 *
 */
public class ControlDock {
    private ViewDock view;

    /**
     * Child controller, handles dock button operations.
     * @param v &emsp; ViewDock obj
     */
    public ControlDock(ViewDock v) {
        view = v;
    }

    /** Concede game, bring up WinLose UI. */
    public void concede() {
        AllZone.getHumanPlayer().concede();
        AllZone.getGameAction().checkStateEffects();
    }

    /** @return ViewDock */
    public ViewDock getView() {
        return view;
    }

    /** Updates and saves ForgePreferences with current shortcuts. */
    public void saveKeyboardShortcuts() {
        ForgePreferences fp = Singletons.getModel().getPreferences();
        Map<String, KeyboardShortcutField> shortcuts = view.getKeyboardShortcutFields();

        fp.setShowStackShortcut(shortcuts.get("showstack").getCodeString());
        fp.setShowCombatShortcut(shortcuts.get("showcombat").getCodeString());
        fp.setShowPlayersShortcut(shortcuts.get("showplayers").getCodeString());
        fp.setShowConsoleShortcut(shortcuts.get("showconsole").getCodeString());
        fp.setShowDevShortcut(shortcuts.get("showdev").getCodeString());
        fp.setConcedeShortcut(shortcuts.get("concede").getCodeString());
        fp.setShowPictureShortcut(shortcuts.get("showpicture").getCodeString());
        fp.setShowDetailShortcut(shortcuts.get("showdetail").getCodeString());

        try {
            fp.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AllZone.getOverlay().hideOverlay();
    }

    /**
     * - Adds keycode to list stored in name of a text field.
     * - Code is not added if already in list.
     * - Backspace removes last code in list.
     * - Sets text of text field with character equivalent of keycodes.
     * 
     * @param e &emsp; KeyEvent
     */
    public void addKeyCode(KeyEvent e) {
        KeyboardShortcutField ksf = (KeyboardShortcutField) e.getSource();
        String newCode = Integer.toString(e.getKeyCode());
        String codestring = ksf.getCodeString();
        List<String> existingCodes;

        if (codestring != null) {
            existingCodes = new ArrayList<String>(Arrays.asList(codestring.split(" ")));
        }
        else {
            existingCodes = new ArrayList<String>();
        }

        // Backspace (8) will remove last code from list.
        if (e.getKeyCode() == 8) {
            existingCodes.remove(existingCodes.size() - 1);
        }
        else if (!existingCodes.contains(newCode)) {
            existingCodes.add(newCode);
        }

        ksf.setCodeString(StringUtils.join(existingCodes, ' '));
    }

    /** */
    public void endTurn() {
        // Big thanks to you, Gameplay Guru, since I was too lazy to figure this out
        // before release.  Doublestrike 24-11-11
        System.err.println("forge.control.match > ControlDock > endTurn()");
        System.out.println("Should skip to the end of turn, or entire turn.");
        System.err.println("If some gameplay guru could implement this, that would be great...");
    }
}
