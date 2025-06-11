/*import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas_bonus {
    // Clase para almacenar estadísticas de cada archivo procesado
    private static class Estadisticas {
        long tiempo;
        long memoria;
        int hebras;

        public Estadisticas(long tiempo, long memoria, int hebras) {
            this.tiempo = tiempo;
            this.memoria = memoria;
            this.hebras = hebras;
        }
    }

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

    // Lista para almacenar estadísticas de todos los archivos
    private static List<Estadisticas> resumenGlobal = new ArrayList<>();

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
}*/

import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas_bonus {
    // Clase para almacenar estadísticas de cada archivo procesado
    private static class Estadisticas {
        long tiempo;
        long memoria;
        int hebras;

        public Estadisticas(long tiempo, long memoria, int hebras) {
            this.tiempo = tiempo;
            this.memoria = memoria;
            this.hebras = hebras;
        }
    }

    // Clase interna para cálculo de filas con hebras
    private static class Mult_filas implements Runnable {
        private final int[][] A;
        private final int[][] B;
        private final int[][] C;
        private final int fila;

        public Mult_filas(int[][] A, int[][] B, int[][] C, int fila) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.fila = fila;
        }

        @Override
        public void run() {
            int m = A[0].length;
            int p = B[0].length;
            for (int j = 0; j < p; j++) {
                int suma = 0;
                for (int k = 0; k < m; k++) {
                    suma += A[fila][k] * B[k][j];
                }
                C[fila][j] = suma;
            }
        }
    }

    // Lista para almacenar estadísticas de todos los archivos
    private static List<Estadisticas> resumenGlobal = new ArrayList<>();

    public static void main(String[] args) {
        
        // Crear directorio base de salida para la dificultad seleccionada
        new File("SalidaThreads/medium").mkdirs();
        
        String baseDir = "medium/";
        File folder = new File(baseDir);
        File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
        
        // Recorremos todos los archivos
        for (File file : files) {
            try {
                // Inicio de medición de tiempo y memoria
                long inicioTiempo = System.nanoTime();
                Runtime runtime = Runtime.getRuntime();
                long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();
                
                // Procesar archivo
                List<int[][]> matrices = leer_matrices(file);
                int[][] result = matrices.get(0);
                boolean possible = true;
                int totalHebras = 0;
                
                for (int idx = 1; idx < matrices.size(); idx++) {
                    int[][] next = matrices.get(idx);
                    if (result[0].length != next.length) {
                        possible = false;
                        break;
                    }
                    
                    // Contar hebras para esta multiplicación
                    totalHebras += result.length;
                    result = multiplicar(result, next);
                }
                
                // Fin de medición
                long finTiempo = System.nanoTime();
                long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
                long tiempoTotal = (finTiempo - inicioTiempo) / 1_000_000; // ms
                long memoriaUsada = (memoriaFinal - memoriaInicial) / 1024; // KB
                boolean simetrica = possible && es_simetrica(result);
                
                // Guardar estadísticas
                resumenGlobal.add(new Estadisticas(tiempoTotal, memoriaUsada, totalHebras));
                
                // Escribir resultado con estadísticas
                esc_resultado(file.getName(), result, possible, simetrica, "medium", 
                             tiempoTotal, memoriaUsada, totalHebras);
                
            } catch (IOException e) {
                System.err.println("Error en " + file.getName() + ": " + e.getMessage());
            }
        }
        
        
        // Escribir resumen global
        escribirResumenGlobal("medium");

        new File("SalidaThreads/hard").mkdirs();
        
        resumenGlobal = new ArrayList<>();
        baseDir = "hard/";
        folder = new File(baseDir);
        files = folder.listFiles((d, name) -> name.endsWith(".txt"));
        
        // Recorremos todos los archivos
        for (File file : files) {
            try {
                // Inicio de medición de tiempo y memoria
                long inicioTiempo = System.nanoTime();
                Runtime runtime = Runtime.getRuntime();
                long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();
                
                // Procesar archivo
                List<int[][]> matrices = leer_matrices(file);
                int[][] result = matrices.get(0);
                boolean possible = true;
                int totalHebras = 0;
                
                for (int idx = 1; idx < matrices.size(); idx++) {
                    int[][] next = matrices.get(idx);
                    if (result[0].length != next.length) {
                        possible = false;
                        break;
                    }
                    
                    // Contar hebras para esta multiplicación
                    totalHebras += result.length;
                    result = multiplicar(result, next);
                }
                
                // Fin de medición
                long finTiempo = System.nanoTime();
                long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
                long tiempoTotal = (finTiempo - inicioTiempo) / 1_000_000; // ms
                long memoriaUsada = (memoriaFinal - memoriaInicial) / 1024; // KB
                boolean simetrica = possible && es_simetrica(result);
                
                // Guardar estadísticas
                resumenGlobal.add(new Estadisticas(tiempoTotal, memoriaUsada, totalHebras));
                
                // Escribir resultado con estadísticas
                esc_resultado(file.getName(), result, possible, simetrica, "hard", 
                             tiempoTotal, memoriaUsada, totalHebras);
                
            } catch (IOException e) {
                System.err.println("Error en " + file.getName() + ": " + e.getMessage());
            }
        }
        
        
        // Escribir resumen global
        escribirResumenGlobal("hard");
    }

    private static List<int[][]> leer_matrices(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            int count = Integer.parseInt(br.readLine().trim());
            List<int[][]> list = new ArrayList<>();
            for (int m = 0; m < count; m++) {
                while ((linea = br.readLine()) != null && linea.trim().isEmpty());
                if (linea == null) break;
                
                String[] dim = linea.trim().split("\\s+");
                int n = Integer.parseInt(dim[0]);
                int p = Integer.parseInt(dim[1]);
                int[][] M = new int[n][p];
                for (int i = 0; i < n; i++) {
                    linea = br.readLine();
                    String[] vals = linea.trim().split("\\s+");
                    for (int j = 0; j < p; j++) M[i][j] = Integer.parseInt(vals[j]);
                }
                list.add(M);
            }
            return list;
        }
    }

    // Multiplicación paralelizada por filas
    private static int[][] multiplicar(int[][] A, int[][] B) {
        int n = A.length;
        int p = B[0].length;
        int[][] C = new int[n][p];
        Thread[] threads = new Thread[n];
        
        for (int i = 0; i < n; i++) {
            Runnable task = new Mult_filas(A, B, C, i);
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

    private static boolean es_simetrica(int[][] M) {
        int n = M.length;
        if (n == 0 || n != M[0].length) return false;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (M[i][j] != M[j][i]) return false;
            }
        }
        return true;
    }

    private static void esc_resultado(
        String originalName, 
        int[][] C, 
        boolean possible,
        boolean sym, 
        String dificultad,
        long tiempo,
        long memoria,
        int hebras
    ) throws IOException {
        String outName = "SalidaThreads/" + dificultad + "/resultado_" + originalName;
        
        // Asegurar que exista el directorio
        File outputDir = new File("SalidaThreads/" + dificultad);
        if (!outputDir.exists()) outputDir.mkdirs();
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outName))) {
            bw.write("# Simétrica: " + sym + "\n");
            bw.write("# Tiempo de procesamiento: " + tiempo + " ms\n");
            bw.write("# Memoria usada: " + memoria + " KB\n");
            bw.write("# Hebras utilizadas: " + hebras + "\n");
            bw.write(C.length + " " + (C.length > 0 ? C[0].length : 0) + "\n");
            
            for (int[] fila : C) {
                for (int j = 0; j < fila.length; j++) {
                    bw.write(fila[j] + (j < fila.length - 1 ? " " : ""));
                }
                bw.newLine();
            }
        }
    }

    private static void escribirResumenGlobal(String dificultad) {
        File resumenFile = new File("SalidaThreads/"+ dificultad +".txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(resumenFile, false))) {
            long tiempoTotal = 0;
            long memoriaMaxima = 0;
            long hebrasTotales = 0;
            int archivosProcesados = resumenGlobal.size();
            
            // Calcular estadísticas globales
            for (Estadisticas stats : resumenGlobal) {
                tiempoTotal += stats.tiempo;
                hebrasTotales += stats.hebras;
                if (stats.memoria > memoriaMaxima) {
                    memoriaMaxima = stats.memoria;
                }
            }
            
            
            // Escribir resumen para esta dificultad
            pw.println("Resumen Final:");
            pw.println("--------------");
            pw.println("Archivos procesados: " + archivosProcesados);
            pw.println("Tiempo total: " + tiempoTotal + " ms");
            pw.println("Memoria máxima utilizada: " + memoriaMaxima + " KB");
            pw.println("Total hebras utilizadas: " + hebrasTotales);
            pw.println("Promedio hebras por archivo: " + (hebrasTotales / (double)archivosProcesados));
            
            
        } catch (IOException e) {
            System.err.println("Error al escribir el resumen global: " + e.getMessage());
        }
    }
}