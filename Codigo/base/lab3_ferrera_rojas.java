package Codigo.base;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class lab3_ferrera_rojas {
    
    public static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static AtomicLong totalMultiplications = new AtomicLong(0);
    
    public static void main(String[] args) {
        
        // Crear carpeta de salida
        File salidaDir = new File("SalidaThreads");
        if (!salidaDir.exists()) {
            if (!salidaDir.mkdirs()) {
                System.err.println("No se pudo crear la carpeta de salida");
                return;
            }
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CORES * 2);
        List<Future<Void>> resultadosFuturos = new ArrayList<>();
        
        long tiempoInicialGlobal = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            final int archivoId = i;
            Future<Void> future = executor.submit(() -> {
                procesarYGuardarArchivo(archivoId, salidaDir);
                return null;
            });
            resultadosFuturos.add(future);
        }
        
        // Esperar a que terminen todas las hebras
        try {
            for (Future<Void> future : resultadosFuturos) {
                future.get();
            }
        } catch (Exception e) {
            System.err.println("Error procesando resultados: " + e.getMessage());
        }
        
        executor.shutdown();
        long tiempoFinalGlobal = System.currentTimeMillis();
        long tiempo = tiempoFinalGlobal - tiempoInicialGlobal;
        
        // Obtener memoria peak
        long peakMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    
        // Crear resumen global
        File resumen = new File(salidaDir, "resumen_global.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resumen))) {
            writer.write("TiempoTotal: " + tiempo + " ms");
            writer.newLine();
            writer.write("MemoriaPico: " + peakMemory + " bytes");
            writer.newLine();
            writer.write("TotalHebrasCreadas: " + (NUM_CORES * 2 + NUM_CORES * totalMultiplications.get()));
        } 
        catch (IOException e) {
            System.err.println("Error escribiendo resumen global: " + e.getMessage());
        }
        
        System.out.println("\nProcesamiento completo! Archivos guardados en: " + salidaDir.getAbsolutePath());
        System.out.println("Resumen global guardado en: " + resumen.getAbsolutePath());
    }
    
    private static void procesarYGuardarArchivo(int archivoId, File salidaDir) {
        long tiempoInicial = System.currentTimeMillis();
        long memoriaInicial = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        try {
            // Procesar el archivo
            ProcesarArchivoResult result = procesarArchivo(archivoId);
            List<int[]> resultado = result.matriz;
            int numMultiplicaciones = result.numMultiplicaciones;
            
            if (resultado.isEmpty()) {
                System.out.println("Archivo " + archivoId + " no produjo resultado");
                return;
            }
            
            // Calcular métricas
            long tiempoFinal = System.currentTimeMillis();
            long tiempoProcesamiento = tiempoFinal - tiempoInicial;
            long memoriaFinal = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoriaUtilizada = memoriaFinal - memoriaInicial;
            int hebrasCreadas = numMultiplicaciones * NUM_CORES;
            
            // Crear archivo de salida
            File salidaFile = new File(salidaDir, "salidaThread" + archivoId + ".txt");
            
            // Escribir el resultado en el archivo
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(salidaFile))) {
                // Escribir métricas
                writer.write("# Tiempo: " + tiempoProcesamiento + " ms");
                writer.newLine();
                writer.write("# Memoria Utilizada: " + memoriaUtilizada + " bytes");
                writer.newLine();
                writer.write("# Hebras Creadas: " + hebrasCreadas);
                writer.newLine();
                
                // Escribir dimensiones
                int filas = resultado.size();
                int columnas = resultado.get(0).length;
                writer.write(filas + " " + columnas);
                writer.newLine();
                
                // Escribir matriz
                for (int[] fila : resultado) {
                    for (int j = 0; j < fila.length; j++) {
                        writer.write(String.valueOf(fila[j]));
                        if (j < fila.length - 1) {
                            writer.write(" ");
                        }
                    }
                    writer.newLine();
                }
            }
            
            System.out.println("Guardado: " + salidaFile.getName());
            
        } catch (Exception e) {
            System.err.println("Error procesando archivo " + archivoId + ": " + e.getMessage());
        }
    }
    
    private static class ProcesarArchivoResult {
        List<int[]> matriz;
        int numMultiplicaciones;
    }
    
    private static ProcesarArchivoResult procesarArchivo(int archivoId) throws Exception {
        ProcesarArchivoResult result = new ProcesarArchivoResult();
        result.matriz = new ArrayList<>();
        result.numMultiplicaciones = 0;
        
        String basePath = System.getProperty("user.dir") + "/Codigo/base/easy/";
        File archivo = new File(basePath + archivoId + ".txt");

        if (!archivo.exists()) {
            System.out.println("Archivo no encontrado: " + archivo.getAbsolutePath());
            return result;
        }

        try (FileReader fr = new FileReader(archivo);
             BufferedReader br = new BufferedReader(fr)) {

            System.out.println("Leyendo: " + archivo.getName());
            
            String linea = br.readLine().trim();
            if (linea.isEmpty()) return result;
            int cantM = Integer.parseInt(linea);
            
            List<List<int[]>> todasMatrices = new ArrayList<>();
            
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
                
                todasMatrices.add(matrizActual);
            }
            
            if (todasMatrices.isEmpty()) {
                return result;
            }
            
            List<int[]> resultado = todasMatrices.get(0);
            int numMultiplicaciones = cantM - 1;
            result.numMultiplicaciones = numMultiplicaciones;
            
            if (numMultiplicaciones > 0) {
                totalMultiplications.addAndGet(numMultiplicaciones);
            }
            
            for (int j = 1; j < todasMatrices.size(); j++) {
                resultado = multiplicarMatricesConcurrente(resultado, todasMatrices.get(j));
            }
            
            result.matriz = resultado;
            return result;
            
        } catch (Exception e) {
            throw new Exception("Error procesando archivo " + archivoId + ": " + e.getMessage());
        }
    }
    
    public static List<int[]> multiplicarMatricesConcurrente(List<int[]> matrizA, List<int[]> matrizB) {
        int filasA = matrizA.size();
        int columnasA = matrizA.get(0).length;
        int columnasB = matrizB.get(0).length;

        int[][] resultado = new int[filasA][columnasB];
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CORES);
        
        for (int i = 0; i < filasA; i++) {
            final int fila = i;
            executor.submit(() -> {
                for (int j = 0; j < columnasB; j++) {
                    int suma = 0;
                    for (int k = 0; k < columnasA; k++) {
                        suma += matrizA.get(fila)[k] * matrizB.get(k)[j];
                    }
                    resultado[fila][j] = suma;
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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