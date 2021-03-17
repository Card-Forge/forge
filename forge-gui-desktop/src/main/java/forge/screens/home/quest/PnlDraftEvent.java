package forge.screens.home.quest;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import forge.gamemodes.quest.QuestEventDraft;
import forge.gamemodes.quest.QuestUtil;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FTextArea;
import net.miginfocom.swing.MigLayout;

public class PnlDraftEvent extends JPanel {
	private static final long serialVersionUID = 7348489421342846451L;

	private final Color clr2 = new Color(255, 255, 0, 0);
	private final SkinColor clr3 = FSkin.getColor(FSkin.Colors.CLR_THEME2).alphaColor(200);

	private final FRadioButton radButton;

	public PnlDraftEvent(final QuestEventDraft event) {
		super();

		radButton = new FRadioButton(event.getTitle());
		radButton.setFont(FSkin.getRelativeBoldFont(20));
		radButton.setIconTextGap(10);

		final FTextArea eventBoosters = new FTextArea();
		final FTextArea eventFee = new FTextArea();

		eventBoosters.setText(event.getBoosterList());
		eventBoosters.setFont(FSkin.getFont());

		eventFee.setText(QuestUtil.formatCredits(event.getEntryFee()) + " Credit Entry Fee");
		eventFee.setFont(FSkin.getFont());

		radButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (radButton.isSelected()) {
					QuestUtil.setDraftEvent(event);
				}
			}
		});

		this.addMouseListener(mouseListener);
		eventBoosters.addMouseListener(mouseListener);
		eventFee.addMouseListener(mouseListener);

		this.setOpaque(false);
		this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
		this.add(radButton, "gap 25px 0 20px 0");
		this.add(eventBoosters, "w 100% - 25px!, gap 25px 0 15px 0");
		this.add(eventFee, "w 100% - 25px!, gap 25px 0 10px 0");

	}

	private MouseAdapter mouseListener = new MouseAdapter() {

		@Override
		public void mouseEntered(final MouseEvent e) {
			radButton.getModel().setRollover(true);
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			radButton.getModel().setRollover(false);
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			mousePressed(e);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				VSubmenuQuestDraft.SINGLETON_INSTANCE.getBtnStartDraft().doClick();
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				radButton.setSelected(true);
			}
		}

	};

	public FRadioButton getRadioButton() {
		return radButton;
	}

	@Override
	public void paintComponent(final Graphics g) {

		Graphics2D g2d = (Graphics2D) g.create();

		FSkin.setGraphicsGradientPaint(g2d, 0, 0, clr3, (int) (getWidth() * 0.75), 0, clr2);
		g2d.fillRect(0, 0, (int) (getWidth() * 0.75), getHeight());

		g2d.dispose();

	}

}
