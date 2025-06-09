import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas {
    public static void main(String[] args) {
        String baseDir = "easy/";
        File folder = new File(baseDir);
        File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) return;
        for (File file : files) {
            try {
                List<int[][]> matrices = readMatrices(file);
                if (matrices.size() < 2) {
                    System.out.println("Archivo " + file.getName() + " no contiene dos matrices");
                    continue;
                }
                int[][] A = matrices.get(0);
                int[][] B = matrices.get(1);
                int[][] C = multiply(A, B);
                writeResult(file.getName(), C);
            } catch (IOException e) {
                System.err.println("Error en " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Procesamiento EASY completado.");
    }

    private static List<int[][]> readMatrices(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int count = Integer.parseInt(br.readLine().trim());
            List<int[][]> list = new ArrayList<>();
            for (int m = 0; m < count && m < 2; m++) {
                // Leer dimensiones
                while ((line = br.readLine()) != null && line.trim().isEmpty());
                if (line == null) break;
                String[] dim = line.trim().split("\\s+");
                int n = Integer.parseInt(dim[0]);
                int p = Integer.parseInt(dim[1]);
                int[][] M = new int[n][p];
                for (int i = 0; i < n; i++) {
                    line = br.readLine();
                    String[] vals = line.trim().split("\\s+");
                    for (int j = 0; j < p; j++) M[i][j] = Integer.parseInt(vals[j]);
                }
                list.add(M);
            }
            return list;
        }
    }

    private static int[][] multiply(int[][] A, int[][] B) {
        int n = A.length;
        int m = A[0].length;
        int p = B[0].length;
        int[][] C = new int[n][p];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p; j++) {
                int sum = 0;
                for (int k = 0; k < m; k++) sum += A[i][k] * B[k][j];
                C[i][j] = sum;
            }
        }
        return C;
    }

    private static void writeResult(String originalName, int[][] C) throws IOException {
        String outName = "SalidaThreads/easy/resultado_" + originalName;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outName))) {
            bw.write(C.length + " " + C[0].length + "\n");
            for (int[] row : C) {
                for (int j = 0; j < row.length; j++) {
                    bw.write(row[j] + (j < row.length - 1 ? " " : ""));
                }
                bw.newLine();
            }
        }
    }
}