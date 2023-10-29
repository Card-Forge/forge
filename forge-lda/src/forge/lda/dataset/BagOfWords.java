/*
* Copyright 2015 Kohei Yamamoto
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package forge.lda.dataset;

import forge.deck.Deck;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;

import java.io.*;
import java.util.*;

/**
 * This class is immutable.
 */
public final class BagOfWords {

    private final int numDocs;
    private final int numVocabs;
    private final int numNNZ;
    private final int numWords;
    private List<Deck> legalDecks;

    public Vocabularies getVocabs() {
        return vocabs;
    }

    private final Vocabularies vocabs;

    // docID -> the vocabs sequence in the doc
    private Map<Integer, List<Integer>> words;

    // docID -> the doc length 
    private Map<Integer, Integer> docLength;

    
    /**
     * Read the bag-of-words dataset.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     * @throws NullPointerException filePath is null
     */
    public BagOfWords(GameFormat format) throws FileNotFoundException, IOException, Exception {
        IStorage<Deck> decks = new StorageImmediatelySerialized<Deck>("Generator", new DeckStorage(new File(ForgeConstants.DECK_GEN_DIR+ForgeConstants.PATH_SEPARATOR+format.getName()),
                ForgeConstants.DECK_GEN_DIR, false),
                true);

        Set<PaperCard> cardSet = new HashSet<>();
        legalDecks = new ArrayList<>();
        for(Deck deck:decks){
            try {
                if (format.isDeckLegal(deck) && deck.getMain().toFlatList().size() == 60) {
                    legalDecks.add(deck);
                    for (PaperCard card : deck.getMain().toFlatList()) {
                        cardSet.add(card);
                    }
                }
            }catch(Exception e){
                System.out.println("Skipping deck "+deck.getName());
            }
        }
        List<PaperCard> cardList = new ArrayList<>(cardSet);

        this.words     = new HashMap<>();
        this.docLength = new HashMap<>();
        ArrayList<Vocabulary> vocabList    = new ArrayList<Vocabulary>();

        int numDocs     = legalDecks.size();
        int numVocabs   = cardList.size();
        int numNNZ      = 0;
        int numWords    = 0;

        Map<String, Integer> cardIntegerMap = new HashMap<>();
        Map<Integer, PaperCard> integerCardMap = new HashMap<>();
        for (int i=0; i<cardList.size(); ++i){
            cardIntegerMap.put(cardList.get(i).getName(), i);
            vocabList.add(new Vocabulary(i,cardList.get(i).getName()));
            integerCardMap.put(i, cardList.get(i));
        }

        this.vocabs = new Vocabularies(vocabList);
        int deckID = 0;
        for (Deck deck:legalDecks){
            numNNZ += deck.getMain().countDistinct();
            List<Integer> cardNumbers = new ArrayList<>();
            for(PaperCard card:deck.getMain().toFlatList()){
                if(cardIntegerMap.get(card.getName()) == null){
                    System.out.println(card.getName() + " is missing!!");
                }
                cardNumbers.add(cardIntegerMap.get(card.getName()));
            }
            words.put(deckID,cardNumbers);
            numWords+=cardNumbers.size();
            docLength.put(deckID,cardNumbers.size());
            deckID ++;
        }

        /*String s = null;
        while ((s = reader.readLine()) != null) {
            List<Integer> numbers
            = Arrays.asList(s.split(" ")).stream().map(Integer::parseInt).collect(Collectors.toList());

            if (numbers.size() == 1) {
                if (headerCount == 2)      numDocs   = numbers.get(0);
                else if (headerCount == 1) numVocabs = numbers.get(0);
                else if (headerCount == 0) numNNZ    = numbers.get(0);
                --headerCount;
                continue;
            }
            else if (numbers.size() == 3) {
                final int docID   = numbers.get(0);
                final int vocabID = numbers.get(1);
                final int count   = numbers.get(2);

                // Set up the words container
                if (!words.containsKey(docID)) {
                    words.put(docID, new ArrayList<>());
                }  
                for (int c = 0; c < count; ++c) {
                    words.get(docID).add(vocabID);
                }

                // Set up the doc length map
                Optional<Integer> currentCount
                    = Optional.ofNullable(docLength.putIfAbsent(docID, count));
                currentCount.ifPresent(c -> docLength.replace(docID, c + count));

                numWords += count;
            }
            else {
                throw new Exception("Invalid dataset form was detected.");
            }
        }
        reader.close();*/

        this.numDocs   = numDocs;
        this.numVocabs = numVocabs;
        this.numNNZ    = numNNZ;
        this.numWords  = numWords;

        System.out.println("Num Decks: " + this.numDocs);
        System.out.println("Num Vocabs: " + this.numVocabs);
        System.out.println("Num NNZ: " + this.numNNZ);
        System.out.println("Num Cards: " + this.numWords);
    }

    public int getNumDocs() {
        return numDocs;
    }

    /**
     * Get the length of the document.
     * @param docID
     * @return length of the document
     * @throws IllegalArgumentException docID <= 0 || #documents < docID
     */
    public int getDocLength(int docID) {
        if (docID < 0 || getNumDocs() < docID) {
            throw new IllegalArgumentException();
        }

        return docLength.get(docID);
    }

    /**
     * Get the unmodifiable list of words in the document.
     * @param docID
     * @return the unmodifiable list of words
     * @throws IllegalArgumentException docID <= 0 || #documents < docID
     */
    public List<Integer> getWords(final int docID) {
        if (docID < 0 || getNumDocs() < docID) {
            throw new IllegalArgumentException();
        }
        return Collections.unmodifiableList(words.get(docID));
    }

    public int getNumVocabs() {
        return numVocabs;
    }

    public int getNumNNZ() {
        return numNNZ;
    }

    public int getNumWords() {
        return numWords;
    }

    public List<Deck> getLegalDecks() { return legalDecks; }
}

