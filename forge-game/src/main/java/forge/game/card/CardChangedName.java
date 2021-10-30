package forge.game.card;

public class CardChangedName {

    protected String newName;
    protected boolean addNonLegendaryCreatureNames = false;

    public CardChangedName(String newName, boolean addNonLegendaryCreatureNames) {
        this.newName = newName;
        this.addNonLegendaryCreatureNames = addNonLegendaryCreatureNames;
    }

    public String getNewName() {
        return newName;
    }

    public boolean isOverwrite() {
        return newName != null;
    }

    public boolean isAddNonLegendaryCreatureNames() {
        return addNonLegendaryCreatureNames;
    }
}
