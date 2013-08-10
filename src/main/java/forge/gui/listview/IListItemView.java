package forge.gui.listview;

import javax.swing.JTable;

import forge.gui.listview.SListViewUtil;
import forge.gui.toolbox.FLabel;

/** 
 * Dictates methods needed for a class to act as a container for
 * a EditorTableView deck editing component.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * 
 */
public interface IListItemView {
    /**
     * Sets the table used for displaying cards in this
     * deck editor container.
     * 
     * @param tbl0 &emsp; {@link forge.gui.listview.ListView}
     */
     void setTableView(JTable tbl0);

     FLabel getStatLabel(SListViewUtil.StatTypes s);
 }
