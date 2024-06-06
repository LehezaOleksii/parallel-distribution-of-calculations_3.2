package oleksii.leheza.labs.pro;

import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;

public class ParallelComputation {
    private static final int N = 100;
    private static final int THREAD_COUNT = 4;

    private static double[] A = new double[N];
    private static double[] B = new double[N];
    private static double[] E = new double[N];
    private static double[][] MA = new double[N][N];
    private static double[][] MM = new double[N][N];
    private static double[][] MT = new double[N][N];
    private static double[][] MZ = new double[N][N];
    private static final String FILE_B = "B.txt";
    private static final String FILE_E = "E.txt";
    private static final String FILE_MM = "MM.txt";
    private static final String FILE_MT = "MT.txt";
    private static final String FILE_MZ = "MZ.txt";

    private static final String DELIMITER_LINE = "-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";

    public static void main(String[] args) {
        if (areFilesNotEmpty()) {
            readDataFromFiles();
        } else {
            generateInputData();
            saveDataToFiles();
        }

        System.out.println("Main data:");
        System.out.println("Generated matrices:");
        System.out.println("Vector B: " + Arrays.toString(B));
        System.out.println(DELIMITER_LINE);
        System.out.println("Vector E: " + Arrays.toString(E));
        System.out.println(DELIMITER_LINE);
        System.out.println("Matrix MM: ");
        printMatrix(MM);
        System.out.println("Matrix MT: ");
        printMatrix(MT);
        System.out.println("Matrix MZ: ");
        printMatrix(MZ);

        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(new MyCallable(i, startLatch, endLatch));
        }

        startLatch.countDown();

        try {
            endLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        System.out.println("Vector A: " + Arrays.toString(A));
        System.out.println("Matrix MA: ");
        printMatrix(MA);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println(DELIMITER_LINE);
        System.out.println("Execution time: " + executionTime + " milliseconds");
        System.out.println(DELIMITER_LINE);
    }

    private static void generateInputData() {
        Random random = new Random();

        for (int i = 0; i < N; i++) {
            B[i] = random.nextDouble();
            E[i] = random.nextDouble();
        }

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                MM[i][j] = random.nextDouble();
                MT[i][j] = random.nextDouble();
                MZ[i][j] = random.nextDouble();
            }
        }
    }

    private static class MyCallable implements Callable<Void> {
        private int threadId;
        private CountDownLatch startLatch;
        private CountDownLatch endLatch;

        public MyCallable(int threadId, CountDownLatch startLatch, CountDownLatch endLatch) {
            this.threadId = threadId;
            this.startLatch = startLatch;
            this.endLatch = endLatch;
        }

        @Override
        public Void call() {
            try {
                startLatch.await();
                calculateA();
                calculateMA(threadId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
            return null;
        }

        private void calculateA() {
            synchronized (A) {
                for (int i = 0; i < N; i++) {
                    double sum = 0;
                    for (int j = 0; j < N; j++) {
                        sum += B[j] * (MM[j][i] + MZ[i][j]) + E[i] * MM[j][i];
                    }
                    A[i] = sum;
                }
            }
        }

        private void calculateMA(int threadId) {
            int startRow = (N / THREAD_COUNT) * threadId;
            int endRow = (N / THREAD_COUNT) * (threadId + 1);
            Arrays.sort(B);
            Arrays.sort(E);
            double maxDifference = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < B.length; i++) {
                double difference = B[i] - E[i];
                if (difference > maxDifference) {
                    maxDifference = difference;
                }
            }

            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < N; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < N; k++) {
                        sum += maxDifference * MM[k][j] * MT[i][k] - MZ[i][k] * (MT[i][k] + MM[k][j]);
                    }
                    synchronized (MA) {
                        MA[i][j] = sum;
                    }
                }
            }
        }
    }

    private static boolean areFilesNotEmpty() {
        return new File(FILE_B).length() > 0 &&
                new File(FILE_E).length() > 0 &&
                new File(FILE_MM).length() > 0 &&
                new File(FILE_MT).length() > 0 &&
                new File(FILE_MZ).length() > 0;
    }

    private static void readDataFromFiles() {
        try {
            B = readArrayFromFile(FILE_B);
            E = readArrayFromFile(FILE_E);
            MM = readMatrixFromFile(FILE_MM);
            MT = readMatrixFromFile(FILE_MT);
            MZ = readMatrixFromFile(FILE_MZ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double[] readArrayFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            return Arrays.stream(reader.readLine().split("\\s+"))
                    .mapToDouble(Double::parseDouble)
                    .toArray();
        }
    }

    private static double[][] readMatrixFromFile(String filename) throws IOException {
        double[][] matrix = new double[N][N];
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            for (int i = 0; i < N; i++) {
                matrix[i] = Arrays.stream(reader.readLine().split("\\s+"))
                        .mapToDouble(Double::parseDouble)
                        .toArray();
            }
        }
        return matrix;
    }

    private static void saveDataToFiles() {
        try {
            writeArrayToFile(FILE_B, B);
            writeArrayToFile(FILE_E, E);
            writeMatrixToFile(FILE_MM, MM);
            writeMatrixToFile(FILE_MT, MT);
            writeMatrixToFile(FILE_MZ, MZ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeArrayToFile(String filename, double[] array) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(Arrays.toString(array).replace("[", "").replace("]", "").replace(",", ""));
        }
    }

    private static void writeMatrixToFile(String filename, double[][] matrix) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < N; i++) {
                writer.write(Arrays.toString(matrix[i]).replace("[", "").replace("]", "").replace(",", ""));
                writer.newLine();
            }
        }
    }

    private static void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println(DELIMITER_LINE);
    }
}
