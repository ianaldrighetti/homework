class Test {
  public static void main(String[] ignore) {
    boolean b;
    b = false != true; // OK
    b = false == 0;    // incompatible args
  }
}
