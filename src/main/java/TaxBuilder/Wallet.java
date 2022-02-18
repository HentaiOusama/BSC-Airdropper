package TaxBuilder;

import java.io.IOException;
import java.util.HashMap;

public class Wallet {
    final int chainCount = 3;

    final String walletAddress;
    final boolean[] chains;
    final FileBuilderAndPrinter fileBuilderAndPrinter;
    final HashMap<String, Integer> chainToPrintIndexMapping = new HashMap<>();

    Wallet(String walletName, String walletAddress, boolean[] chains) throws IOException {
        this.walletAddress = walletAddress;
        if (chains.length == chainCount) {
            this.chains = chains;
        } else {
            throw new IOException("Invalid Number of  chains");
        }

        int index = 0;
        for (int i = 0; i < chainCount; i++) {
            if (chains[i]) {
                chainToPrintIndexMapping.put(MainClass.allChainData[i].chainName, index);
                index++;
            }
        }

        fileBuilderAndPrinter = new FileBuilderAndPrinter(walletName, chainToPrintIndexMapping.keySet().toArray(String[]::new));
    }

    public void printData(String chainName, String message) {
        if (chainToPrintIndexMapping.containsKey(chainName)) {
            fileBuilderAndPrinter.writeData(chainToPrintIndexMapping.get(chainName), message);
        }
    }

    public void close() throws IOException {
        fileBuilderAndPrinter.close();
    }
}
