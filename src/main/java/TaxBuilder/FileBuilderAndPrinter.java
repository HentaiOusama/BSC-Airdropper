package TaxBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class FileBuilderAndPrinter {

    final int chainCounts;

    final String walletName;

    final ArrayList<FileOutputStream> fileOutputStreams = new ArrayList<>();
    final ArrayList<PrintStream> printStreams = new ArrayList<>();

    FileBuilderAndPrinter(String walletName, String[] chainNames) throws IOException {
        this.chainCounts = chainNames.length;
        this.walletName = walletName;

        File[] files = new File[chainCounts];

        for (int i = 0; i < chainCounts; i++) {
            files[i] = new File(walletName + "-" + chainNames[i] + "-Transactions.csv");
            if (!files[i].exists()) {
                if (!files[i].createNewFile()) {
                    throw new IOException("Unable to create file...");
                }
            }
        }

        for (int i = 0; i < chainCounts; i++) {
            fileOutputStreams.add(new FileOutputStream(files[i]));
            printStreams.add(new PrintStream(fileOutputStreams.get(i)));
        }
    }

    public void writeData(int index, String message) {
        printStreams.get(index).println(message);
    }

    public void close() throws IOException {

        for (int i = 0; i < chainCounts; i++) {
            printStreams.get(i).close();
            fileOutputStreams.get(i).close();
        }

    }
}
