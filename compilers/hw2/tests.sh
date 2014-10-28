#!/bin/bash

printf "hello.java: "
java mjParser0 tst/hello.java

printf "sorting.java: "
java mjParser0 tst/sorting.java
for i in $(seq -f "%02g" $1 $2)
do
	printf "test%s.java: " $i
	java mjParser0 tst/test$i.java
done