class Test {
  public int f(int i) { return 1; }

  public static void main(String[] ignore) {
    int[] x = new int[2];
    int y = x.i;  // a is not an object
  }
}
