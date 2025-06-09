# Archivos fuente y binarios
CPP_SRC       = lab3_ferrera_rojas.cpp
CPP_BIN       = lab3_ferrera_rojas
JAVA_SRC      = lab3_ferrera_rojas.java lab3_ferrera_rojas_bonus.java
JAVA_CLASSES  = $(JAVA_SRC:.java=.class)

# Compiladores
CXX           = g++
CXXFLAGS      = -Wall
JAVAC         = javac

# Targets generales
.PHONY: all cpp java clean run_cpp run_java_base run_java_bonus

all: cpp java

# Compilación C++
cpp: $(CPP_BIN)

$(CPP_BIN): $(CPP_SRC)
	$(CXX) $(CXXFLAGS) -o $@ $<

# Compilación Java (ambas clases)
java: $(JAVA_CLASSES)

%.class: %.java
	$(JAVAC) $<

# Targets de ejecución
run_cpp: $(CPP_BIN)
	@echo "Ejecutando C++:"
	./$(CPP_BIN)

run_java_base: lab3_ferrera_rojas.class
	@echo "Ejecutando Java (base):"
	java lab3_ferrera_rojas

run_java_bonus: lab3_ferrera_rojas_bonus.class
	@echo "Ejecutando Java (bonus):"
	java lab3_ferrera_rojas_bonus

# Limpieza
clean:
	rm -f $(CPP_BIN) *.class