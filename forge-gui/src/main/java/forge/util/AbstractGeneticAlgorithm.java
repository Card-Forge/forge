package forge.util;
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

    protected abstract T expandPool();

    public void pruneWeakest(){
        population = population.subList(0, Float.valueOf(population.size()*pruneRatio).intValue());
    }

    protected void generateChildren(){
        int prunedSize = population.size();
        while(population.size()<targetPopulationSize){
            int randomIndex = Double.valueOf(prunedSize*Math.pow(MyRandom.getRandom().nextDouble(), 0.25)/2d).intValue();
            float rand = MyRandom.getRandom().nextFloat();
            if(rand>0.85f){
                T child = mutateObject(population.get(randomIndex));
                if(child != null) {
                    population.add(child);
                }
            }else if(rand>0.70f){
                int secondIndex = randomIndex;
                while(secondIndex != randomIndex){
                    secondIndex = Double.valueOf(prunedSize*Math.pow(MyRandom.getRandom().nextDouble(), 0.25)/2d).intValue();
                }
                T child = createChild(population.get(randomIndex)
                        , population.get(secondIndex));
                if(child != null) {
                    population.add(child);
                }
            }else{
                population.add(expandPool());
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
