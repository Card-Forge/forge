package forge.adventure.player;

import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class PlayerStatistic implements SaveFileContent {

    HashMap<String, Pair<Integer,Integer>> winLossRecord=new HashMap<>();
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
        return (float) totalWins()/(float)totalLoss();
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
        return data;
    }


    public void clear() {
        winLossRecord.clear();
    }
}
