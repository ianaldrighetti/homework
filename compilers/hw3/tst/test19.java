// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Method calls
// 
class Test {
  public static void main(String[] ignore) {
    A a;
    a = new A();
    System.out.println(a.go(5));
  }
}

class A {
  public int go(int n) {
    int i;
    i = 0;
    if (n > 0) {
      System.out.println(n);
      i = back(n-1);
    }
    return i;
  }

  public int back(int n) {
    int i = go(n);
    return 0;
  }
}
