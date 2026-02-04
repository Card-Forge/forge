package forge.adventure.stage;


import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.StaticData;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.InnScene;
import forge.adventure.scene.InventoryScene;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Current;
import forge.adventure.util.Paths;
import forge.adventure.world.WorldSave;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.screens.CoverScreen;
import forge.util.Aggregates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleCommandInterpreter {
    private static ConsoleCommandInterpreter instance;
    Command root = new Command();

    static class Command {
        HashMap<String, Command> children = new HashMap<>();
        Function<String[], String> function;
    }

    public String complete(String text) {
        String[] words = splitOnSpace(text);
        Command currentCommand = root;
        StringBuilder completionString = new StringBuilder();
        for (String name : words) {
            if (!currentCommand.children.containsKey(name)) {
                for (String key : currentCommand.children.keySet()) {
                    if (key.startsWith(name)) {
                        return completionString + key + " ";
                    }
                }
                break;
            }
            completionString.append(name).append(" ");
            currentCommand = currentCommand.children.get(name);
        }
        return text;
    }

    private String[] splitOnSpace(String text) {
        List<String> matchList = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(text);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                matchList.add(regexMatcher.group(2));
            } else {
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[0]);
    }

    public String command(String text) {
        String[] words = splitOnSpace(text);
        Command currentCommand = root;
        int i;

        for (i = 0; i < words.length; i++) {
            String name = words[i];
            if (!currentCommand.children.containsKey(name)) break;
            currentCommand = currentCommand.children.get(name);
        }
        if (currentCommand.function == null) {
            return "Command not found. Available commands:\n" + String.join(" ", Arrays.copyOfRange(words, 0, i)) + "\n" + String.join("\n", currentCommand.children.keySet());
        }
        String[] parameters = Arrays.copyOfRange(words, i, words.length);
        // this removes apostrophe...
        /*for (int j = 0; j < parameters.length; j++)
            parameters[j] = parameters[j].replaceAll("[\"']", "");*/
        return currentCommand.function.apply(parameters);
    }

    void registerCommand(String[] path, Function<String[], String> function) {
        if (path.length == 0) return;
        Command currentCommand = root;

        for (String name : path) {
            if (!currentCommand.children.containsKey(name))
                currentCommand.children.put(name, new Command());
            currentCommand = currentCommand.children.get(name);
        }
        currentCommand.function = function;
    }

    public static ConsoleCommandInterpreter getInstance() {
        if (instance == null)
            instance = new ConsoleCommandInterpreter();
        return instance;
    }

    GameStage currentGameStage() {
        return MapStage.getInstance().isInMap() ? MapStage.getInstance() : WorldStage.getInstance();
    }

    PlayerSprite currentSprite() {
        return currentGameStage().getPlayerSprite();
    }

    private ConsoleCommandInterpreter() {
        registerCommand(new String[]{"teleport", "to"}, s -> {
            if (s.length < 2)
                return "Command needs 2 parameter";
            try {
                int x = Integer.parseInt(s[0]);
                int y = Integer.parseInt(s[1]);
                WorldStage.getInstance().setPosition(new Vector2(x, y));
                WorldStage.getInstance().player.playEffect(Paths.EFFECT_TELEPORT, 10);
                return "teleport to (" + s[0] + "," + s[1] + ")";
            } catch (Exception e) {
                return "Exception occurred, Invalid input";
            }
        });
        registerCommand(new String[]{"teleport", "to", "poi"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: PoI name.";
            PointOfInterest poi = Current.world().findPointsOfInterest(s[0]);
            if (poi == null)
                return "PoI " + s[0] + " not found";

            Forge.advFreezePlayerControls = true;
            FThreads.invokeInEdtNowOrLater(() -> Forge.setTransitionScreen(new CoverScreen(() -> {
                Forge.advFreezePlayerControls = false;
                WorldStage.getInstance().setPosition(new Vector2(poi.getPosition().x - 16f, poi.getPosition().y + 16f));
                WorldStage.getInstance().loadPOI(poi);
                Forge.clearTransitionScreen();
            }, Forge.takeScreenshot())));
            return "Teleported to " + s[0] + "(" + poi.getPosition() + ")";
        });
        registerCommand(new String[]{"spawn", "enemy"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: enemy name.";

            if (WorldStage.getInstance().spawn(s[0]))
                return "Spawn " + s[0];
            return "Can not find enemy " + s[0];
        });
        registerCommand(new String[]{"give", "gold"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount.";
            int amount;
            try {
                amount = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to number";
            }
            Current.player().giveGold(amount);
            return "Added " + amount + " gold";
        });
        registerCommand(new String[]{"give", "quest"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: QuestID";
            int ID;
            try {
                ID = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to number";
            }
            Current.player().addQuest(ID, false);
            return "Quest generated";
        });
        registerCommand(new String[]{"give", "shards"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount.";
            int amount;
            try {
                amount = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to number";
            }
            Current.player().addShards(amount);
            return "Added " + amount + " shards";
        });
        registerCommand(new String[]{"give", "life"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount.";
            int amount;
            try {
                amount = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to number";
            }
            Current.player().addMaxLife(amount);
            return "Added " + amount + " max life";
        });
        registerCommand(new String[]{"leave"}, s -> {
            if (!MapStage.getInstance().isInMap()) return "not on a map";
            MapStage.getInstance().exitDungeon(false, false);
            return "Got out";
        });
        registerCommand(new String[]{"debug", "collision"}, s -> {
            currentGameStage().debugCollision(true);
            return "Got out";
        });
        registerCommand(new String[]{"give", "card"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Card name.";
            PaperCard card = StaticData.instance().fetchCard(s[0]);
            if (card == null) return "Cannot find card: " + s[0];
            if (s.length >= 2) {
                try {
                    int amount = Integer.parseInt(s[1]);
                    Current.player().addCard(card, amount);
                    return String.format("Added %d cards: %s", amount, card.getName());
                } catch (NumberFormatException ignored) {
                }
            }
            Current.player().addCard(card);
            return "Added card: " + card.getName();
        });
        registerCommand(new String[]{"give", "nosell", "card"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Card name.";
            PaperCard card = StaticData.instance().fetchCard(s[0]);
            if (card == null) return "Cannot find card: " + s[0];
            if (s.length >= 2) {
                try {
                    int amount = Integer.parseInt(s[1]);
                    Current.player().addCard(card.getNoSellVersion(), amount);
                    return String.format("Added %d cards: %s", amount, card.getName());
                } catch (NumberFormatException ignored) {
                }
            }
            Current.player().addCard(card.getNoSellVersion());
            return "Added card: " + card.getName();
        });
        registerCommand(new String[]{"give", "print"}, s -> {
            if (s.length < 2) return "Command needs 2 parameters: Set code, collector number.";
            CardEdition edition = StaticData.instance().getCardEdition(s[0]);
            if (edition == null) return "Cannot find edition: " + s[0];
            CardEdition.EditionEntry cis = edition.getCardFromCollectorNumber(s[1]);
            if (cis == null)
                return String.format("Set '%s' does not have a card with collector number '%s'.", edition.getName(), s[1]);
            PaperCard card = StaticData.instance().fetchCard(cis.name(), edition.getCode(), cis.collectorNumber());
            if (card == null) {
                //Found in the set, not supported.
                return String.format("Failed to fetch (%s, %s, %s) - Not currently supported.", cis.name(), edition.getCode(), cis.collectorNumber());
            }
            if (s.length >= 3) {
                try {
                    int amount = Integer.parseInt(s[2]);
                    Current.player().addCard(card, amount);
                    return String.format("Added %d cards: %s", amount, card.getName());
                } catch (NumberFormatException ignored) {
                }
            }
            Current.player().addCard(card);
            return "Added card: " + card.getName();
        });
        registerCommand(new String[]{"give", "set"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Edition code.";
            CardEdition edition = StaticData.instance().getCardEdition(s[0]);
            if (edition == null) return "Cannot find edition: " + s[0];

            for (CardEdition.EditionEntry entry : edition.getObtainableCards()) {
                PaperCard card = StaticData.instance().fetchCard(entry.name(), edition.getCode(), entry.collectorNumber());

                if (card != null) {
                    Current.player().addCard(card.getNoSellVersion(), 4);
                } else {
                    System.out.println("Card " + entry.name() + " (" + entry.collectorNumber() + ") does not exist.");
                }
            }

            return "Added all cards from: " + edition.getCode();
        });
        registerCommand(new String[]{"give", "boosters"}, s -> {
            if (s.length < 1)
                return "Command needs at least 1 parameter: Edition code.";
            CardEdition edition = StaticData.instance().getCardEdition(s[0]);
            if (edition == null)
                return "Cannot find edition: " + s[0];
            if (!edition.hasBoosterTemplate())
                return edition.getCode() + " doesn't have a booster template.";

            int amount = 1;
            if (s.length >= 2) {
                try {
                    amount = Integer.parseInt(s[1]);
                } catch (NumberFormatException ignored) {
                }
            }

            for (int i = 0; i < amount; i++) {
                Current.player().addBooster(AdventureEventController.instance().generateBooster(edition.getCode()));
            }

            return "Added " + amount + " " + edition.getCode() + " booster(s)";
        });
        registerCommand(new String[]{"clearnosell"}, s -> {
            CardPool cards = Current.player().getCards();
            for (PaperCard c : cards.getFilteredPool(c -> c.getMarkedFlags().noSellValue).toFlatList()) {
                cards.remove(c);
            }
            return "Removed all no sell flagged cards.";
        });
        registerCommand(new String[]{"give", "item"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Item name.";
            if (Current.player().addItem(s[0])) {
                if (s[0].contains("Key"))
                    GameHUD.getInstance().updateKeys();
                return "Added item " + s[0] + ".";
            }
            return "Cannot find item " + s[0];
        });
        registerCommand(new String[]{"fullHeal"}, s -> {
            Current.player().fullHeal();
            currentSprite().playEffect(Paths.EFFECT_HEAL);
            return "Player fully healed. Health set to " + Current.player().getLife() + ".";
        });
        registerCommand(new String[]{"listPOI"}, s -> {
            ArrayList<String> poiNames = new ArrayList<>();
            List<BiomeData> biomeData = WorldSave.getCurrentSave().getWorld().getData().GetBiomes();
            for (BiomeData data : biomeData) {
                for (PointOfInterestData poi : data.getPointsOfInterest())
                    poiNames.add(poi.name + " - " + poi.type);
            }
            System.out.println("POI Names - Types\n" + String.join("\n", poiNames));
            return "POI lists dumped to stdout.";
        });
        registerCommand(new String[]{"reveal"}, s -> {
            WorldSave save = WorldSave.getCurrentSave();
            if (save == null || save.getWorld() == null) {
                return "No world loaded.";
            }
            if (s.length < 1) {
                return "Please specify at least one PointOfInterest type (e.g. \"reveal cave\", \"reveal dungeon\", \"reveal town\", \"reveal capital\").";
            }
            HashSet<String> types = new HashSet<>();
            for (String type : s) {
                String normalized = type.toLowerCase();
                if (normalized.endsWith("ies")) {
                    normalized = normalized.substring(0, normalized.length() - 3) + "y";
                } else if (normalized.endsWith("s") && normalized.length() > 1) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                if ("city".equals(normalized)) {
                    normalized = "town";
                }
                if (!normalized.isEmpty()) {
                    types.add(normalized);
                }
            }
            if (types.isEmpty()) {
                return "Please specify at least one PointOfInterest type (e.g. \"reveal cave\", \"reveal dungeon\", \"reveal town\", \"reveal capital\").";
            }
            int revealed = 0;
            for (PointOfInterest poi : save.getWorld().getAllPointOfInterest()) {
                PointOfInterestData data = poi.getData();
                if (data == null || data.type == null) {
                    continue;
                }
                if (!types.contains(data.type.toLowerCase())) {
                    continue;
                }
                if (!save.getPointOfInterestChanges(poi.getID()).isVisited()) {
                    save.getPointOfInterestChanges(poi.getID()).visit();
                    revealed++;
                }
            }
            ArrayList<String> sortedTypes = new ArrayList<>(types);
            Collections.sort(sortedTypes);
            return "Revealed " + revealed + " " + String.join("/", sortedTypes) + " names on the map.";
        });
        registerCommand(new String[]{"setColorID"}, s -> {
            if (s.length < 1)
                return "Please specify color ID: Valid choices: B, G, R, U, W, C. Example:\n\"setColorID G\"";
            Current.player().setColorIdentity(s[0]);
            return "Player color identity set to " + Current.player().getColorIdentity() + ".";
        });
        registerCommand(new String[]{"resetQuests"}, s -> {
            Current.player().resetQuestFlags();
            return "All global quest flags have been reset.";
        });
        registerCommand(new String[]{"resetMapQuests"}, s -> {
            if (!MapStage.getInstance().isInMap()) return "Only supported inside a map.";
            MapStage.getInstance().resetQuestFlags();
            return "All local quest flags have been reset.";
        });
        registerCommand(new String[]{"dumpEnemyDeckColors"}, s -> {
            for (EnemyData E : new Array.ArrayIterator<>(WorldData.getAllEnemies())) {
                Deck D = E.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck() || Current.player().isHardorInsaneDifficulty());
                DeckProxy DP = new DeckProxy(D, "Constructed", GameType.Constructed, null);
                ColorSet colorSet = DP.getColor();
                System.out.printf("%s: Colors: %s (%s%s%s%s%s%s)\n", D.getName(), DP.getColor(),
                        (colorSet.hasBlack() ? "B" : ""),
                        (colorSet.hasGreen() ? "G" : ""),
                        (colorSet.hasRed() ? "R" : ""),
                        (colorSet.hasBlue() ? "U" : ""),
                        (colorSet.hasWhite() ? "W" : ""),
                        (colorSet.isColorless() ? "C" : "")
                );
            }
            return "Enemy deck color list dumped to stdout.";
        });
        registerCommand(new String[]{"dumpEnemyDeckList"}, s -> {
            for (EnemyData E : new Array.ArrayIterator<>(WorldData.getAllEnemies())) {
                Deck D = E.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck() || Current.player().isHardorInsaneDifficulty());
                DeckProxy DP = new DeckProxy(D, "Constructed", GameType.Constructed, null);
                System.out.printf("Deck: %s\n%s\n\n", D.getName(), DP.getDeck().getMain().toCardList("\n")
                );
            }
            return "Enemy deck list dumped to stdout.";
        });
        registerCommand(new String[]{"dumpEnemyColorIdentity"}, s -> {
            for (EnemyData E : new Array.ArrayIterator<>(WorldData.getAllEnemies())) {
                Deck D = E.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck() || Current.player().isHardorInsaneDifficulty());
                DeckProxy DP = new DeckProxy(D, "Constructed", GameType.Constructed, null);
                System.out.printf("%s Colors: %s | Deck Colors: %s (%s)%s\n", E.name, E.colors, DP.getColorIdentity().toEnumSet().toString(), DP.getName()
                        , E.boss ? " - BOSS" : "");
            }
            return "Enemy color Identity dumped to stdout.";
        });
        registerCommand(new String[]{"heal", "amount"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount";
            int N;
            try {
                N = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to integer";
            }
            Current.player().heal(N);
            currentSprite().playEffect(Paths.EFFECT_HEAL);
            return "Player healed to " + Current.player().getLife() + "/" + Current.player().getMaxLife();
        });
        registerCommand(new String[]{"heal", "percent"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount";
            float value;
            try {
                value = Float.parseFloat(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to integer";
            }
            Current.player().heal(value);
            currentSprite().playEffect(Paths.EFFECT_HEAL);
            return "Player healed to " + Current.player().getLife() + "/" + Current.player().getMaxLife();
        });
        registerCommand(new String[]{"heal", "full"}, s -> {
            Current.player().fullHeal();
            currentSprite().playEffect(Paths.EFFECT_HEAL);
            return "Player healed to " + Current.player().getLife() + "/" + Current.player().getMaxLife();
        });

        registerCommand(new String[]{"getShards", "amount"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount";
            int value;
            try {
                value = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to integer";
            }
            Current.player().addShards(value);
            return "Player now has " + Current.player().getShards() + " shards";
        });
        registerCommand(new String[]{"debug", "map"}, s -> {
            GameHUD.getInstance().setDebug(true);
            return "Debug map ON";
        });
        registerCommand(new String[]{"debug", "off"}, s -> {
            GameHUD.getInstance().setDebug(true);
            currentGameStage().debugCollision(false);
            return "Debug  OFF";
        });
        registerCommand(new String[]{"remove", "enemy", "all"}, s -> {
            if (!MapStage.getInstance().isInMap()) {
                WorldStage ws = WorldStage.getInstance();
                int enemiesCount = ws.enemies.size();
                for (int i = 0; i < enemiesCount; i++) {
                    ws.removeNearestEnemy();
                }
            } else {
                MapStage.getInstance().removeAllEnemies();
            }
            return "Removed all enemies";
        });

        registerCommand(new String[]{"hide"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount";
            float value;
            try {
                value = Float.parseFloat(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to float";
            }
            currentGameStage().hideFor(value);
            return "removed all enemies";
        });

        registerCommand(new String[]{"fly"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount";
            float value;
            try {
                value = Float.parseFloat(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to float";
            }
            currentGameStage().flyFor(value);
            return "removed all enemies";
        });
        registerCommand(new String[]{"sprint"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Amount";
            float value;
            try {
                value = Float.parseFloat(s[0]);
            } catch (Exception e) {
                return "Can not convert " + s[0] + " to float";
            }
            currentGameStage().sprintFor(value);
            return "removed all enemies";
        });
        registerCommand(new String[]{"remove", "enemy", "nearest"}, s -> {
            WorldStage.getInstance().removeNearestEnemy();
            return "removed all enemies";
        });
        registerCommand(new String[]{"remove", "enemy"}, s -> {
            if (s.length < 1) return "Command needs 1 parameter: Enemy map ID.";
            int id;
            try {
                id = Integer.parseInt(s[0]);
            } catch (Exception e) {
                return "Cannot convert " + s[0] + " to number";
            }
            if (!MapStage.getInstance().isInMap())
                return "Only supported for PoI";
            MapStage.getInstance().deleteObject(id);
            return "Removed enemy " + s[0];
        });
        // this is for test purposes unless you want to crack your items
        registerCommand(new String[]{"crack"}, s -> {
            ItemData itemData = Current.player().getRandomEquippedItem();
            String value = Current.player().isHardorInsaneDifficulty() ? "items" : "armor";
            String message = "Ok, no equipped " + value + " to crack... :)";
            if (itemData != null) {
                itemData.isCracked = true;
                Current.player().equip(itemData); //Unequipped the itemData
                InventoryScene.instance().clearItemDescription();
                message = itemData.name + " " + Forge.getLocalizer().getMessage("lblCracked");
            }
            return message;
        });
        registerCommand(new String[]{"set", "event"}, s -> {
            if(s.length < 1) return "Command needs 1 parameter: Block or edition name. ";
            String blockName = s[0];
            if(MapStage.getInstance().findLocalInn() == null)
                return "Must be used within a town with an inn.";
            CardBlock eventCardBlock = FModel.getBlocks().find(b -> b.getName().equalsIgnoreCase(blockName));
            if(eventCardBlock == null) {
                CardEdition edition = FModel.getMagicDb().getEditions().find(e -> e.getCode().equalsIgnoreCase(blockName) || e.getName().equalsIgnoreCase(blockName));
                if(edition == null)
                    return "Unable to find edition or block: " + blockName;
                eventCardBlock = Aggregates.random(AdventureEventData.getValidDraftBlocks(List.of(edition)));
                if(eventCardBlock == null)
                    return "Unable to find a valid event block that exclusively contains edition " + edition.getName();
            }
            AdventureEventController.EventFormat eventFormat = s.length > 1 ? AdventureEventController.EventFormat.smartValueOf(s[1])
                    : eventCardBlock.getName().contains("Jumpstart") ? AdventureEventController.EventFormat.Jumpstart : AdventureEventController.EventFormat.Draft;
            if(eventFormat == null)
                return "Unknown event format: " + s[1];
            InnScene.replaceLocalEvent(eventFormat, eventCardBlock);
            return "Replaced local event with " + eventFormat.name() + " - " + eventCardBlock.getName();
        });
        registerCommand(new String[]{"reset", "map"}, s -> {
            if(!MapStage.getInstance().isInMap()) {
                return "Can only be used in maps.";
            }

            MapStage.getInstance().clearOnExit();
            
            return "Exit the map to reset it.";
        });
    }
}
