class Test {
  public boolean m() {
    return true; // OK
    return 4;    // wrong return type
  }
  public static void main(String[] ignore) {
  }
}
