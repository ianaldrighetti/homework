class A { }

class Test {
  public static void main(String[] ignore) {
    A u = new A();
    A v = new A();
    int[] x = new int[5];
    int[] y = new int[4];
    int[] z = x;
    System.out.println(u==v); // OK
    System.out.println(x==y); // OK
    System.out.println(a==x); // not valid
    System.out.println(u<v);  // not valid
    System.out.println(x<z);  // not valid
    
  }
}
