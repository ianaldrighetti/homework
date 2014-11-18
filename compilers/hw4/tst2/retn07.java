// correct program
class Test {
  public int m() {
    return 0;
    if (true)
      if (true)
        return 2;
      else
        return 3;
  }
  public static void main(String[] ignore) { }
}
