package forge.screens.home.quest;

import forge.UiCommand;
import forge.card.MagicColor;
import forge.gui.SOverlayUtils;
import forge.localinstance.assets.FSkinProp;
import forge.quest.StartingPoolPreferences.PoolType;
import forge.toolbox.*;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class DialogChoosePoolDistribution {
	final Localizer localizer = Localizer.getInstance();
	private final FPanel mainPanel = new FPanel(new MigLayout("insets 20, gap 25, center, wrap 2"));

	private final FCheckBox cbxBlack     = new FCheckBox(localizer.getMessage("lblBlack"));
	private final FCheckBox cbxBlue      = new FCheckBox(localizer.getMessage("lblBlue"));
	private final FCheckBox cbxGreen     = new FCheckBox(localizer.getMessage("lblGreen"));
	private final FCheckBox cbxRed       = new FCheckBox(localizer.getMessage("lblRed"));
	private final FCheckBox cbxWhite     = new FCheckBox(localizer.getMessage("lblWhite"));
	private final FCheckBox cbxColorless = new FCheckBox(localizer.getMessage("lblColorless"));
	private final FCheckBox cbxArtifacts = new FCheckBox(localizer.getMessage("lblIncludeArtifacts"));

	private final FRadioButton radBalanced   = new FRadioButton(localizer.getMessage("lblBalanced"));
	private final FRadioButton radRandom     = new FRadioButton(localizer.getMessage("lblTrueRandom"));
	private final FRadioButton radSurpriseMe = new FRadioButton(localizer.getMessage("lblSurpriseMe"));
	private final FRadioButton radBoosters   = new FRadioButton(localizer.getMessage("lblBoosters"));

	private final ButtonGroup poolTypeButtonGroup = new ButtonGroup();

	private final FTextField numberOfBoostersField = new FTextField.Builder().text("0").maxLength(10).build();

	private final FButton btnOk = new FButton(localizer.getMessage("lblOk"));

	private Runnable callback;

	@SuppressWarnings("serial")
	public DialogChoosePoolDistribution(final List<Byte> preferredColors, final PoolType poolType, final boolean includeArtifacts) {

		if (poolTypeButtonGroup.getButtonCount() == 0) {
			poolTypeButtonGroup.add(radBalanced);
			poolTypeButtonGroup.add(radRandom);
			poolTypeButtonGroup.add(radSurpriseMe);
			poolTypeButtonGroup.add(radBoosters);
		}

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
				break;
			case RANDOM:
				radRandom.setSelected(true);
				break;
			case RANDOM_BALANCED:
				radSurpriseMe.setSelected(true);
				break;
			case BOOSTERS:
				radBoosters.setSelected(true);
				break;
		}

		mainPanel.setOpaque(false);
		mainPanel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

		final String contentPanelConstraints = "w 200px!, h 340px!, left, gap 5, insets 10 25 10 25, wrap 1";

		//Right side
		final FPanel right = new FPanel(new MigLayout(contentPanelConstraints));
		right.setOpaque(false);

		final FLabel clearColors = new FLabel.Builder().text(localizer.getMessage("lblClearAll")).fontSize(12).opaque(true).hoverable(true).build();
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

		final FLabel boosterPackLabel = new FLabel.Builder().text(localizer.getMessage("lblNumberofBoosters") + ":").fontSize(14).build();
		final FLabel colorsLabel = new FLabel.Builder().text(localizer.getMessage("lblColors")).fontSize(18).build();
		final FTextPane noSettingsText = new FTextPane(localizer.getMessage("lblnoSettings"));

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
		left.add(new FLabel.Builder().text(localizer.getMessage("lblDistribution")).fontSize(18).build(), "gaptop 10");

		final JXButtonPanel poolTypePanel    = new JXButtonPanel();
		final String        radioConstraints = "h 25px!, gaptop 5";
		poolTypePanel.add(radBalanced, radioConstraints);
		poolTypePanel.add(radRandom, radioConstraints);
		poolTypePanel.add(radSurpriseMe, radioConstraints);
		poolTypePanel.add(radBoosters, radioConstraints);

		left.add(poolTypePanel, "gaptop 15");
		left.add(new FTextPane(localizer.getMessage("lblHoverforDescription")), "gaptop 20");

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

		radBalanced.setToolTipText(localizer.getMessage("lblradBalanced"));
		radRandom.setToolTipText(localizer.getMessage("lblradRandom"));
		radSurpriseMe.setToolTipText(localizer.getMessage("lblradSurpriseMe"));
		radBoosters.setToolTipText(localizer.getMessage("lblradBoosters"));
		cbxArtifacts.setToolTipText(localizer.getMessage("lblcbxArtifacts"));

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

		FButton btnCancel = new FButton(localizer.getMessage("lblCancel"));
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
