package forge.adventure.editor;

import forge.adventure.data.EnemyData;

import javax.swing.*;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EnemyEdit extends FormPanel {
    EnemyData currentData;
    JTextField nameField=new JTextField();
    JTextField colorField=new JTextField();
    JTextField ai=new JTextField();
    JCheckBox flying=new JCheckBox();
    JCheckBox boss=new JCheckBox();
    FloatSpinner lifeFiled= new FloatSpinner(0, 1000, 1);
    FloatSpinner spawnRate= new FloatSpinner(  0.f, 1, 0.1f);
    FloatSpinner difficulty= new FloatSpinner(  0.f, 1, 0.1f);
    FloatSpinner speed= new FloatSpinner( 0.f, 100.f, 1.0f);
    FilePicker deck=new FilePicker(new String[]{"dck","json"});
    FilePicker atlas=new FilePicker(new String[]{"atlas"});
    JTextField equipment=new JTextField();
    RewardsEditor rewards=new RewardsEditor();
    SwingAtlasPreview preview=new SwingAtlasPreview();
    private boolean updating=false;

    public EnemyEdit()
    {

        FormPanel center=new FormPanel() {  };

        center.add("Name:",nameField);
        center.add("Life:",lifeFiled);
        center.add("Spawn rate:",spawnRate);
        center.add("Difficulty:",difficulty);
        center.add("Speed:",speed);
        center.add("Deck:",deck);
        center.add("Sprite:",atlas);
        center.add("Equipment:",equipment);
        center.add("Colors:",colorField);

        center.add("ai:",ai);
        center.add("flying:",flying);
        center.add("boss:",boss);


        add(preview);
        add(center);
        add(rewards);

        equipment.getDocument().addDocumentListener(new DocumentChangeListener(() -> EnemyEdit.this.updateEnemy()));
        atlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> EnemyEdit.this.updateEnemy()));
        colorField.getDocument().addDocumentListener(new DocumentChangeListener(() -> EnemyEdit.this.updateEnemy()));
        ai.getDocument().addDocumentListener(new DocumentChangeListener(() -> EnemyEdit.this.updateEnemy()));
        flying.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        boss.addChangeListener(e -> EnemyEdit.this.updateEnemy());

        nameField.getDocument().addDocumentListener(new DocumentChangeListener(() -> EnemyEdit.this.updateEnemy()));
        deck.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> EnemyEdit.this.updateEnemy()));
        lifeFiled.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        speed.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        difficulty.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        spawnRate.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        rewards.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        lifeFiled.addChangeListener(e -> EnemyEdit.this.updateEnemy());
        refresh();
    }

    private void updateEnemy() {
        if(currentData==null||updating)
            return;
        currentData.name=nameField.getText();
        currentData.colors=colorField.getText();
        currentData.ai=ai.getText();
        currentData.flying=flying.isSelected();
        currentData.boss=boss.isSelected();
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
        colorField.setText(currentData.colors);
        ai.setText(currentData.ai);
        boss.setSelected(currentData.boss);
        flying.setSelected(currentData.flying);
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