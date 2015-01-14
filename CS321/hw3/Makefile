#----------------------------------------------------------------------
# CS321 Assignment 3 (Fall 2014)
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

all:	parser

parser: ast/Ast.class mjParser.class

clean:	
	rm *.class
