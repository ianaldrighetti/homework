// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Parameter passing
// 
class Test {
  public static void main(String[] ignore) {
    B b = new B();
    System.out.println(b.go());
  }
}

class B {
  public int value(int i, int j, int k) {
    return i + j + k;
  }
  public int go() {
    return value(1, 1, 1) + this.value(2, 2, 2);
  }
}
