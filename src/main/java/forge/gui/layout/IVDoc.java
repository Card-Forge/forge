package forge.gui.layout;

import java.awt.Component;

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
     * @return {@link forge.gui.layout.EDocID}
     */
    EDocID getDocumentID();

    /**
     * Returns top-level component containing all of the content in this tab.
     * This is used to attach the component to the card layout of its parent.
     * 
     * @return {@link java.awt.Component}
     */
    Component getDocument();

    /**
     * Returns tab label object used in title bars.
     * 
     * @return {@link forge.gui.layout.DragTab}
     */
    DragTab getTabLabel();

    /** Retrieves control object associated with this document.
     * @return {@link forge.gui.home.ICSubmenu}
     */
    ICDoc getControl();

    /**
     * Removes all children and (re)populates panel components,
     * independent of constructor.  Expected to provide
     * a completely fresh layout on the component.
     */
    void populate();
}
