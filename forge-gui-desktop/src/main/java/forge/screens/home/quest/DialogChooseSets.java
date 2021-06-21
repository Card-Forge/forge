package forge.screens.home.quest;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.gui.SOverlayUtils;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FCheckBoxList;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FRadioButton;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextField;
import forge.util.Localizer;
import forge.util.TextUtil;
import net.miginfocom.swing.MigLayout;

public class DialogChooseSets {

	private final List<String> selectedSets = new ArrayList<>();
	private boolean wantReprints = true;
	private Runnable okCallback;

	private final Map<CardEdition.Type, List<FCheckBox>> selectedCheckBoxes = new TreeMap<>();
	private final FCheckBox cbWantReprints = new FCheckBox(Localizer.getInstance().getMessage("lblDisplayRecentSetReprints"));

	public DialogChooseSets(Collection<String> preselectedSets, Collection<String> unselectableSets, boolean showWantReprintsCheckbox) {

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
				preselectedTypes.add(edition.getType());
			}
		}
		Set<CardEdition.Type> unSelectableTypes = null;
		if (unselectableSets != null){
			unSelectableTypes = new HashSet<>();
			for (String code: unselectableSets){
				CardEdition edition = FModel.getMagicDb().getCardEdition(code);
				unSelectableTypes.add(edition.getType());
			}
		}

		// Create the map of Edition Checkboxes, organised per type (label) for immediate access.
		/* NOTE: Here the (String) label representation of CardEdition.Type will be used as keys
				 (i.e. Title case and no underscore) instead of the actual enum instance.
				 This is to simplify the retrieval of checkboxes for each edition type, since the very same labels will
				 be also used in the UI for selection (see the `editionTypes` ArrayList definition below).*/
		Map<String, List<FCheckBox>> editionsCheckBoxes = new HashMap<>();
		// Store the list of all Edition Types available to be added in the UI panel
		List<String> editionTypes = new ArrayList<>();  // List of all EditionTypes

		for (CardEdition.Type editionType : editionsTypeMap.keySet()) {
			if (null != unSelectableTypes && unSelectableTypes.contains(editionType))  // skip unselectable types
				continue;
			List<CardEdition> editionsOfType = editionsTypeMap.get(editionType);
			if (editionsOfType.size() == 0)  // skip empty set types
				continue;
			String editionTypeLabel = editionType.toString();
			editionTypes.add(editionTypeLabel);

			// initialise map of selected checkboxes
			selectedCheckBoxes.put(editionType, new ArrayList<>());
			List<FCheckBox> edTypeCheckBoxes = new ArrayList<>();
			for (CardEdition ce: editionsOfType){
				String code = ce.getCode();
				FCheckBox edBox = new FCheckBox(TextUtil.concatWithSpace(ce.getName(), TextUtil.enclosedParen(code)));
				edBox.setName(code);
				// set the status of UI components
				boolean isSelected = null != preselectedSets && preselectedSets.contains(code);
				edBox.setSelected(isSelected);
				edBox.setEnabled(null == unselectableSets || !unselectableSets.contains(code));
				edTypeCheckBoxes.add(edBox);
				if (isSelected)  // also add to the list of selected boxes
					selectedCheckBoxes.get(editionType).add(edBox);
			}
			editionsCheckBoxes.put(editionTypeLabel, edTypeCheckBoxes);
		}
		// Initialise UI - MAIN PANEL WINDOW
		FPanel panel = new FPanel(new MigLayout("insets 10, gap 5, center, wrap 3"));
		panel.setOpaque(false);
		panel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

		// === BOTTOM OPTIONS PANEL ===
		JPanel optionsPanel = new JPanel(new MigLayout("insets 10, gap 5, center, wrap 3"));
		optionsPanel.setVisible(false);
		optionsPanel.setOpaque(false);
		optionsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2, growx");
		optionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSelectRandomSets"))
				.fontSize(17).fontStyle(Font.BOLD).build(), "h 40!, span 2");

		// a. Left Options
		// TO BE DECIDED on these three
		FTextField coreField = new FTextField.Builder().text("0").maxLength(3).build();
		FTextField expansionField = new FTextField.Builder().text("0").maxLength(3).build();
		FTextField otherField = new FTextField.Builder().text("0").maxLength(3).build();

		JPanel leftOptionsPanel = new JPanel(new MigLayout("insets 10, gap 5, center, wrap 3"));
		leftOptionsPanel.setOpaque(false);
		leftOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSelectNumber") + ":")
				.fontSize(14).fontStyle(Font.BOLD).build(), " span 2");

		leftOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblCore") + ":").build());
		leftOptionsPanel.add(coreField, "w 40!");

		leftOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblExpansion") + ":").build());
		leftOptionsPanel.add(expansionField, "w 40!");

		leftOptionsPanel.add(new FLabel.Builder().text("Other:").build());
		leftOptionsPanel.add(otherField, "w 40!");

		// b. Right Options
		JPanel rightOptionsPanel = new JPanel(new MigLayout("insets 10, gap 25 5, center, wrap 3"));
		rightOptionsPanel.setOpaque(false);
		rightOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblFormatRestrictions") +":")
				.fontSize(14).fontStyle(Font.BOLD).build(), "span 2");

		// Format Options
		ButtonGroup formatButtonGroup = new ButtonGroup();
		List<GameFormat> gameFormats = new ArrayList<>();
		FModel.getFormats().getSanctionedList().forEach(gameFormats::add);

		// TODO: Automate game formats using appropriate Enum?
		gameFormats.forEach(item -> {
			if (item.getName().equals("Legacy")) {
				FRadioButton button = new FRadioButton(Localizer.getInstance().getMessage("lblLegacyOrVintage"));
				button.setActionCommand(item.getName());
				formatButtonGroup.add(button);
				rightOptionsPanel.add(button);
			} else if (!item.getName().equals("Vintage")) {
				FRadioButton button = new FRadioButton(item.getName());
				button.setActionCommand(item.getName());
				formatButtonGroup.add(button);
				rightOptionsPanel.add(button);
			}
		});

		// TODO: Remember to cross check what's the effect of this button
		FRadioButton button = new FRadioButton(Localizer.getInstance().getMessage("lblModernCardFrame"));
		button.setActionCommand("Modern Card Frame");
		formatButtonGroup.add(button);
		rightOptionsPanel.add(button);

		FRadioButton noFormatSelectionButton = new FRadioButton(Localizer.getInstance().getMessage("lblNoFormatRestriction"));
		noFormatSelectionButton.setActionCommand("No Format Restriction");
		formatButtonGroup.add(noFormatSelectionButton);
		rightOptionsPanel.add(noFormatSelectionButton);
		noFormatSelectionButton.setSelected(true);

		optionsPanel.add(leftOptionsPanel, "w 33%:40%:78%");
		optionsPanel.add(rightOptionsPanel, "w 33%:60%:78%");

		// FIXME: this button is so far ineffective
		FButton randomSelectionButton = new FButton(Localizer.getInstance().getMessage("lblRandomizeSets"));
//		randomSelectionButton.addActionListener(actionEvent -> {
//
//			int numberOfCoreSets = Integer.parseInt(coreField.getText());
//			int numberOfExpansionSets = Integer.parseInt(expansionField.getText());
//			int numberOfOtherSets = Integer.parseInt(otherField.getText());
//			int numberOfCustomeSets = 0;
//
//			for (FCheckBox coreSet : coreSets) {
//				coreSet.setSelected(false);
//			}
//			for (FCheckBox expansionSet : expansionSets) {
//				expansionSet.setSelected(false);
//			}
//			for (FCheckBox otherSet : otherSets) {
//				otherSet.setSelected(false);
//			}
//
//
//			Predicate<CardEdition> formatPredicate = null;
//			for (GameFormat gameFormat : gameFormats) {
//				if (gameFormat.getName().equals(formatButtonGroup.getSelection().getActionCommand())) {
//					formatPredicate = edition -> gameFormat.editionLegalPredicate.apply(edition) && (unselectableSets == null || !unselectableSets.contains(edition.getCode()));
//				} else if (formatButtonGroup.getSelection().getActionCommand().equals("Modern Card Frame")) {
//					formatPredicate = edition -> edition.getDate().after(new Date(1059350399L * 1000L)) && (unselectableSets == null || !unselectableSets.contains(edition.getCode()));
//				} else if (formatButtonGroup.getSelection().getActionCommand().equals("No Format Restriction")) {
//					formatPredicate = edition -> unselectableSets == null || !unselectableSets.contains(edition.getCode());
//				}
//			}
//
//			List<CardEdition> filteredCoreSets = new ArrayList<>();
//			for (CardEdition edition : editions) {
//				if (edition.getType() == CardEdition.Type.CORE) {
//					if (formatPredicate != null && formatPredicate.test(edition)) {
//						filteredCoreSets.add(edition);
//					}
//				}
//			}
//
//			List<CardEdition> filteredExpansionSets = new ArrayList<>();
//			for (CardEdition edition : editions) {
//				if (edition.getType() == CardEdition.Type.EXPANSION) {
//					if (formatPredicate != null && formatPredicate.test(edition)) {
//						filteredExpansionSets.add(edition);
//					}
//				}
//			}
//
//			List<CardEdition> filteredOtherSets = new ArrayList<>();
//			for (CardEdition edition : editions) {
//				if (edition.getType() != CardEdition.Type.CORE && edition.getType() != CardEdition.Type.EXPANSION) {
//					if (formatPredicate != null && formatPredicate.test(edition)) {
//						filteredOtherSets.add(edition);
//					}
//				}
//			}
//
//			Collections.shuffle(filteredCoreSets);
//			Collections.shuffle(filteredExpansionSets);
//			Collections.shuffle(filteredOtherSets);
//
//			List<CardEdition> filteredCustomSets = new ArrayList<>();
//			if (customSetsExist){
//				for (CardEdition edition : customEditions) {
//					if (formatPredicate != null && formatPredicate.test(edition)) {
//						filteredCustomSets.add(edition);
//					}
//				}
//				Collections.shuffle(filteredCustomSets);
//			}
//
//			for (int i = 0; i < numberOfCoreSets && i < filteredCoreSets.size(); i++) {
//				String name = TextUtil.concatWithSpace(filteredCoreSets.get(i).getName(), TextUtil.enclosedParen(filteredCoreSets.get(i).getCode()));
//				for (FCheckBox set : coreSets) {
//					if (set.getText().equals(name)) {
//						set.setSelected(true);
//					}
//				}
//			}
//
//			for (int i = 0; i < numberOfExpansionSets && i < filteredExpansionSets.size(); i++) {
//				String name = TextUtil.concatWithSpace(filteredExpansionSets.get(i).getName(), TextUtil.enclosedParen(filteredExpansionSets.get(i).getCode()));
//				for (FCheckBox set : expansionSets) {
//					if (set.getText().equals(name)) {
//						set.setSelected(true);
//					}
//				}
//			}
//
//			for (int i = 0; i < numberOfOtherSets && i < filteredOtherSets.size(); i++) {
//				String name = TextUtil.concatWithSpace(filteredOtherSets.get(i).getName(), TextUtil.enclosedParen(filteredOtherSets.get(i).getCode()));
//				for (FCheckBox set : otherSets) {
//					if (set.getText().equals(name)) {
//						set.setSelected(true);
//					}
//				}
//			}
//
//			if (customSetsExist){
//				for (int i = 0; i < numberOfCustomeSets && i < filteredCustomSets.size(); i++) {
//					String name = TextUtil.concatWithSpace(filteredCustomSets.get(i).getName(),
//							                               TextUtil.enclosedParen(filteredCustomSets.get(i).getCode()));
//					for (FCheckBox set : customSets) {
//						if (set.getText().equals(name)) {
//							set.setSelected(true);
//						}
//					}
//				}
//			}
//
//			panel.repaintSelf();
//
//		});

		// Clear Selection Button
		// CLEAR Selection button
		FButton clearSelectionButton = new FButton(Localizer.getInstance().getMessage("lblClearSelection"));
		clearSelectionButton.addActionListener(actionEvent -> {
			for (List<FCheckBox> checkBoxes : editionsCheckBoxes.values()){
				for (FCheckBox cBox : checkBoxes)
					cBox.setSelected(false);
			}
			// Remove all the checkboxes from the selected sets
			selectedCheckBoxes.replaceAll((t, v) -> new ArrayList<>());
			panel.repaintSelf();
		});

		// === MIDDLE: SHOW OPTIONS BUTTON ===
		FButton showOptionsButton = new FButton(Localizer.getInstance().getMessage("lblShowOptions"));
		showOptionsButton.addActionListener(actionEvent -> {
			optionsPanel.setVisible(true);
			showOptionsButton.setVisible(false);
		});

		// Hide Option Button
		FButton hideOptionsButton = new FButton(Localizer.getInstance().getMessage("lblHideOptions"));
		hideOptionsButton.addActionListener(actionEvent -> {
			optionsPanel.setVisible(false);
			showOptionsButton.setVisible(true);
		});

		// Button Panel to enable the three button sets: "random selection; clear selection, hide options"
		JPanel buttonPanel = new JPanel(new MigLayout("h 50!, center, gap 10, insets 0, ay center"));
		buttonPanel.setOpaque(false);
		buttonPanel.add(randomSelectionButton, "w 175!, h 28!");
		buttonPanel.add(clearSelectionButton, "w 175!, h 28!");
		buttonPanel.add(hideOptionsButton, " w 175!, h 28!");
		optionsPanel.add(buttonPanel, "span 2, growx");
		if (showWantReprintsCheckbox) {
			optionsPanel.add(cbWantReprints, "center, span, wrap");
		}

		// === SEPARATOR ===
		optionsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2, growx");

		// === TOP PANEL ===

		panel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblChooseSets"))
				.fontSize(20).build(), "center, span, wrap, gaptop 10");

		// Selected Editions Panel
		JPanel selectedEditionsPnl = new JPanel(new MigLayout("center, wrap"));
		selectedEditionsPnl.setOpaque(false);
		selectedEditionsPnl.add(new FLabel.Builder().text("Selected Editions").build());
		setupSelectedSetsList(selectedCheckBoxes, editionsCheckBoxes, selectedEditionsPnl);

		// Checkboxes Panel
		JPanel setsCheckBoxesPanel = new JPanel(new MigLayout("center, wrap"));
		setsCheckBoxesPanel.setOpaque(false);
		setsCheckBoxesPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSetEdition")).build());
		FCheckBoxList<FCheckBox> setsCheckboxesList = setupEditionCheckBoxList(selectedCheckBoxes, setsCheckBoxesPanel);


		// Edition Type Panel
		JPanel editionTypesPanel = new JPanel(new MigLayout("center, wrap"));
		editionTypesPanel.setOpaque(false);
		editionTypesPanel.add(new FLabel.Builder().text("Edition Types").build());
		setupEditionTypesPanel(editionTypes, editionsCheckBoxes, setsCheckboxesList, editionTypesPanel);

		// ADD all panels to the main UI Panel
		panel.add(editionTypesPanel, "aligny top");
		panel.add(setsCheckBoxesPanel, "aligny top");
		panel.add(selectedEditionsPnl, "aligny top");

//		panel.add(showOptionsButton, "center, w 260!, h 30!, gap 10 0 20 0, span 3, hidemode 3");
//		panel.add(optionsPanel, "center, w 100, span 3, growx, hidemode 3");

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

		panel.add(southPanel, "dock south, gapBottom 10");

		overlay.add(panel);
		panel.getRootPane().setDefaultButton(btnOk);
		SOverlayUtils.showOverlay();

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
	
	public void setWantReprintsCB(boolean isSet) {
	    cbWantReprints.setSelected(isSet);
	}

	private String getSelectedSetLabel(String editionTypeLabel, String editionLabel){
		editionTypeLabel = TextUtil.enclosedParen(editionTypeLabel);
		return editionTypeLabel + " " + editionLabel + "\n";
	}

	private void refreshSelectedEditions(Map<CardEdition.Type, List<FCheckBox>> selectedEditions, JTextArea textArea){
		textArea.setText("");
		String selectedEditionsLines = "";
		for (CardEdition.Type editionType : selectedEditions.keySet()){
			List<FCheckBox> selectedCheckBoxes = selectedEditions.get(editionType);
			for (FCheckBox cbox : selectedCheckBoxes)
				selectedEditionsLines += getSelectedSetLabel(editionType.toString(), cbox.getText());
		}
		textArea.setText(selectedEditionsLines);
	}

	private void setupSelectedSetsList(Map<CardEdition.Type, List<FCheckBox>> selectedEditions,
									   Map<String, List<FCheckBox>> editionsCheckBoxes, JPanel txtAreaPanel){

		JTextArea selectedEditionsTxtArea = new JTextArea(26, 33);
		selectedEditionsTxtArea.setEditable(false);
		txtAreaPanel.add(new FScrollPane(selectedEditionsTxtArea, true,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
		refreshSelectedEditions(selectedEditions, selectedEditionsTxtArea);
		// Program the event for checkboxes
		for (String editionTypeLabel : editionsCheckBoxes.keySet()) {
			List<FCheckBox> checkBoxes = editionsCheckBoxes.get(editionTypeLabel);
			for (FCheckBox checkBox: checkBoxes){
				checkBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						CardEdition.Type edType = CardEdition.Type.fromString(editionTypeLabel);
						if (checkBox.isSelected()){
							selectedEditions.get(edType).add(checkBox);
						} else {
							selectedEditions.get(edType).remove(checkBox);
						}
						refreshSelectedEditions(selectedEditions, selectedEditionsTxtArea);
					}});
			}
		}
	}

	private FCheckBoxList<FCheckBox> setupEditionCheckBoxList(Map<CardEdition.Type, List<FCheckBox>> selectedEditions,
															  JPanel checkBoxesPnl) {
		// Setup Sets Checkboxes
		List<FCheckBox> checkBoxesToAddToScrollPane = new ArrayList<>();
		for (CardEdition.Type editionType : selectedEditions.keySet()) {
			List<FCheckBox> checkBoxes = selectedEditions.get(editionType);
			checkBoxesToAddToScrollPane.addAll(checkBoxes);
		}
		FCheckBoxList<FCheckBox> setsCbl = new FCheckBoxList<>(false);
		setsCbl.setListData(checkBoxesToAddToScrollPane.toArray(new FCheckBox[checkBoxesToAddToScrollPane.size()]));
		setsCbl.setVisibleRowCount(25);
		checkBoxesPnl.add(new FScrollPane(setsCbl, true,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));                                                                                                                                                                                                                                                       ;
		return setsCbl;
	}

	private void setupEditionTypesPanel(List<String> editionTypes, Map<String, List<FCheckBox>> editionsCheckBoxes,
										FCheckBoxList<FCheckBox> checkBoxList, JPanel editionTypesPanel) {

		JList editionTypesList = new JList(editionTypes.toArray());
		editionTypesList.setFixedCellHeight(20);
		editionTypesList.setFixedCellWidth(120);
		editionTypesList.setVisibleRowCount(20);
		ListSelectionModel listSelectionModel = editionTypesList.getSelectionModel();
		listSelectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				boolean isAdjusting = lsm.getValueIsAdjusting();
				if (!lsm.isSelectionEmpty() && !isAdjusting) {
					// Find out which indexes are selected.
					int minIndex = lsm.getMinSelectionIndex();
					int maxIndex = lsm.getMaxSelectionIndex();
					for (int i = minIndex; i <= maxIndex; i++) {
						if (lsm.isSelectedIndex(i)) {
							String editionTypeLabel = editionTypes.get(i);
							List<FCheckBox> editionCheckBoxes = editionsCheckBoxes.get(editionTypeLabel);
							checkBoxList.setListData(editionCheckBoxes.toArray(new FCheckBox[editionCheckBoxes.size()]));
						}
					}
				}
			}});
		editionTypesPanel.add(new FScrollPane(editionTypesList, true,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				editionTypesList.requestFocusInWindow();
			}
		});
	}

	private void handleOk() {

		for (CardEdition.Type editionType : selectedCheckBoxes.keySet()){
			List<FCheckBox> choices = selectedCheckBoxes.get(editionType);
			for (FCheckBox box : choices) {
				if (box.isSelected()) {
					selectedSets.add(box.getName());
				}
			}
		}
		wantReprints = cbWantReprints.isSelected();

		if (null != okCallback) {
			okCallback.run();
		}
	}

}
