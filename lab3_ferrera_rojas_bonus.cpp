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

struct Matriz {
    int rows, cols;
    vector<vector<int>> data;
};

bool esSimetrica(const Matriz& mat) {
    if (mat.rows != mat.cols) return false;
    
    for (int i = 0; i < mat.rows; ++i) {
        for (int j = 0; j < i; ++j) {
            if (mat.data[i][j] != mat.data[j][i]) {
                return false;
            }
        }
    }
    return true;
}

Matriz leerMatriz(ifstream& archivo) {
    Matriz mat;
    archivo >> mat.rows >> mat.cols;
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i)
        for (int j = 0; j < mat.cols; ++j)
            archivo >> mat.data[i][j];
    return mat;
}

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

void guardarMatriz(const Matriz& mat, const string& rutaSalida, double tiempoEjecucion, long memoriaUsada, bool esSimetricaResultado, bool multiplicable) {
    ofstream salida(rutaSalida);
    salida << "# Tiempo de ejecucion (ms): " << tiempoEjecucion << endl;
    salida << "# Uso de memoria (KB): " << memoriaUsada << endl;
    
    if (!multiplicable) {
        salida << "# Error: Las matrices no son compatibles para multiplicacion" << endl;
        salida.close();
        return;
    }

    salida << "# Matriz resultado: " << mat.rows << "x" << mat.cols << endl;
    salida << "# Es simetrica: " << (esSimetricaResultado ? "SI" : "NO") << endl;
    
    salida << mat.rows << " " << mat.cols << endl;
    for (const auto& fila : mat.data) {
        for (int valor : fila) salida << valor << " ";
        salida << endl;
    }
}

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

void procesarDirectorio(const string& directorioEntrada, const string& directorioSalida) {
    system(("mkdir -p " + directorioSalida).c_str());

    DIR* dir = opendir(directorioEntrada.c_str());
    if (!dir) {
        cerr << "No se pudo abrir la carpeta: " << directorioEntrada << endl;
        return;
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
        
        // Leer número de matrices
        string linea;
        while (getline(archivo, linea) && linea.empty());
        int numMatrices;
        stringstream ss(linea);
        if (!(ss >> numMatrices) || numMatrices < 2) {
            cerr << "Formato inválido para número de matrices" << endl;
            continue;
        }

        // Leer todas las matrices
        vector<Matriz> matrices;
        bool error = false;
        for (int i = 0; i < numMatrices && !error; ++i) {
            Matriz mat = analizarBloqueMatriz(archivo);
            matrices.push_back(mat);
            
            // Verificar compatibilidad con matriz anterior
            if (i > 0 && matrices[i-1].cols != matrices[i].rows) {
                cerr << "Error: Matrices incompatibles para multiplicación (" 
                     << matrices[i-1].rows << "x" << matrices[i-1].cols << " * "
                     << matrices[i].rows << "x" << matrices[i].cols << ")" << endl;
                error = true;
            }
        }
        
        // Variables para resultados
        double tiempoTotal = 0;
        long memoriaMaxima = 0;
        Matriz resultado;
        bool multiplicable = !error && matrices.size() >= 2;
        bool simetrica = false;

        if (multiplicable) {
            // Multiplicación en cadena
            resultado = matrices[0];
            
            for (size_t i = 1; i < matrices.size(); ++i) {
                double tiempoParcial;
                long memoriaParcial;
                
                resultado = multiplicarConForks(resultado, matrices[i], nombreArchivo, tiempoParcial, memoriaParcial);
                tiempoTotal += tiempoParcial;
                memoriaMaxima = max(memoriaMaxima, memoriaParcial);
            }

            // Análisis de simetría solo si la multiplicación fue posible
            simetrica = esSimetrica(resultado);
        }

        string rutaSalida = directorioSalida + nombreArchivo;
        guardarMatriz(resultado, rutaSalida, tiempoTotal, memoriaMaxima, simetrica, multiplicable);
    }

    closedir(dir);
}

int main() {
    // Procesar carpeta medium
    cout << "Procesando carpeta medium..." << endl;
    procesarDirectorio("medium/", "Salidafork/medium/");
    
    // Procesar carpeta hard
    cout << "\nProcesando carpeta hard..." << endl;
    procesarDirectorio("hard/", "Salidafork/hard/");

    return 0;
}