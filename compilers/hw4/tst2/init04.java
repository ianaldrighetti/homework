class Test {
  public void m() {
    int i = 1;
    int j;
    while (i == 1)
      j = 1;
    System.out.println(i + j);  // j not initialized
  }
  public static void main(String[] ignore) { }
}
