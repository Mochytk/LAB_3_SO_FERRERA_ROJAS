package Codigo.base;

import java.io.*;
import java.util.*;

public class lab3_ferrera_rojas {
    
    public static void main(String[] args) {
        for (int i = 0; i < 500; i++) {
            String basePath = System.getProperty("user.dir") + "/Codigo/base/easy/";
            File archivo = new File(basePath + i + ".txt");

            if (!archivo.exists()) {
                System.out.println("⚠️ Archivo no encontrado: " + archivo.getAbsolutePath());
                continue;
            }

            try (FileReader fr = new FileReader(archivo);
                 BufferedReader br = new BufferedReader(fr)) {

                System.out.println("\n📖 Leyendo: " + archivo.getName());
                
                // Leer cantidad de matrices
                String linea = br.readLine().trim();
                if (linea.isEmpty()) continue;
                int cantM = Integer.parseInt(linea);
                
                List<List<int[]>> todasMatrices = new ArrayList<>();
                
                for (int j = 0; j < cantM; j++) {
                    // Saltar líneas vacías
                    while ((linea = br.readLine()) != null && linea.trim().isEmpty()) {}
                    if (linea == null) break;
                    
                    String[] dimensiones = linea.split("\\s+");
                    int n = Integer.parseInt(dimensiones[0]);
                    int m = Integer.parseInt(dimensiones[1]);
                    
                    List<int[]> matrizActual = new ArrayList<>();
                    
                    // Leer filas de la matriz
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
                    
                    System.out.println("\nMatriz " + (j + 1) + " (" + n + "x" + m + "):");
                    for (int[] fila : matrizActual) {
                        System.out.println(Arrays.toString(fila));
                    }
                }
                
                // Realizar multiplicación secuencial
                if (!todasMatrices.isEmpty()) {
                    List<int[]> resultado = todasMatrices.get(0);
                    
                    for (int j = 1; j < todasMatrices.size(); j++) {
                        try {
                            resultado = multiplicarMatrices(resultado, todasMatrices.get(j));
                        } catch (IllegalArgumentException e) {
                            System.err.println("⛔ Error en multiplicación: " + e.getMessage());
                            break;
                        }
                    }
                    
                    // Imprimir resultado final
                    System.out.println("\n✅ Resultado final de multiplicación:");
                    for (int[] fila : resultado) {
                        System.out.println(Arrays.toString(fila));
                    }
                }
                
            } catch (Exception e) {
                System.err.println("🚨 Error en " + archivo.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public static List<int[]> multiplicarMatrices(List<int[]> matrizA, List<int[]> matrizB) {
        int filasA = matrizA.size();
        int columnasA = matrizA.get(0).length;
        int filasB = matrizB.size();
        int columnasB = matrizB.get(0).length;

        if (columnasA != filasB) {
            throw new IllegalArgumentException(
                "Columnas de A (" + columnasA + ") " +
                "!= Filas de B (" + filasB + ")"
            );
        }

        List<int[]> resultado = new ArrayList<>();
        
        for (int i = 0; i < filasA; i++) {
            int[] filaResultado = new int[columnasB];
            for (int j = 0; j < columnasB; j++) {
                int suma = 0;
                for (int k = 0; k < columnasA; k++) {
                    suma += matrizA.get(i)[k] * matrizB.get(k)[j];
                }
                filaResultado[j] = suma;
            }
            resultado.add(filaResultado);
        }
        
        return resultado;
    }
}