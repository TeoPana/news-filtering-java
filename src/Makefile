.PHONY: build run clean

# Jackson library jars
JACKSON_DATABIND = jackson-databind-2.18.1.jar
JACKSON_CORE = jackson-core-2.18.1.jar
JACKSON_ANNOTATIONS = jackson-annotations-2.18.1.jar

CLASSPATH = .:$(JACKSON_DATABIND):$(JACKSON_CORE):$(JACKSON_ANNOTATIONS)

build:
	@test -f $(JACKSON_DATABIND) || wget -q https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.18.1/jackson-databind-2.18.1.jar
	@test -f $(JACKSON_CORE) || wget -q https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.18.1.jar
	@test -f $(JACKSON_ANNOTATIONS) || wget -q https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.18.1.jar
	javac -cp $(CLASSPATH) *.java

run:
	java -cp $(CLASSPATH) Tema1 $(ARGS)

clean:
	rm -f *.class
	rm -f *.txt