package forge.card;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;

import forge.FileUtil;
import forge.HttpUtil;

/** 
 * Contains Maps of card ratings
 *
 */
public class CardRatings {
    private static Map<String, Integer> fullRatings = new TreeMap<String, Integer>();
    private static Map<String, Integer> blockRatings = new TreeMap<String, Integer>();
    private static Map<String, Integer> customRatings = new TreeMap<String, Integer>();
    
    private static ArrayList<String> tempRatings = new ArrayList<String>();
    
    public CardRatings() {
        if (fullRatings.size() < 1) {
            loadFullRatings();
        }
    
        if (blockRatings.size() < 1) {
            loadBlockRatings();
        }
        
        if (customRatings.size() < 1) {
            loadCustomRatings();
        }
        
        if (tempRatings.size() < 1) {
            tempRatings = FileUtil.readFile("res/draft/tempRatings.dat");            
        }
    }
    
    private void loadFullRatings() {
        ArrayList<String> sRatings = FileUtil.readFile("res/draft/fullRatings.dat");
        if (sRatings.size() > 1) {
            for (String s : sRatings) {
                if (s.length() > 3) {
                    String[] ss = s.split(":");
                    if (ss.length > 1) {
                        fullRatings.put(ss[0], new Integer(ss[1]));
                    }
                }
            }
        }
    }
    
    private void loadBlockRatings() {
        ArrayList<String> sRatings = FileUtil.readFile("res/draft/blockRatings.dat");
        if (sRatings.size() > 1) {
            for (String s : sRatings) {
                if (s.length() > 3) {
                    String[] ss = s.split(":");
                    if (ss.length > 1) {
                        blockRatings.put(ss[0], new Integer(ss[1]));
                    }
                }
            }
        }
    }
    
    private void loadCustomRatings() {
        ArrayList<String> sRatings = FileUtil.readFile("res/draft/customRatings.dat");
        if (sRatings.size() > 1) {
            for (String s : sRatings) {
                if (s.length() > 3) {
                    String[] ss = s.split(":");
                    if (ss.length > 1) {
                        customRatings.put(ss[0], new Integer(ss[1]));
                    }
                }
            }
        }
    }
    
    public void saveRatings() {
        if (fullRatings.size() > 1) {
            String[] keys = fullRatings.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            ArrayList<String> ratings = new ArrayList<String>();
            
            for (String k : keys) {
                ratings.add(k + ":" + fullRatings.get(k));
            }
            
            FileUtil.writeFile("res/draft/fullRatings.dat", ratings);
        }
        
        if (blockRatings.size() > 1) {
            String[] keys = blockRatings.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            ArrayList<String> ratings = new ArrayList<String>();
            
            for (String k : keys) {
                ratings.add(k + ":" + blockRatings.get(k));
            }
            
            FileUtil.writeFile("res/draft/blockRatings.dat", ratings);
        }

        if (customRatings.size() > 1) {
            String[] keys = customRatings.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            ArrayList<String> ratings = new ArrayList<String>();
            
            for (String k : keys) {
                ratings.add(k + ":" + customRatings.get(k));
            }
            
            FileUtil.writeFile("res/draft/customRatings.dat", ratings);
        }

        if (tempRatings.size() > 1) {
            FileUtil.writeFile("res/draft/tempRatings.dat", tempRatings);
        }
    }

    
    public int getFullRating(String cardName) {
        if (fullRatings.containsKey(cardName)) {
            return fullRatings.get(cardName);
        }
        
        return 0;
    }
    
    public int getBlockRating(String cardName, String setCode) {
        String cNsC = cardName + "|" + setCode;
        if (blockRatings.containsKey(cNsC)) {
            return blockRatings.get(cNsC);
        }
        
        return 0;
    }
    
    public int getCustomRatings(String cardName, String custName) {
        String cNcN = cardName + "|" + custName;
        if (customRatings.containsKey(cNcN)) {
            return customRatings.get(cNcN);
        }
        
        return 0;
    }
    
    public void putFullRating(String cardName, int Rating) {
        if (fullRatings.containsKey(cardName)) {
            int r = fullRatings.get(cardName);
            int nr = (r + Rating) / 2;
            fullRatings.put(cardName, nr);
        } else {
            fullRatings.put(cardName, Rating);
        }
        
        tempRatings.add("Full:" + cardName + ":" + Rating);
    }
    
    public void putBlockRating(String cardName, String setCode, int Rating) {
        String cNsC = cardName + "|" + setCode;
        if (blockRatings.containsKey(cNsC)) {
            int r = blockRatings.get(cNsC);
            int nr = (r + Rating) / 2;
            blockRatings.put(cNsC, nr);
        } else {
            blockRatings.put(cNsC, Rating);
        }
        
        tempRatings.add("Block:" + cNsC + ":" + Rating);
    }
    
    public void putCustomRating(String cardName, String custName, int Rating) {
        String cNcN = cardName + "|" + custName;
        if (customRatings.containsKey(cNcN)) {
            int r = customRatings.get(cNcN);
            int nr = (r + Rating) / 2;
            customRatings.put(cNcN, nr);
        } else {
            customRatings.put(cNcN, Rating);
        }
        
        tempRatings.add("Custom:" + cNcN + ":" + Rating);
    }
    
    public void uploadRatings() {
        FileUtil.writeFile("res/draft/tempRatings.dat", tempRatings);
        
        HttpUtil httpPost = new HttpUtil();
        httpPost.upload("http://cardforge.org/draftAI/submitRatingsData.php?", "res/draft/tempRatings.dat");
        
        FileUtil.writeFile("res/draft/tempRatings.dat", new ArrayList<String>());
    }
    
    public void downloadRatings() {
        HttpUtil httpGet = new HttpUtil();
        ArrayList<String> tmpList = new ArrayList<String>();
        String tmpData = new String();
        
        tmpData = httpGet.getURL("http://cardforge.org/draftAI/getRatingsData.php?fmt=Full");
        tmpList.add(tmpData);
        FileUtil.writeFile("res/draft/fullRatings.dat", tmpList);
        fullRatings.clear();
        loadFullRatings();
        
        tmpList.clear();
        
        tmpData = httpGet.getURL("http://cardforge.org/draftAI/getRatingsData.php?fmt=Block");
        tmpList.add(tmpData);
        FileUtil.writeFile("res/draft/blockRatings.dat", tmpList);
        blockRatings.clear();
        loadBlockRatings();
        
        tmpList.clear();
        
        tmpData = httpGet.getURL("http://cardforge.org/draftAI/getRatingsData.php?fmt=Custom");
        tmpList.add(tmpData);
        FileUtil.writeFile("res/draft/customRatings.dat", tmpList);
        customRatings.clear();
        loadCustomRatings();
    }
}
