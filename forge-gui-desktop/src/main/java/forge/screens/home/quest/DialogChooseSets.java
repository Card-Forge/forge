package forge.screens.home.quest;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;
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

	private final List<FCheckBox> choices = new ArrayList<>();
	private final FCheckBox cbWantReprints = new FCheckBox(Localizer.getInstance().getMessage("lblDisplayRecentSetReprints"));

	// lists are of set codes (e.g. "2ED")
	public DialogChooseSets(Collection<String> preselectedSets, Collection<String> unselectableSets, boolean showWantReprintsCheckbox) {

		// create a local copy of the editions list so we can sort it
		List<CardEdition> editions = Lists.newArrayList(FModel.getMagicDb().getEditions());
		Collections.sort(editions);
		Collections.reverse(editions);

		List<CardEdition> customEditions = Lists.newArrayList(FModel.getMagicDb().getCustomEditions());
		boolean customSetsExist = (customEditions.size() > 0);
		if (customSetsExist){
			Collections.sort(customEditions);
			Collections.reverse(customEditions);
		}
		List<FCheckBox> coreSets = new ArrayList<>();
		List<FCheckBox> expansionSets = new ArrayList<>();
		List<FCheckBox> otherSets = new ArrayList<>();

		for (CardEdition ce : editions) {
			String code = ce.getCode();
			FCheckBox box = new FCheckBox(TextUtil.concatWithSpace(ce.getName(), TextUtil.enclosedParen(code)));
			box.setName(code);
			box.setSelected(null != preselectedSets && preselectedSets.contains(code));
			box.setEnabled(null == unselectableSets || !unselectableSets.contains(code));
			switch (ce.getType()) {
				case CORE:
					coreSets.add(box);
					break;
				case EXPANSION:
					expansionSets.add(box);
					break;
				default:
					otherSets.add(box);
					break;
			}
		}
		// CustomSet
		List<FCheckBox> customSets = new ArrayList<>();
		if (customSetsExist) {
			for (CardEdition ce : customEditions){
				String code = ce.getCode();
				FCheckBox box = new FCheckBox(TextUtil.concatWithSpace(ce.getName(), TextUtil.enclosedParen(code)));
				box.setName(code);
				box.setSelected(null != preselectedSets && preselectedSets.contains(code));
				box.setEnabled(null == unselectableSets || !unselectableSets.contains(code));
				customSets.add(box);
			}

		}
		int wrapCol = !customSetsExist ? 3 : 4;
		FPanel panel = new FPanel(new MigLayout(String.format("insets 10, gap 5, center, wrap %d", wrapCol)));
		panel.setOpaque(false);
		panel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

		FTextField coreField = new FTextField.Builder().text("0").maxLength(3).build();
		FTextField expansionField = new FTextField.Builder().text("0").maxLength(3).build();
		FTextField otherField = new FTextField.Builder().text("0").maxLength(3).build();
		FTextField customField = new FTextField.Builder().text("0").maxLength(3).build();

		JPanel optionsPanel = new JPanel(new MigLayout("insets 10, gap 5, center, wrap 2"));
		optionsPanel.setVisible(false);
		optionsPanel.setOpaque(false);
		optionsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2, growx");
		optionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSelectRandomSets")).fontSize(17).fontStyle(Font.BOLD).build(), "h 40!, span 2");

		JPanel leftOptionsPanel = new JPanel(new MigLayout("insets 10, gap 5, center, wrap 2"));
		leftOptionsPanel.setOpaque(false);
		leftOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSelectNumber") + ":").fontSize(14).fontStyle(Font.BOLD).build(), " span 2");

		leftOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblCore") + ":").build());
		leftOptionsPanel.add(coreField, "w 40!");

		leftOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblExpansion") + ":").build());
		leftOptionsPanel.add(expansionField, "w 40!");

		leftOptionsPanel.add(new FLabel.Builder().text("Other:").build());
		leftOptionsPanel.add(otherField, "w 40!");

		if (customSetsExist){
			leftOptionsPanel.add(new FLabel.Builder().text("Custom Editions:").build());
			leftOptionsPanel.add(customField, "w 40!");
		}

		JPanel rightOptionsPanel = new JPanel(new MigLayout("insets 10, gap 25 5, center, wrap 2"));
		rightOptionsPanel.setOpaque(false);
		rightOptionsPanel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblFormatRestrictions") +":").fontSize(14).fontStyle(Font.BOLD).build(), "span 2");

		ButtonGroup formatButtonGroup = new ButtonGroup();
		List<GameFormat> gameFormats = new ArrayList<>();
		FModel.getFormats().getSanctionedList().forEach(gameFormats::add);

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

		FButton randomSelectionButton = new FButton(Localizer.getInstance().getMessage("lblRandomizeSets"));
		randomSelectionButton.addActionListener(actionEvent -> {

			int numberOfCoreSets = Integer.parseInt(coreField.getText());
			int numberOfExpansionSets = Integer.parseInt(expansionField.getText());
			int numberOfOtherSets = Integer.parseInt(otherField.getText());
			int numberOfCustomeSets = 0;
			if (customSetsExist)
			 	numberOfCustomeSets = Integer.parseInt(customField.getText());

			for (FCheckBox coreSet : coreSets) {
				coreSet.setSelected(false);
			}
			for (FCheckBox expansionSet : expansionSets) {
				expansionSet.setSelected(false);
			}
			for (FCheckBox otherSet : otherSets) {
				otherSet.setSelected(false);
			}
			if (customSetsExist){
				for (FCheckBox customSet : customSets) {
					customSet.setSelected(false);
				}
			}

			Predicate<CardEdition> formatPredicate = null;
			for (GameFormat gameFormat : gameFormats) {
				if (gameFormat.getName().equals(formatButtonGroup.getSelection().getActionCommand())) {
					formatPredicate = edition -> gameFormat.editionLegalPredicate.apply(edition) && (unselectableSets == null || !unselectableSets.contains(edition.getCode()));
				} else if (formatButtonGroup.getSelection().getActionCommand().equals("Modern Card Frame")) {
					formatPredicate = edition -> edition.getDate().after(new Date(1059350399L * 1000L)) && (unselectableSets == null || !unselectableSets.contains(edition.getCode()));
				} else if (formatButtonGroup.getSelection().getActionCommand().equals("No Format Restriction")) {
					formatPredicate = edition -> unselectableSets == null || !unselectableSets.contains(edition.getCode());
				}
			}

			List<CardEdition> filteredCoreSets = new ArrayList<>();
			for (CardEdition edition : editions) {
				if (edition.getType() == CardEdition.Type.CORE) {
					if (formatPredicate != null && formatPredicate.test(edition)) {
						filteredCoreSets.add(edition);
					}
				}
			}

			List<CardEdition> filteredExpansionSets = new ArrayList<>();
			for (CardEdition edition : editions) {
				if (edition.getType() == CardEdition.Type.EXPANSION) {
					if (formatPredicate != null && formatPredicate.test(edition)) {
						filteredExpansionSets.add(edition);
					}
				}
			}

			List<CardEdition> filteredOtherSets = new ArrayList<>();
			for (CardEdition edition : editions) {
				if (edition.getType() != CardEdition.Type.CORE && edition.getType() != CardEdition.Type.EXPANSION) {
					if (formatPredicate != null && formatPredicate.test(edition)) {
						filteredOtherSets.add(edition);
					}
				}
			}

			Collections.shuffle(filteredCoreSets);
			Collections.shuffle(filteredExpansionSets);
			Collections.shuffle(filteredOtherSets);

			List<CardEdition> filteredCustomSets = new ArrayList<>();
			if (customSetsExist){
				for (CardEdition edition : customEditions) {
					if (formatPredicate != null && formatPredicate.test(edition)) {
						filteredCustomSets.add(edition);
					}
				}
				Collections.shuffle(filteredCustomSets);
			}

			for (int i = 0; i < numberOfCoreSets && i < filteredCoreSets.size(); i++) {
				String name = TextUtil.concatWithSpace(filteredCoreSets.get(i).getName(), TextUtil.enclosedParen(filteredCoreSets.get(i).getCode()));
				for (FCheckBox set : coreSets) {
					if (set.getText().equals(name)) {
						set.setSelected(true);
					}
				}
			}

			for (int i = 0; i < numberOfExpansionSets && i < filteredExpansionSets.size(); i++) {
				String name = TextUtil.concatWithSpace(filteredExpansionSets.get(i).getName(), TextUtil.enclosedParen(filteredExpansionSets.get(i).getCode()));
				for (FCheckBox set : expansionSets) {
					if (set.getText().equals(name)) {
						set.setSelected(true);
					}
				}
			}

			for (int i = 0; i < numberOfOtherSets && i < filteredOtherSets.size(); i++) {
				String name = TextUtil.concatWithSpace(filteredOtherSets.get(i).getName(), TextUtil.enclosedParen(filteredOtherSets.get(i).getCode()));
				for (FCheckBox set : otherSets) {
					if (set.getText().equals(name)) {
						set.setSelected(true);
					}
				}
			}

			if (customSetsExist){
				for (int i = 0; i < numberOfCustomeSets && i < filteredCustomSets.size(); i++) {
					String name = TextUtil.concatWithSpace(filteredCustomSets.get(i).getName(),
							                               TextUtil.enclosedParen(filteredCustomSets.get(i).getCode()));
					for (FCheckBox set : customSets) {
						if (set.getText().equals(name)) {
							set.setSelected(true);
						}
					}
				}
			}

			panel.repaintSelf();

		});

		FButton clearSelectionButton = new FButton(Localizer.getInstance().getMessage("lblClearSelection"));
		clearSelectionButton.addActionListener(actionEvent -> {
			for (FCheckBox coreSet : coreSets) {
				coreSet.setSelected(false);
			}
			for (FCheckBox expansionSet : expansionSets) {
				expansionSet.setSelected(false);
			}
			for (FCheckBox otherSet : otherSets) {
				otherSet.setSelected(false);
			}
			if (customSetsExist){
				for (FCheckBox cmSet : customSets) {
					cmSet.setSelected(false);
				}
			}
			panel.repaintSelf();
		});

		FButton showOptionsButton = new FButton(Localizer.getInstance().getMessage("lblShowOptions"));
		showOptionsButton.addActionListener(actionEvent -> {
			optionsPanel.setVisible(true);
			showOptionsButton.setVisible(false);
		});

		FButton hideOptionsButton = new FButton(Localizer.getInstance().getMessage("lblHideOptions"));
		hideOptionsButton.addActionListener(actionEvent -> {
			optionsPanel.setVisible(false);
			showOptionsButton.setVisible(true);
		});

		JPanel buttonPanel = new JPanel(new MigLayout("h 50!, center, gap 10, insets 0, ay center"));
		buttonPanel.setOpaque(false);
		buttonPanel.add(randomSelectionButton, "w 175!, h 28!");
		buttonPanel.add(clearSelectionButton, "w 175!, h 28!");
		buttonPanel.add(hideOptionsButton, " w 175!, h 28!");

		optionsPanel.add(buttonPanel, "span 2, growx");

		if (showWantReprintsCheckbox) {
			optionsPanel.add(cbWantReprints, "center, span, wrap");
		}

		optionsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "w 100%, span 2, growx");

		panel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblChooseSets")).fontSize(20).build(), "center, span, wrap, gaptop 10");

		String constraints = "aligny top";
		panel.add(makeCheckBoxList(coreSets,
									Localizer.getInstance().getMessage("lblCoreSets"), true),
									constraints);
		panel.add(makeCheckBoxList(expansionSets,
									Localizer.getInstance().getMessage("lblExpansions"), false),
									constraints);
		panel.add(makeCheckBoxList(otherSets,
									Localizer.getInstance().getMessage("lblOtherSets"), false),
									constraints);
		if (customSetsExist){
			panel.add(makeCheckBoxList(customSets,
										Localizer.getInstance().getMessage("lblCustomSets"), false),
										constraints);
		}
		panel.add(showOptionsButton, "center, w 230!, h 30!, gap 10 0 20 0, span 3, hidemode 3");
		panel.add(optionsPanel, "center, w 100, span 3, growx, hidemode 3");

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
		southPanel.add(btnOk, "center, w 200!, h 30!");
		southPanel.add(btnCancel, "center, w 200!, h 30!");

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

	private JPanel makeCheckBoxList(List<FCheckBox> sets, String title, boolean focused) {
		choices.addAll(sets);
		final FCheckBoxList<FCheckBox> cbl = new FCheckBoxList<>(false);
		cbl.setListData(sets.toArray(new FCheckBox[sets.size()]));
		cbl.setVisibleRowCount(20);

		if (focused) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					cbl.requestFocusInWindow();
				}
			});
		}

		JPanel pnl = new JPanel(new MigLayout("center, wrap"));
		pnl.setOpaque(false);
		pnl.add(new FLabel.Builder().text(title).build());
		pnl.add(new FScrollPane(cbl, true));
		return pnl;

	}

	private void handleOk() {

		for (FCheckBox box : choices) {
			if (box.isSelected()) {
				selectedSets.add(box.getName());
			}
			wantReprints = cbWantReprints.isSelected();
		}

		if (null != okCallback) {
			okCallback.run();
		}

	}

}
