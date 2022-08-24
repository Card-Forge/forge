package forge.adventure.stage;


import com.badlogic.gdx.utils.Array;
import forge.StaticData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Current;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleCommandInterpreter {
    Command root = new Command();

    class Command {
        HashMap<String, Command> children = new HashMap<>();
        Function<String[], String> function;
    }

    public String complete(String text) {
        String[] words=splitOnSpace(text);
        Command currentCommand=root;
        String completionString="";
        for(String name : words) {
            if (!currentCommand.children.containsKey(name)) {
                for (String key : currentCommand.children.keySet()) {
                    if (key.startsWith(name)) {
                        return completionString + key + " ";
                    }
                }
                break;
            }
            completionString += name + " ";
            currentCommand = currentCommand.children.get(name);
        }
        return text;
    }

    private String[] splitOnSpace(String text) {
        List<String> matchList = new ArrayList<String>();
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

        for(i = 0; i < words.length; i++) {
            String name=words[i];
            if(!currentCommand.children.containsKey(name)) break;
            currentCommand=currentCommand.children.get(name);
        }
        if(currentCommand.function == null) {
            return "Command not found. Available commands:\n" + String.join(" ",Arrays.copyOfRange(words, 0, i))+"\n"+String.join("\n",currentCommand.children.keySet());
        }
        String[] parameters=Arrays.copyOfRange(words, i, words.length);
        for(int j=0;j<parameters.length;j++)
            parameters[j]=parameters[j].replaceAll("[\"']","");
        return currentCommand.function.apply(parameters);
    }

    void registerCommand(String[] path,Function<String[],String> function) {
        if(path.length==0) return;
        Command currentCommand=root;

        for(String name:path) {
            if(!currentCommand.children.containsKey(name))
                currentCommand.children.put(name,new Command());
            currentCommand = currentCommand.children.get(name);
        }
        currentCommand.function = function;
    }

    public ConsoleCommandInterpreter() {
        registerCommand(new String[]{"teleport", "to"}, s -> {
            if(s.length<2)
                return "Command needs 2 parameter";
            try {
                int x = Integer.parseInt(s[0]);
                int y = Integer.parseInt(s[1]);
                WorldStage.getInstance().GetPlayer().setPosition(x,y);
                return  "teleport to ("+s[0]+","+s[1]+")";
            } catch (Exception e) {
                return "Exception occured, Invalid input";
            }
        });
        registerCommand(new String[]{"teleport", "to", "poi"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: PoI name.";
            PointOfInterest poi=Current.world().findPointsOfInterest(s[0]);
            if(poi==null)
                return "PoI " + s[0] + " not found";
            WorldStage.getInstance().GetPlayer().setPosition(poi.getPosition());
            return  "Teleported to " + s[0] + "(" + poi.getPosition() + ")";
        });
        registerCommand(new String[]{"spawn","enemy"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: enemy name.";

            if(WorldStage.getInstance().spawn(s[0]))
                return  "Spawn " + s[0];
            return "Can not find enemy "+s[0];
        });
        registerCommand(new String[]{"give", "gold"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: Amount.";
            int amount;
            try {
                amount=Integer.parseInt(s[0]);
            }
            catch (Exception e) {
                return "Can not convert "+s[0]+" to number";
            }
            Current.player().giveGold(amount);
            return "Added "+amount+" gold";
        });
        registerCommand(new String[]{"give", "life"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: Amount.";
            int amount;
            try {
                amount=Integer.parseInt(s[0]);
            }
            catch (Exception e) {
                return "Can not convert " + s[0] + " to number";
            }
            Current.player().addMaxLife(amount);
            return "Added " + amount + " max life";
        });
        registerCommand(new String[]{"give", "card"}, s -> {
            //TODO: Specify optional amount.
            if(s.length<1) return "Command needs 1 parameter: Card name.";
            PaperCard card = StaticData.instance().getCommonCards().getCard(s[0]);
            if(card==null) return "Cannot find card: "+s[0];
            Current.player().addCard(card);
            return "Added card: " + s[0];
        });
        registerCommand(new String[]{"give", "item"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: Item name.";
            if(Current.player().addItem(s[0])) return "Added item " + s[0] + ".";
            return "Cannot find item "+s[0];
        });
        registerCommand(new String[]{"fullHeal"}, s -> {
            Current.player().fullHeal();
            return "Player fully healed. Health set to " + Current.player().getLife() + ".";
        });
        registerCommand(new String[]{"setColorID"}, s -> {
            if(s.length < 1) return "Please specify color ID: Valid choices: B, G, R, U, W, C. Example:\n\"setColorID G\"";
            Current.player().setColorIdentity(s[0]);
            return "Player color identity set to " + Current.player().getColorIdentity() + ".";
        });
        registerCommand(new String[]{"reloadScenes"}, s -> {
            SceneType.InventoryScene.instance.resLoaded();
            SceneType.PlayerStatisticScene.instance.resLoaded();
            return "Force reload status scenes. WARNING: Game might be unstable.";
        });
        registerCommand(new String[]{"resetQuests"}, s -> {
            Current.player().resetQuestFlags();
            return "All global quest flags have been reset.";
        });
        registerCommand(new String[]{"resetMapQuests"}, s -> {
            if(!MapStage.getInstance().isInMap()) return "Only supported inside a map.";
            MapStage.getInstance().resetQuestFlags();
            return "All local quest flags have been reset.";
        });
        registerCommand(new String[]{"dumpEnemyDeckColors"}, s -> {
            for(EnemyData E : new Array.ArrayIterator<>(WorldData.getAllEnemies())){
                Deck D = E.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck()||Current.player().getDifficulty().name.equalsIgnoreCase("Hard"));
                DeckProxy DP = new DeckProxy(D, "Constructed", GameType.Constructed, null);
                ColorSet colorSet = DP.getColor();
                System.out.printf("%s: Colors: %s (%s%s%s%s%s%s)\n", D.getName(), DP.getColor(),
                    (colorSet.hasBlack()    ? "B" : ""),
                    (colorSet.hasGreen()    ? "G" : ""),
                    (colorSet.hasRed()      ? "R" : ""),
                    (colorSet.hasBlue()     ? "U" : ""),
                    (colorSet.hasWhite()    ? "W" : ""),
                    (colorSet.isColorless() ? "C" : "")
                );
            }
            return "Enemy deck color list dumped to stdout.";
        });
        registerCommand(new String[]{"dumpEnemyDeckList"}, s -> {
            for(EnemyData E : new Array.ArrayIterator<>(WorldData.getAllEnemies())){
                Deck D = E.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck()||Current.player().getDifficulty().name.equalsIgnoreCase("Hard"));
                DeckProxy DP = new DeckProxy(D, "Constructed", GameType.Constructed, null);
                ColorSet colorSet = DP.getColor();
                System.out.printf("Deck: %s\n%s\n\n", D.getName(), DP.getDeck().getMain().toCardList("\n")
                );
            }
            return "Enemy deck list dumped to stdout.";
        });
        registerCommand(new String[]{"dumpEnemyColorIdentity"}, s -> {
            for(EnemyData E : new Array.ArrayIterator<>(WorldData.getAllEnemies())){
                Deck D = E.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck()||Current.player().getDifficulty().name.equalsIgnoreCase("Hard"));
                DeckProxy DP = new DeckProxy(D, "Constructed", GameType.Constructed, null);
                ColorSet colorSet = DP.getColor();
                System.out.printf("%s Colors: %s | Deck Colors: %s (%s)\n", E.name, E.colors, DP.getColorIdentity().toEnumSet().toString(), DP.getName()
                );
            }
            return "Enemy color Identity dumped to stdout.";
        });
        registerCommand(new String[]{"heal", "amount"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: Amount";
            int N = 0;
            try { N = Integer.parseInt(s[0]); }
            catch (Exception e) { return "Can not convert " + s[0] + " to integer"; }
            Current.player().heal(N);
            return "Player healed to " + Current.player().getLife() + "/" + Current.player().getMaxLife();
        });
        registerCommand(new String[]{"debug","on"}, s -> {
            Current.setDebug(true);
            return "Debug mode ON";
        });
        registerCommand(new String[]{"debug","off"}, s -> {
            Current.setDebug(false);
            return "Debug mode OFF";
        });
        registerCommand(new String[]{"remove","enemy","all"}, s -> {
            //TODO: Remove all overworld enemies if not inside a map.
            if(!MapStage.getInstance().isInMap()) {
                return "Only supported for PoI";
            }
            MapStage.getInstance().removeAllEnemies();
            return "removed all enemies";
        });
        registerCommand(new String[]{"remove","enemy"}, s -> {
            if(s.length<1) return "Command needs 1 parameter: Enemy map ID.";
            int id;
            try { id=Integer.parseInt(s[0]); }
            catch (Exception e) {
                return "Cannot convert " + s[0] + " to number";
            }
            if(!MapStage.getInstance().isInMap())
                return "Only supported for PoI";
            MapStage.getInstance().deleteObject(id);
            return "Femoved enemy "+s[0];
        });
    }
}
