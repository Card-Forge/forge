package forge.adventure.world;

import java.util.Random;





abstract class Model {
  protected boolean[][] wave;
  protected int[][][] propagator;
  int[][][] compatible;
  protected int[] observed;

  int[] stack;
  int stacksize;

  protected Random random;
  protected int FMX, FMY, T;
  protected boolean periodic;
  protected Double[] weights;
  double[] weightLogWeights;

  int[] sumsOfOnes;
  double sumOfWeights, sumOfWeightLogWeights, startingEntropy;
  double[] sumsOfWeights, sumsOfWeightLogWeights, entropies;
  private double[] distribution;

  protected Model(int width, int height) {
    this.FMX = width;
    this.FMY = height;
  }

  protected abstract boolean onBoundary(int x, int y);

  public abstract ColorMap graphics();

  protected static int[] DX = { -1, 0, 1, 0 };
  protected static int[] DY = { 0, 1, 0, -1 };
  static int[] oppposite = { 2, 3, 0, 1 };

  static int randomIndice(double[] arr, double r) {
    double sum = 0;

    for (int j = 0; j < arr.length; j++) sum += arr[j];

    for (int j = 0; j < arr.length; j++) arr[j] /= sum;

    int i = 0;
    double x = 0;

    while (i < arr.length) {
      x += arr[i];
      if (r <= x) return i;
      i++;
    }

    return 0;
  }

  public static long toPower(int a, int n) {
    long product = 1;
    for (int i = 0; i < n; i++) product *= a;
    return product;
  }

  void init() {
    this.wave = new boolean[this.FMX * this.FMY][];
    this.compatible = new int[this.wave.length][][];
    for (int i = 0; i < wave.length; i++) {
      this.wave[i] = new boolean[this.T];
      this.compatible[i] = new int[this.T][];
      for (int t = 0; t < this.T; t++) this.compatible[i][t] = new int[4];
    }

    this.weightLogWeights = new double[this.T];
    this.distribution = new double[this.T];
    this.sumOfWeights = 0;
    this.sumOfWeightLogWeights = 0;

    for (int t = 0; t < this.T; t++) {
      this.weightLogWeights[t] = this.weights[t] * Math.log(this.weights[t]);
      this.sumOfWeights += this.weights[t];
      this.sumOfWeightLogWeights += this.weightLogWeights[t];
    }

    this.startingEntropy =
            Math.log(this.sumOfWeights) -
                    this.sumOfWeightLogWeights /
                            this.sumOfWeights;

    this.sumsOfOnes = new int[this.FMX * this.FMY];
    this.sumsOfWeights = new double[this.FMX * this.FMY];
    this.sumsOfWeightLogWeights = new double[this.FMX * this.FMY];
    this.entropies = new double[this.FMX * this.FMY];

    this.stack = new int[(this.wave.length * this.T)*2];
    this.stacksize = 0;
  }

  Boolean observe() {
    double min = 1e+3;
    int argmin = -1;

    for (int i = 0; i < this.wave.length; i++) {
      if (this.onBoundary(i % this.FMX, i / this.FMX)) continue;

      int amount = this.sumsOfOnes[i];
      if (amount == 0) return false;


      double entropy = this.entropies[i];

      if (amount > 1 && entropy <= min) {
        double noise = 1e-6 * this.random.nextDouble();
        if (entropy + noise < min) {
          min = entropy + noise;
          argmin = i;
        }
      }
    }


    if (argmin == -1) {
      this.observed = new int[this.FMX * this.FMY];
      for (int i = 0; i < this.wave.length; i++) for (int t = 0; t <
              this.T; t++) if (this.wave[i][t]) {
        this.observed[i] = t;
        break;
      }
      return true;
    }

    for (int t = 0; t < this.T; t++) distribution[t] =
            this.wave[argmin][t] ? this.weights[t] : 0;

    int r = Model.randomIndice(distribution, this.random.nextDouble());


    boolean[] w = this.wave[argmin];
    for (int t = 0; t < this.T; t++) if (w[t] != (t == r)) this.ban(argmin, t);

    return null;
  }

  protected void ban(int i, int t) {
    this.wave[i][t] = false;

    int[] comp = this.compatible[i][t];
    for (int d = 0; d < 4; d++) comp[d] = 0;
    this.stack[this.stacksize]=i;
    this.stack[this.stacksize+1]=t;
    this.stacksize+=2;

    this.sumsOfOnes[i] -= 1;
    this.sumsOfWeights[i] -= this.weights[t];
    this.sumsOfWeightLogWeights[i] -= this.weightLogWeights[t];

    double sum = this.sumsOfWeights[i];
    this.entropies[i] = Math.log(sum) - this.sumsOfWeightLogWeights[i] / sum;
  }

  protected void propagate() {
    while (this.stacksize >= 2) {

      int i1 = this.stack[this.stacksize - 2];
      int x1 = i1 % this.FMX;
      int y1 = i1 / this.FMX;
      int stack2 = this.stack[this.stacksize - 1];
      this.stacksize-=2;
      for (int d = 0; d < 4; d++) {
        int dx = Model.DX[d], dy = Model.DY[d];
        int x2 = x1 + dx, y2 = y1 + dy;

        if (this.onBoundary(x2, y2)) continue;

        if (x2 < 0) x2 += this.FMX; else if (x2 >= this.FMX) x2 -= this.FMX;
        if (y2 < 0) y2 += this.FMY; else if (y2 >= this.FMY) y2 -= this.FMY;

        int i2 = x2 + y2 * this.FMX;
        int[] p = this.propagator[d][stack2];
        int[][] compat = this.compatible[i2];

        for (int l = 0; l < p.length; l++) {
          int t2 = p[l];
          int[] comp = compat[t2];

          comp[d]--;

          if (comp[d] == 0) this.ban(i2, t2);
        }
      }
    }
  }

  public boolean run(int seed, int limit) {
    if (this.wave == null) this.init();

    this.Clear();
    this.random = new Random(seed);

    for (int l = 0; l < limit || limit == 0; l++) {
      Boolean result = this.observe();
      if (result != null)
        return (boolean) result;
      this.propagate();
    }

    return true;
  }

  protected void Clear() {
    for (int i = 0; i < this.wave.length; i++) {
      for (int t = 0; t < this.T; t++) {
        this.wave[i][t] = true;
        for (int d = 0; d < 4; d++) this.compatible[i][t][d] =
                this.propagator[Model.oppposite[d]][t].length;
      }

      this.sumsOfOnes[i] = this.weights.length;
      this.sumsOfWeights[i] = this.sumOfWeights;
      this.sumsOfWeightLogWeights[i] = this.sumOfWeightLogWeights;
      this.entropies[i] = this.startingEntropy;
    }
  }
}
