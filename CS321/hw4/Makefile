#----------------------------------------------------------------------
# CS321 Assignment 4 (Fall 2014)
#----------------------------------------------------------------------

JFLAGS = -g
JC = javac
JCC = ../javacc-5.0/bin/javacc
#JCC = javacc

.SUFFIXES: .jj .java .class

.PRECIOUS: %.java

.jj.java:
	$(JCC) $*.jj

.java.class:
	$(JC) $(JFLAGS) $*.java

all:	checker

checker: ast/Ast.class ast/astParser.class Checker.class

clean:
	rm *.class
