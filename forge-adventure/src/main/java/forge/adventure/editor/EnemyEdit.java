package forge.adventure.editor;

import forge.adventure.data.EnemyData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    JTextField equipment=new JTextField();
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
        center.add(new JLabel("Equipment:")); center.add(equipment);
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(center,BorderLayout.PAGE_START);
        add(rewards,BorderLayout.CENTER);
        add(preview,BorderLayout.LINE_START);

        equipment.getDocument().addDocumentListener(new DocumentChangeListener(new Runnable() {
            @Override
            public void run() {
                EnemyEdit.this.updateEnemy();
            }
        }));
        atlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(new Runnable() {
            @Override
            public void run() {
                EnemyEdit.this.updateEnemy();
            }
        }));
        nameField.getDocument().addDocumentListener(new DocumentChangeListener(new Runnable() {
            @Override
            public void run() {
                EnemyEdit.this.updateEnemy();
            }
        }));
        deck.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(new Runnable() {
            @Override
            public void run() {
                EnemyEdit.this.updateEnemy();
            }
        }));
        lifeFiled.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        speed.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        difficulty.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        spawnRate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        rewards.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        lifeFiled.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                EnemyEdit.this.updateEnemy();
            }
        });
        refresh();
    }

    private void updateEnemy() {
        if(currentData==null||updating)
            return;
        currentData.name=nameField.getText();
        currentData.life= (int) lifeFiled.getValue();
        currentData.sprite= atlas.getEdit().getText();
        if(equipment.getText().isEmpty())
            currentData.equipment=null;
        else
            currentData.equipment=equipment.getText().split(",");
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
        if(currentData.equipment!=null)
            equipment.setText(String.join(",",currentData.equipment));
        else
            equipment.setText("");
        deck.getEdit().setText(currentData.deck);
        speed.setValue(new Float(currentData.speed).doubleValue());
        spawnRate.setValue(new Float(currentData.spawnRate).doubleValue());
        difficulty.setValue(new Float(currentData.difficulty).doubleValue());
        rewards.setRewards(currentData.rewards);
        preview.setSpritePath(currentData.sprite);
        updating=false;
    }
}