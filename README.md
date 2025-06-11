# LAB_3_SO_FERRERA_ROJAS
## Integrantes:
- Sergio Rojas 202273619-4 P.200
- Martín Ferrera 202273552-K P.200

## Especificación de los algoritmos y desarrollo realizada
Vamos con el flujo principal:

Creamos la carpeta de salida: "Salida{fork, Threads}/easy/"
Leemos los archivos de la carpeta /easy y las matrices A y B de cada uno.
Multiplicamos y escribimos el resultado en el archivo correspondiente en "/Salida{fork, Threads}/easy/num_archivo.txt"

Las variaciones van en que:
- C++: Usamos un hijo por fila para calcular la multiplicación
- Java: Usamos un thread por fila para calcular la multiplicación

Luego de procesar todos los archivos escribimos un resumen global para tener el tiempo y la memoria maxima utilizada.

Generamos distintas funciones para que el código fuera más sencillo de leer, entre estas está leer_matrices, multiplicar, multiplicarConForks y la escritura en los archivos de resultado y el global.


## Procedimiento para compilar
Se probó en Arch Linux y Ubuntu WSL2; con g++ 11.4.0 y Make 4.3
Para ejecutar el programa, se debe compilar desde el directorio del proyecto de la siguiente manera:

Si se quiere compilar todos los archivos con
- make

Si se quiere compilar archivos de C++ con
- make cpp

Si se quiere compilar archivos de Java con
- make java

Posteriormente ejecutar cada archivo por separado con
- make run_cpp
- make run_java
- make run_cpp_bonus
- make run_java_bonus

Notar que si se ejecuta de nuevo, el único archivo que cambiará será el de resumen global(que se llama easy.txt), esto no quiere decir que no se estén haciendo las multiplicaciones, solo que se está sobrescribiendo el archivo de resultado, si quiere que aparezcan de nuevo, tendrá que borrar las carpetas.

Por último si quiere eliminar los archivos ejecutables de c++ y los .class de java puede usar
- make clean

## Supuestos:
Los archivos: 
- makefile
- lab_3_ferrera_rojas.cpp
- lab_3_ferrera_rojas.java
- lab_3_ferrera_rojas_bonus.cpp
- lab_3_ferrera_rojas_bonus.java 

estan en el mismo directorio, ademas en este mismo directorio deben estar las carpetas easy, medium, hard con los archivos de prueba correspondientes a cada una de estas.

Una última cosa, las pruebas que se ven en el excel fueron medidas después de compilar cada código un par de veces, esto para que se normalizara el procedimiento y no hubiera demasiadas variaciones.