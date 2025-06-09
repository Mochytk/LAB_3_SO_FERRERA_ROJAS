import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.management.*;

public class lab3_ferrera_rojas {
    
    public static final int cant_cpus = Runtime.getRuntime().availableProcessors();
    private static AtomicLong mult_total = new AtomicLong(0);
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Selección de dificultad
        final String  dificultad;
        int num_archivos = 0;
        while (true) {
            System.out.println("Seleccione la dificultad:");
            System.out.println("1. Easy (500 archivos)");
            System.out.println("2. Medium (300 archivos)");
            System.out.println("3. Hard (200 archivos)");
            System.out.print("Opción: ");
            
            int opcion = scanner.nextInt();
            switch (opcion) {
                case 1:
                    dificultad = "easy";
                    num_archivos = 500;
                    break;
                case 2:
                    dificultad = "medium";
                    num_archivos = 300;
                    break;
                case 3:
                    dificultad = "hard";
                    num_archivos = 200;
                    break;
                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
                    continue;
            }
            break;
        }
        scanner.close();
        
        // Crear carpeta de salida
        File carpeta_salida = new File("SalidaThreads/" + dificultad);
        if (!carpeta_salida.exists()) {
            if (!carpeta_salida.mkdirs()) {
                System.err.println("No se pudo crear la carpeta de salida");
                return;
            }
        }
        
        ExecutorService ejecutor = Executors.newFixedThreadPool(cant_cpus * 2);
        List<Future<Void>> resultados_futuros = new ArrayList<>();
        
        long tiempo_inicial = System.currentTimeMillis();
        for (int i = 0; i < num_archivos; i++) {
            final int arch_id = i;
            
            Future<Void> futuro = ejecutor.submit(() -> {
                procesarYGuardarArchivo(arch_id, carpeta_salida, dificultad);
                return null;
            });
            
            resultados_futuros.add(futuro);
        }
        
        // Esperar a que terminen todas las tareas
        try {
            for (Future<Void> futuro : resultados_futuros) {
                futuro.get();
            }
        } catch (Exception e) {
            System.err.println("Error procesando resultados: " + e.getMessage());
        }
        
        ejecutor.shutdown();
        long tiempo_final = System.currentTimeMillis();
        long tiempo_total = tiempo_final - tiempo_inicial;
        
        // Obtener memoria pico de manera alternativa
        long mem_peak = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Crear resumen global
        File res_glob = new File(carpeta_salida, "resumen_global.txt");
        try (BufferedWriter escribe = new BufferedWriter(new FileWriter(res_glob))) {
            escribe.write("Dificultad: " + dificultad + "\n");
            escribe.write("Archivos procesados: " + num_archivos + "\n");
            escribe.write("tiempo_total: " + tiempo_total + " ms\n");
            escribe.write("MemoriaPico: " + mem_peak + " bytes\n");
            escribe.write("TotalHebrasCreadas: " + (cant_cpus * 2 + cant_cpus * mult_total.get()) + "\n");
        } catch (IOException e) {
            System.err.println("Error escribiendo resumen global: " + e.getMessage());
        }
        
        System.out.println("\nProcesamiento completo! Archivos guardados en: " + carpeta_salida.getAbsolutePath());
        System.out.println("Resumen global guardado en: " + res_glob.getAbsolutePath());
    }
    
    private static void procesarYGuardarArchivo(int arch_id, File carpeta_salida, String dificultad) {
        long tiempo_i = System.currentTimeMillis();
        long mem_i = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        try {
            // Procesar el archivo
            ProcesarArchivoResult result = procesarArchivo(arch_id, dificultad);
            List<int[]> resultado = result.matriz;
            int num_mult = result.num_mult;
            
            if (resultado.isEmpty()) {
                System.out.println("Archivo " + arch_id + " no produjo resultado");
                return;
            }
            
            // Calcular métricas
            long endTime = System.currentTimeMillis();
            long tiempoProcesamiento = endTime - tiempo_i;
            long memoriaFinal = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoriaUtilizada = memoriaFinal - mem_i;
            int hebrasCreadas = num_mult * cant_cpus;
            
            // Crear archivo de salida
            File salidaFile = new File(carpeta_salida, "salidaThread" + arch_id + ".txt");
            
            // Escribir el resultado en el archivo
            try (BufferedWriter escribe = new BufferedWriter(new FileWriter(salidaFile))) {
                // Escribir métricas
                escribe.write("# Tiempo: " + tiempoProcesamiento + " ms\n");
                escribe.write("# Memoria Utilizada: " + memoriaUtilizada + " bytes\n");
                escribe.write("# Hebras Creadas: " + hebrasCreadas + "\n");
                
                // Escribir dimensiones
                int filas = resultado.size();
                int columnas = resultado.get(0).length;
                escribe.write(filas + " " + columnas + "\n");
                
                // Escribir matriz
                for (int[] fila : resultado) {
                    for (int j = 0; j < fila.length; j++) {
                        escribe.write(String.valueOf(fila[j]));
                        if (j < fila.length - 1) {
                            escribe.write(" ");
                        }
                    }
                    escribe.newLine();
                }
            }
            
            System.out.println("Guardado: " + salidaFile.getName());
            
        } catch (Exception e) {
            System.err.println("Error procesando archivo " + arch_id + ": " + e.getMessage());
        }
    }
    
    private static class ProcesarArchivoResult {
        List<int[]> matriz;
        int num_mult;
    }
    
    private static ProcesarArchivoResult procesarArchivo(int arch_id, String dificultad) throws Exception {
        ProcesarArchivoResult result = new ProcesarArchivoResult();
        result.matriz = new ArrayList<>();
        result.num_mult = 0;
        
        String basePath = System.getProperty("user.dir") + "/" + dificultad + "/";
        File archivo = new File(basePath + arch_id + ".txt");

        try (FileReader fr = new FileReader(archivo);
             BufferedReader br = new BufferedReader(fr)) {

            System.out.println("Leyendo: " + archivo.getName());
            
            String linea = br.readLine().trim();
            if (linea.isEmpty()) return result;
            int cantM = Integer.parseInt(linea);
            
            List<List<int[]>> todas_las_matrices = new ArrayList<>();
            
            for (int j = 0; j < cantM; j++) {
                while ((linea = br.readLine()) != null && linea.trim().isEmpty()) {}
                if (linea == null) break;
                
                String[] dimensiones = linea.split("\\s+");
                int n = Integer.parseInt(dimensiones[0]);
                int m = Integer.parseInt(dimensiones[1]);
                
                List<int[]> matrizActual = new ArrayList<>();
                
                for (int fila = 0; fila < n; fila++) {
                    while ((linea = br.readLine()) != null && linea.trim().isEmpty()) {}
                    if (linea == null) break;
                    
                    String[] valores = linea.split("\\s+");
                    int[] arrFila = new int[m];
                    
                    for (int col = 0; col < m && col < valores.length; col++) {
                        arrFila[col] = Integer.parseInt(valores[col]);
                    }
                    
                    matrizActual.add(arrFila);
                }
                
                todas_las_matrices.add(matrizActual);
            }
            
            List<int[]> resultado = todas_las_matrices.get(0);
            int num_mult = cantM - 1;
            result.num_mult = num_mult;
            
            if (num_mult > 0) {
                mult_total.addAndGet(num_mult);
            }
            
            for (int j = 1; j < todas_las_matrices.size(); j++) {
                resultado = multiplicarMatricesConcurrente(resultado, todas_las_matrices.get(j));
            }
            
            result.matriz = resultado;
            return result;
            
        } catch (Exception e) {
            throw new Exception("Error procesando archivo " + arch_id + ": " + e.getMessage());
        }
    }
    
    public static List<int[]> multiplicarMatricesConcurrente(List<int[]> A, List<int[]> B) {
        int fil_A = A.size();
        int col_A = A.get(0).length;
        int col_B = B.get(0).length;

        int[][] resultado = new int[fil_A][col_B];
        ExecutorService ejecutor = Executors.newFixedThreadPool(cant_cpus);
        
        for (int i = 0; i < fil_A; i++) {
            final int fila = i;
            ejecutor.submit(() -> {
                for (int j = 0; j < col_B; j++) {
                    int suma = 0;
                    for (int k = 0; k < col_A; k++) {
                        suma += A.get(fila)[k] * B.get(k)[j];
                    }
                    resultado[fila][j] = suma;
                }
            });
        }
        
        ejecutor.shutdown();
        try {
            ejecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        List<int[]> resultadoList = new ArrayList<>();
        for (int[] fila : resultado) {
            resultadoList.add(fila);
        }
        
        return resultadoList;
    }
}