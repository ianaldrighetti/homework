// correct program
class Test {
  public void m() {
    int i = 1;
    int j;
    if (i == 1)
      if (true)
        j = 2;
      else
        j = 3;
    else
      j = 4;
    System.out.println(i + j);
  }
  public static void main(String[] ignore) { }
}
