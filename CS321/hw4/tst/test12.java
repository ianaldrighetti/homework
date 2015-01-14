// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Method calls
// 
class Test {
  public static void main(String[] ignore) {
    B b = new B();
    b.go();
  }
}

class B {
  public void go() {
    System.out.println("Go!");
  }
}
