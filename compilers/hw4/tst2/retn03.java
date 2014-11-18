class Test {
  public int m() {
    if (true)
      if (true)
        return 2;
      else
        return 3;
    // missing return statement
  }
  public static void main(String[] ignore) { }
}
