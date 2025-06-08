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

    vector<int*> pipes(M);
    vector<pid_t> pids(M);

    auto start = high_resolution_clock::now();

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
                int sum = 0;
                for (int k = 0; k < N; ++k) {
                    sum += A.data[i][k] * B.data[k][j];
                }
                if (write(pipes[i][1], &sum, sizeof(int)) == -1) {
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
            int val;
            if (read(pipes[i][0], &val, sizeof(int)) != sizeof(int)) {
                cerr << "Error leyendo pipe " << i << "," << j << endl;
                val = 0;
            }
            result.data[i][j] = val;
        }
        
        close(pipes[i][0]);
        delete[] pipes[i];
        
        int status;
        waitpid(pids[i], &status, 0);
    }

    auto end = high_resolution_clock::now();
    duration = duration_cast<milliseconds>(end - start).count();

    struct rusage usage;
    getrusage(RUSAGE_CHILDREN, &usage);
    memoryUsage = usage.ru_maxrss;

    return result;
}

void saveMatrix(const Matrix& mat, const string& outputPath, double timeTaken, long memoryUsed) {
    ofstream out(outputPath);
    out << "# Tiempo de ejecucion (ms): " << timeTaken << endl;
    out << "# Uso de memoria (KB): " << memoryUsed << endl;
    out << "# Matriz resultado: " << mat.rows << "x" << mat.cols << endl;
    
    out << mat.rows << " " << mat.cols << endl;
    for (const auto& row : mat.data) {
        for (int val : row) out << val << " ";
        out << endl;
    }
}

Matrix parseMatrixBlock(ifstream& file) {
    Matrix mat;
    string line;
    
    while (getline(file, line)) {
        if (line.empty()) continue;
        stringstream ss(line);
        if (ss >> mat.rows >> mat.cols) break;
    }
    
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i) {
        while (getline(file, line) && line.empty());
        
        stringstream ss(line);
        for (int j = 0; j < mat.cols; ++j) {
            if (!(ss >> mat.data[i][j])) {
                cerr << "Error leyendo elemento [" << i << "][" << j << "]" << endl;
                mat.data[i][j] = 0;
            }
        }
    }
    return mat;
}

int main() {
    string inputFolder = "easy/";
    string outputFolder = "Salidafork/";
    system(("mkdir -p " + outputFolder).c_str());

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
        
        if (A.cols != B.rows) {
            cerr << "Error: Las matrices no son compatibles para multiplicación (" 
                << A.rows << "x" << A.cols << " * " << B.rows << "x" << B.cols << ")" << endl;
            continue;
        }

        double timeTaken;
        long memoryUsed;
        Matrix result = multiplyWithForks(A, B, filename, timeTaken, memoryUsed);

        string outputPath = outputFolder + filename;
        saveMatrix(result, outputPath, timeTaken, memoryUsed);
    }

    closedir(dir);
    return 0;
}