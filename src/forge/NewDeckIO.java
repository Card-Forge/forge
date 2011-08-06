
package forge;


import static java.lang.Integer.*;
import static java.lang.String.*;
import static java.util.Arrays.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.error.ErrorViewer;


//reads and write Deck objects
public class NewDeckIO implements DeckIO {
    private static FilenameFilter dck = new FilenameFilter() {
                                          public boolean accept(File dir, String name) {
                                              return name.endsWith(".dck");
                                          }
                                      };
    private static FilenameFilter bdk = new FilenameFilter() {
                                          public boolean accept(File dir, String name) {
                                              return name.endsWith(".bdk");
                                          }
                                      };
    
    private File                  dir;
    List<Deck>                    deckList;
    Map<String, Deck[]>           boosterMap;
    
    public NewDeckIO(String fileName) {
        this(new File(fileName));
    }
    
    public NewDeckIO(File dir) {
        if(dir == null) throw new IllegalArgumentException("No deck directory specified");
        try {
            this.dir = dir;
            
            if(dir.isFile()) {
                throw new IOException("Not a directory");
            } else {
                dir.mkdirs();
                if(!dir.isDirectory()) throw new IOException("Directory can't be created");
                this.deckList = new ArrayList<Deck>();
                this.boosterMap = new HashMap<String, Deck[]>();
                readFile();
            }
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckIO : write() error, " + ex.getMessage());
        }
    }
    
    public NewDeckIO(File dir, List<Deck> deckList, Map<String, Deck[]> boosterMap) {
        this(dir);
        this.deckList.addAll(deckList);
        this.boosterMap.putAll(boosterMap);
    }
    
    public boolean isUnique(String deckName) {
        Deck d;
        for(int i = 0; i < deckList.size(); i++) {
            d = deckList.get(i);
            if(d.getName().equals(deckName)) return false;
        }
        return true;
    }
    
    public boolean isUniqueDraft(String deckName) {
        ArrayList<String> key = new ArrayList<String>(boosterMap.keySet());
        
        for(int i = 0; i < key.size(); i++) {
            if(key.get(i).equals(deckName)) return false;
        }
        return true;
    }
    
    public boolean hasName(String deckName) {
        ArrayList<String> string = new ArrayList<String>();
        
        for(int i = 0; i < deckList.size(); i++)
            string.add(deckList.get(i).toString());
        
        Iterator<String> it = boosterMap.keySet().iterator();
        while(it.hasNext())
            string.add(it.next().toString());
        
        return string.contains(deckName);
    }
    
    public Deck readDeck(String deckName) {
        return deckList.get(findDeckIndex(deckName));
    }
    
    private int findDeckIndex(String deckName) {
        int n = -1;
        for(int i = 0; i < deckList.size(); i++)
            if((deckList.get(i)).getName().equals(deckName)) n = i;
        
        if(n == -1) throw new RuntimeException("DeckIO : findDeckIndex() error, deck name not found - " + deckName);
        
        return n;
    }
    
    public void writeDeck(Deck deck) {
        if(deck.getDeckType().equals(Constant.GameType.Draft)) throw new RuntimeException(
                "DeckIO : writeDeck() error, deck type is Draft");
        
        deckList.add(deck);
    }
    
    public void deleteDeck(String deckName) {
        deckList.remove(findDeckIndex(deckName));
    }
    
    public Deck[] readBoosterDeck(String deckName) {
        if(!boosterMap.containsKey(deckName)) throw new RuntimeException(
                "DeckIO : readBoosterDeck() error, deck name not found - " + deckName);
        
        return boosterMap.get(deckName);
    }
    
    public void writeBoosterDeck(Deck[] deck) {
        checkBoosterDeck(deck);
        
        boosterMap.put(deck[0].toString(), deck);
    }//writeBoosterDeck()
    
    public void deleteBoosterDeck(String deckName) {
        if(!boosterMap.containsKey(deckName)) throw new RuntimeException(
                "DeckIO : deleteBoosterDeck() error, deck name not found - " + deckName);
        
        boosterMap.remove(deckName);
    }
    
    private void checkBoosterDeck(Deck[] deck) {
        if(deck == null || deck.length != 8 || deck[0].getName().equals("")
                || (!deck[0].getDeckType().equals(Constant.GameType.Draft))) {
            throw new RuntimeException("DeckIO : checkBoosterDeck() error, invalid deck");
        }
//    for(int i = 0; i < deck.length; i++)
//      if(deck[i].getName().equals(""))
//        throw new RuntimeException("DeckIO : checkBoosterDeck() error, deck does not have name - " +deck[i].getName());
    }//checkBoosterDeck()
    

    public Deck[] getDecks() {
        Deck[] out = new Deck[deckList.size()];
        deckList.toArray(out);
        return out;
    }
    
    public Map<String, Deck[]> getBoosterDecks() {
        return new HashMap<String, Deck[]>(boosterMap);
    }
    
    public void close() {
        writeFile();
    }
    
    public void readFile() {
        try {
            deckList.clear();
            
            File[] files;
            files = dir.listFiles(dck);
            for(File file:files) {
                BufferedReader in = new BufferedReader(new FileReader(file));
                deckList.add(read(in));
                in.close();
            }
            
            boosterMap.clear();
            files = dir.listFiles(bdk);
            for(File file:files) {
                Deck[] d = new Deck[8];
                for(int i = 0; i < d.length; i++) {
                    BufferedReader in = new BufferedReader(new FileReader(new File(file, i + ".dck")));
                    d[i] = read(in);
                    in.close();
                }
                boosterMap.put(d[0].getName(), d);
            }
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckIO : read() error, " + ex.getMessage());
        }
    }
    
    private Deck read(BufferedReader in) throws IOException {
        String line;
        
        //read name
        String name = in.readLine();
        if(name == null) throw new IOException("Unexpected end of file");
        
        //read comments
        String comment = null;
        while((line = in.readLine()) != null && !line.equals("[general]")) {
            if(comment == null) comment = line;
            else comment += "\n" + line;
        }
        
        //read deck type
        String deckType = in.readLine();
        if(deckType == null) throw new IOException("Unexpected end of file");
        
        Deck d = new Deck(deckType);
        d.setName(name);
        d.setComment(comment);
        
        //go to [main]
        while((line = in.readLine()) != null && !line.equals("[main]"))
            System.err.println("unexpected line: " + line);
        if(line == null) throw new IOException("Unexpected end of file");
        
        Pattern p = Pattern.compile("\\s*((\\d+)\\s+)?(.*?)\\s*");
        
        //read main deck
        while((line = in.readLine()) != null && !line.equals("[sideboard]")) {
            Matcher m = p.matcher(line);
            if(!m.matches()) throw new IOException("unexpected line: " + line);
            String s = m.group(2);
            int count = s == null? 1:parseInt(s);
            
            for(int i = 0; i < count; i++) {
                d.addMain(m.group(3));
            }
        }
        if(line == null) throw new IOException("Unexpected end of file");
        
        //read sideboard
        while((line = in.readLine()) != null && line.length() != 0) {
            Matcher m = p.matcher(line);
            if(!m.matches()) throw new IOException("unexpected line: " + line);
            String s = m.group(2);
            int count = s == null? 1:parseInt(s);
            for(int i = 0; i < count; i++) {
                d.addSideboard(m.group(3));
            }
        }
        
        return d;
    }
    
    private String deriveFileName(String deckName) {
        //skips all but the listed characters
        return deckName.replaceAll("[^-_$#@.{[()]} a-zA-Z0-9]", "");
    }
    
    public void writeFile() {
        try {
            //store the files that do exist
            List<File> files = new ArrayList<File>();
            files.addAll(asList(dir.listFiles(dck)));
            
            //save the files and remove them from the list
            for(Deck deck:deckList) {
                File f = new File(dir, deriveFileName(deck.getName()) + ".dck");
                files.remove(f);
                BufferedWriter out = new BufferedWriter(new FileWriter(f));
                write(deck, out);
                out.close();
            }
            //delete the files that were not written out: the decks that were deleted
            for(File file:files)
                file.delete();
            
            //store the files that do exist
            files.clear();
            files.addAll(asList(dir.listFiles(bdk)));
            
            //save the files and remove them from the list
            for(Entry<String, Deck[]> e:boosterMap.entrySet()) {
                File f = new File(dir, deriveFileName(e.getValue()[0].getName()) + ".bdk");
                f.mkdir();
                for(int i = 0; i < e.getValue().length; i++) {
                    BufferedWriter out = new BufferedWriter(new FileWriter(new File(f, i + ".dck")));
                    write(e.getValue()[i], out);
                    out.close();
                }
            }
            //delete the files that were not written out: the decks that were deleted
            for(File file:files) {
                for(int i = 0; i < 8; i++)
                    new File(file, i + ".dck").delete();
                file.delete();
            }
        } catch(IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckIO : write() error, " + ex.getMessage());
        }
    }
    
    private void write(Deck d, BufferedWriter out) throws IOException {
        out.write(format("%s%n", d.getName()));
        if(d.getComment() != null) out.write(format("%s%n", d.getComment()));
        out.write(format("%s%n", "[general]"));
        out.write(format("%s%n", d.getDeckType()));
        out.write(format("%s%n", "[main]"));
        for(Entry<String, Integer> e:count(d.getMain()).entrySet()) {
            out.write(format("%d %s%n", e.getValue(), e.getKey()));
        }
        out.write(format("%s%n", "[sideboard]"));
        for(Entry<String, Integer> e:count(d.getSideboard()).entrySet()) {
            out.write(format("%d %s%n", e.getValue(), e.getKey()));
        }
    }
    
    private Map<String, Integer> count(List<String> src) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        for(String s:src) {
            Integer dstValue = result.get(s);
            if(dstValue == null) result.put(s, 1);
            else result.put(s, dstValue.intValue() + 1);
        }
        return result;
    }
}
