import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas_bonus {
    // Clase interna para cálculo de filas con hebras
    private static class RowMultiplier implements Runnable {
        private final int[][] A;
        private final int[][] B;
        private final int[][] C;
        private final int row;

        public RowMultiplier(int[][] A, int[][] B, int[][] C, int row) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.row = row;
        }

        @Override
        public void run() {
            int m = A[0].length;
            int p = B[0].length;
            for (int j = 0; j < p; j++) {
                int sum = 0;
                for (int k = 0; k < m; k++) {
                    sum += A[row][k] * B[k][j];
                }
                C[row][j] = sum;
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese dificultad (medium/hard): ");
        String dificultad = sc.nextLine().trim().toLowerCase();
        
        // Crear directorio base de salida para la dificultad seleccionada
        new File("SalidaThreads/" + dificultad).mkdirs();
        
        String baseDir = dificultad + "/";
        File folder = new File(baseDir);
        File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
        
        for (File file : files) {
            try {
                List<int[][]> matrices = readMatrices(file);
                int[][] result = matrices.get(0);
                boolean possible = true;
                for (int idx = 1; idx < matrices.size(); idx++) {
                    int[][] next = matrices.get(idx);
                    if (result[0].length != next.length) {
                        possible = false;
                        break;
                    }
                    result = multiply(result, next); // Multiplicación paralelizada
                }
                boolean symmetric = possible && isSymmetric(result);
                writeResult(file.getName(), result, possible, symmetric, dificultad);
            } catch (IOException e) {
                System.err.println("Error en " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Procesamiento " + dificultad.toUpperCase() + " completado.");
        sc.close();
    }

    private static List<int[][]> readMatrices(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine().trim();
            int count = Integer.parseInt(line);
            List<int[][]> list = new ArrayList<>();
            for (int m = 0; m < count; m++) {
                while ((line = br.readLine()) != null && line.trim().isEmpty());
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

    // Multiplicación paralelizada por filas
    private static int[][] multiply(int[][] A, int[][] B) {
        int n = A.length;
        int p = B[0].length;
        int[][] C = new int[n][p];
        Thread[] threads = new Thread[n];
        
        for (int i = 0; i < n; i++) {
            Runnable task = new RowMultiplier(A, B, C, i);
            threads[i] = new Thread(task);
            threads[i].start();
        }
        
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.err.println("Interrupción en hebra: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return C;
    }

    private static boolean isSymmetric(int[][] M) {
        int n = M.length;
        if (n == 0 || n != M[0].length) return false;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (M[i][j] != M[j][i]) return false;
            }
        }
        return true;
    }

    private static void writeResult(String originalName, int[][] C, boolean possible, boolean sym, String dificultad) throws IOException {
        String outName = "SalidaThreads/" + dificultad + "/resultado_" + originalName;
        
        // Asegurar que exista el directorio
        File outputDir = new File("SalidaThreads/" + dificultad);
        if (!outputDir.exists()) outputDir.mkdirs();
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outName))) {
            bw.write("# Simétrica: " + sym + "\n");
            bw.write(C.length + " " + (C.length > 0 ? C[0].length : 0) + "\n");
            for (int[] row : C) {
                for (int j = 0; j < row.length; j++) {
                    bw.write(row[j] + (j < row.length - 1 ? " " : ""));
                }
                bw.newLine();
            }
        }
    }
}