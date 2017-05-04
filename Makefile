all:
	mkdir -p bin
	javac -d bin -cp src src/metdetection/*.java
	javac -d bin -cp src src/metdetection/*/*.java
	javac -d bin -cp src src/metdetection/*/*/*.java
