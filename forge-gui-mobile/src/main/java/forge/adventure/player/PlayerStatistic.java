package forge.adventure.player;

import forge.adventure.data.AdventureEventData;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStatistic implements SaveFileContent {

    HashMap<String, Pair<Integer,Integer>> winLossRecord=new HashMap<>();
    List<AdventureEventData> completedEvents = new ArrayList<>();
    int secondPlayed=0;

    public HashMap<String, Pair<Integer,Integer>> getWinLossRecord()
    {
        return winLossRecord;
    }
    public int totalWins()
    {
        int wins=0;
        for(Map.Entry<String, Pair<Integer, Integer>> value:winLossRecord.entrySet())
        {
            wins+=value.getValue().getLeft();
        }
        return wins;
    }
    public int totalLoss()
    {
        int loss=0;
        for(Map.Entry<String, Pair<Integer, Integer>> value:winLossRecord.entrySet())
        {
            loss+=value.getValue().getRight();
        }
        return loss;
    }
    public float winLossRatio()
    {
        if (totalLoss() == 0) {
            // Not a true ratio but fixes division by zero
            return totalWins();
        }

        return (float) totalWins()/(float)totalLoss();
    }
    public int eventWins(){
        int win = 0;
        for (AdventureEventData event : completedEvents){
            if (event.playerWon)
                win++;
        }
        return win;
    }
    public int eventLosses(){
        int loss = 0;
        for (AdventureEventData event : completedEvents){
            if (!event.playerWon)
                loss++;
        }
        return loss;
    }
    public float eventWinLossRatio()
    {
        if (eventLosses() == 0) {
            // Not a true ratio but fixes division by zero
            return eventWins();
        }

        return (float) eventWins()/(float)eventLosses();
    }

    public int eventMatchWins(){
        int win = 0;
        for (AdventureEventData event : completedEvents){
            win+= event.matchesWon;
        }
        return win;
    }
    public int eventMatchLosses(){
        int loss = 0;
        for (AdventureEventData event : completedEvents){
            loss += event.matchesLost;
        }
        return loss;
    }
    public float eventMatchWinLossRatio()
    {
        if (eventMatchLosses() == 0) {
            // Not a true ratio but fixes division by zero
            return eventMatchWins();
        }

        return (float) eventMatchWins()/(float)eventMatchLosses();
    }

    public int getPlayTime()
    {
        return secondPlayed;
    }
    @Override
    public void load(SaveFileData data) {

        if(data!=null&&data.containsKey("winLossRecord"))
            winLossRecord = (HashMap<String, Pair<Integer, Integer>>) data.readObject("winLossRecord");
        else
            winLossRecord.clear();

        if (data!=null&&data.containsKey("completedEvents")) {
            completedEvents = (ArrayList<AdventureEventData>) data.readObject("completedEvents");
            if (completedEvents == null) {
                completedEvents = new ArrayList<>();
            }
        }
        else
            completedEvents.clear();
    }

    public void setResult(String enemy,boolean win)
    {
        if(!winLossRecord.containsKey(enemy))
        {
            if(win)
                winLossRecord.put(enemy,Pair.of(1,0));
            else
                winLossRecord.put(enemy,Pair.of(0,1));
        }
        else
        {

            if(win)
                winLossRecord.put(enemy,Pair.of(winLossRecord.get(enemy).getLeft()+1,winLossRecord.get(enemy).getRight()));
            else
                winLossRecord.put(enemy,Pair.of(winLossRecord.get(enemy).getLeft(),winLossRecord.get(enemy).getRight()+1));
        }
    }


    @Override
    public SaveFileData save() {

        SaveFileData data=new SaveFileData();
        data.storeObject("winLossRecord",winLossRecord);
        data.storeObject("completedEvents", completedEvents);
        return data;
    }


    public void clear() {
        winLossRecord.clear();
    }

    public void setResult(AdventureEventData completedEvent) {
        completedEvents.add(completedEvent);
        AdventureQuestController.instance().updateEventComplete(completedEvent);
    }
}
