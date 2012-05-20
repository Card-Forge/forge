package forge.gui.deckeditor.views;

import javax.swing.JLabel;
import javax.swing.JTable;

/** 
 * Dictates methods needed for a class to act as a container for
 * a TableView deck editing component.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * 
 */
public interface ITableContainer {
    /**
     * Sets the table used for displaying cards in this
     * deck editor container.
     * 
     * @param tbl0 &emsp; {@link forge.gui.deckeditor.tables.TableView}
     */
     void setTableView(JTable tbl0);

     // Various card count total labels

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblTotal();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblBlack();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblBlue();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblGreen();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblRed();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblWhite();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblColorless();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblArtifact();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblEnchantment();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblCreature();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblSorcery();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblInstant();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblPlaneswalker();

     /** @return {@link javax.swing.JLabel} */
     JLabel getLblLand();
}
