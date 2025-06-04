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
    vector<int[2]> pipes(total);

    auto start = high_resolution_clock::now();

    for (int i = 0; i < total; ++i) {
        pipe(pipes[i]);
        pid_t pid = fork();
        if (pid == 0) {
            int row = i / P;
            int col = i % P;
            int sum = 0;
            for (int k = 0; k < N; ++k)
                sum += A.data[row][k] * B.data[k][col];
            write(pipes[i][1], &sum, sizeof(int));
            close(pipes[i][1]);
            exit(0);
        }
    }

    for (int i = 0; i < total; ++i)
        wait(NULL);

    for (int i = 0; i < total; ++i) {
        int val;
        read(pipes[i][0], &val, sizeof(int));
        int row = i / P;
        int col = i % P;
        result.data[row][col] = val;
        close(pipes[i][0]);
    }

    auto end = high_resolution_clock::now();
    duration = duration_cast<milliseconds>(end - start).count();

    struct rusage usage;
    getrusage(RUSAGE_SELF, &usage);
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
    while (getline(file, line) && line.empty()); // skip blank lines
    stringstream ss(line);
    Matrix mat;
    ss >> mat.rows >> mat.cols;
    mat.data.resize(mat.rows, vector<int>(mat.cols));
    for (int i = 0; i < mat.rows; ++i) {
        for (int j = 0; j < mat.cols; ++j) {
            file >> mat.data[i][j];
        }
    }
    return mat;
}

int main() {
    string inputFolder = "input/";
    string outputFolder = "output/";
    DIR* dir = opendir(inputFolder.c_str());
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
