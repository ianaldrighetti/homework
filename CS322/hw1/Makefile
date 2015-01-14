# Makefile for miniJava compiler. (Jingke Li)
#
JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.PRECIOUS: %.java

.java.class:
	$(JC) $(JFLAGS) $*.java

ir1int: ir1psr IR1Interp.class

ir1psr: ir1/IR1.class ir1/ir1Parser.class

clean:	
	rm ir1/*.class *.class
