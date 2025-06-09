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
#include <cstring>  // para strerror

using namespace std;
using namespace std::chrono;

struct Matrix {
    int rows, cols;
    vector<vector<int>> data;
};

Matrix readMatrix(ifstream& file) {
    Matrix mat;
    file >> mat.rows >> mat.cols;
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i)
        for (int j = 0; j < mat.cols; ++j)
            file >> mat.data[i][j];
    return mat;
}

Matrix multiplyWithForks(const Matrix& A, const Matrix& B, string filename, double& duration, long& memoryUsage) {
    int M = A.rows, N = A.cols, P = B.cols;
    Matrix result;
    result.rows = M;
    result.cols = P;
    result.data.resize(M, vector<int>(P));

    int total = M * P;
    vector<int*> pipes(total);
    vector<pid_t> pids(total);

    auto start = high_resolution_clock::now();

    for (int i = 0; i < total; ++i) {
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
            // Proceso hijo
            close(pipes[i][0]); // Cierra el extremo de lectura
            
            int row = i / P;
            int col = i % P;
            int sum = 0;
            for (int k = 0; k < N; ++k) {
                sum += A.data[row][k] * B.data[k][col];
            }

            // Escribe el resultado en el pipe
            if (write(pipes[i][1], &sum, sizeof(int)) == -1) {
                cerr << "Error escribiendo en pipe: " << strerror(errno) << endl;
            }
            close(pipes[i][1]);
            exit(0);
        } else {
            // Proceso padre
            pids[i] = pid;
            // No cerramos el extremo de escritura aquí todavía
        }
    }

    // Esperar hijos y leer pipes
    for (int i = 0; i < total; ++i) {
        int status;
        waitpid(pids[i], &status, 0); // Espera al hijo específico
        
        if (WIFEXITED(status)) {  // Aquí estaba el error - faltaba el paréntesis de cierre
            int val;
            // Ahora cerramos el extremo de escritura antes de leer
            close(pipes[i][1]);
            
            ssize_t bytes_read = read(pipes[i][0], &val, sizeof(int));
            if (bytes_read != sizeof(int)) {
                cerr << "Error leyendo pipe " << i << ": " 
                     << (bytes_read == -1 ? strerror(errno) : "tamaño incorrecto") << endl;
                val = 0; // fallback
            }
            
            int row = i / P;
            int col = i % P;
            result.data[row][col] = val;
        } else {
            cerr << "Proceso hijo " << pids[i] << " terminó anormalmente" << endl;
        }
        
        close(pipes[i][0]);
        delete[] pipes[i];
    }

    auto end = high_resolution_clock::now();
    duration = duration_cast<milliseconds>(end - start).count();

    // Medición de memoria mejorada
    struct rusage usage;
    getrusage(RUSAGE_CHILDREN, &usage); // Cambiado a RUSAGE_CHILDREN
    memoryUsage = usage.ru_maxrss;

    return result;
}

void saveMatrix(const Matrix& mat, const string& outputPath) {
    ofstream out(outputPath);
    out << mat.rows << " " << mat.cols << endl;
    for (const auto& row : mat.data) {
        for (int val : row) out << val << " ";
        out << endl;
    }
}

Matrix parseMatrixBlock(ifstream& file) {
    string line;
    while (getline(file, line) && line.empty()); // saltar líneas vacías
    stringstream ss(line);
    Matrix mat;
    ss >> mat.rows >> mat.cols;
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i)
        for (int j = 0; j < mat.cols; ++j)
            file >> mat.data[i][j];
    return mat;
}

int main() {
    string inputFolder = "easy/";
    string outputFolder = "Salidafork/";
    system(("mkdir -p " + outputFolder).c_str()); // crear carpeta si no existe (Linux)

    DIR* dir = opendir(inputFolder.c_str());
    if (!dir) {
        cerr << "No se pudo abrir la carpeta: " << inputFolder << endl;
        return 1;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL) {
        string filename = entry->d_name;
        if (filename.find(".txt") == string::npos) continue;

        string inputPath = inputFolder + filename;
        ifstream file(inputPath);
        if (!file.is_open()) {
            cerr << "No se pudo abrir el archivo: " << inputPath << endl;
            continue;
        }

        cout << "Procesando archivo: " << filename << endl;
        Matrix A = parseMatrixBlock(file);
        Matrix B = parseMatrixBlock(file);
        double timeTaken;
        long memoryUsed;
        Matrix result = multiplyWithForks(A, B, filename, timeTaken, memoryUsed);

        string outputPath = outputFolder + filename;
        saveMatrix(result, outputPath);

        cout << "Tiempo de ejecución (ms): " << timeTaken << endl;
        cout << "Uso de memoria (KB): " << memoryUsed << endl << endl;
    }

    closedir(dir);
    return 0;
}
