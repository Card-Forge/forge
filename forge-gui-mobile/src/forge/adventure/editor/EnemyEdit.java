package forge.adventure.editor;

import forge.adventure.data.EnemyData;

import javax.swing.*;
import java.awt.*;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EnemyEdit extends JComponent {
    EnemyData currentData;


    JTextField nameField=new JTextField();
    JSpinner lifeFiled= new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JSpinner spawnRate= new JSpinner(new SpinnerNumberModel(0.0, 0., 1, 0.1));
    JSpinner difficulty= new JSpinner(new SpinnerNumberModel(0.0, 0., 1, 0.1));
    JSpinner speed= new JSpinner(new SpinnerNumberModel(0.0, 0., 100., 1.0));
    FilePicker deck=new FilePicker(new String[]{"dck","json"});
    FilePicker atlas=new FilePicker(new String[]{"atlas"});
    RewardsEditor rewards=new RewardsEditor();
    SwingAtlasPreview preview=new SwingAtlasPreview();
    private boolean updating=false;

    public EnemyEdit()
    {

        JComponent center=new JComponent() {  };
        center.setLayout(new GridLayout(8,2));

        center.add(new JLabel("Name:")); center.add(nameField);
        center.add(new JLabel("Life:")); center.add(lifeFiled);
        center.add(new JLabel("Spawn rate:")); center.add(spawnRate);
        center.add(new JLabel("Difficulty:")); center.add(difficulty);
        center.add(new JLabel("Speed:")); center.add(speed);
        center.add(new JLabel("Deck:")); center.add(deck);
        center.add(new JLabel("Sprite:")); center.add(atlas);
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(center,BorderLayout.PAGE_START);
        add(rewards,BorderLayout.CENTER);
        add(preview,BorderLayout.LINE_START);

        atlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(()->updateEnemy()));
        nameField.getDocument().addDocumentListener(new DocumentChangeListener(()->updateEnemy()));
        deck.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(()->updateEnemy()));
        lifeFiled.addChangeListener(e -> updateEnemy());
        speed.addChangeListener(e -> updateEnemy());
        difficulty.addChangeListener(e -> updateEnemy());
        spawnRate.addChangeListener(e -> updateEnemy());
        rewards.addChangeListener(e -> updateEnemy());
        lifeFiled.addChangeListener(e -> updateEnemy());
        refresh();
    }

    private void updateEnemy() {
        if(currentData==null||updating)
            return;
        currentData.name=nameField.getText();
        currentData.life= (int) lifeFiled.getValue();
        currentData.sprite= atlas.getEdit().getText();
        currentData.speed=  ((Double)  speed.getValue()).floatValue();
        currentData.spawnRate=((Double)  spawnRate.getValue()).floatValue();
        currentData.difficulty=((Double)  difficulty.getValue()).floatValue();
        currentData.deck= deck.getEdit().getText();
        currentData.rewards= rewards.getRewards();
        preview.setSpritePath(currentData.sprite);
    }

    public void setCurrentEnemy(EnemyData data)
    {
        currentData=data;
        refresh();
    }

    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        updating=true;
        nameField.setText(currentData.name);
        lifeFiled.setValue(currentData.life);
        atlas.getEdit().setText(currentData.sprite);
        deck.getEdit().setText(currentData.deck);
        speed.setValue(new Float(currentData.speed).doubleValue());
        spawnRate.setValue(new Float(currentData.spawnRate).doubleValue());
        difficulty.setValue(new Float(currentData.difficulty).doubleValue());
        rewards.setRewards(currentData.rewards);
        preview.setSpritePath(currentData.sprite);
        updating=false;
    }
}