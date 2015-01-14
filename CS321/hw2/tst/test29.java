// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Variable, method, and class shared the same name
// 
class Test {
  public static void main(String[] ignore) {
    int A;
    A a = new A();
    a.A = a.A(1);
    A = a.A;
    System.out.println(A);
  }
}

class A {
  int A;
  public int A(int i) {
    return i;
  }
}
