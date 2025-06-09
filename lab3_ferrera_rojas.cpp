#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <dirent.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/resource.h>
#include <chrono>
#include <cstring>

using namespace std;
using namespace std::chrono;

/**
 * Estructura que representa una matriz con:
 * - rows: número de filas
 * - cols: número de columnas
 * - data: vector bidimensional que almacena los valores
 */
struct Matriz {
    int rows, cols;
    vector<vector<int>> data;
};

/**
 * Lee una matriz desde un archivo de entrada
 * @param archivo Referencia al archivo abierto
 * @return Matriz leída con sus dimensiones y datos
 */
Matriz leerMatriz(ifstream& archivo) {
    Matriz mat;
    archivo >> mat.rows >> mat.cols;
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i)
        for (int j = 0; j < mat.cols; ++j)
            archivo >> mat.data[i][j];
    return mat;
}

/**
 * Multiplica dos matrices usando procesos hijos (fork) y pipes para comunicación
 * @param A Primera matriz a multiplicar
 * @param B Segunda matriz a multiplicar
 * @param nombreArchivo Nombre del archivo que se está procesando (para logs)
 * @param duracion Referencia para almacenar el tiempo de ejecución
 * @param usoMemoria Referencia para almacenar el uso de memoria
 * @return Matriz resultante de la multiplicación A × B
 */
Matriz multiplicarConForks(const Matriz& A, const Matriz& B, string nombreArchivo, double& duracion, long& usoMemoria) {
    int M = A.rows, N = A.cols, P = B.cols;
    Matriz resultado;
    resultado.rows = M;
    resultado.cols = P;
    resultado.data.resize(M, vector<int>(P));

    vector<int*> pipes(M);
    vector<pid_t> pids(M);

    auto inicio = high_resolution_clock::now();

    for (int i = 0; i < M; ++i) {
        pipes[i] = new int[2];
        if (pipe(pipes[i]) == -1) {
            cerr << "Error creando pipe: " << strerror(errno) << endl;
            exit(1);
        }

        pid_t pid = fork();
        if (pid == -1) {
            cerr << "Error en fork(): " << strerror(errno) << endl;
            exit(1);
        }

        if (pid == 0) {
            close(pipes[i][0]);
            
            for (int j = 0; j < P; ++j) {
                int suma = 0;
                for (int k = 0; k < N; ++k) {
                    suma += A.data[i][k] * B.data[k][j];
                }
                if (write(pipes[i][1], &suma, sizeof(int)) == -1) {
                    cerr << "Error escribiendo en pipe: " << strerror(errno) << endl;
                    exit(1);
                }
            }
            
            close(pipes[i][1]);
            exit(0);
        } else {
            pids[i] = pid;
        }
    }

    for (int i = 0; i < M; ++i) {
        close(pipes[i][1]);
        
        for (int j = 0; j < P; ++j) {
            int valor;
            if (read(pipes[i][0], &valor, sizeof(int)) != sizeof(int)) {
                cerr << "Error leyendo pipe " << i << "," << j << endl;
                valor = 0;
            }
            resultado.data[i][j] = valor;
        }
        
        close(pipes[i][0]);
        delete[] pipes[i];
        
        int estado;
        waitpid(pids[i], &estado, 0);
    }

    auto fin = high_resolution_clock::now();
    duracion = duration_cast<milliseconds>(fin - inicio).count();

    struct rusage uso;
    getrusage(RUSAGE_CHILDREN, &uso);
    usoMemoria = uso.ru_maxrss;

    return resultado;
}

/**
 * Guarda una matriz en un archivo de salida incluyendo métricas de rendimiento
 * @param mat Matriz a guardar
 * @param rutaSalida Ruta del archivo de salida
 * @param tiempoEjecucion Tiempo de ejecución en milisegundos
 * @param memoriaUsada Memoria utilizada en KB
 */
void guardarMatriz(const Matriz& mat, const string& rutaSalida, double tiempoEjecucion, long memoriaUsada) {
    ofstream salida(rutaSalida);
    salida << "# Tiempo de ejecucion (ms): " << tiempoEjecucion << endl;
    salida << "# Uso de memoria (KB): " << memoriaUsada << endl;
    salida << "# Matriz resultado: " << mat.rows << "x" << mat.cols << endl;
    
    salida << mat.rows << " " << mat.cols << endl;
    for (const auto& fila : mat.data) {
        for (int valor : fila) salida << valor << " ";
        salida << endl;
    }
}

/**
 * Lee un bloque de matriz desde un archivo, manejando líneas vacías
 * @param archivo Referencia al archivo abierto
 * @return Matriz leída con sus dimensiones y datos
 */
Matriz analizarBloqueMatriz(ifstream& archivo) {
    Matriz mat;
    string linea;
    
    while (getline(archivo, linea)) {
        if (linea.empty()) continue;
        stringstream ss(linea);
        if (ss >> mat.rows >> mat.cols) break;
    }
    
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i) {
        while (getline(archivo, linea) && linea.empty());
        
        stringstream ss(linea);
        for (int j = 0; j < mat.cols; ++j) {
            if (!(ss >> mat.data[i][j])) {
                cerr << "Error leyendo elemento [" << i << "][" << j << "]" << endl;
                mat.data[i][j] = 0;
            }
        }
    }
    return mat;
}

/**
 * Función principal que procesa todos los archivos de matrices en un directorio
 * @return 0 si termina correctamente, 1 si hay error al abrir el directorio
 */
int main() {
    string directorioEntrada = "easy/";
    string directorioSalida = "Salidafork/easy/";
    system(("mkdir -p " + directorioSalida).c_str());

    DIR* dir = opendir(directorioEntrada.c_str());
    if (!dir) {
        cerr << "No se pudo abrir la carpeta: " << directorioEntrada << endl;
        return 1;
    }

    struct dirent* entrada;
    while ((entrada = readdir(dir)) != NULL) {
        string nombreArchivo = entrada->d_name;
        if (nombreArchivo.find(".txt") == string::npos) continue;

        string rutaEntrada = directorioEntrada + nombreArchivo;
        ifstream archivo(rutaEntrada);
        if (!archivo.is_open()) {
            cerr << "No se pudo abrir el archivo: " << rutaEntrada << endl;
            continue;
        }

        cout << "Procesando archivo: " << nombreArchivo << endl;
        Matriz A = analizarBloqueMatriz(archivo);
        Matriz B = analizarBloqueMatriz(archivo);
        
        if (A.cols != B.rows) {
            cerr << "Error: Las matrices no son compatibles para multiplicación (" 
                << A.rows << "x" << A.cols << " * " << B.rows << "x" << B.cols << ")" << endl;
            continue;
        }

        double tiempoEjecucion;
        long memoriaUsada;
        Matriz resultado = multiplicarConForks(A, B, nombreArchivo, tiempoEjecucion, memoriaUsada);

        string rutaSalida = directorioSalida + nombreArchivo;
        guardarMatriz(resultado, rutaSalida, tiempoEjecucion, memoriaUsada);
    }

    closedir(dir);
    return 0;
}