import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas {
    public static void main(String[] args) {
        // Creamos la carpeta para los resultados
        new File("SalidaThreads/easy").mkdirs();
        
        // buscaremos los archivos en la carpeta (se asume que hay archivos y que los archivos están en la misma carpeta que el .java)
        String base = "easy/";
        File carpeta = new File(base);
        File[] archivos = carpeta.listFiles((d, name) -> name.endsWith(".txt"));
        
        // Recorremos todos los archivos
        for (File archivo : archivos) {
            try {
                // Leemos las matrices (Aquí se asume que hay 2 máximo)
                List<int[][]> matrices = leer_matrices(archivo);
                int[][] A = matrices.get(0);
                int[][] B = matrices.get(1);
                // Se llama a la función para multiplicarlas
                int[][] C = multiplicar(A, B);
                // Se llama a la función para escribir el archivo resultante
                esc_resultado(archivo.getName(), C);
            } catch (IOException e) {
                System.err.println("Error en " + archivo.getName() + ": " + e.getMessage());
            }
        }
    }

    private static List<int[][]> leer_matrices(File f) throws IOException {
        // Para leer
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            // Sacamos la cantidad de matrices (podria saltarse este paso pero mejor mantenerlo para el bonus)
            int cant_M = Integer.parseInt(br.readLine().trim());

            // Se genera la lista de matrices como un ArrayList
            List<int[][]> matrices = new ArrayList<>();

            // for para leer las matrices
            for (int k = 0; k < cant_M; k++) {

                // Leer los vacios hasta tener datos y si es nulo es que se termino el archivo
                while ((linea = br.readLine()) != null && linea.trim().isEmpty());
                if (linea == null) break;
                
                // Leemos las dimensiones
                String[] dimensiones = linea.trim().split("\\s+");
                int n = Integer.parseInt(dimensiones[0]);
                int m = Integer.parseInt(dimensiones[1]);
                // Creamos la matriz de las dimensiones que recien leimos
                int[][] M = new int[n][m];
                
                // for para leer las filas
                for (int i = 0; i < n; i++) {
                    linea = br.readLine();
                    String[] valores = linea.trim().split("\\s+");
                    for (int j = 0; j < m; j++) {
                        M[i][j] = Integer.parseInt(valores[j]);
                    } 
                }

                // La guardamos en la lista de matrices
                matrices.add(M);
            }
            return matrices;
        }
    }

    // Clase interna para el cálculo por filas usando hebras (Mult por Balatro)
    private static class Mult_filas implements Runnable {
        private final int[][] A;
        private final int[][] B;
        private final int[][] C;
        private final int row;

        public Mult_filas(int[][] A, int[][] B, int[][] C, int row) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.row = row;
        }

        @Override
        public void run() {
            int n = A[0].length;
            int m = B[0].length;
            for (int j = 0; j < m; j++) {
                int sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += A[row][k] * B[k][j];
                }
                C[row][j] = sum;
            }
        }
    }

    private static int[][] multiplicar(int[][] A, int[][] B) {
        // Metodo para multiplicar usando las hebras
        // Leemos las dimensiones y creamos la nueva matriz
        int n = A.length;
        int m = B[0].length;
        int[][] C = new int[n][m];

        // Generamos tantas hebras como filas haya
        Thread[] threads = new Thread[n];
        
        // Creamos y ejecutamos las hebras
        for (int i = 0; i < n; i++) {
            Runnable task = new Mult_filas(A, B, C, i);
            threads[i] = new Thread(task);
            threads[i].start();
        }
        
        // Esperamos a que todas las hebras terminen
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

    private static void esc_resultado(String originalName, int[][] C) throws IOException {
        // función para escribir el resultado
        
        // nombre del archivo donde escribir
        String outName = "SalidaThreads/easy/resultado_" + originalName;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outName))) {
            // escribimos las dimensiones
            bw.write(C.length + " " + C[0].length + "\n");

            // escribimos la matriz
            for (int[] row : C) {
                for (int j = 0; j < row.length; j++) {
                    bw.write(row[j] + (j < row.length - 1 ? " " : ""));
                }
                bw.newLine();
            }
        }
    }
}