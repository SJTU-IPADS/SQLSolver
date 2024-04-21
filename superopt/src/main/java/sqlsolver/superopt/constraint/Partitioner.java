package sqlsolver.superopt.constraint;

import java.util.Arrays;

class Partitioner {
  private final int cardinality;
  private final byte[][][] partitions;
  private int index;

  public Partitioner(byte cardinality) {
    this.cardinality = cardinality;
    this.partitions = partitionsOf(cardinality);
  }

  public void reset() {
    index = 0;
  }

  public byte[][] partition() {
    return partitions[index];
  }

  public boolean forward() {
    if (index >= partitions.length - 1) return false;
    ++index;
    return true;
  }

  public int cardinality() {
    return cardinality;
  }

  public int numPartitions() {
    return partitions.length;
  }

  private static final int[] NUM_PARTITIONS = {
    1, 1, 2, 5, 15, 52, 203, 877, 4140, 21147, 115975, 678570, 4213597
  };

  private static final byte[][][][] LOOKUP = new byte[13][][][];

  static {
    LOOKUP[0] = new byte[1][0][0];
    for (byte i = 0; i < 12; i++) partitionsOf(i);
  }

  private static byte[][][] partitionsOf(byte cardinality) {
    final byte[][][] existing = LOOKUP[cardinality];
    if (existing != null) return existing;

    final byte[][][] base = partitionsOf((byte) (cardinality - 1));
    final byte[][][] partitions = LOOKUP[cardinality] = new byte[NUM_PARTITIONS[cardinality]][][];

    final byte element = (byte) (cardinality - 1);

    int idx = 0;
    for (byte[][] basePartition : base)
      //      for (int i = 0; i <= basePartition.length; ++i)
      for (int i = basePartition.length; i >= 0; i--)
        partitions[idx++] = addElementTo(basePartition, element, i);

    assert idx == NUM_PARTITIONS[cardinality];

    return partitions;
  }

  private static byte[][] addElementTo(byte[][] target, byte element, int idx) {
    final int len = target.length;
    final int index = Math.min(idx, len);
    final byte[][] ret = Arrays.copyOf(target, index >= len ? len + 1 : len);

    ret[index] = addElementTo(ret[index], element);
    return ret;
  }

  private static byte[] addElementTo(byte[] target, byte element) {
    if (target == null) return new byte[] {element};
    else {
      final int len = target.length;
      final byte[] newArr = Arrays.copyOf(target, len + 1);
      newArr[len] = element;
      return newArr;
    }
  }
}
