package oleksii.leheza.labs.pro;
import java.io.*;
import java.util.Arrays;
import java.util.Random;


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

    private static  final String DELIMITER_LINE = "-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";

    public static void main(String[] args) {
        // Генерація вхідних даних
        if (areFilesNotEmpty()) {
            // Якщо файли не пусті то зчитати дані з файлу
            readDataFromFiles();
        } else {
            // інакше згенерувати дані та зберегти їх
            generateInputData();
            saveDataToFiles();
        }
        // Виведення згенерованих матриць
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

        // Створення потоків для обчислення функцій A та MA
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(new MyRunnable(i));
            threads[i].start();
        }

        // Очікування завершення всіх потоків
        for (int i = 0; i < THREAD_COUNT; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Виведення результатів
        System.out.println("Vector A: " + Arrays.toString(A));
        System.out.println("Matrix MA: ");
        for (double[] row : MA) {
            System.out.println(Arrays.toString(row));
        }
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println(DELIMITER_LINE);
        System.out.println("Execution time: " + executionTime + " milliseconds");
        System.out.println(DELIMITER_LINE);
    }

    private static void generateInputData() {
        Random random = new Random();

        // Генерація даних для векторів B та E
        for (int i = 0; i < N; i++) {
            B[i] = random.nextDouble();
            E[i] = random.nextDouble();
        }

        // Генерація даних для матриць MM, MT, MZ
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                MM[i][j] = random.nextDouble();
                MT[i][j] = random.nextDouble();
                MZ[i][j] = random.nextDouble();
            }
        }
    }
    private static class MyRunnable implements Runnable {
        private int threadId;
        public MyRunnable(int threadId) {
            this.threadId = threadId;
        }

        @Override
        public void run() {
                calculateA();
                calculateMA(threadId);
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
            double maxDifference = Double.NEGATIVE_INFINITY; // Ініціалізуємо максимальну різницю найменшим можливим значенням

            for (int i = 0; i < B.length; i++) {
                double difference = B[i] - E[i]; // Знаходимо різницю між B[i] та E[i]
                if (difference > maxDifference) {
                    maxDifference = difference; // Оновлюємо максимальну різницю, якщо поточна різниця більша
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
