# Makefile for CS322 Homework 3.
#
JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

all:	irgen

ast:	ast/Ast.class

astpsr: ast/astParser.class

ir:	ir/IR.class

irgen: 	astpsr ir IRGen.class

clean:
	'rm' ast/*.class ir/*.class *.class


