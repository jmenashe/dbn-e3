.PHONY: clean test

default: RunAll.class

RunAll.class: RunAll.java
	javac RunAll.java

test: default
	java RunAll

clean:
	rm *.class
