#!/bin/bash

for i in $(seq -f "%02g" $1 $2)
do
	printf "syntaxerror%s.java: " $i
	java mjParser0 err/syntaxerror$i.java
done