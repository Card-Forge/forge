package forge.screens.home.quest;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import forge.quest.QuestEventDraft;
import forge.quest.QuestUtil;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FTextArea;

public class PnlDraftEvent extends JPanel {
    private static final long serialVersionUID = 7348489421342846451L;

    private final Color clr2 = new Color(255, 255, 0, 0);
    private final SkinColor clr3 = FSkin.getColor(FSkin.Colors.CLR_THEME2).alphaColor(200);
    
    private final FRadioButton radButton;
    
    public PnlDraftEvent(final QuestEventDraft event) {
        super();

        radButton = new FRadioButton(event.getTitle());
        radButton.setFont(FSkin.getBoldFont(20));
        radButton.setIconTextGap(10);
        
        final FTextArea eventBoosters = new FTextArea();
        final FTextArea eventFee = new FTextArea();
        
        eventBoosters.setText(event.getBoosterList());
        eventBoosters.setFont(FSkin.getFont(12));

        eventFee.setText(QuestUtil.formatCredits(event.getEntryFee()) + " Credit Entry Fee");
        eventFee.setFont(FSkin.getFont(12));
        
        radButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (radButton.isSelected()) {
                    QuestUtil.setDraftEvent(event);
                }
            }
        });
        
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.add(radButton, "gap 25px 0 20px 0");
        this.add(eventBoosters, "w 100% - 25px!, gap 25px 0 15px 0");
        this.add(eventFee, "w 100% - 25px!, gap 25px 0 10px 0");
        
    }
    
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
