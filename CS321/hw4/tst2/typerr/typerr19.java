class A { int i; }

class Test {
  public static void main(String[] ignore) {
    A x = new A();
    int y = x.j;  // incorrect field name
  }
}
