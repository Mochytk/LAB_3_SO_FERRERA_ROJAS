import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas_bonus {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese dificultad (medium/hard): ");
        String dificultad = sc.nextLine().trim().toLowerCase();
        String baseDir = dificultad + "/";
        File folder = new File(baseDir);
        File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) return;
        for (File file : files) {
            try {
                List<int[][]> matrices = readMatrices(file);
                if (matrices.size() < 2) {
                    System.out.println("Archivo " + file.getName() + " no contiene suficientes matrices");
                    continue;
                }
                int[][] result = matrices.get(0);
                boolean possible = true;
                for (int idx = 1; idx < matrices.size(); idx++) {
                    int[][] next = matrices.get(idx);
                    if (result[0].length != next.length) {
                        possible = false;
                        break;
                    }
                    result = multiply(result, next);
                }
                boolean symmetric = possible && isSymmetric(result);
                writeResult(file.getName(), result, possible, symmetric);
            } catch (IOException e) {
                System.err.println("Error en " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("Procesamiento MEDIUM/HARD completado.");
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

    private static void writeResult(String originalName, int[][] C, boolean possible, boolean sym) throws IOException {
        String outName = "SalidaThreads/" + (sym ? "medium_hard_sym" : "medium_hard") + "/resultado_" + originalName;
        File dir = new File(outName).getParentFile();
        if (!dir.exists()) dir.mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outName))) {
            bw.write("# Simétrica: " + sym + "\n");
            bw.write(C.length + " " + (C.length>0?C[0].length:0) + "\n");
            for (int[] row : C) {
                for (int j = 0; j < row.length; j++) {
                    bw.write(row[j] + (j < row.length - 1 ? " " : ""));
                }
                bw.newLine();
            }
        }
    }
}
