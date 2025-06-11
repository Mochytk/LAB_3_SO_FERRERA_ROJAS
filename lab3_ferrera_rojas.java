import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas {
    // Clase interna para almacenar las estadísticas de cada archivo procesado
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
            for (int i = 0; i < m; i++) {
                int suma = 0;
                for (int j = 0; j < n; j++) {
                    suma += A[fila][j] * B[j][i];
                }
                C[fila][i] = suma;
            }
        }
    }

    // Lista para almacenar estadísticas de todos los archivos y luego colocarlas en el resumen
    private static List<Estadisticas> resumenGlobal = new ArrayList<>();

    public static void main(String[] args) {

        // Creamos la carpeta para los resultados
        new File("SalidaThreads/easy").mkdirs();
        
        // buscaremos los archivos en la carpeta y los guardamos en un arreglo (se asume que existen archivos y que estos son .txt)
        String base = "easy/";
        File carpeta = new File(base);
        File[] archivos = carpeta.listFiles((_, name) -> name.endsWith(".txt"));
        
        // Recorremos todos los archivos
        for (File archivo : archivos) {
            try {
                // Leemos las matrices
                List<int[][]> matrices = leer_matrices(archivo);
                int[][] A = matrices.get(0);
                int[][] B = matrices.get(1);
                
                // Medición de tiempo y memoria inicial
                long inicioTiempo = System.nanoTime();
                Runtime runtime = Runtime.getRuntime();
                long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();
                
                // Multiplicamos con el método
                int[][] C = multiplicar(A, B);
                int hebrasUsadas = A.length; // el número de hebras será igual a las filas de A
                
                // Calculamos de tiempo y memoria usada después de multiplicar
                long finTiempo = System.nanoTime();
                long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
                long tiempoTotal = (finTiempo - inicioTiempo) / 1000000; // medimos en ms
                long memoriaUsada = (memoriaFinal - memoriaInicial) / 1024; // medimos en KB
                
                // Guardamos las estadísticas en la lista para el resumen global
                resumenGlobal.add(new Estadisticas(tiempoTotal, memoriaUsada, hebrasUsadas));
                
                // Escribimos el resultado con las estadísticas en el archivo resultado_i.txt
                esc_resultado(archivo.getName(), C, tiempoTotal, memoriaUsada, hebrasUsadas);
                
            } catch (IOException e) {
                System.err.println("Error en " + archivo.getName() + ": " + e.getMessage());
            }
        }
        
        // Escribimos el resumen global
        escribirResumenGlobal();
    }

    private static List<int[][]> leer_matrices(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            
            // String para leer las lineas correspondientes
            String linea;
            
            // Leemos la cantidad de matrices (método escalable para el bonus, aquí siempre serán 2)
            // Con parseInt transformamos de String a Int
            int cant_M = Integer.parseInt(br.readLine().trim());

            // Generamos la lista de arreglos de enteros de dos dimensiones (las matrices pues)
            List<int[][]> matrices = new ArrayList<>();

            // Leemos hasta que se nos acaben las matrices
            for (int k = 0; k < cant_M; k++) {

                // Leemos las lineas vacías y seguimos
                while ((linea = br.readLine()) != null && linea.trim().isEmpty());
                // Si la linea esta vacia entoces llegamos al final del archivo
                if (linea == null) break;
                
                // Primero leemos las dimensiones n y m 
                // Con parseInt transformamos de String a Int
                String[] dimensiones = linea.trim().split("\\s+");
                int n = Integer.parseInt(dimensiones[0]);
                int m = Integer.parseInt(dimensiones[1]);
                // Generamos la matriz del tamaño correspondiente
                int[][] M = new int[n][m];
                
                // Llenamos la matriz de los valores correspondientes, separando la linea en un arreglo que usará los espacios como separador
                // Con parseInt transformamos de String a Int
                for (int i = 0; i < n; i++) {
                    linea = br.readLine();
                    String[] valores = linea.trim().split("\\s+");
                    for (int j = 0; j < m; j++) {
                        M[i][j] = Integer.parseInt(valores[j]);
                    } 
                }
                // añadimos al arreglo de matrices del archivo total
                matrices.add(M);
            }
            // Retornamos el arreglo de matrices
            return matrices;
        }
    }

    
    // Multiplicación paralelizada por filas
    private static int[][] multiplicar(int[][] A, int[][] B) {
        int n = A.length;
        int m = B[0].length;
        // Generamos la matriz del tamaño resultante
        int[][] C = new int[n][m];
        // Generamos un arreglo de Threads del tamaño de n
        Thread[] threads = new Thread[n];
        
        // Creamos las hebras con la tarea correspondiente y les damos arranque
        for (int i = 0; i < n; i++) {
            Runnable tarea = new Mult_filas(A, B, C, i);
            threads[i] = new Thread(tarea);
            threads[i].start();
        }
        
        // Esperamos a todas las hebras, catch por si algo falla
        for (int i = 0; i < n; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.err.println("Interrupción en hebra: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        // Retornamos la matriz resultante
        return C;
    }

    private static void esc_resultado(String id_archivo, int[][] C, long tiempo, long memoria, int hebras) throws IOException {
        
        String archivo_salida = "SalidaThreads/easy/resultado_" + id_archivo;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo_salida))) {
            // Escribimos las estadísticas
            bw.write("# Tiempo de multiplicación: " + tiempo + " ms\n");
            bw.write("# Memoria usada: " + memoria + " KB\n");
            bw.write("# Hebras utilizadas: " + hebras + "\n");
            
            // Escribimos la matriz
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
        
        // Creamos la direccion del resumen
        File salida_resumen = new File("SalidaThreads/easy.txt");
        
        try (PrintWriter pw = new PrintWriter(salida_resumen)) {
            // Inicializamos variables de tiempo, memoria y hebras totales
            long tiempoTotal = 0;
            long memoriaMaxima = 0;
            long hebrasTotales = 0;
            int archivosProcesados = resumenGlobal.size();
            
            // Leemos y calculamos las estadísticas globales
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