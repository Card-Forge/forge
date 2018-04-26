package forge.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardRulesPredicates;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.DeckFormat;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameRules;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.limited.CardRanker;
import forge.model.FModel;
import forge.properties.ForgeConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGeneticAlgorithm<T> {

    protected List<T> population;
    private int targetPopulationSize;
    private float pruneRatio = 0.5f;
    public int generationCount = 0;

    public void initializePopulation(List<T> population){
        this.population = population;
        targetPopulationSize = population.size();
    }

    protected abstract void evaluateFitness();

    public void pruneWeakest(){
        population = population.subList(0, new Float(population.size()*pruneRatio).intValue());
    }

    protected void generateChildren(){
        while(population.size()<targetPopulationSize){
            int randomIndex = new Double(population.size()*Math.pow(MyRandom.getRandom().nextDouble(), 0.25)/2d).intValue();
            if(MyRandom.getRandom().nextBoolean()){
                population.add(mutateObject(population.get(randomIndex)));
            }else{
                int secondIndex = randomIndex;
                while(secondIndex != randomIndex){
                    secondIndex = new Double(population.size()*Math.pow(MyRandom.getRandom().nextDouble(), 0.25)/2d).intValue();
                }
                population.add(createChild(population.get(randomIndex)
                        , population.get(secondIndex)));
            }
        }
    }

    protected abstract T mutateObject(T parent1);

    protected abstract T createChild(T parent1, T parent2);

    public void run(){
        while(true){
            evaluateFitness();
            pruneWeakest();
            generationCount++;
            if(!shouldContinue()) {
                break;
            }
            generateChildren();
        }
    }

    public List<T> listFinalPopulation(){
        pruneWeakest();
        return population;
    }

    protected abstract boolean shouldContinue();





}
