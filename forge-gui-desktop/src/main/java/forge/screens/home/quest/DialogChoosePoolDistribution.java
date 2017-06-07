package forge.screens.home.quest;

import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.card.MagicColor;
import forge.gui.SOverlayUtils;
import forge.quest.StartingPoolPreferences.PoolType;
import forge.toolbox.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class DialogChoosePoolDistribution {

	private final FPanel mainPanel = new FPanel(new MigLayout("insets 20, gap 25, center, wrap 2"));

	private final FCheckBox cbxBlack     = new FCheckBox("Black");
	private final FCheckBox cbxBlue      = new FCheckBox("Blue");
	private final FCheckBox cbxGreen     = new FCheckBox("Green");
	private final FCheckBox cbxRed       = new FCheckBox("Red");
	private final FCheckBox cbxWhite     = new FCheckBox("White");
	private final FCheckBox cbxColorless = new FCheckBox("Colorless");
	private final FCheckBox cbxArtifacts = new FCheckBox("Include Artifacts");

	private final FRadioButton radBalanced   = new FRadioButton("Balanced");
	private final FRadioButton radRandom     = new FRadioButton("True Random");
	private final FRadioButton radSurpriseMe = new FRadioButton("Surprise Me");
	private final FRadioButton radBoosters   = new FRadioButton("Boosters");

	private final FTextField numberOfBoostersField = new FTextField.Builder().text("0").maxLength(10).build();

	private final FButton btnOk = new FButton("OK");

	private Runnable callback;

	@SuppressWarnings("serial")
	public DialogChoosePoolDistribution(final List<Byte> preferredColors, final PoolType poolType, final boolean includeArtifacts) {

		for (Byte color : preferredColors) {
			switch (color) {
				case MagicColor.BLACK:
					cbxBlack.setSelected(true);
					break;
				case MagicColor.BLUE:
					cbxBlue.setSelected(true);
					break;
				case MagicColor.GREEN:
					cbxGreen.setSelected(true);
					break;
				case MagicColor.RED:
					cbxRed.setSelected(true);
					break;
				case MagicColor.WHITE:
					cbxWhite.setSelected(true);
					break;
				case MagicColor.COLORLESS:
					cbxColorless.setSelected(true);
					break;
			}
		}

		cbxArtifacts.setSelected(includeArtifacts);

		switch (poolType) {
			case BALANCED:
				radBalanced.setSelected(true);
				radRandom.setSelected(false);
				radSurpriseMe.setSelected(false);
				radBoosters.setSelected(false);
				break;
			case RANDOM:
				radBalanced.setSelected(false);
				radRandom.setSelected(true);
				radSurpriseMe.setSelected(false);
				radBoosters.setSelected(false);
				break;
			case RANDOM_BALANCED:
				radBalanced.setSelected(false);
				radRandom.setSelected(false);
				radSurpriseMe.setSelected(true);
				radBoosters.setSelected(false);
				break;
			case BOOSTERS:
				radBalanced.setSelected(false);
				radRandom.setSelected(false);
				radSurpriseMe.setSelected(false);
				radBoosters.setSelected(true);
				break;
		}

		mainPanel.setOpaque(false);
		mainPanel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

		final String contentPanelConstraints = "w 200px!, h 340px!, left, gap 5, insets 10 25 10 25, wrap 1";

		//Right side
		final FPanel right = new FPanel(new MigLayout(contentPanelConstraints));
		right.setOpaque(false);

		final FLabel clearColors = new FLabel.Builder().text("Clear All").fontSize(12).opaque(true).hoverable(true).build();
		clearColors.setCommand(new UiCommand() {
			@Override
			public void run() {
				cbxBlack.setSelected(false);
				cbxBlue.setSelected(false);
				cbxGreen.setSelected(false);
				cbxRed.setSelected(false);
				cbxWhite.setSelected(false);
				cbxColorless.setSelected(false);
			}
		});

		final FLabel boosterPackLabel = new FLabel.Builder().text("Number of Boosters:").fontSize(14).build();
		final FLabel colorsLabel = new FLabel.Builder().text("Colors").fontSize(18).build();
		final FTextPane noSettingsText = new FTextPane("No settings are available for this selection.");

		if (radBoosters.isSelected()) {
			right.add(boosterPackLabel, "gaptop 10");
			right.add(numberOfBoostersField, "w 100px!, gaptop 5");
		} else if (radSurpriseMe.isSelected()) {
			right.add(noSettingsText, "gaptop 10");
		} else if (radRandom.isSelected()) {
			right.add(cbxArtifacts, "gaptop 10");
		} else {
			right.add(colorsLabel, "gaptop 10");
			right.add(clearColors, "w 75px!, h 20px!, gaptop 10");
			right.add(cbxBlack, "gaptop 10");
			right.add(cbxBlue);
			right.add(cbxGreen);
			right.add(cbxRed);
			right.add(cbxWhite);
			right.add(cbxColorless);
			right.add(cbxArtifacts, "gaptop 25");
		}

		//Left Side
		final FPanel left = new FPanel(new MigLayout(contentPanelConstraints));
		left.setOpaque(false);
		left.add(new FLabel.Builder().text("Distribution").fontSize(18).build(), "gaptop 10");

		final JXButtonPanel poolTypePanel    = new JXButtonPanel();
		final String        radioConstraints = "h 25px!, gaptop 5";
		poolTypePanel.add(radBalanced, radioConstraints);
		poolTypePanel.add(radRandom, radioConstraints);
		poolTypePanel.add(radSurpriseMe, radioConstraints);
		poolTypePanel.add(radBoosters, radioConstraints);

		left.add(poolTypePanel, "gaptop 15");
		left.add(new FTextPane("Hover over each item for a more detailed description."), "gaptop 20");

		ActionListener radioButtonListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				right.removeAll();

				if (radBoosters.isSelected()) {
					right.add(boosterPackLabel, "gaptop 10");
					right.add(numberOfBoostersField, "w 100px!, gaptop 5");
				} else if (radSurpriseMe.isSelected()) {
					right.add(noSettingsText, "gaptop 10");
				} else if (radRandom.isSelected()) {
					right.add(cbxArtifacts, "gaptop 10");
				} else {
					right.add(colorsLabel, "gaptop 10");
					right.add(clearColors, "w 75px!, h 20px!, gaptop 10");
					right.add(cbxBlack, "gaptop 10");
					right.add(cbxBlue);
					right.add(cbxGreen);
					right.add(cbxRed);
					right.add(cbxWhite);
					right.add(cbxColorless);
					right.add(cbxArtifacts, "gaptop 25");
				}

				clearColors.setVisible(radBalanced.isSelected());
				cbxBlack.setVisible(radBalanced.isSelected());
				cbxBlue.setVisible(radBalanced.isSelected());
				cbxGreen.setVisible(radBalanced.isSelected());
				cbxRed.setVisible(radBalanced.isSelected());
				cbxWhite.setVisible(radBalanced.isSelected());
				cbxColorless.setVisible(radBalanced.isSelected());
				cbxArtifacts.setVisible(!radSurpriseMe.isSelected() && !radBoosters.isSelected());
				numberOfBoostersField.setVisible(radBoosters.isSelected());

				right.validate();
				right.repaint();

			}
		};

		clearColors.setVisible(radBalanced.isSelected());
		cbxBlack.setVisible(radBalanced.isSelected());
		cbxBlue.setVisible(radBalanced.isSelected());
		cbxGreen.setVisible(radBalanced.isSelected());
		cbxRed.setVisible(radBalanced.isSelected());
		cbxWhite.setVisible(radBalanced.isSelected());
		cbxColorless.setVisible(radBalanced.isSelected());
		cbxArtifacts.setVisible(!radSurpriseMe.isSelected() && !radBoosters.isSelected());
		numberOfBoostersField.setVisible(radBoosters.isSelected());

		radBalanced.setToolTipText("A \"Balanced\" distribution will provide a roughly equal number of cards in each selected color.");
		radRandom.setToolTipText("A \"True Random\" distribution will be almost entirely randomly selected. This ignores any color selections.");
		radSurpriseMe.setToolTipText("This is the same as a \"Balanced\" distribution, except the colors picked will be random and you will not be told what they are.");
		radBoosters.setToolTipText("This ignores all color settings and instead generates a card pool out of a specified number of booster packs.");
		cbxArtifacts.setToolTipText("When selected, artifacts will be included in your pool regardless of color selections. This mimics the old card pool behavior.");

		radBalanced.addActionListener(radioButtonListener);
		radRandom.addActionListener(radioButtonListener);
		radSurpriseMe.addActionListener(radioButtonListener);
		radBoosters.addActionListener(radioButtonListener);

		//Add Bottom and Panels
		mainPanel.add(left);
		mainPanel.add(right);

		btnOk.setCommand(new UiCommand() {
			@Override
			public void run() {
				SOverlayUtils.hideOverlay();
				callback.run();
			}
		});

		FButton btnCancel = new FButton("Cancel");
		btnCancel.setCommand(new UiCommand() {
			@Override
			public void run() {
				SOverlayUtils.hideOverlay();
			}
		});

		JPanel southPanel = new JPanel(new MigLayout("insets 10, gap 20, ax right"));
		southPanel.setOpaque(false);
		southPanel.add(btnOk, "w 150px!, h 30px!");
		southPanel.add(btnCancel, "w 150px!, h 30px!");

		mainPanel.add(southPanel, "dock south, gapBottom 10");

	}

	public void show(final Runnable callback) {

		this.callback = callback;

		final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
		overlay.setLayout(new MigLayout("insets 30, gap 15, wrap, ax center, ay center"));
		overlay.add(mainPanel);

		mainPanel.getRootPane().setDefaultButton(btnOk);

		SOverlayUtils.showOverlay();

	}

	public List<Byte> getPreferredColors() {

		List<Byte> preferredColors = new ArrayList<>();

		if (cbxBlack.isSelected()) {
			preferredColors.add(MagicColor.BLACK);
		}
		if (cbxBlue.isSelected()) {
			preferredColors.add(MagicColor.BLUE);
		}
		if (cbxGreen.isSelected()) {
			preferredColors.add(MagicColor.GREEN);
		}
		if (cbxRed.isSelected()) {
			preferredColors.add(MagicColor.RED);
		}
		if (cbxWhite.isSelected()) {
			preferredColors.add(MagicColor.WHITE);
		}
		if (cbxColorless.isSelected()) {
			preferredColors.add(MagicColor.COLORLESS);
		}

		return preferredColors;

	}

	public PoolType getPoolType() {

		if (radRandom.isSelected()) {
			return PoolType.RANDOM;
		} else if (radSurpriseMe.isSelected()) {
			return PoolType.RANDOM_BALANCED;
		} else if (radBoosters.isSelected()) {
			return PoolType.BOOSTERS;
		}

		return PoolType.BALANCED;

	}

	public boolean includeArtifacts() {
		return cbxArtifacts.isSelected();
	}

	public int getNumberOfBoosters() {
		try {
			return Integer.valueOf(numberOfBoostersField.getText());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
