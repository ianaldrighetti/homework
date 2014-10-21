// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Variable, method, and class shared the same name
// 
class Test {
  public static void main(String[] ignore) {
    A A = new A();
    A.A = A.A(2);
    System.out.println(A.A);
  }
}

class A {
  int A;
  public int A(int i) {
    return i;
  }
}
