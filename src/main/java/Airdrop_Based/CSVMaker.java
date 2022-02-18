package Airdrop_Based;

import SupportingClasses.TheGraphQueryMaker;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

/*
* Depending upon use, one might have to change the timestamp in the query string
* timestamp_lt: 1627057182
* */

public class CSVMaker {

    final static MathContext mathContext = new MathContext(20, RoundingMode.HALF_EVEN);
    final static HashMap<String, MovedBNBAmount> walletAndTheirMovedBNBAmounts = new HashMap<>();
    final static TheGraphQueryMaker theGraphQueryMaker = new TheGraphQueryMaker(
            "https://bsc.streamingfast.io/subgraphs/name/pancakeswap/exchange-v2",
            System.out);

    final static ArrayList<String> exclusiveToAddresses = new ArrayList<>();

    final static String pancakeRouter = "0x10ed43c718714eb63d5aa57b78b54704e256024e";
    final static BigDecimal zeroBNB = new BigDecimal("0", mathContext);
    static String lastCheckedId = "0x0";

    final static BigDecimal anonInuInitialPrice = new BigDecimal("0.0000000002", mathContext);
    final static BigDecimal oneBNB = new BigDecimal("1", mathContext).divide(anonInuInitialPrice, mathContext);
    final static BigDecimal oneBUSD = oneBNB.divide(new BigDecimal("300"), mathContext);

    public static void main(String[] args) throws Exception {
        preInitialize();

        Scanner scanner = new Scanner(System.in);
        int choice;
        System.out.println("""
                Menu: -
                0. Exit
                1. Build Original CSV (0x9f)
                2. Build Original CSV (0xd7)
                3. Update CSV for amount0In = 500,00
                4. Update CSV for amount0In = 2,500,000
                5. Get user specific swaps
                6. Build Function Call Parameters""");

        choice = Integer.parseInt(scanner.nextLine());

        switch (choice) {
            case 1 -> makeCSVForPair_0x9f();

            case 2 -> makeCSVForPair_0xd7();

            case 3 -> updateCSV500_000();

            case 4 -> updateCSV2_500_000();

            case 5 -> getUserSpecificSwaps();

            case 6 -> buildFunctionCallParameters();

            default -> { }
        }

        System.out.println("Exiting System...");
        scanner.close();
    }

    private static void updateCSV2_500_000() throws Exception {
        updateCSV(2500000);
    }

    private static void updateCSV500_000() throws Exception {
        updateCSV(500000);
    }

    private static void updateCSV(long amount0In) throws Exception {
        int skip = 0;
        final String queryString = """
                {
                  swaps(orderBy: timestamp, orderDirection: asc, skip: %d, first: 1000,
                    where: {pair: "0x9f460071b38e7663f48c6f460edd9e01e49430d0", sender: "0x10ed43c718714eb63d5aa57b78b54704e256024e",
                      to: "0x10ed43c718714eb63d5aa57b78b54704e256024e", amount0In: %d, amount0Out: 0, amount1In: 0, timestamp_lt: 1627057182}) {
                    from
                    amount1Out
                  }
                }""";

        int roundCount = 1;

        File file = new File("Original BNB Gains and Losses.csv");
        FileInputStream fileInputStream = new FileInputStream(file);
        Scanner scanner = new Scanner(fileInputStream);
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().trim().split(",");
            if (line.length == 0 || line[0] == null || line[0].equalsIgnoreCase("")) {
                break;
            }
            walletAndTheirMovedBNBAmounts.put(line[0], new MovedBNBAmount().addAmount(new BigDecimal(line[1], mathContext)));
        }
        scanner.close();
        fileInputStream.close();

        HashMap<String, MovedBNBAmount> updatedList = new HashMap<>();
        final BigDecimal minusOne = new BigDecimal("-1", mathContext);

        while (true) {
            System.out.println("Round : " + roundCount++);
            theGraphQueryMaker.setGraphQLQuery(String.format(queryString, skip, amount0In));
            skip += 1000;

            JSONObject mainReply = theGraphQueryMaker.sendQuery();
            if (mainReply == null) {
                throw new Exception("Some Garbage Exception.... See how to resolve it....");
            }

            JSONArray current1000Swaps = mainReply.getJSONArray("swaps");
            int swapListLength = current1000Swaps.length();

            for (int i = 0; i < swapListLength; i++) {
                JSONObject temp = current1000Swaps.getJSONObject(i);
                String from = temp.getString("from");

                BigDecimal amountOfBNBMoved = zeroBNB.add(new BigDecimal(temp.getString("amount1Out"))); // False gain assumed correct in makeCSV

                if (updatedList.containsKey(from)) {
                    updatedList.get(from).addAmount(amountOfBNBMoved.multiply(minusOne));
                } else if (walletAndTheirMovedBNBAmounts.containsKey(from)) {
                    updatedList.put(from, new MovedBNBAmount()
                            .addAmount(walletAndTheirMovedBNBAmounts.get(from).movedBNBAmount)
                            .addAmount(amountOfBNBMoved.multiply(minusOne)));
                } else {
                    System.out.println("Found Address that is not already counted.... WTF ???");
                    System.out.println("Address : " + from);
                }
            }

            if (swapListLength < 1000) {
                System.out.println("Querying Complete...");
                break;
            } else {
                pauseSystem();
            }
        }

        System.out.println("Do you want to continue for printing?");
        scanner = new Scanner(System.in);
        if (scanner.nextLine().equalsIgnoreCase("No")) {
            scanner.close();
            System.exit(1);
        }

        file = new File("New Gains and Losses.csv");
        File file1 = new File("New Airdrop Amounts.csv"),
                file2 = new File("Wallets with increased airdrop amount.csv");

        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Unable to create the file");
            }
        }
        if (!file1.exists()) {
            if (!file1.createNewFile()) {
                throw new IOException("Unable to create the file");
            }
        }
        if (!file2.exists()) {
            if (!file2.createNewFile()) {
                throw new IOException("Unable to create the file");
            }
        }

        FileOutputStream fileOutputStream = new FileOutputStream(file),
                fileOutputStream1 = new FileOutputStream(file1),
                fileOutputStream2 = new FileOutputStream(file2);

        PrintStream printStream = new PrintStream(fileOutputStream),
                printStream1 = new PrintStream(fileOutputStream1),
                printStream2 = new PrintStream(fileOutputStream2);

        Set<String> keys = walletAndTheirMovedBNBAmounts.keySet();
        for (String key : keys) {
            BigDecimal BNBAmount;
            boolean flag = false;
            if (!updatedList.containsKey(key)) {
                BNBAmount = walletAndTheirMovedBNBAmounts.get(key).movedBNBAmount;
            } else {
                BNBAmount = updatedList.get(key).movedBNBAmount;
                flag = true;
            }

            printStream.println(key + "," + BNBAmount);
            if (BNBAmount.compareTo(BigDecimal.ZERO) < 0) {
                String x = key + "," + BNBAmount.abs().multiply(oneBNB);
                printStream1.println(x);
                if (flag) {
                    printStream2.println(x);
                }
            }
        }

        printStream.close();
        printStream1.close();
        printStream2.close();
        fileOutputStream.close();
        fileOutputStream1.close();
        fileOutputStream2.close();
    }

    private static void preInitialize() {
        exclusiveToAddresses.add("0x6da0c01b95abe608671949558ab66954c9e6818a"); // Marketing Wallet
        exclusiveToAddresses.add("0x50e6e406fdd01d4206f15e6b30efceceb50e28ff"); // Charity Wallet
        exclusiveToAddresses.add("0x4f01a5fc97b5b1a123f81e792d98073fe1e5906e"); // Joe Wallet
        exclusiveToAddresses.add("0xb7d5b75de26bcadb92f48eaca263cf0194456a28"); // Sonny Wallet

        // ---------------  BELOW CAN NEVER BE 'FROM'. THEY CAN ONLY BE 'TO' OR 'SENDER'  --------------- //
        exclusiveToAddresses.add("0x9425315fea3412fd4a0afbfb69b99d8312dc749a"); // ANON INU - Contract Address
        exclusiveToAddresses.add("0x58f876857a02d6762e0101bb5c46a8c1ed44dc16"); // Binance-Peg BUSD
        exclusiveToAddresses.add("0x16b9a82891338f9ba80e2d6970fdda79d1eb0dae"); // Binance-Peg BSC-USD
        exclusiveToAddresses.add("0x74e4716e431f45807dcf19f284c7aa99f18a4fbc"); // Binance-Peg ETH
        exclusiveToAddresses.add("0x0ed7e52944161450477ee417de9cd3a859b14fd0"); // PancakeSwap Token: Cake
        exclusiveToAddresses.add("0xd99c7f6c65857ac913a8f880a4cb84032ab2fc5b"); // Binance-Peg USDC
        exclusiveToAddresses.add("0xc736ca3d9b1e90af4230bd8f9626528b3d4e0ee0"); // Baby Doge

        exclusiveToAddresses.add("0x54a49dd3a0a66c6d0286fc9e6c26b697091710dc"); // ANON INU - BUSD Pair
        exclusiveToAddresses.add("0x1c9bf61956631248a39b39369e7997dd6a905c46"); // LOWB Coin
        exclusiveToAddresses.add("0xc7c3ccce4fa25700fd5574da7e200ae28bbd36a3"); // Binance-Peg DAI
        exclusiveToAddresses.add("0x20ffdbc2723fac6baf4fbf6bd434286a9288da72"); // ANON - WBNB Pair
        exclusiveToAddresses.add("0x2615960958063993cc01ec10b71b2f9603d4caab"); // SATOZ
        exclusiveToAddresses.add("0xa86b6018ffb7d29f9a3ab59ba27d9e424c1e2812"); // Baby Doge : Router 1
        exclusiveToAddresses.add("0x348e414212e99fab834df43155947a2d6eba70f9"); // UST Pair
        exclusiveToAddresses.add("0x53bfcdb6e8d5993004474fb655a4bbd04aa0187b"); // Cake - ANON Pair
    }

    private static void makeCSVForPair_0x9f() throws Exception {
        final String queryString = """
                    {
                      swaps(orderBy: id, orderDirection: asc, first: 1000,
                        where: { pair: "0x9f460071b38e7663f48c6f460edd9e01e49430d0", id_gt: "%s",
                          to_not: "0x000000000000000000000000000000000000dead", timestamp_lt: 1627057182 }) {
                        id
                        from
                        sender
                        to
                        amount1In
                        amount1Out
                      }
                    }""";

        int roundCount = 1;

        while (true) {
            System.out.println("Round : " + roundCount++);
            theGraphQueryMaker.setGraphQLQuery(String.format(queryString, lastCheckedId));

            JSONObject mainReply = theGraphQueryMaker.sendQuery();
            if (mainReply == null) {
                throw new Exception("Some Garbage Exception.... See how to resolve it....");
            }

            JSONArray current1000Swaps = mainReply.getJSONArray("swaps");
            int swapListLength = current1000Swaps.length();

            for (int i = 0; i < swapListLength; i++) {
                JSONObject temp = current1000Swaps.getJSONObject(i);

                lastCheckedId = temp.getString("id");

                String from = temp.getString("from"),
                        sender = temp.getString("sender"),
                        to = temp.getString("to");

                BigDecimal amountOfBNBMoved = zeroBNB
                        .add(new BigDecimal(temp.getString("amount1Out")))
                        .subtract(new BigDecimal(temp.getString("amount1In")));

                if (exclusiveToAddresses.contains(from)) {
                    return;
                }

                if (to.equalsIgnoreCase(pancakeRouter) && !sender.equalsIgnoreCase(pancakeRouter)) {
                    throw new Exception("To == Router, but Sender != Router.... WTF???");
                }
                updateHashmapForExistingWallet(from, amountOfBNBMoved);
            }

            if (swapListLength < 1000) {
                break;
            } else {
                pauseSystem();
            }
        }

        Set<String> keys = walletAndTheirMovedBNBAmounts.keySet();
        printOriginalBNBGains(keys);
        printAirdropAmounts(keys, oneBNB);
        System.out.println(".......Process Over.......");
    }

    private static void makeCSVForPair_0xd7() throws Exception {
        final String queryString = """
                    {
                      swaps(orderBy: id, orderDirection: asc, first: 1000,
                        where: { pair: "0xd7537590f8bdb62bd135330c8d8a2b3c90f6ed0f", id_gt: "%s",
                          to_not: "0x000000000000000000000000000000000000dead", timestamp_lt: 1627057182 }) {
                        id
                        from
                        sender
                        to
                        amount0In
                        amount0Out
                      }
                    }""";

        int roundCount = 1;

        while (true) {
            System.out.println("Round : " + roundCount++);
            theGraphQueryMaker.setGraphQLQuery(String.format(queryString, lastCheckedId));

            JSONObject mainReply = theGraphQueryMaker.sendQuery();
            if (mainReply == null) {
                throw new Exception("Some Garbage Exception.... See how to resolve it....");
            }

            JSONArray current1000Swaps = mainReply.getJSONArray("swaps");
            int swapListLength = current1000Swaps.length();

            for (int i = 0; i < swapListLength; i++) {
                JSONObject temp = current1000Swaps.getJSONObject(i);

                lastCheckedId = temp.getString("id");

                String from = temp.getString("from"),
                        sender = temp.getString("sender"),
                        to = temp.getString("to");

                BigDecimal amountOfBNBMoved = zeroBNB
                        .add(new BigDecimal(temp.getString("amount0Out")))
                        .subtract(new BigDecimal(temp.getString("amount0In")));

                if (exclusiveToAddresses.contains(from)) {
                    return;
                }

                if (to.equalsIgnoreCase(pancakeRouter) && !sender.equalsIgnoreCase(pancakeRouter)) {
                    throw new Exception("To == Router, but Sender != Router.... WTF???");
                }
                updateHashmapForExistingWallet(from, amountOfBNBMoved);
            }

            if (swapListLength < 1000) {
                break;
            } else {
                pauseSystem();
            }
        }

        Set<String> keys = walletAndTheirMovedBNBAmounts.keySet();
        printOriginalBNBGains(keys);
        printAirdropAmounts(keys, oneBUSD);
        System.out.println(".......Process Over.......");
    }

    private static void updateHashmapForExistingWallet(String wallet, BigDecimal BNBAmount) {
        if (walletAndTheirMovedBNBAmounts.containsKey(wallet)) {
            walletAndTheirMovedBNBAmounts.get(wallet).addAmount(BNBAmount);
        } else {
            walletAndTheirMovedBNBAmounts.put(wallet, new MovedBNBAmount().addAmount(BNBAmount));
        }
    }

    private static void printOriginalBNBGains(Set<String> keys) throws IOException {
        System.out.println("Printing BNB Gains...");
        File file = new File("OriginalBNBGains.csv");
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Unable to create file");
            }
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        PrintStream printStream = new PrintStream(fileOutputStream);

        for (String key : keys) {
            printStream.println(key + "," + walletAndTheirMovedBNBAmounts.get(key).movedBNBAmount);
        }
        printStream.close();
        fileOutputStream.close();
    }

    private static void  printAirdropAmounts(Set<String> keys, BigDecimal priceOfOneCoinInTermsOfAirdropCoin) throws IOException {
        System.out.println("Printing Airdrop Amounts...");
        File file = new File("AirdropAmount.csv");
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Unable to create file");
            }
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        PrintStream printStream = new PrintStream(fileOutputStream);

        for (String key : keys) {
            BigDecimal amount = walletAndTheirMovedBNBAmounts.get(key).movedBNBAmount;
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                amount = amount.abs().multiply(priceOfOneCoinInTermsOfAirdropCoin);
                printStream.println(key + "," + amount);
            }
        }

        printStream.close();
        fileOutputStream.close();
    }

    private static void pauseSystem() {
        try {
            System.out.println("Sleeping 2.5 Seconds");
            Thread.sleep(2500);
            System.out.println("Sleep complete");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class MovedBNBAmount {
        BigDecimal movedBNBAmount = new BigDecimal("0", mathContext);

        public MovedBNBAmount addAmount(BigDecimal movedBNB) {
            movedBNBAmount = movedBNBAmount.add(movedBNB);
            return this;
        }
    }

    private static void buildFunctionCallParameters() throws Exception {
        File file = new File("Airdrop Amounts.csv");
        Scanner scanner = new Scanner(file);

        BigInteger decimals = new BigInteger("1000000000");
        int fileNumber = 0;
        int count = 1;
        boolean flag = true;
        File addy, amounts;
        FileOutputStream addyOutputStream = null, amountsOutputStream = null;
        PrintStream addyPrintStream = null, amountPrintStream = null;
        final int maxCount = 1300;
        while (scanner.hasNextLine()) {
            if (count == 1) {
                flag = true;
                addy = new File("Addresses" + fileNumber + ".txt");
                amounts = new File("Amounts" + fileNumber + ".txt");
                if (!addy.exists()) {
                    if (!addy.createNewFile()) {
                        throw new IOException("Unable to create file");
                    }
                }
                if (!amounts.exists()) {
                    if (!amounts.createNewFile()) {
                        throw new Exception("Unable to create file");
                    }
                }
                addyOutputStream = new FileOutputStream(addy);
                amountsOutputStream = new FileOutputStream(amounts);
                addyPrintStream = new PrintStream(addyOutputStream);
                amountPrintStream = new PrintStream(amountsOutputStream);
                addyPrintStream.print("[");
                amountPrintStream.print("[");
                fileNumber++;
                count = maxCount;
            } else {
                count--;
            }


            String[] data = scanner.nextLine().trim().split(",");
            if (flag) {
                flag = false;
            } else {
                addyPrintStream.print(", ");
                amountPrintStream.print(", ");
            }

            if (data.length == 2 && data[0] != null) {
                if (!data[0].equalsIgnoreCase("")) {
                    addyPrintStream.print("\"" + data[0] + "\"");
                    BigInteger printAmount = new BigInteger(data[1].trim().split("\\.")[0]).multiply(decimals);
                    amountPrintStream.print("\"" + printAmount + "\"");
                }
            }

            if (count == 1) {
                addyPrintStream.print("]");
                amountPrintStream.print("]");
                addyPrintStream.close();
                addyOutputStream.close();
                amountPrintStream.close();
                amountsOutputStream.close();
            }
        }

        scanner.close();
    }

    private static void getUserSpecificSwaps() throws Exception {
        System.out.print("Enter the wallet address of the user : ");
        Scanner scanner = new Scanner(System.in);
        String addy = scanner.nextLine().trim().toLowerCase();
        scanner.close();

        final String queryString = """
                    {
                      swaps(orderBy: id, orderDirection: asc, first: 1000,
                        where: { pair: "0x9f460071b38e7663f48c6f460edd9e01e49430d0", id_gt: "%s",
                          from: "%s", timestamp_lt: 1627057182 }) {
                        id
                        from
                        sender
                        to
                        amount0In
                        amount0Out
                        amount1In
                        amount1Out
                      }
                    }""";

        int roundCount = 1;

        System.out.println("\n----------------------\n");

        while (true) {
            System.out.println("Round : " + roundCount++);
            theGraphQueryMaker.setGraphQLQuery(String.format(queryString, lastCheckedId, addy));

            JSONObject mainReply = theGraphQueryMaker.sendQuery();
            if (mainReply == null) {
                throw new Exception("Some Garbage Exception.... See how to resolve it....");
            }

            JSONArray current1000Swaps = mainReply.getJSONArray("swaps");
            int swapListLength = current1000Swaps.length();

            for (int i = 0; i < swapListLength; i++) {
                JSONObject temp = current1000Swaps.getJSONObject(i);
                lastCheckedId = temp.getString("id");

                System.out.println("AnonSell : " + temp.getString("amount0In") + ", AnonBuy : " + temp.getString("amount0Out") +
                        ", BNBSell : " + temp.getString("amount1In") + ", BNBBuy : " + temp.getString("amount1Out") + ", Hash : " +
                        temp.getString("id").trim().split("-")[0] + "\n");
            }

            if (swapListLength < 1000) {
                break;
            } else {
                pauseSystem();
            }
        }

        System.out.println("\n----------------------\n");
    }
}
