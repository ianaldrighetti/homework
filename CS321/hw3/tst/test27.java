// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Variable reference
// 
class Test {
  public static void main(String[] ignore) {
    B b = new B();
    System.out.println(b.go());
  }
}

class B {
  public int go() {
    B2 b;
    b = new B2();
    return b.value(true);
  }
}

class B2 {
  int i;
  public int value(boolean cond) {
    int j;
    int k;
    i = 5;
    j = 6;
    if (cond)
      k = i;
    else
      k = j;
    return k;
  }
}

