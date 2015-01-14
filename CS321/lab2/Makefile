# Makefile for CS321 Lab 2 (J. Li)
#
JFLAGS = -g
JC = javac
JCC = javacc

.SUFFIXES: .jj .java .class

.PRECIOUS: %.java

.jj.java:
	$(JCC) $*.jj

.java.class:
	$(JC) $(JFLAGS) $*.java

all:	Lexer.class

clean:
	rm */*.class */*Constants.java */Parse*.java */Simple*.java */*Token*.java
