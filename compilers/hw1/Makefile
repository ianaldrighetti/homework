#----------------------------------------------------------------------
# CS321 Assignment 1 (Fall 2014)
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

all:	lexer1 lexer2

lexer1: Lexer1.class

lexer2: Lexer2.class mjTokenConstants.class

clean:	
	rm *.class
