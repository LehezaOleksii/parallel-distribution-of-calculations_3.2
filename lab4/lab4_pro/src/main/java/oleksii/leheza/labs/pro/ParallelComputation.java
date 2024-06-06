package oleksii.leheza.labs.pro;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        readDataFromFiles();

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

        Lock lock = new ReentrantLock();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            Future<Void> future = executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        startLatch.await();
                        calculateA();
                        calculateMA(threadId, lock);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                    return null;
                }
            });
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

    private static void calculateA() {
        synchronized (A) {
            for (int i = 0; i < N; i++) {
                double sum = 0;
                for (int j = 0; j < N; j++) {
                    sum += B[j] * (MM[j][i] + MZ[j][i]) + E[j] * MM[j][i];
                }
                A[i] = sum;
            }
        }
    }

    private static void calculateMA(int threadId, Lock lock) {
        int startRow = (N / THREAD_COUNT) * threadId;
        int endRow = (N / THREAD_COUNT) * (threadId + 1);
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
                    sum += maxDifference * MM[i][k] * MT[k][j] - MZ[i][k] * (MT[k][j] + MM[k][j]);
                }
                lock.lock();
                try {
                    MA[i][j] = sum;
                } finally {
                    lock.unlock();
                }
            }
        }
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

    private static void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println(DELIMITER_LINE);
    }
}
