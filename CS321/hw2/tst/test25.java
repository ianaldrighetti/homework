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
  public int go() {
    B2 b2 = new B2();
    return b2.value(1, 2, 3);
  }
}

class B2 {
  public int value(int i, int j, int k) {
    return i + j + k;
  }
}

