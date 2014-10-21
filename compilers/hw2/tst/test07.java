// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Arrays
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    System.out.println(a.go());
  }
}

class A {
  int[] a;
  public int go() {
    int[] b;
    a = new int[2];
    b = new int[2];
    a[0] = 1;
    a[1] = 2;
    b[0] = 3;
    b[1] = 4;
    System.out.println(a[1]);
    System.out.println(b[1]);
    return a[0];
  }
}
