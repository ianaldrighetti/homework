// correct program
class Test {
  public int m() {
    if (true)
      if (true)
        return 2;
      else
        return 3;
    else
      return 4;
  }
  public static void main(String[] ignore) { }
}
