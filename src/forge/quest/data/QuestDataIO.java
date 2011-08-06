package forge.quest.data;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import forge.Deck;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.item.QuestInventory;
import forge.quest.data.pet.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class QuestDataIO {
    public QuestDataIO() {
    }

    static public QuestData loadData() {
        try {
            //read file "questData"
            QuestData data = null;

            File xmlSaveFile = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);

            //if the new file format does not exist, convert the old one and save it as the new copy

            if (!xmlSaveFile.exists()) {
                data = convertDeprecatedSaveFormat();
                data.saveData();
            }

            else {
                GZIPInputStream zin = new GZIPInputStream(new FileInputStream(xmlSaveFile));

                StringBuilder xml = new StringBuilder();
                char[] buf = new char[1024];
                InputStreamReader reader = new InputStreamReader(zin);
                while (reader.ready()) {
                    int len = reader.read(buf);
                    xml.append(buf, 0, len);
                }

                IgnoringXStream xStream = new IgnoringXStream();
                data = (QuestData) xStream.fromXML(xml.toString());

                if (data.versionNumber != QuestData.CURRENT_VERSION_NUMBER){
                    updateSaveFile(data,xml.toString());
                }

                zin.close();
            }
            return data;
        }

        catch (Exception ex) {
            ErrorViewer.showError(ex, "Error loading Quest Data");
            throw new RuntimeException(ex);
        }
    }

    private static void updateSaveFile(QuestData newData, String input) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(input));
            Document document = builder.parse(is);


            switch (newData.versionNumber) {//There should be a fall-through b/w the cases so that each version's changes get applied progressively
                case 0: // First beta release with new file format, inventory needs to be migrated
                    newData.inventory = new QuestInventory();
                    NodeList elements = document.getElementsByTagName("estatesLevel");
                    newData.getInventory().setItemLevel("Estates",Integer.parseInt(elements.item(0).getTextContent()));
                    elements = document.getElementsByTagName("luckyCoinLevel");
                    newData.getInventory().setItemLevel("Lucky Coin",Integer.parseInt(elements.item(0).getTextContent()));
                    elements = document.getElementsByTagName("sleightOfHandLevel");
                    newData.getInventory().setItemLevel("Sleight",Integer.parseInt(elements.item(0).getTextContent()));
                    elements = document.getElementsByTagName("gearLevel");

                    int gearLevel = Integer.parseInt(elements.item(0).getTextContent());
                    if (gearLevel >= 1) {
                        newData.inventory.setItemLevel("Map", 1);
                    }
                    if (gearLevel == 2) {
                        newData.inventory.setItemLevel("Zeppelin", 1);
                    }
                    break;
            }

            //mark the QD as the latest version
            newData.versionNumber = QuestData.CURRENT_VERSION_NUMBER;

        } catch (Exception e) {
            forge.error.ErrorViewer.showError(e);
        }
    }

    public static void saveData(QuestData qd) {
        try {
            File f = ForgeProps.getFile(NewConstants.QUEST.XMLDATA);
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
            GZIPOutputStream zout = new GZIPOutputStream(bout);

            XStream xStream = new XStream();
            xStream.toXML(qd, zout);

            zout.flush();
            zout.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex, "Error saving Quest Data");
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings({"deprecation"})
    public static QuestData convertDeprecatedSaveFormat() {
        forge.QuestData oldData = forge.QuestData.loadData();
        QuestData newData = new QuestData();


        newData.difficulty = oldData.getDifficulty();
        newData.diffIndex = oldData.getDiffIndex();
        newData.rankIndex = oldData.getWin() / QuestPreferences.getWinsForRankIncrease(newData.diffIndex);

        newData.win = oldData.getWin();
        newData.lost = oldData.getLost();

        newData.life = oldData.getLife();
        newData.inventory.setItemLevel("Estates", oldData.getEstatesLevel());
        newData.inventory.setItemLevel("Lucky Coin", oldData.getLuckyCoinLevel());
        newData.inventory.setItemLevel("Sleight", oldData.getSleightOfHandLevel());
        if (oldData.getGearLevel() >= 1) {
            newData.inventory.setItemLevel("Map", 1);
        }
        if (oldData.getGearLevel() == 2) {
            newData.inventory.setItemLevel("Zeppelin", 1);
        }

        newData.questsPlayed = oldData.getQuestsPlayed();
        newData.credits = oldData.getCredits();
        newData.mode = oldData.getMode();

        newData.myDecks = new HashMap<String, Deck>();
        for (String deckName : oldData.getDeckNames()) {
            newData.myDecks.put(deckName, oldData.getDeck(deckName));
        }

        newData.cardPool = oldData.getCardpool();
        newData.newCardList = oldData.getAddedCards();
        newData.shopList = oldData.getShopList();

        newData.availableQuests = oldData.getAvailableQuests();
        newData.completedQuests = oldData.getCompletedQuests();

        QuestPetAbstract newPet;

        if (oldData.getBirdPetLevel() > 0) {
            newPet = new QuestPetBird();
            newPet.setLevel(oldData.getBirdPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getHoundPetLevel() > 0) {
            newPet = new QuestPetHound();
            newPet.setLevel(oldData.getHoundPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getWolfPetLevel() > 0) {
            newPet = new QuestPetWolf();
            newPet.setLevel(oldData.getWolfPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getCrocPetLevel() > 0) {
            newPet = new QuestPetCrocodile();
            newPet.setLevel(oldData.getCrocPetLevel());
            newData.petManager.addPet(newPet);
        }
        if (oldData.getPlantLevel() > 0) {
            newPet = new QuestPetPlant();
            newPet.setLevel(oldData.getPlantLevel());
            newData.petManager.getPlant().setLevel(oldData.getPlantLevel());
        }

        newData.getPetManager().setSelectedPet(null);

        return newData;
    }

    /**
     * Xstream subclass that ignores fields that are present in the save but not in the class
     */
    private static class IgnoringXStream extends XStream {
        List<String> ignoredFields = new ArrayList<String>();

        @Override
        protected MapperWrapper wrapMapper(MapperWrapper next) {
            return new MapperWrapper(next) {
                @Override
                public boolean shouldSerializeMember(Class definedIn,
                                                     String fieldName) {
                    if (definedIn == Object.class) {
                        ignoredFields.add(fieldName);
                        return false;
                    }
                    return super.shouldSerializeMember(definedIn, fieldName);
                }
            };
        }

        public List<String> getIgnoredFields() {
            return ignoredFields;
        }
    }
}