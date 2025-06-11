import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas {
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

    // Lista para almacenar estadísticas de todos los archivos
    private static List<Estadisticas> resumenGlobal = new ArrayList<>();

    public static void main(String[] args) {
        // Creamos la carpeta para los resultados
        new File("SalidaThreads/easy").mkdirs();
        
        // buscaremos los archivos en la carpeta
        String base = "easy/";
        File carpeta = new File(base);
        File[] archivos = carpeta.listFiles((d, name) -> name.endsWith(".txt"));
        if (archivos == null) return;
        
        // Recorremos todos los archivos
        for (File archivo : archivos) {
            try {
                // Leemos las matrices
                List<int[][]> matrices = leer_matrices(archivo);
                int[][] A = matrices.get(0);
                int[][] B = matrices.get(1);
                
                // Medición de tiempo y memoria
                long inicioTiempo = System.nanoTime();
                Runtime runtime = Runtime.getRuntime();
                long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();
                
                // Multiplicación
                int[][] C = multiplicar(A, B);
                int hebrasUsadas = A.length; // Número de hebras = filas de A
                
                // Cálculo de tiempo y memoria usada
                long finTiempo = System.nanoTime();
                long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
                long tiempoTotal = (finTiempo - inicioTiempo) / 1000000; // microsegundos
                long memoriaUsada = (memoriaFinal - memoriaInicial) / 1024; // KB
                
                // Guardar estadísticas
                resumenGlobal.add(new Estadisticas(
                    tiempoTotal, 
                    memoriaUsada, 
                    hebrasUsadas
                ));
                
                // Escribir resultado con estadísticas
                esc_resultado(archivo.getName(), C, tiempoTotal, memoriaUsada, hebrasUsadas);
                
            } catch (IOException e) {
                System.err.println("Error en " + archivo.getName() + ": " + e.getMessage());
            }
        }
        
        // Escribir resumen global
        escribirResumenGlobal();
    }

    private static List<int[][]> leer_matrices(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            int cant_M = Integer.parseInt(br.readLine().trim());
            List<int[][]> matrices = new ArrayList<>();
            for (int k = 0; k < cant_M; k++) {
                while ((linea = br.readLine()) != null && linea.trim().isEmpty());
                if (linea == null) break;
                
                String[] dimensiones = linea.trim().split("\\s+");
                int n = Integer.parseInt(dimensiones[0]);
                int m = Integer.parseInt(dimensiones[1]);
                int[][] M = new int[n][m];
                
                for (int i = 0; i < n; i++) {
                    linea = br.readLine();
                    String[] valores = linea.trim().split("\\s+");
                    for (int j = 0; j < m; j++) {
                        M[i][j] = Integer.parseInt(valores[j]);
                    } 
                }
                matrices.add(M);
            }
            return matrices;
        }
    }

    // Clase interna para el cálculo por filas usando hebras
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
            int n = A[0].length;
            int m = B[0].length;
            for (int j = 0; j < m; j++) {
                int suma = 0;
                for (int k = 0; k < n; k++) {
                    suma += A[fila][k] * B[k][j];
                }
                C[fila][j] = suma;
            }
        }
    }

    private static int[][] multiplicar(int[][] A, int[][] B) {
        int n = A.length;
        int m = B[0].length;
        int[][] C = new int[n][m];
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

    private static void esc_resultado(
        String id_archivo, 
        int[][] C, 
        long tiempo, 
        long memoria, 
        int hebras
    ) throws IOException {
        String archivo_salida = "SalidaThreads/easy/resultado_" + id_archivo;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo_salida))) {
            // Escribir estadísticas
            bw.write("# Tiempo de multiplicación: " + tiempo + " ms\n");
            bw.write("# Memoria usada: " + memoria + " KB\n");
            bw.write("# Hebras utilizadas: " + hebras + "\n");
            
            // Escribir matriz
            bw.write(C.length + " " + C[0].length + "\n");
            for (int[] fila : C) {
                for (int j = 0; j < fila.length; j++) {
                    bw.write(fila[j] + (j < fila.length - 1 ? " " : ""));
                }
                bw.newLine();
            }
        }
    }

    private static void escribirResumenGlobal() {
        File resumenFile = new File("SalidaThreads/easy.txt");
        try (PrintWriter pw = new PrintWriter(resumenFile)) {
            long tiempoTotal = 0;
            long memoriaMaxima = 0;
            long hebrasTotales = 0;
            int archivosProcesados = resumenGlobal.size();
            
            // Datos de cada archivo
            for (Estadisticas stats : resumenGlobal) {
                tiempoTotal += stats.tiempo;
                hebrasTotales += stats.hebras;
                if (stats.memoria > memoriaMaxima) {
                    memoriaMaxima = stats.memoria;
                }
            }
            
            // Totales y promedios
            pw.println("Resumen Final:");
            pw.println("--------------");
            pw.println("Total archivos procesados: " + archivosProcesados);
            pw.println("Tiempo total de procesamiento: " + tiempoTotal + " ms");
            pw.println("Memoria máxima utilizada: " + memoriaMaxima + " KB");
            pw.println("Total hebras utilizadas: " + hebrasTotales);
            pw.println("Promedio hebras por archivo: " + (hebrasTotales / (double)archivosProcesados));
            
        } catch (FileNotFoundException e) {
            System.err.println("Error al escribir el resumen global: " + e.getMessage());
        }
    }
}