class Test {
  public static void main(String[] ignore) {
    boolean b = true;
    boolean c = false;
    b = c && true; // OK
    b = 1 && true; // error
  }
}
