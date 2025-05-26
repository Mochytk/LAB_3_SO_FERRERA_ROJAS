# Makefile para LAB3 en C++ y Java

# Archivos
CPP_SRC = LAB3_Apellido1_Apellido2.cpp
CPP_BIN = LAB3_Apellido1_Apellido2
JAVA_SRC = LAB3_Apellido1_Apellido2.java
JAVA_CLASS = LAB3_Apellido1_Apellido2

# Compiladores
CXX = g++
CXXFLAGS = -Wall
JAVAC = javac
JAVA = java

# Targets
all: cpp java

cpp: $(CPP_BIN)
	@echo "=== Ejecutando código C++ ==="
	./$(CPP_BIN)

$(CPP_BIN): $(CPP_SRC)
	$(CXX) $(CXXFLAGS) -o $(CPP_BIN) $(CPP_SRC)

java: $(JAVA_SRC)
	$(JAVAC) $(JAVA_SRC)
	@echo "=== Ejecutando código Java ==="
	$(JAVA) $(JAVA_CLASS)

clean:
	rm -f $(CPP_BIN) salidaFork.txt salidaThread.txt *.class
