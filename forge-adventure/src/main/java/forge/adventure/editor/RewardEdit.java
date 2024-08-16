package forge.adventure.editor;

import forge.adventure.data.RewardData;
import forge.card.CardType;
import forge.game.keyword.Keyword;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Arrays;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class RewardEdit extends FormPanel {
    RewardData currentData;

    JComboBox typeField =new JComboBox(new String[] { "card", "gold", "life", "deckCard", "item","shards"});
    JSpinner probability = new JSpinner(new SpinnerNumberModel(0f, 0, 1, 0.1f));
    JSpinner count = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JSpinner addMaxCount = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    JTextField cardName =new JTextField();
    JTextField itemName =new JTextField();
    TextListEdit editions =new TextListEdit();
    TextListEdit colors =new TextListEdit(new String[] { "White", "Blue", "Black", "Red", "Green" });
    TextListEdit rarity =new TextListEdit(new String[] { "Basic Land", "Common", "Uncommon", "Rare", "Mythic Rare" });
    TextListEdit subTypes =new TextListEdit();
    TextListEdit cardTypes =new TextListEdit(Arrays.asList(CardType.CoreType.values()).stream().map(CardType.CoreType::toString).toArray(String[]::new));
    TextListEdit superTypes =new TextListEdit(Arrays.asList(CardType.Supertype.values()).stream().map(CardType.Supertype::toString).toArray(String[]::new));
    TextListEdit manaCosts =new TextListEdit();
    TextListEdit keyWords =new TextListEdit(Arrays.asList(Keyword.values()).stream().map(Keyword::toString).toArray(String[]::new));
    JComboBox colorType =new JComboBox(new String[] { "Any", "Colorless", "MultiColor", "MonoColor"});
    JTextField cardText =new JTextField();
    private boolean updating=false;

    public RewardEdit()
    {

        add("Type:",typeField);
        add("probability:",probability);
        add("count:",count);
        add("addMaxCount:",addMaxCount);
        add("cardName:",cardName);
        add("itemName:",itemName);
        add("editions:",editions);
        add("colors:",colors);
        add("rarity:",rarity);
        add("subTypes:",subTypes);
        add("cardTypes:",cardTypes);
        add("superTypes:",superTypes);
        add("manaCosts:",manaCosts);
        add("keyWords:",keyWords);
        add("colorType:",colorType);
        add("cardText:",cardText);


        typeField.addActionListener((e -> RewardEdit.this.updateReward()));
        probability.addChangeListener(e -> RewardEdit.this.updateReward());
        count.addChangeListener(e -> RewardEdit.this.updateReward());
        addMaxCount.addChangeListener(e -> RewardEdit.this.updateReward());
        cardName.getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        itemName.getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        editions.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        colors.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        rarity.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        subTypes.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        cardTypes.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        superTypes.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        manaCosts.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        keyWords.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));
        colorType.addActionListener((e -> RewardEdit.this.updateReward()));
        cardText.getDocument().addDocumentListener(new DocumentChangeListener(RewardEdit.this::updateReward));

    }

    private void updateReward() {
        if(currentData==null||updating)
            return;


        currentData.type=typeField.getSelectedItem()==null?null:typeField.getSelectedItem().toString();
        currentData.probability=((Double)probability.getValue()).floatValue();
        currentData.count= (int) count.getValue();
        currentData.addMaxCount= (int) addMaxCount.getValue();
        currentData.cardName = cardName.getText().isEmpty()?null:cardName.getText();
        currentData.itemNames = itemName.getText().isEmpty()?null:itemName.getText().split(",");
        currentData.editions = editions.getList();
        currentData.colors = colors.getList();
        currentData.rarity = rarity.getList();
        currentData.subTypes = subTypes.getList();
        currentData.cardTypes = cardTypes.getList();
        currentData.superTypes = superTypes.getList();
        currentData.manaCosts = manaCosts.getListAsInt();
        currentData.keyWords = keyWords.getList();
        currentData.colorType=colorType.getSelectedItem()==null?null:colorType.getSelectedItem().toString();
        currentData.cardText = cardText.getText().isEmpty()?null:cardText.getText();

        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }

    }
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
    public void setCurrentReward(RewardData data)
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
        typeField.setSelectedItem(currentData.type);

        probability.setValue((double) currentData.probability);
        count.setValue(currentData.count);
        addMaxCount.setValue(currentData.addMaxCount);
        cardName.setText(currentData.cardName);
        itemName.setText(currentData.itemName);
        editions.setText(currentData.editions);
        colors.setText(currentData.colors);
        rarity.setText(currentData.rarity);
        subTypes.setText(currentData.subTypes);
        cardTypes.setText(currentData.cardTypes);
        superTypes.setText(currentData.superTypes);
        manaCosts.setText(currentData.manaCosts);
        keyWords.setText(currentData.keyWords);
        colorType.setSelectedItem(currentData.colorType);
        cardText.setText(currentData.cardText);


        updating=false;
    }
}
