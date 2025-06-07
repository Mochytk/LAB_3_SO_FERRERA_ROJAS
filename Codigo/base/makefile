# Archivos fuente y binarios
CPP_SRC = lab3_ferrera_rojas.cpp
CPP_BIN = lab3_ferrera_rojas
JAVA_SRC = lab3_ferrera_rojas.java
JAVA_CLASS = lab3_ferrera_rojas

# Compiladores
CXX = g++
CXXFLAGS = -Wall
JAVAC = javac

# Targets
all: cpp java

cpp: $(CPP_BIN)

$(CPP_BIN): $(CPP_SRC)
	$(CXX) $(CXXFLAGS) -o $(CPP_BIN) $(CPP_SRC)

java: $(JAVA_CLASS).class

$(JAVA_CLASS).class: $(JAVA_SRC)
	$(JAVAC) $(JAVA_SRC)

clean:
	rm -f $(CPP_BIN) *.class
