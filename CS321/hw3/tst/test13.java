// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Method call and return value
// 
class Test {
  public static void main(String[] ignore) {
    B b = new B();
    int r = b.go();
    System.out.println(r);
  }
}

class B {
  int i;
  public int go() {
    int j;
    i = 4;
    j = i + 2;
    return j;
  }
}
