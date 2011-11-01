/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * Cyclic focus traversal policy based on array of components.
 * <p>
 * This class may be freely distributed as part of any application or plugin.
 * 
 * @author scheglov_ke
 */
public class FocusTraversalOnArray extends FocusTraversalPolicy {
    private final Component[] mComponents;

    // //////////////////////////////////////////////////////////////////////////
    //
    // Constructor
    //
    // //////////////////////////////////////////////////////////////////////////
    /**
     * 
     * FocusTraversalOnArray.
     * @param components a Component
     */
    public FocusTraversalOnArray(final Component[] components) {
        mComponents = components;
    }

    // //////////////////////////////////////////////////////////////////////////
    //
    // Utilities
    //
    // //////////////////////////////////////////////////////////////////////////
    private int indexCycle(final int index, final int delta) {
        int size = mComponents.length;
        int next = (index + delta + size) % size;
        return next;
    }

    private Component cycle(final Component currentComponent, final int delta) {
        int index = -1;
        loop: for (int i = 0; i < mComponents.length; i++) {
            Component component = mComponents[i];
            for (Component c = currentComponent; c != null; c = c.getParent()) {
                if (component == c) {
                    index = i;
                    break loop;
                }
            }
        }
        // try to find enabled component in "delta" direction
        int initialIndex = index;
        while (true) {
            int newIndex = indexCycle(index, delta);
            if (newIndex == initialIndex) {
                break;
            }
            index = newIndex;
            //
            Component component = mComponents[newIndex];
            if (component.isEnabled() && component.isVisible() && component.isFocusable()) {
                return component;
            }
        }
        // not found
        return currentComponent;
    }

    // //////////////////////////////////////////////////////////////////////////
    //
    // FocusTraversalPolicy
    //
    // //////////////////////////////////////////////////////////////////////////
    /**
     * @param container a Container
     * @param component a Component
     * @return Component
     */
    public final Component getComponentAfter(final Container container, final Component component) {
        return cycle(component, 1);
    }

    /**
     * @param container a Container
     * @param component a Component
     * @return Component
     */
    public final Component getComponentBefore(final Container container, final Component component) {
        return cycle(component, -1);
    }

    /**
     * @param container a Container
     * @return Component
     */
    public final Component getFirstComponent(final Container container) {
        return mComponents[0];
    }

    /**
     * @param container a Container
     * @return Component
     */
    public final Component getLastComponent(final Container container) {
        return mComponents[mComponents.length - 1];
    }

    /**
     * @param container a Container
     * @return Component
     */
    public final Component getDefaultComponent(final Container container) {
        return getFirstComponent(container);
    }
}
