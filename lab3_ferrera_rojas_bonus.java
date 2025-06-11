import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas_bonus {
    // Clase interna para almacenar estadísticas de cada archivo procesado
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

    // Lista para almacenar estadísticas de todos los archivos
    private static List<Estadisticas> resumenGlobal = new ArrayList<>();

    public static void main(String[] args) {
        // Decidí meterlo a una función porque se veía feo uno despues del otro
        ejecutar("medium");
        ejecutar("hard");
    }

    private static void ejecutar(String dificultad){
        
        // Creamos la carpeta para los resultados
        new File("SalidaThreads/" + dificultad).mkdirs();
        
        // vaciamos el resumen global anterior (si no hay sigue funcionando, es vaciar algo que ya está vacio)
        resumenGlobal = new ArrayList<>();
        
        // buscaremos los archivos en la carpeta y los guardamos en un arreglo (se asume que existen archivos y que estos son .txt)
        String baseDir = dificultad + "/";
        File folder = new File(baseDir);
        File[] files = folder.listFiles((_, name) -> name.endsWith(".txt"));
        
        // Recorremos todos los archivos
        for (File file : files) {
            try {
                // Leemos las matrices
                List<int[][]> matrices = leer_matrices(file);
                int[][] matriz_actual = matrices.get(0);
                
                // booleano que comprueba si se puede multiplicar
                boolean mult_posible = true;
                int totalHebras = 0;
                
                // Inicio de medición de tiempo y memoria
                long inicioTiempo = System.nanoTime();
                Runtime runtime = Runtime.getRuntime();
                long memoriaInicial = runtime.totalMemory() - runtime.freeMemory();

                // Vamos compribando si se puede multiplicar y si se puede multiplicamos
                for (int i = 1; i < matrices.size(); i++) {
                    
                    int[][] matriz_sig = matrices.get(i);
                    if (matriz_actual[0].length != matriz_sig.length) {
                        mult_posible = false;
                        break;
                    }
                    
                    // Contar hebras para cada multiplicación y multiplicar
                    totalHebras += matriz_actual.length;
                    matriz_actual = multiplicar(matriz_actual, matriz_sig);
                }
                
                // Calculamos de tiempo y memoria usada después de multiplicar
                long finTiempo = System.nanoTime();
                long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
                long tiempoTotal = (finTiempo - inicioTiempo) / 1_000_000; // medimos en ms
                long memoriaUsada = (memoriaFinal - memoriaInicial) / 1024; // medimos en KB
                
                // Si no se pudo multiplicar claramente no tendremos una matriz simetrica, si se pudo entonces comprobamos con un método
                boolean simetrica = mult_posible && es_simetrica(matriz_actual);
                
                // Guardamos las estadísticas en la lista para el resumen global
                resumenGlobal.add(new Estadisticas(tiempoTotal, memoriaUsada, totalHebras));
                
                // Escribimos el resultado con las estadísticas en el archivo resultado_i.txt
                esc_resultado(file.getName(), matriz_actual, mult_posible, simetrica, dificultad, tiempoTotal, memoriaUsada, totalHebras);
                
            } catch (IOException e) {
                System.err.println("Error en " + file.getName() + ": " + e.getMessage());
            }
        }
        
        // Escribimos el resumen global
        escribirResumenGlobal(dificultad);
    }
    private static List<int[][]> leer_matrices(File f) throws IOException /*Para no escribir otro catch*/{
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
                String[] dim = linea.trim().split("\\s+");
                int n = Integer.parseInt(dim[0]);
                int m = Integer.parseInt(dim[1]);
                // Generamos la matriz del tamaño correspondiente
                int[][] M = new int[n][m];

                // Llenamos la matriz de los valores correspondientes, separando la linea en un arreglo que usará los espacios como separador
                // Con parseInt transformamos de String a Int
                for (int i = 0; i < n; i++) {
                    linea = br.readLine();
                    String[] vals = linea.trim().split("\\s+");
                    for (int j = 0; j < m; j++){ 
                        M[i][j] = Integer.parseInt(vals[j]);
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
            Runnable task = new Mult_filas(A, B, C, i);
            threads[i] = new Thread(task);
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

    private static boolean es_simetrica(int[][] M) {
        // Leemos las dimensiones de la matriz y comprobamos si puede ser simetrica
        int n = M.length;
        
        // Comprobamos si es cuadrada
        if (n == 0 || n != M[0].length) return false;
        
        // Comprobamos todos los valores
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (M[i][j] != M[j][i]) return false;
            }
        }
        // Si nada pasa entonces retornamos true
        return true;
    }

    private static void esc_resultado(String id_archivo, int[][] C, boolean mult_posible, boolean sim, String dificultad, long tiempo, long memoria, int hebras) throws IOException {
        
        String archivo_salida = "SalidaThreads/" + dificultad + "/resultado_" + id_archivo;
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo_salida))) {
            // Escribimos las estadísticas y si es simetrica
            bw.write("# Simétrica: " + sim + "\n");
            bw.write("# Tiempo de procesamiento: " + tiempo + " ms\n");
            bw.write("# Memoria usada: " + memoria + " KB\n");
            bw.write("# Hebras utilizadas: " + hebras + "\n");

            // Escribimos la matriz
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

        // Creamos la direccion del resumen
        File salida_resumen = new File("SalidaThreads/"+ dificultad +".txt");

        try (PrintWriter pw = new PrintWriter(new FileWriter(salida_resumen, false))) {
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
            
            
            // Escribir resumen para la dificultad elegida
            pw.println("Resumen Final " + dificultad + ":");
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