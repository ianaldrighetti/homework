class Test {
  public int f(int i) { return 1; }
  public void test() {
    int x;
    x = this.f(true);  // wrong type of arg
  }
  public static void main(String[] ignore) {
  }
}
