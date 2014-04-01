.PHONY: clean test

default: RunAll.class

RunAll.class: RunAll.java
	javac RunAll.java

test:
	java RunAll

clean:
	rm *.class
