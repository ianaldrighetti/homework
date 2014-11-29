class Test {
  public int f() { return 1; }
  public void test() {
    int x;
    x = this.f();  // OK
    x = this.f(1); // wrong number of args
  }
  public static void main(String[] ignore) {
  }
}
