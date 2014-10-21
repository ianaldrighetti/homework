#----------------------------------------------------------------------
# CS321 Assignment 2 (Fall 2014)
#----------------------------------------------------------------------

JFLAGS = -g
JC = javac
JCC = javacc

.SUFFIXES: .jj .java .class

.PRECIOUS: %.java

.jj.java:
	$(JCC) $*.jj

.java.class:
	$(JC) $(JFLAGS) $*.java

all:	mjParsr0

mjParser0: mjParser0.class

clean:	
	rm *.class
