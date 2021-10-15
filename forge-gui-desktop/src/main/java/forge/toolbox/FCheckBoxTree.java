package forge.toolbox;

import net.miginfocom.swing.MigLayout;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;

/**
 * A custom JTree of FCheckBox items using Forge skin properties that allows Path selection.
 * The implementation details:
 * ===========================
 * 1) Custom `TreeCellRenderer` that renders a Tree node as a FCheckbox.
 *      1.1) When selected, the checkbox selection is changed instead of the label background and border.
 * 2) Replaced the `Selection Model` by a `DefaultTreeSelectionModel` overridden inline, that has empty implementation
 *      to override default selection mechanism.
 *
 * 3) New (custom) event type for checking of the checkboxes, i.e. `CheckChangeEvent`
 * 4) Inner HashSet of paths (i.e. TreePath) that helps retrieving the state of each node.
 *      4.1) A custom Data Object (i.e. `TreeNodeState`) is defined to embed the current state of each tree node
 *
 * <p>
 * based on code at
 * https://stackoverflow.com/questions/21847411/java-swing-need-a-good-quality-developed-jtree-with-checkboxes
 */
public class FCheckBoxTree extends JTree {

    // === FTreeNodeData ===
    /** Custom Data Class for each node in the Tree.
     * The Data class is pretty straightforward, and embeds three main properties:
     * --> label: that will be used by the TreeCellRenderer to label the checkbox rendering the node
     * --> value: the actual value stored in the NodeInfo to be collected and used afterwards. This must
     *            be any Comparable object.
     * --> key: a unique value identifying this node data entry (if not provided, the hashcode of value will
     *          be used by default). This property is mirrored by the FTreeNode instance encapsulating the data
     *          class to uniquely reference a node into the tree. Therefore, this key value should be
     *          passed in accordingly, with this in mind!
     */
    public static class FTreeNodeData implements Comparable<FTreeNodeData> {
        public Object key;
        public String label;
        public Comparable item;
        public boolean isEnabled = true;
        public boolean isSelected = false;

        public FTreeNodeData(Comparable value) {
            this(value, value.toString(), value.hashCode());
        }

        public FTreeNodeData(Comparable value, String label) {
            this(value, label, value.hashCode());
        }

        public FTreeNodeData(Comparable value, String name, Object key) {
            this.item = value;
            this.label = name;
            this.key = key;
        }

        public Object getKey(){ return this.key; }

        @Override
        public int hashCode(){
            return this.item.hashCode();
        }

        @Override
        public String toString() { return "FTreeNodeInfo["+this.label+", "+this.item.toString()+"]";}

        @Override
        public int compareTo(FTreeNodeData o) {
            return this.item.compareTo(o.item);
        }
    }

    // === FTreeNode ===
    /**
     * Custom TreeNode instance used as a proxy to handle recursive data structures
     * The custom class defines a bunch of helpers overloaded methods to ensure
     * data encapsulation and data types in custom JTree Model.
     */
    public static class FTreeNode extends DefaultMutableTreeNode {

        public FTreeNode(FTreeNodeData object){
            super(object, true);
        }

        // Helper Method to quickly add child nodes from a list of FTreeNodeInfo instances
        public void add(List<FTreeNodeData> nodesData){
            for (FTreeNodeData dataObject : nodesData)
                this.add(new FTreeNode(dataObject));
        }

        public void add(FTreeNodeData dataObject){
            this.add(new FTreeNode(dataObject));
        }

        public FTreeNodeData getUserObject(){
            return (FTreeNodeData) super.getUserObject();
        }

        @Override
        public String toString() { return "FTreeNode["+this.getUserObject().toString()+"]";}
        public Object getKey() { return this.getUserObject().getKey(); }
    }

    /**
     * Defining data structure that will enable to fast check-indicate the state of each node.
     * This class is central to properly handle the interaction with the component, and so
     * to recursively traverse the tree and update||query the status of each nested component.
      */
    private static class TreeNodeState {
        boolean isSelected;
        boolean isEnabled;
        int numberOfChildren;
        int selectedChildrenCount;
        int enabledChildrenCount;

        public TreeNodeState(boolean isSelected, boolean isEnabled, int numberOfChildren,
                             int selectedChildrenCount, int enabledChildrenCount) {
            this.isSelected = isSelected && isEnabled;
            this.isEnabled = isEnabled;
            this.numberOfChildren = numberOfChildren;
            this.selectedChildrenCount = selectedChildrenCount;
            this.enabledChildrenCount = enabledChildrenCount;
        }
        public boolean hasChildren() { return this.numberOfChildren > 0;}
        public boolean allChildrenSelected(){ return this.numberOfChildren == this.selectedChildrenCount; };
        public boolean allChildrenEnabled(){ return this.enabledChildrenCount == this.numberOfChildren; };
    }

    // == Fields of the FCheckboxTree class ==
    // =======================================
    FCheckBoxTree selfPointer = this;
    private static final String ROOTNODE_LABEL = "/";  // won't be displayed
    private Map<Object, FTreeNode> nodesSet = new HashMap<>();  // A map of all nodes in the tree (model)

    private HashMap<TreePath, TreeNodeState> treeNodesStates;
    private HashSet<TreePath> checkedPaths = new HashSet<>();
    private TreePath lastSelectedPath;  // the last path user interacted with (either checked, or unchecked)
    private TreePath lastCheckedPath;   // last path checked

    // == CONSTRUCTOR METHOD ==
    // ========================

    public FCheckBoxTree(){
        super(new FTreeNode(new FTreeNodeData(ROOTNODE_LABEL)));
        // Disabling toggling by double-click
        this.setToggleClickCount(0);
        // Replacing default TreeUI class to customise the Icons and Look and feel
        this.setUI(new FCheckBoxTreeUI());
        // Replacing default Cell Renderer
        FCheckBoxTreeCellRenderer cellRenderer = new FCheckBoxTreeCellRenderer();
        this.setCellRenderer(cellRenderer);

        // Replacing default selection model with an empty one
        DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel() {
            // Totally disabling the selection mechanism
            public void setSelectionPath(TreePath path) {}
            public void addSelectionPath(TreePath path) {}
            public void removeSelectionPath(TreePath path) {}
            public void setSelectionPaths(TreePath[] pPaths) {}
        };

        // Enabling Path Auto-selection Mechanism and Tree-Check on MouseClick
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath tp = selfPointer.getPathForLocation(e.getX(), e.getY());
                if (tp == null) {
                    return;
                }
                boolean enabledStatus = treeNodesStates.get(tp).isEnabled;
                // NOTE: this is PARAMOUNT IMPORTANT!
                // Checkbox selection will be inhibited when nodes are disabled!
                if (!enabledStatus)
                    return;
                boolean checkStatus = !treeNodesStates.get(tp).isSelected;
                setPathCheckStatus(tp, checkStatus);
            }
            @Override public void mousePressed(MouseEvent e) {}
            @Override public void mouseReleased(MouseEvent e) {}
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
        });
        this.setSelectionModel(dtsm);
        this.setRootVisible(false);
        this.setShowsRootHandles(true);
    }

    // == PUBLIC API METHODS ==
    // ========================

    public TreePath getLastSelectedPath(){ return this.lastSelectedPath; }
    public TreePath getLastCheckedBox(){ return this.lastCheckedPath; }

    @Override
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        initModelCheckState();
    }

    public TreePath[] getCheckedPaths() {
        return checkedPaths.toArray(new TreePath[checkedPaths.size()]);
    }

    /**
     * Returns all the values stored in each checked node.
     * Note: Values in FTreeNodeData are save as objects, therefore these array should
     * be casted afterwards, accordingly.
     * */
    public Object[] getCheckedValues(boolean leafNodesOnly) {
        ArrayList<Object> checkedValues = new ArrayList<>();
        for (TreePath tp : this.getCheckedPaths()){
            FTreeNode node = (FTreeNode) tp.getLastPathComponent();
            boolean getValueFromNode = (!leafNodesOnly) || node.isLeaf();
            if (getValueFromNode) {
                FTreeNodeData data = node.getUserObject();
                checkedValues.add(data.item);
            }
        }
        return checkedValues.toArray(new Object[checkedValues.size()]);
    }

    // Returns true in case that the node is selected, has children but not all of them are selected
    public boolean isSelectedPartially(TreePath path) {
        TreeNodeState cn = treeNodesStates.get(path);
        return cn.isEnabled && cn.isSelected && cn.hasChildren() && !cn.allChildrenSelected();
    }

    public void resetCheckingState() {
        treeNodesStates = new HashMap<>();
        checkedPaths = new HashSet<>();
        nodesSet = new HashMap<>();
        FTreeNode node = (FTreeNode) getModel().getRoot();
        if (node == null)
            return;
        addSubtreeToCheckingStateTracking(node, true);
    }

    public FTreeNode getNodeByKey(Object key){
        FTreeNode node = nodesSet.getOrDefault(key, null);
        if (node != null)
            return node;
        return nodesSet.getOrDefault(key.hashCode(), null);
    }

    public int getNumberOfActiveChildNodes(FTreeNode node){
        TreeNodeState cn = getTreeNodeState(node);
        if (cn != null)
            return cn.enabledChildrenCount;
        return -1;
    }

    public List<FTreeNode> getActiveChildNodes(FTreeNode parent){
        List<FTreeNode> activeChildren = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            FTreeNode childNode = (FTreeNode) parent.getChildAt(i);
            TreeNodeState cn = getTreeNodeState(childNode);
            if ((cn != null) && (cn.isEnabled))
                activeChildren.add(childNode);
        }
        return activeChildren;
    }

    public int getNumberOfSelectedChildNodes(FTreeNode node){
        TreeNodeState cn = getTreeNodeState(node);
        if (cn != null)
            return cn.selectedChildrenCount;
        return -1;
    }

    public void setNodeCheckStatus(FTreeNode node, boolean isChecked){
        TreeNode[] path = node.getPath();
        TreePath treePath = new TreePath(path);
        setPathCheckStatus(treePath, isChecked);
    }

    public void setNodeEnabledStatus(FTreeNode node, boolean isEnabled){
        TreeNode[] path = node.getPath();
        TreePath treePath = new TreePath(path);
        setPathEnableStatus(treePath, isEnabled);
    }

    /** Initialise a new TreeModel from the input Subtree represented as a Map of FTreeNodeInfo instances.
     * In particular, each key will be interpreted as sibling nodes, and directly attached to the main ROOT
     * (not displayed), whilst each FTreeNodeInfo in the corresponding lists will be treated as  as child leaf nodes.
     *
     */
    public void setTreeData(TreeMap<FTreeNodeData, List<FTreeNodeData>> nodesMap) {
        FTreeNode rootNode = new FTreeNode(new FTreeNodeData(FCheckBoxTree.ROOTNODE_LABEL));
        for (FTreeNodeData keyNodeInfo : nodesMap.keySet()) {
            FTreeNode keyNode = new FTreeNode(keyNodeInfo);
            rootNode.add(keyNode);
            for (FTreeNodeData childNodeInfo : nodesMap.get(keyNodeInfo))
                keyNode.add(childNodeInfo);
        }
        DefaultTreeModel defaultTreeModel = new DefaultTreeModel(rootNode);
        this.setModel(defaultTreeModel);
    }

    // Set an option in the cell rendered to enable or disable the visualisation of child nodes count
    public void showNodesCount(){
        this.setCellRenderer(new FCheckBoxTreeCellRenderer(true));
    }
    public void hideNodesCount(){
        this.setCellRenderer(new FCheckBoxTreeCellRenderer(false));
    }

    // == PRIVATE API METHODS ==
    // ========================

    private void initModelCheckState(){
        treeNodesStates = new HashMap<>();
        checkedPaths = new HashSet<>();
        nodesSet = new HashMap<>();
        FTreeNode node = (FTreeNode) getModel().getRoot();
        if (node == null || node.getChildCount() == 0)
            return;
        addSubtreeToCheckingStateTracking(node, false);
    }

    private void addSubtreeToCheckingStateTracking(FTreeNode node, boolean resetSelectState) {
        FTreeNode prevNode = nodesSet.put(node.getKey(), node);
        if (prevNode != null)
            throw new RuntimeException("Node " + node + "already present in Nodes Set (key:"+node.getKey()+")");
        TreeNode[] path = node.getPath();
        FTreeNodeData nodeData = node.getUserObject();
        boolean selectStatus = !resetSelectState && nodeData.isSelected;
        TreePath treePath = new TreePath(path);
        TreeNodeState nodeState = new TreeNodeState(selectStatus, nodeData.isEnabled, node.getChildCount(),
                                                    0, node.getChildCount());
        treeNodesStates.put(treePath, nodeState);
        TreePath lastChildNodePath = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            lastChildNodePath = treePath.pathByAddingChild(node.getChildAt(i));
            addSubtreeToCheckingStateTracking((FTreeNode) lastChildNodePath.getLastPathComponent(), resetSelectState);
        }
        if (lastChildNodePath != null)
            updatePredecessors(lastChildNodePath);
        else {
            // leafNode
            if (selectStatus)
                checkedPaths.add(treePath);
            else
                checkedPaths.remove(treePath);
        }
    }

    private void setPathCheckStatus(TreePath tp, boolean checkStatus) {
        setCheckedStatusOnTree(tp, checkStatus);
        updatePredecessors(tp);
        // Firing the check change event
        fireCheckChangeEvent(new TreeCheckChangeEvent(new Object()));
        // Repainting tree after the data structures were updated
        selfPointer.repaint();
    }

    private void setPathEnableStatus(TreePath tp, boolean enableStatus) {
        percolateEnabledStatusOnSubtree(tp, enableStatus);
        updatePredecessors(tp);
        // Firing the enabled change event
        fireEnabledChangeEvent(new TreeEnabledChangeEvent(new Object()));
        // Repainting tree after the data structures were updated
        selfPointer.repaint();
    }

    private TreeNodeState getTreeNodeState(FTreeNode node) {
        TreeNode[] path = node.getPath();
        TreePath treePath = new TreePath(path);
        return this.treeNodesStates.get(treePath);
    }

    protected boolean isRoot(TreePath tp){
        return (tp.getParentPath() == null);
    }

    // Whenever a node state changes, updates the inner state of ancestors accordingly
    protected void updatePredecessors(TreePath tp) {
        if (isRoot(tp))
            return;  // STOP recursion
        TreePath parentPath = tp.getParentPath();
        TreeNodeState parentTreeNodeState = treeNodesStates.get(parentPath);
        FTreeNode parentTreeNode = (FTreeNode) parentPath.getLastPathComponent();

        parentTreeNodeState.selectedChildrenCount = 0;
        parentTreeNodeState.enabledChildrenCount = 0;
        parentTreeNodeState.isSelected = false;
        parentTreeNodeState.isEnabled = true;
        for (int i = 0; i < parentTreeNode.getChildCount(); i++) {
            TreePath childPath = parentPath.pathByAddingChild(parentTreeNode.getChildAt(i));
            TreeNodeState childTreeNodeState = treeNodesStates.get(childPath);
            if (childTreeNodeState == null)
                continue;
            if (childTreeNodeState.isEnabled) {
                parentTreeNodeState.enabledChildrenCount += 1;
                if (childTreeNodeState.isSelected) {
                    parentTreeNodeState.selectedChildrenCount += 1;
                    parentTreeNodeState.isSelected = true;
                }
            }
        }

        if (parentTreeNodeState.enabledChildrenCount == 0)
            parentTreeNodeState.isEnabled = false;

        if (parentTreeNodeState.isSelected)
            checkedPaths.add(parentPath);
        else
            checkedPaths.remove(parentPath);

        // Go Up onto the ancestors hierarchy
        updatePredecessors(parentPath);
    }

    // This method is the one that should be used whenever a new state change event happens
    protected void setCheckedStatusOnTree(TreePath tp, boolean isChecked){
        this.lastSelectedPath = tp;
        if (isChecked)
            this.lastCheckedPath = tp;
        percolateCheckedStatusOnSubTree(tp, isChecked);
    }

    // Recursively checks/unchecks a subtree using DFS on the subtree induced by current node
    private void percolateCheckedStatusOnSubTree(TreePath tp, boolean isChecked) {
        TreeNodeState cn = treeNodesStates.get(tp);
        cn.isSelected = cn.isEnabled && isChecked;
        FTreeNode node = (FTreeNode) tp.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++)
            percolateCheckedStatusOnSubTree(tp.pathByAddingChild(node.getChildAt(i)), isChecked);
        cn.selectedChildrenCount = isChecked ? cn.enabledChildrenCount : 0;
        if (cn.isEnabled) {
            if (isChecked)
                checkedPaths.add(tp);
            else
                checkedPaths.remove(tp);
        }
    }

    private void percolateEnabledStatusOnSubtree(TreePath tp, boolean isEnabled){
        TreeNodeState cn = treeNodesStates.get(tp);
        cn.isEnabled = isEnabled;
        cn.isSelected = isEnabled && cn.isSelected;
        if (!cn.isSelected) {
            cn.selectedChildrenCount = 0;  // selection applies to all nodes in subtree, so we can safely set this.
            checkedPaths.remove(tp);
        }
        FTreeNode node = (FTreeNode) tp.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++)
            percolateEnabledStatusOnSubtree(tp.pathByAddingChild(node.getChildAt(i)), isEnabled);
    }

    // === CUSTOM CELL RENDERED ===
    // ============================
    // NOTE: This class ignores the original "selection" mechanism and determines the status
    // of a single (FCheckBox) node based on the "checked" property.
    private class FCheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
        FCheckBox checkBox;
        private final FSkin.SkinFont CHECKBOX_LABEL_FONT = FSkin.getFont(14);
        private final FSkin.SkinColor CHECKBOX_LABEL_COLOUR = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        private final Color CHECKBOX_SELECTED_LABEL_COLOUR = new Color(252, 226, 137);
        private final boolean displayNodesCount;

        public FCheckBoxTreeCellRenderer(boolean displayNodesCount) {
            super();
            this.setLayout(new MigLayout("insets 0, gap 0"));
            this.setBorder(null);
            this.setOpaque(false);
            this.checkBox = new FCheckBox();
            this.displayNodesCount = displayNodesCount;
            add(this.checkBox, "left, gaptop 2, w 250::450, h 20!");

        }

        public FCheckBoxTreeCellRenderer() {
            this(true);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            FTreeNode node = (FTreeNode) value;
            FTreeNodeData nodeInfo = node.getUserObject();
            TreePath tp = new TreePath(node.getPath());
            TreeNodeState cn = treeNodesStates.get(tp);
            if (cn == null)
                return this;
            this.checkBox.setEnabled(cn.isEnabled);
            this.checkBox.setSelected(cn.isSelected);
            String chkBoxTxt = nodeInfo.label;
            int disabledNodes = cn.numberOfChildren - cn.enabledChildrenCount;
            int totalActiveNodes = cn.numberOfChildren - disabledNodes;
            if (this.displayNodesCount && !node.isLeaf() && cn.numberOfChildren > 0 && totalActiveNodes > 0) {
                chkBoxTxt += String.format(" (%d/%d)", cn.selectedChildrenCount, totalActiveNodes);
            }
            this.checkBox.setText(chkBoxTxt);
            this.checkBox.setName(nodeInfo.item.toString());
            if (cn.isSelected) {
                this.checkBox.setForeground(CHECKBOX_SELECTED_LABEL_COLOUR);
            } else {
                this.checkBox.setForeground(CHECKBOX_LABEL_COLOUR);
            }
            this.checkBox.setFont(CHECKBOX_LABEL_FONT);
            return this;
        }
    }

    // === CUSTOM TREE UI ==
    // =====================
    // Note: This class rewrites icons for collapsed and expanded nodes with the same polygon
    // glyph used in ImageView. Also, no lines are drawn (neither vertical or horizontal)
    private static class FCheckBoxTreeUI extends BasicTreeUI {

        private final IconUIResource nodeCollapsedIcon;
        private final IconUIResource nodeExpandedIcon;

        public FCheckBoxTreeUI(){
            super();
            this.nodeCollapsedIcon = new IconUIResource(new NodeIcon(true));
            this.nodeExpandedIcon = new IconUIResource(new NodeIcon(false));
        }

        @Override
        protected void paintHorizontalLine(Graphics g,JComponent c,int y,int left,int right){}
        @Override
        protected void paintVerticalLine(Graphics g,JComponent c,int x,int top,int bottom){}
        @Override
        public Icon getCollapsedIcon(){ return this.nodeCollapsedIcon;}
        @Override
        public Icon getExpandedIcon(){ return this.nodeExpandedIcon; }

        static class NodeIcon implements Icon {

            private static final int SIZE = 9;
            private static final int HEADER_HEIGHT = 6;
            private static final int HEADER_GLYPH_WIDTH = 8;
            private final boolean isCollapsed;

            public NodeIcon(boolean isCollapsed) {
                this.isCollapsed = isCollapsed;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                final Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                FSkin.setGraphicsFont(g2d, FSkin.getFont());
                Polygon glyph = new Polygon();
                int offset = HEADER_GLYPH_WIDTH / 2 + 1;
                x += 4;
                y += HEADER_HEIGHT / 2;
                if (!this.isCollapsed) {
                    glyph.addPoint(x - offset + 2, y + offset - 1);
                    glyph.addPoint(x + offset, y + offset - 1);
                    glyph.addPoint(x + offset, y - offset + 1);
                } else {
                    y++;
                    glyph.addPoint(x, y - offset);
                    glyph.addPoint(x + offset, y);
                    glyph.addPoint(x, y + offset);
                }
                g2d.fill(glyph);
            }
            @Override public int getIconWidth() {
                return SIZE;
            }
            @Override public int getIconHeight() {
                return SIZE;
            }
        }

    }

    // === CUSTOM EVENT TYPE AND EVENT HANDLER ===
    // ===========================================

    // NEW EVENT TYPE
    protected EventListenerList listenerList = new EventListenerList();

    public static class TreeCheckChangeEvent extends EventObject {
        public TreeCheckChangeEvent(Object source) { super(source); }
    }

    // NEW Custom Event Listener for the new `CheckChangeEvent`, which is fired every time a check state of a
    // checkbox changes.
    interface CheckChangeEventListener extends EventListener {
        public void checkStateChanged(TreeCheckChangeEvent event);
    }

    public void addCheckChangeEventListener(CheckChangeEventListener listener) {
        if (listenerList == null)
            return;
        listenerList.add(CheckChangeEventListener.class, listener);
    }

    public void removeCheckChangeEventListener(CheckChangeEventListener listener) {
        if (listenerList == null)
            return;
        listenerList.remove(CheckChangeEventListener.class, listener);
    }

    void fireCheckChangeEvent(TreeCheckChangeEvent evt) {
        if (listenerList == null)
            return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == CheckChangeEventListener.class) {
                ((CheckChangeEventListener) listeners[i + 1]).checkStateChanged(evt);
            }
        }
    }

    public static class TreeEnabledChangeEvent extends EventObject {
        public TreeEnabledChangeEvent(Object source) { super(source); }
    }

    // NEW Custom Event Listener for the new `CheckChangeEvent`, which is fired every time a check state of a
    // checkbox changes.
    interface EnableChangeEventListener extends EventListener {
        public void enabledStateChanged(TreeEnabledChangeEvent event);
    }

    public void addEnableChangeEventListener(EnableChangeEventListener listener) {
        if (listenerList == null)
            return;
        listenerList.add(EnableChangeEventListener.class, listener);
    }

    public void removeEnableChangeEventListener(EnableChangeEventListener listener) {
        if (listenerList == null)
            return;
        listenerList.remove(EnableChangeEventListener.class, listener);
    }

    void fireEnabledChangeEvent(TreeEnabledChangeEvent evt) {
        if (listenerList == null)
            return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == CheckChangeEventListener.class) {
                ((EnableChangeEventListener) listeners[i + 1]).enabledStateChanged(evt);
            }
        }
    }
}