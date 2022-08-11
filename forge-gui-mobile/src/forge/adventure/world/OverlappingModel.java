package forge.adventure.world;


import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class OverlappingModel extends Model {
  int N;
  Integer[][] patterns;
  int ground;
  List<Color> colors;

  @FunctionalInterface
  interface Agrees<One, Two, Three, Four, Five> {
    public Five apply(One one, Two two, Three three, Four four);
  }

  /**
   * Creates a new instance of the Overlapping Model
   * @param data BufferedImage data of source image.
   * @param N Size of the patterns.
   * @param width The width of the generation (in pixels).
   * @param height The height of the generation (in pixels).
   * @param periodicInput Whether the source image is to be considered as periodic (repeatable).
   * @param periodicOutput Whether the generation should be periodic (repeatable).
   * @param symmetry Allowed symmetries from 1 (no symmetry) to 8 (all mirrored / rotated variations).
   * @param ground Id of the specific pattern to use as the bottom of the generation.
   */
  public OverlappingModel(
          ColorMap data,
    int N,
    int width,
    int height,
    boolean periodicInput,
    boolean periodicOutput,
    int symmetry,
    int ground
  ) {
    super(width, height);
    this.N = N;
    this.periodic = periodicOutput;
    

    int SMX = data.getWidth(), SMY = data.getHeight();
    Integer[][] sample = new Integer[SMX][SMY];

    this.colors = new ArrayList<Color>();

    for (int y = 0; y < SMY; y++) for (int x = 0; x < SMX; x++) {
      Color color = data.getColor(x, y);
    if(color==null)
      break;
      int i = 0;
      for (Color c : colors) {
        if (c.equals(color)) break;
        i++;
      }
      if (i == colors.size()) colors.add(color);
      sample[x][y] = i;
    }

    int C = this.colors.size();
    long W = OverlappingModel.toPower(C, this.N * this.N);

    Function<BiFunction<Integer, Integer, Integer>, Integer[]> pattern =
      (BiFunction<Integer, Integer, Integer> f) -> {
        Integer[] result = new Integer[this.N * this.N];
        for (int y = 0; y < this.N; y++) for (int x = 0; x <
          this.N; x++) result[x + y * this.N] = f.apply(x, y);

        return result;
      };

    BiFunction<Integer, Integer, Integer[]> patternFromSample =
      (Integer x, Integer y) -> pattern.apply(
        (Integer dx, Integer dy) -> sample[(x + dx) % SMX][(y + dy) % SMY]
      );

    Function<Integer[], Integer[]> rotate =
      (Integer[] p) -> pattern.apply(
        (Integer x, Integer y) -> p[this.N - 1 - y + x * this.N]
      );

    Function<Integer[], Integer[]> reflect =
      (Integer[] p) -> pattern.apply(
        (Integer x, Integer y) -> p[this.N - 1 - x + y * this.N]
      );

    Function<Integer[], Long> index =
      (Integer[] p) -> {
        long result = 0, power = 1;
        for (int i = 0; i < p.length; i++) {
          result += p[p.length - 1 - i] * power;
          power *= C;
        }
        return result;
      };

    Function<Long, Integer[]> patternFromIndex =
      (Long ind) -> {
        long residue = ind, power = W;
        Integer[] result = new Integer[this.N * this.N];

        for (int i = 0; i < result.length; i++) {
          power /= C;
          int count = 0;

          while (residue >= power) {
            residue -= power;
            count++;
          }

          result[i] = count;
        }

        return result;
      };

    HashMap<Long, Integer> weights = new HashMap<Long, Integer>();
    List<Long> ordering = new ArrayList<Long>();

    for (int y = 0; y < (periodicInput ? SMY : SMY - N + 1); y++) for (int x =
      0; x < (periodicInput ? SMX : SMX - this.N + 1); x++) {
      Integer[][] ps = new Integer[8][];

      ps[0] = patternFromSample.apply(x, y);
      ps[1] = reflect.apply(ps[0]);
      ps[2] = rotate.apply(ps[0]);
      ps[3] = reflect.apply(ps[2]);
      ps[4] = rotate.apply(ps[2]);
      ps[5] = reflect.apply(ps[4]);
      ps[6] = rotate.apply(ps[4]);
      ps[7] = reflect.apply(ps[6]);

      for (int k = 0; k < symmetry; k++) {
        long ind = index.apply(ps[k]);
        if (weights.containsKey(ind)) weights.put(
          ind,
          weights.get(ind) + 1
        ); else {
          weights.put(ind, 1);
          ordering.add(ind);
        }
      }
    }

    this.T = weights.size();
    this.ground = (ground + this.T) % this.T;
    this.patterns = new Integer[this.T][];
    this.weights = new Double[this.T];

    int counter = 0;

    for (long w : ordering) {
      this.patterns[counter] = patternFromIndex.apply(w);
      this.weights[counter] = (double) weights.get(w);
//      System.out.println(this.weights[counter]);

      //        	weights[counter] = weights[(int) w];
      counter++;
    }

    Agrees<Integer[], Integer[], Integer, Integer, Boolean> agrees =
      (Integer[] p1, Integer[] p2, Integer dx, Integer dy) -> {
        int xmin = dx < 0 ? 0 : dx;
        int xmax = dx < 0 ? dx + N : N;
        int ymin = dy < 0 ? 0 : dy;
        int ymax = dy < 0 ? dy + N : N;

        for (int y = ymin; y < ymax; y++) for (int x = xmin; x < xmax; x++) if (
          p1[x + this.N * y] != p2[x - dx + this.N * (y - dy)]
        ) return false;
        return true;
      };

    this.propagator = new int[4][][];
    
//    System.out.println(this.T);

    for (int d = 0; d < 4; d++) {
      this.propagator[d] = new int[this.T][];
      for (int t = 0; t < this.T; t++) {
        List<Integer> list = new ArrayList<Integer>();
        for (int t2 = 0; t2 < this.T; t2++) if (
          agrees.apply(
            this.patterns[t],
            this.patterns[t2],
            Model.DX[d],
            Model.DY[d]
          )
        ) list.add(t2);
        this.propagator[d][t] = new int[list.size()];
        for (int c = 0; c < list.size(); c++) this.propagator[d][t][c] =
          list.get(c);
      }
    }
  }

  @Override
  protected boolean onBoundary(int x, int y) {
    return (
      !this.periodic &&
      (x + this.N > this.FMX || y + this.N > this.FMY || x < 0 || y < 0)
    );
  }

  /**
   * Returns a new BufferedImage generated by the model.
   * Requires Run() to have been run.
   */
  @Override
  public ColorMap graphics() {
    ColorMap result = new ColorMap(
      this.FMX,
      this.FMY
    );

    if (this.observed != null) {
      for (int y = 0; y < this.FMY; y++) {
        int dy = y < this.FMY - this.N + 1 ? 0 : this.N - 1;
        for (int x = 0; x < this.FMX; x++) {
          int dx = x < this.FMX - this.N + 1 ? 0 : this.N - 1;
          Color c =
            this.colors.get(
                this.patterns[this.observed[x - dx + (y - dy) * this.FMX]][dx +
                    dy *
                    this.N]
              );

          result.setColor(x, y, c);
        }
      }
    } else {
      for (int i = 0; i < this.wave.length; i++) {
        float contributors = 0, r = 0, g = 0, b = 0;
        int x = i % this.FMX, y = i / this.FMX;

        for (int dy = 0; dy < this.N; dy++) for (int dx = 0; dx <
          this.N; dx++) {
          int sx = x - dx;
          if (sx < 0) sx += this.FMX;

          int sy = y - dy;
          if (sy < 0) sy += this.FMY;

          int s = sx + sy * this.FMX;
          if (this.onBoundary(sx, sy)) continue;
          for (int t = 0; t < this.T; t++) if (wave[s][t]) {
            contributors++;
            Color color = this.colors.get(this.patterns[t][dx + dy * this.N]);
            r += color.r;
            g += color.g;
            b += color.b;
          }
        }

        Color c = new Color(
          r / contributors,
          g / contributors,
          b / contributors,1f
        );
        result.setColor(x, y, c);
      }
    }

    return result;
  }

  protected void Clear() {
    super.Clear();

    if (this.ground != 0) {
      for (int x = 0; x < this.FMX; x++) {
        for (int t = 0; t < this.T; t++) if (t != this.ground) this.ban(
            x + (this.FMY - 1) * this.FMX,
            t
          );

        for (int y = 0; y < this.FMY - 1; y++) this.ban(
            x + y * this.FMX,
            this.ground
          );
      }

      this.propagate();
    }
  }
}
