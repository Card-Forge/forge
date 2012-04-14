package forge.gui.framework;



/**
 * This interface provides a unifying type to any component
 * (usually JPanels or JScrollPanes) which could be used as
 * a tab. A single one of these components is referred to as
 * a "document" throughout the codebase.  The tabs and their
 * documents are contained in "cells" for resizing and dragging.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(V at beginning of class name denotes a view class.)</i>
 */
public interface IVDoc {
    /**
     * Returns the ID used to identify this tab in save XML and card layouts.
     * 
     * @return {@link forge.gui.framework.EDocID}
     */
    EDocID getDocumentID();

    /**
     * Returns tab label object used in title bars.
     * 
     * @return {@link forge.gui.framework.DragTab}
     */
    DragTab getTabLabel();

    /** Retrieves control object associated with this document.
     * @return {@link forge.gui.home.ICSubmenu}
     */
    ICDoc getControl();

    /** Sets the current parent cell of this view,
     * allowing access to its body and head sections.
     * 
     * @param cell0 &emsp; {@link forge.gui.framework.DragCell}
     */
    void setParentCell(DragCell cell0);

    /**
     * Gets parent cell for this view.
     * 
     * @return {@link forge.gui.framework.DragCell}
     */
    DragCell getParentCell();

    /**
     * Targets the drag cell body (use <code>parentCell.getBody()</code>).
     * Populates panel components, independent of constructor.
     * Expected to provide a completely fresh layout to the body panel.
     * <br><br>
     * Styling and adding of lower-level components for this view
     * should happen once, in constructor. This method is
     * only for removing / adding top-level components.
     * <br><br>
     * The body panel will be empty when this method is called.
     * However, its layout may need to be redefined as required.
     */
    void populate();
}
