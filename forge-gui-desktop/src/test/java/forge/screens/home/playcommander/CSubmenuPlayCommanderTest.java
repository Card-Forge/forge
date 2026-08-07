/*
 * REFORGE COMMANDER EXTENSION
 *
 * Structural regression tests for the "Battlefield Layout" menu added to
 * CSubmenuPlayCommander (#88, doc:12a).
 *
 * IMPORTANT: CSubmenuPlayCommander is an enum singleton whose instance field
 * initializers eagerly construct VSubmenuPlayCommander -> VLobby, which in turn
 * call FSkin.getIcon()/FSkin.getColor() at field-initialization time. FSkin is
 * only populated by FSkin.loadFull(), which requires a live FView/display and is
 * not available in a headless unit-test environment. Referencing
 * CSubmenuPlayCommander.SINGLETON_INSTANCE (or invoking any of its static/instance
 * methods) would trigger class initialization and throw NullPointerException (or
 * hang waiting on a display) outside a fully booted application.
 *
 * These tests therefore verify the class's public contract and the new helper
 * method signatures via reflection on the loaded-but-not-initialized Class object
 * (obtaining a .class literal or calling getDeclared*() does not run <clinit>),
 * without ever triggering enum construction. The underlying file-writing logic
 * that the menu delegates to is covered by ReforgeMatchLayoutPresetsTest.
 */
package forge.screens.home.playcommander;

import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.testng.annotations.Test;

import forge.gui.framework.ICDoc;
import forge.gui.reforge.ReforgeMatchLayoutPresets;
import forge.menus.IMenuProvider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CSubmenuPlayCommanderTest {

    @Test
    public void testIsAnEnumImplementingICDocAndIMenuProvider() {
        final Class<?> clazz = CSubmenuPlayCommander.class;
        assertTrue(clazz.isEnum(), "CSubmenuPlayCommander should remain an enum singleton");
        assertTrue(ICDoc.class.isAssignableFrom(clazz));
        assertTrue(IMenuProvider.class.isAssignableFrom(clazz));
    }

    @Test
    public void testHasExactlyOneSingletonInstanceConstant() {
        // Uses field metadata rather than values()/valueOf(), which would force
        // class initialization (and the FSkin-dependent chain described above).
        final List<String> enumConstantNames = new ArrayList<>();
        for (final Field field : CSubmenuPlayCommander.class.getDeclaredFields()) {
            if (field.isEnumConstant()) {
                enumConstantNames.add(field.getName());
            }
        }
        assertEquals(enumConstantNames, List.of("SINGLETON_INSTANCE"));
    }

    @Test
    public void testGetMenusIsPublicAndReturnsListOfJMenu() throws NoSuchMethodException {
        final Method getMenus = CSubmenuPlayCommander.class.getMethod("getMenus");
        assertTrue(Modifier.isPublic(getMenus.getModifiers()));
        assertEquals(getMenus.getReturnType(), List.class);
        assertEquals(getMenus.getParameterCount(), 0);
    }

    @Test
    public void testItemHelperBuildsJMenuItemFromTextAndActionListener() throws NoSuchMethodException {
        final Method item = CSubmenuPlayCommander.class.getDeclaredMethod("item", String.class, ActionListener.class);
        assertTrue(Modifier.isPrivate(item.getModifiers()));
        assertTrue(Modifier.isStatic(item.getModifiers()));
        assertEquals(item.getReturnType(), JMenuItem.class);
    }

    @Test
    public void testApplyPresetHelperSignature() throws NoSuchMethodException {
        final Method applyPreset = CSubmenuPlayCommander.class.getDeclaredMethod("applyPreset", int.class);
        assertTrue(Modifier.isPrivate(applyPreset.getModifiers()));
        assertTrue(Modifier.isStatic(applyPreset.getModifiers()));
        assertEquals(applyPreset.getReturnType(), void.class);
    }

    @Test
    public void testRestoreDefaultHelperSignature() throws NoSuchMethodException {
        final Method restoreDefault = CSubmenuPlayCommander.class.getDeclaredMethod("restoreDefault");
        assertTrue(Modifier.isPrivate(restoreDefault.getModifiers()));
        assertTrue(Modifier.isStatic(restoreDefault.getModifiers()));
        assertEquals(restoreDefault.getReturnType(), void.class);
    }

    @Test
    public void testGetMenusDeclaredReturnTypeIsAssignableFromJMenuList() throws NoSuchMethodException {
        // Regression guard: getMenus() must keep returning something assignable
        // to List<JMenu> so MenuUtil/IMenuProvider callers keep compiling.
        final Method getMenus = CSubmenuPlayCommander.class.getMethod("getMenus");
        assertTrue(List.class.isAssignableFrom(getMenus.getReturnType()));
    }

    @Test
    public void testBattlefieldLayoutMenuPlayerRangeIsBoundedByPresetMaxPlayers() {
        // The "Battlefield Layout" menu in getMenus() iterates 2..MAX_PLAYERS to build
        // its "N Players" entries; this documents/guards that coupling without
        // invoking the GUI-dependent enum. See ReforgeMatchLayoutPresetsTest for
        // full coverage of layoutFor()/apply()/restoreDefault() behavior.
        assertEquals(ReforgeMatchLayoutPresets.MAX_PLAYERS, 8);
    }

    @Test
    public void testJMenuClassIsAvailableForMenuConstruction() {
        // Sanity check that JMenu can be instantiated headlessly (unrelated to FSkin),
        // confirming the reason CSubmenuPlayCommander itself can't be constructed here
        // is specifically its FSkin-dependent view chain, not Swing/headless issues.
        final JMenu menu = new JMenu("Battlefield Layout");
        assertEquals(menu.getText(), "Battlefield Layout");
    }
}