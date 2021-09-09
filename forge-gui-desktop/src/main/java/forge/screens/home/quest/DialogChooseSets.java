package forge.screens.home.quest;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import forge.Singletons;
import forge.toolbox.*;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.gui.SOverlayUtils;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;
import forge.toolbox.FCheckBoxTree.FTreeNode;
import forge.toolbox.FCheckBoxTree.FTreeNodeData;

public class DialogChooseSets {

	private final List<String> selectedSets = new ArrayList<>();
	private boolean wantReprints = true;
	private Runnable okCallback;

	private final FCheckBox cbWantReprints = new FCheckBox(Localizer.getInstance().getMessage("lblDisplayRecentSetReprints"));
	private final FCheckBoxTree checkBoxTree = new FCheckBoxTree();

	public DialogChooseSets(Collection<String> preselectedSets, Collection<String> unselectableSets,
							boolean showWantReprintsCheckbox) {
		this(preselectedSets, unselectableSets, showWantReprintsCheckbox, false);
	}

	public DialogChooseSets(Collection<String> preselectedSets, Collection<String> unselectableSets,
							Collection<String> limitedSets, boolean showWantReprintsCheckbox) {
		this(preselectedSets, unselectableSets, limitedSets, showWantReprintsCheckbox, false);
	}

	public DialogChooseSets(Collection<String> preselectedSets, Collection<String> unselectableSets,
							boolean showWantReprintsCheckbox, boolean allowReprints) {
		this(preselectedSets, unselectableSets, null, showWantReprintsCheckbox, allowReprints);
	}

	public DialogChooseSets(Collection<String> preselectedSets, Collection<String> unselectableSets,
							Collection<String> limitedSets, boolean showWantReprintsCheckbox, boolean allowReprints) {

		if (limitedSets != null && limitedSets.size() == 0)
			limitedSets = null;

		// Sanitise input set lists to avoid any inconsistency
		// Sanitise Unselectable Sets, by checking with limitedSets (if any)
		if (unselectableSets != null && limitedSets != null){
			Set<String> blackList = new HashSet<>();
			for (String set: unselectableSets){
				if (!limitedSets.contains(set))
					blackList.add(set);
			}

			if (blackList.size() > 0){
				for (String setToRemove : blackList)
					unselectableSets.remove(setToRemove);
			}
		}
		// Sanitise Preselected Sets
		if (preselectedSets != null){
			Set<String> blackList = new HashSet<>();
			for (String set : preselectedSets){
				if (unselectableSets != null && unselectableSets.contains(set))
					blackList.add(set);
				if (limitedSets != null && !limitedSets.contains(set))
					blackList.add(set);
			}
			if (blackList.size() > 0){
				for (String setToRemove : blackList)
					preselectedSets.remove(setToRemove);
			}
		}

		// get the map of each editions per type
		Map<CardEdition.Type, List<CardEdition>> editionsTypeMap = FModel.getMagicDb().getEditionsTypeMap();

		/* Gather all the different types among preselected and unselectable sets (if any)
		   lists are of set codes (e.g. "2ED", edition.getCode())

		   NOTE: preselected SetTypes will be created as TreeSet as the ordering of Types will be used to
		   decide which type in the UI list of types should be selected, in case of multiple types
		   available in preselected types (e.g. expansion and core, core will be selected as it comes first) */
		Set<CardEdition.Type> preselectedTypes = null;
		if (preselectedSets != null){
			preselectedTypes = new TreeSet<>();
			for (String code: preselectedSets){
				CardEdition edition = FModel.getMagicDb().getCardEdition(code);
				if (edition != null)
					preselectedTypes.add(edition.getType());
			}
		}

		Set<CardEdition.Type> limitedSetTypes = null;
		if (limitedSets != null){
			limitedSetTypes = new TreeSet<>();
			for (String code: limitedSets){
				CardEdition edition = FModel.getMagicDb().getCardEdition(code);
				if (edition != null)
					limitedSetTypes.add(edition.getType());
			}
		}

		TreeMap<FTreeNodeData, List<FTreeNodeData>> editionTypeTreeData = new TreeMap<>();
		TreeMap<CardEdition.Type, Integer> allEditionTypes = new TreeMap<>();
		List<CardEdition> allCardEditions = new ArrayList<>();
		for (CardEdition.Type editionType : editionsTypeMap.keySet()) {
			List<CardEdition> editionsOfType = editionsTypeMap.get(editionType);
			if (editionsOfType.size() == 0)  // skip empty set types
				continue;
			if (limitedSetTypes != null && !limitedSetTypes.contains(editionType))
				continue;
			List<FTreeNodeData> editionPerTypeNodes = new ArrayList<>();
			allCardEditions.addAll(editionsOfType);
			int enabledEditionsOfTypeCounter = 0;
			for (CardEdition ce: editionsOfType){
				String code = ce.getCode();
				if (limitedSets != null && !limitedSets.contains(code))
					continue;
				boolean isSelected = null != preselectedSets && preselectedSets.contains(code);
				boolean isEnabled = null == unselectableSets || !unselectableSets.contains(code);
				FTreeNodeData editionNode = new FTreeNodeData(ce, ce.getName(), ce.getCode());
				editionNode.isEnabled = isEnabled;
				editionNode.isSelected = isSelected;
				if (isEnabled)
					enabledEditionsOfTypeCounter += 1;
				editionPerTypeNodes.add(editionNode);
			}
			editionTypeTreeData.put(new FTreeNodeData(editionType), editionPerTypeNodes);
			allEditionTypes.put(editionType, enabledEditionsOfTypeCounter);
		}
		this.checkBoxTree.setTreeData(editionTypeTreeData);

		// === 0. MAIN PANEL WINDOW ===
		// ===================================================================
		// Initialise UI
		FPanel mainDialogPanel = new FPanel(new MigLayout(
				String.format("insets 10, gap 5, center, wrap 2, w %d!", getMainDialogWidth())));
		mainDialogPanel.setOpaque(false);
		mainDialogPanel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

		// === 1. RANDOM SELECTION PANEL ===
		// ===================================================================
		JPanel randomSelectionPanel = new JPanel(new MigLayout("insets 10, gap 5, right, wrap 2"));
		randomSelectionPanel.setOpaque(false);

		// === 2. RANDOM OPTIONS PANEL ===
		// Setup components for the random selection panel.
		// NOTES: These components need to be defined first, as they will also be controlled by
		// format selection buttons (enabled/disabled accordingly).
		randomSelectionPanel.add(new FLabel.Builder().text(
				Localizer.getInstance().getMessage("lblSelectRandomSets")).fontSize(14)
				.fontStyle(Font.BOLD).build(), "h 40!, w 100%, center, span 2");
		FButton randomSelectionButton = new FButton(Localizer.getInstance().getMessage("lblRandomizeSets"));
		randomSelectionButton.setFont(FSkin.getBoldFont(13));
		randomSelectionButton.setEnabled(false);  // by default is not enabled

		// === SPINNER AND LABELS ===
		TreeMap<CardEdition.Type, FSpinner> spinnersEditionTypeMap = new TreeMap<>();
		TreeMap<CardEdition.Type, FLabel> labelsEditionTypeMap = new TreeMap<>();
		List<FSpinner> editionTypeSpinners = new ArrayList<>();
		for (CardEdition.Type editionType: allEditionTypes.keySet()) {
			int enabledEditionCount = allEditionTypes.get(editionType);

			FSpinner spinner = new FSpinner.Builder().initialValue(0).minValue(0).maxValue(enabledEditionCount).build();
			String labTxt = "<html>" + editionType.toString().replaceAll(" ", "<br>") + ": </html>";
			FLabel label = new FLabel.Builder().text(labTxt).fontSize(13).build();

			// Determine status of component
			if (enabledEditionCount == 0) {
				// No editions enabled meaning:
				// the edition type HAS extensions but none of them is enabled!
				spinner.setEnabled(false);
				label.setEnabled(false);
			}
			editionTypeSpinners.add(spinner);
			labelsEditionTypeMap.put(editionType, label);
			spinnersEditionTypeMap.put(editionType, spinner);
		}
		// == SPINNERS ACTION PERFORMED ==
		editionTypeSpinners.forEach(spinner -> {
			spinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					// As soon as the value of a spinner becomes different from zero,
					// enabled the random selection button.
					int spinValue = (int) spinner.getValue();
					if (spinValue > 0) {
						if (!randomSelectionButton.isEnabled())
							randomSelectionButton.setEnabled(true);
					} else {
						// Similarly, when all spinners are set to zero,
						// disable the random selection button
						boolean allZeros = true;
						for (FSpinner spin : editionTypeSpinners) {
							int value = (int) spin.getValue();
							if (value != 0) {
								allZeros = false;
								break;
							}
						}
						if (allZeros)
							randomSelectionButton.setEnabled(false);
					}
				}
			});
		});

		// == ADD SPINNERS AND LABELS TO THE PANEL ==
		JPanel typeFieldsPanel = null;
		randomSelectionPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2, center");
		randomSelectionPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("nlSelectRandomSets"))
				.fontSize(12).fontStyle(Font.ITALIC).build(), "w 80%!, h 22px!, gap 5 0 0 0, span 2, center");
		String pairPanelLayout = "wrap 2, w 30%";
		int componentIndex = 0;
		int pairPerPanel = 3;
		int panelCompsCount = 0;
		for (CardEdition.Type editionType : allEditionTypes.keySet()) {
			if (panelCompsCount == 0)
				typeFieldsPanel = new JPanel(new MigLayout("insets 5, wrap 3"));
			typeFieldsPanel.setOpaque(false);
			JPanel pairPanel = new JPanel(new MigLayout(pairPanelLayout));
			pairPanel.setOpaque(false);
			pairPanel.add(labelsEditionTypeMap.get(editionType), "w 100!, align left, span 1");
			pairPanel.add(spinnersEditionTypeMap.get(editionType), "w 45!, align right, span 1");
			typeFieldsPanel.add(pairPanel, "span 1, center, growx, h 50!");
			panelCompsCount += 1;
			componentIndex += 1;
			if ((panelCompsCount == pairPerPanel) || (componentIndex == editionTypeSpinners.size())) {
				// add panel to outer container if we ran out of space, or we are processing the last item
				randomSelectionPanel.add(typeFieldsPanel, "w 100%, span 2");
				panelCompsCount = 0; // reset counter for the new panel, in case
			}
		}
		randomSelectionPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2, gap 0");
		FButton clearSelectionButton = new FButton(Localizer.getInstance().getMessage("lblClearSelection"));
		clearSelectionButton.setFont(FSkin.getBoldFont(13));

		// == UPDATE RANDOM PANEL LAYOUT ==
		randomSelectionPanel.add(clearSelectionButton, "gaptop 15, w 40%, h 26!, center");
		randomSelectionPanel.add(randomSelectionButton, "gaptop 15, w 40%, h 26!, center");
		if (showWantReprintsCheckbox) {
			cbWantReprints.setSelected(allowReprints);
			randomSelectionPanel.add(cbWantReprints, "gaptop 10, left, span, wrap");
		}

		// === 2. OPTIONS PANEL ===
		// ===================================================================
		JPanel optionsPanel = new JPanel(new MigLayout("insets 10, gap 5, center, wrap 2"));
		optionsPanel.setOpaque(false);

		// === 2. FORMAT OPTIONS PANEL ===
		// This will include a button for each format and a NO-Format Radio Button (default)
		JPanel formatOptionsPanel = new JPanel(new MigLayout("insets 10, gap 25 5, center"));
		formatOptionsPanel.setOpaque(false);
		formatOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblFormatRestrictions") + ":")
				.fontSize(14).fontStyle(Font.BOLD).build(), "span 1");

		ButtonGroup formatButtonGroup = new ButtonGroup();
		List<GameFormat> gameFormats = new ArrayList<>();
		FModel.getFormats().getSanctionedList().forEach(gameFormats::add);
		Map<String, FRadioButton> formatButtonGroupMap = new HashMap<>();
		gameFormats.forEach(item -> {
			FRadioButton button = new FRadioButton(item.getName());
			button.setActionCommand(item.getName());
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					/* Whenever a Format button will be pressed, the status of the UI will be
					   updated accordingly.
					   In particular, for each format, the number of allowed editions will be retrieved.
					   (EMPTY LIST in case of NO RESTRICTIONS).
					*/
					List<String> allowedSetCodes = item.getAllowedSetCodes();
					/* A. NO RESTRICTIONS:
					   -------------------
					   All the components will be enabled, namely:
					   - all nodes in the checkbox tree;
					   - all spinners are enabled and their maximum value updated accordingly from the Tree status
					*/
					if (allowedSetCodes.size() == 0) {
						for (CardEdition ce : allCardEditions) {
							String code = ce.getCode();
							FTreeNode node = checkBoxTree.getNodeByKey(code);
							if (node != null)
								checkBoxTree.setNodeEnabledStatus(node, true);
						}
						for (CardEdition.Type editionType : allEditionTypes.keySet()) {
							int numberOfEnabledEditions = allEditionTypes.get(editionType);
							if (numberOfEnabledEditions == 0)
								// This component will remain disabled, no matter the format selected
								continue;
							FSpinner spinner = spinnersEditionTypeMap.get(editionType);
							FLabel label = labelsEditionTypeMap.get(editionType);
							spinner.setEnabled(true);
							label.setEnabled(true);
							FTreeNode node = checkBoxTree.getNodeByKey(editionType);
							if (node != null){
								int maxValue = checkBoxTree.getNumberOfActiveChildNodes(node);
								int currentValue = (int) spinner.getValue();
								spinner.setValue(Math.min(currentValue, maxValue));
								SpinnerNumberModel m = (SpinnerNumberModel) spinner.getModel();
								m.setMaximum(maxValue);
							} else {
								spinner.setValue(0);
							}
						}
						return;
					}
					/* B. FORMAT RESTRICTIONS:
					   -----------------------
					   All components matching with **allowed** editions will be ENABLED.
					   This includes:
					   - nodes in the checkbox tree;
					   - spinners (along with their corresponding MAX values as returned from Tree status).
					   All components matching with the **BLACK LIST** of editions will be DISABLED
					   (Same as in the previous case).
					*/
					List<String> codesToDisable = new ArrayList<>();
					Set<CardEdition.Type> typesToDisable = new HashSet<>();
					Set<CardEdition.Type> allowedTypes = new HashSet<>();
					for (CardEdition ce : allCardEditions) {
						String code = ce.getCode();
						if (unselectableSets != null && unselectableSets.contains(code))
							continue;
						if (!allowedSetCodes.contains(code)) {
							codesToDisable.add(code);
							typesToDisable.add(ce.getType());
						} else {
							allowedTypes.add(ce.getType());
						}
					}
					// NOTE: We need to distinguish CardEdition.Type not having any actual CardEdition
					// in the allowed sets (i.e. to be completely disabled) from those still
					// having partial sets to be allowed.
					// The latter will result in adjusted maxValues of the corresponding spinner,
					// as well as their current value, when necessary.
					typesToDisable.removeAll(allowedTypes);

					// == Update Checkbox Tree ==
					for (String code : codesToDisable) {
						FTreeNode node = checkBoxTree.getNodeByKey(code);
						if (node != null)
							checkBoxTree.setNodeEnabledStatus(node, false);
					}
					for (String code : allowedSetCodes) {
						FTreeNode node = checkBoxTree.getNodeByKey(code);
						if (node != null)
							checkBoxTree.setNodeEnabledStatus(node, true);
					}
					// == update spinners ==
					for (CardEdition.Type editionType : typesToDisable) {
						FSpinner spinner = spinnersEditionTypeMap.get(editionType);
						FLabel label = labelsEditionTypeMap.get(editionType);
						spinner.setEnabled(false);
						spinner.setValue(0);
						label.setEnabled(false);
					}
					for (CardEdition.Type editionType : allowedTypes) {
						if (allEditionTypes.get(editionType) == 0)
							continue;
						FLabel label = labelsEditionTypeMap.get(editionType);
						label.setEnabled(true);
						FSpinner spinner = spinnersEditionTypeMap.get(editionType);
						spinner.setEnabled(true);
						FTreeNode node = checkBoxTree.getNodeByKey(editionType);
						if (node != null){
							int maxValue = checkBoxTree.getNumberOfActiveChildNodes(node);
							int currentValue = (int) spinner.getValue();
							spinner.setValue(Math.min(currentValue, maxValue));
							SpinnerNumberModel m = (SpinnerNumberModel) spinner.getModel();
							m.setMaximum(maxValue);
						} else {
							spinner.setValue(0);
						}
					}
				}
			});
			formatButtonGroup.add(button);
			formatOptionsPanel.add(button);
			formatButtonGroupMap.put(item.getName(), button);
		});

		// NO FORMAT Button
		FRadioButton noFormatSelectionButton = new FRadioButton(Localizer.getInstance().getMessage("lblNoFormatRestriction"));
		noFormatSelectionButton.setActionCommand("No Format");
		noFormatSelectionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (CardEdition ce: allCardEditions){
					String code = ce.getCode();
					FTreeNode node = checkBoxTree.getNodeByKey(code);
					if (node != null)
						checkBoxTree.setNodeEnabledStatus(node, true);
				}
				for (CardEdition.Type editionType : allEditionTypes.keySet()) {
					if (allEditionTypes.get(editionType) == 0)
						// This component will remain disabled, no matter the format selected
						continue;
					FSpinner spinner = spinnersEditionTypeMap.get(editionType);
					FLabel label = labelsEditionTypeMap.get(editionType);
					spinner.setEnabled(true);
					label.setEnabled(true);
					FTreeNode node = checkBoxTree.getNodeByKey(editionType);
					if (node != null){
						int maxValue = checkBoxTree.getNumberOfActiveChildNodes(node);
						int currentValue = (int) spinner.getValue();
						spinner.setValue(Math.min(currentValue, maxValue));
						SpinnerNumberModel m = (SpinnerNumberModel) spinner.getModel();
						m.setMaximum(maxValue);
					} else {
						spinner.setValue(0);
					}
				}
			}
		});
		formatButtonGroup.add(noFormatSelectionButton);
		formatOptionsPanel.add(noFormatSelectionButton);
		formatButtonGroupMap.put("No Format", noFormatSelectionButton);
		noFormatSelectionButton.setSelected(true);

		// === Update Option Panel ===
		optionsPanel.add(formatOptionsPanel, "span 2, w 100%");
		optionsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2");

		// === EDITION (PER TYPE) SELECTION PANEL ===
		// Selected Editions Panel
		JPanel editionSelectionPanel = new JPanel(new MigLayout("insets 10, gap 25 5, wrap 1, align left"));
		editionSelectionPanel.setOpaque(false);
		editionSelectionPanel.add(new FLabel.Builder().text(
				Localizer.getInstance().getMessage("lblCardEditionTypeList")).fontSize(14)
				.fontStyle(Font.BOLD).build(), "h 40!, w 100%, center, span 1");
		this.checkBoxTree.setOpaque(false);
		FScrollPane selectionScroller = new FScrollPane(checkBoxTree, true);
		editionSelectionPanel.add(selectionScroller, "span 1, w 100%");

		// ======== ADD ACTION LISTENERS TO CLEAR AND RANDOM SELECT BUTTONS
		clearSelectionButton.addActionListener(actionEvent -> {
			this.checkBoxTree.resetCheckingState();
			allEditionTypes.forEach((editionType, count) -> {
				if (count == 0)
					return;
				FSpinner spinner = spinnersEditionTypeMap.get(editionType);
				FLabel label = labelsEditionTypeMap.get(editionType);
				spinner.setValue(0);
				spinner.setEnabled(true);
				label.setEnabled(true);
			});
			noFormatSelectionButton.setSelected(true);
			cbWantReprints.setSelected(false);
			mainDialogPanel.repaintSelf();
		});
		randomSelectionButton.addActionListener(actionEvent -> {
			Map<CardEdition.Type, Integer> countPerEditionType = new HashMap<>();
			for (CardEdition.Type editionType: allEditionTypes.keySet()){
				if (allEditionTypes.get(editionType) == 0)
					continue;
				FSpinner spinner = spinnersEditionTypeMap.get(editionType);
				if (!spinner.isEnabled())
					continue;
				int value = (int) spinner.getValue();
				if (value > 0)
					countPerEditionType.put(editionType, value);
			}
			// We can safely reset selections as this button would not be enabled at all
			// if at least one spinner has been modified, and so countPerEdition updated.
			checkBoxTree.resetCheckingState();
			String selectedFormat = formatButtonGroup.getSelection().getActionCommand();
			FRadioButton formatButton = formatButtonGroupMap.get(selectedFormat);
			formatButton.doClick();
			for (CardEdition.Type editionType : countPerEditionType.keySet()){
				int totalToSelect = countPerEditionType.get(editionType);
				FTreeNode setTypeNode = checkBoxTree.getNodeByKey(editionType);
				if (setTypeNode != null){
					List<FTreeNode> activeChildNodes = checkBoxTree.getActiveChildNodes(setTypeNode);
					Collections.shuffle(activeChildNodes);
					for (int i = 0; i < totalToSelect; i++)
						checkBoxTree.setNodeCheckStatus(activeChildNodes.get(i), true);
				}
			}
			mainDialogPanel.repaintSelf();
		});

		// ===================================================================

		mainDialogPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblChooseSets"))
				.fontSize(20).build(), "center, span, wrap, gaptop 10");
		mainDialogPanel.add(editionSelectionPanel, "aligny top, w 50%, span 1");
		mainDialogPanel.add(randomSelectionPanel, "aligny top, w 50%, span 1");
		mainDialogPanel.add(optionsPanel, "center, w 100, span 2");

		final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
		overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));

		final Runnable cleanup = new Runnable() {
			@Override
			public void run() {
				SOverlayUtils.hideOverlay();
			}
		};

		FButton btnOk = new FButton(Localizer.getInstance().getMessage("lblOK"));
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				cleanup.run();
				handleOk();
			}
		});

		FButton btnCancel = new FButton(Localizer.getInstance().getMessage("lblCancel"));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cleanup.run();
			}
		});

		JPanel southPanel = new JPanel(new MigLayout("insets 10, gap 30, ax center"));
		southPanel.setOpaque(false);
		southPanel.add(btnOk, "center, w 250!, h 30!");
		southPanel.add(btnCancel, "center, w 250!, h 30!");

		mainDialogPanel.add(southPanel, "dock south, gapBottom 10");

		overlay.add(mainDialogPanel);
		mainDialogPanel.getRootPane().setDefaultButton(btnOk);
		SOverlayUtils.showOverlay();
	}

	private int getMainDialogWidth() {
		int winWidth = Singletons.getView().getFrame().getSize().width;
		int[] sizeBoundaries = new int[] {800, 1024, 1280, 2048};
		return calculateRelativePanelDimension(winWidth, 90, sizeBoundaries);
	}

	// So far, not yet used, but left here just in case
	private int getMainDialogHeight() {
		int winHeight = Singletons.getView().getFrame().getSize().height;
		int[] sizeBoundaries = new int[] {600, 720, 780, 1024};
		return calculateRelativePanelDimension(winHeight, 40, sizeBoundaries);
	}

	private int calculateRelativePanelDimension(int winDim, int ratio, int[] sizeBoundaries){
		int relativeWinDimension = winDim * ratio / 100;
		if (winDim < sizeBoundaries[0])
			return relativeWinDimension;
		for (int i = 1; i < sizeBoundaries.length; i++){
			int left = sizeBoundaries[i-1];
			int right = sizeBoundaries[i];
			if (winDim <= left || winDim > right)
				continue;
			return Math.min(right*90/100, relativeWinDimension);
		}
		return sizeBoundaries[sizeBoundaries.length - 1] * 90 / 100;  // Max Size fixed
	}


	public void setOkCallback(Runnable onOk) {
		okCallback = onOk;
	}

	public List<String> getSelectedSets() {
		return selectedSets;
	}

	public boolean getWantReprints() {
		return wantReprints;
	}

	private void handleOk() {
		Object[] checkedValues = this.checkBoxTree.getCheckedValues(true);
		for (Object data: checkedValues){
			CardEdition edition = (CardEdition) data;
			selectedSets.add(edition.getCode());
		}
		wantReprints = cbWantReprints.isSelected();

		if (null != okCallback) {
			okCallback.run();
		}
	}
}
