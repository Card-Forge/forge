package forge.screens.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestEventDuel;
import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.bazaar.QuestPetController;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.toolbox.JXButtonPanel;
import forge.util.Localizer;

/**
 * Controls the quest duels submenu in the home UI.
 * <p>
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CSubmenuDuels implements ICDoc {

	SINGLETON_INSTANCE;

	@Override
	public void register() {
	}

	/* (non-Javadoc)
	 * @see forge.gui.control.home.IControlSubmenu#initialize()
	 */
	@SuppressWarnings("serial")
	@Override
	public void initialize() {
		final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

		view.getBtnSpellShop().setCommand(
				new UiCommand() {
					@Override
					public void run() {
						QuestUtil.showSpellShop();
					}
				});

		view.getBtnBazaar().setCommand(
				new UiCommand() {
					@Override
					public void run() {
						QuestUtil.showBazaar();
					}
				});

		view.getBtnTravel().setCommand(
				new UiCommand() {
					@Override
					public void run() {
						QuestUtil.travelWorld();
						CSubmenuDuels.this.update();
					}
				});

		view.getBtnUnlock().setCommand(
				new UiCommand() {
					@Override
					public void run() {
						QuestUtil.chooseAndUnlockEdition();
						CSubmenuDuels.this.update();
					}
				});

		view.getBtnStart().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						QuestUtil.startGame();
					}
				});

		final QuestController quest = FModel.getQuest();
		view.getCbPlant().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				quest.selectPet(0, view.getCbPlant().isSelected() ? "Plant" : null);
				quest.save();
			}
		});

		view.getCbxMatchLength().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				String match = view.getCbxMatchLength().getSelectedItem();
				if (match != null) {
					quest.setMatchLength(match.substring(match.length() - 1));
					quest.save();
				}
			}
		});

		view.getCbxPet().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final int slot = 1;
				final int index = view.getCbxPet().getSelectedIndex();
				final List<QuestPetController> pets = quest.getPetsStorage().getAvaliablePets(slot, quest.getAssets());
				final String petName = index <= 0 || index > pets.size() ? null : pets.get(index - 1).getName();
				quest.selectPet(slot, petName);
				quest.save();
			}
		});

	}

	private final KeyAdapter startOnEnter = new KeyAdapter() {
		@Override
		public void keyPressed(final KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_ENTER) {
				VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().doClick();
			}
		}
	};

	private final MouseAdapter mouseClickListener = new MouseAdapter() {

		@Override
		public void mouseEntered(final MouseEvent e) {
			if (e.getComponent() instanceof PnlEvent) {
				((PnlEvent) e.getComponent()).getRad().getModel().setRollover(true);
			} else {
				((PnlEvent) e.getComponent().getParent()).getRad().getModel().setRollover(true);
			}
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			if (e.getComponent() instanceof PnlEvent) {
				((PnlEvent) e.getComponent()).getRad().getModel().setRollover(false);
			} else {
				((PnlEvent) e.getComponent().getParent()).getRad().getModel().setRollover(false);
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			mousePressed(e);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().doClick();
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getComponent() instanceof PnlEvent) {
					((PnlEvent) e.getComponent()).getRad().setSelected(true);
				} else {
					((PnlEvent) e.getComponent().getParent()).getRad().setSelected(true);
				}
			}
		}
	};

	/* (non-Javadoc)
	 * @see forge.gui.control.home.IControlSubmenu#update()
	 */
	@Override
	public void update() {
		QuestUtil.updateQuestView(VSubmenuDuels.SINGLETON_INSTANCE);

		final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

		if (FModel.getQuest().getAchievements() != null) {
			final Localizer localizer = Localizer.getInstance();
			view.getLblTitle().setText(localizer.getMessage("lblDuels") + ": " + FModel.getQuest().getRank());

			view.getPnlDuels().removeAll();
			final List<QuestEventDuel> duels = FModel.getQuest().getDuelsManager().generateDuels();

			final JXButtonPanel grpPanel = new JXButtonPanel();

			assert duels != null;
			for (int i = 0; i < duels.size(); i++) {
				final PnlEvent temp = new PnlEvent(duels.get(i));
				final JRadioButton rad = temp.getRad();
				if (i == 0) {
					rad.setSelected(true);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							rad.requestFocusInWindow();
						}
					});
				}
				temp.addKeyListener(startOnEnter);
				temp.addMouseListener(mouseClickListener);
				grpPanel.add(temp, rad, "w 100%!, h 95px!, gapy 8px");
			}
			view.getPnlDuels().add(grpPanel, "w 100%!");

			StringBuilder sb = new StringBuilder();
			sb.append(localizer.getMessage("lblMatchBestof")).append(" ").append(FModel.getQuest().getMatchLength());
			view.getCbxMatchLength().setSelectedItem(sb.toString());
		}
	}

}
