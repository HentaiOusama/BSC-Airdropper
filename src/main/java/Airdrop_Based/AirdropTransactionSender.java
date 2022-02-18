package Airdrop_Based;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainIdLong;
import org.web3j.tx.RawTransactionManager;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.*;

public class AirdropTransactionSender {
    public static void main(String[] args) throws Exception {

        // Make sure to approve the coins.
        // Make sure to change the private key.

        // Change Endpoint to : https://bsc-dataseed.binance.org/
        Web3j web3j = Web3j.build(new HttpService("https://bsc-dataseed.binance.org/"));

        String tokenAddress = "0x64F36701138f0E85cC10c34Ea535FdBADcB54147"; // Change
        String disperseContractAddress = "0xCB0586Bb428e207658da608521A9828B91770c06"; // Change it to "0xCB0586Bb428e207658da608521A9828B91770c06"
        String gasLimit = "70000000"; // Change it to "70000000"


        String gasPrice = "5000000000";
        int limitPerAddy = 70000;
        int maxLimit = Integer.parseInt(gasLimit);
        maxLimit = (maxLimit / 100) * 30;
        int maxTransferLimit = maxLimit / limitPerAddy;
        System.out.println("Max Transfer Count : " + maxTransferLimit);

        File file = new File("Airdrop Amounts.csv");
        FileInputStream fileInputStream = new FileInputStream(file);
        Scanner scanner = new Scanner(fileInputStream);

        System.out.print("Enter start and end position (both inclusive) separated by space : ");
        Scanner scanner1 = new Scanner(System.in);
        String[] params = scanner1.nextLine().trim().split("[ ]+");
        int start, end = 99999;
        if (params.length == 2) {
            start = Integer.parseInt(params[0]);
            end = Integer.parseInt(params[1]);
        } else if (params.length == 1) {
            start = Integer.parseInt(params[0]);
        } else {
            start = 1;
        }
        System.out.println("Limit transfer to 1 (Y/N) : ");
        params = scanner1.nextLine().trim().split("[ ]+");
        boolean limiter = params.length != 1 || !params[0].equalsIgnoreCase("N");
        Credentials credentials = Credentials.create(System.getenv("PrivateKey"));
        System.out.println("Start : " + start + ", End : " + end + ", Limiter Set : " + limiter + ", Wallet : " +
                credentials.getAddress());
        System.out.println("Do you want to continue (Y/N) : ");
        params = scanner1.nextLine().trim().split("[ ]+");
        boolean exitFlag = params.length == 1 && params[0].equalsIgnoreCase("Y");
        if (!exitFlag) {
            System.exit(1);
        }
        scanner1.close();

        if (start < 1) {
            throw new Exception("Invalid Start...");
        }

        int loopPosition = 0;

        long chainId = web3j.ethChainId().send().getChainId().longValue();
        System.out.println("Chain Id : " + chainId);

        RawTransactionManager rawTransactionManager = new RawTransactionManager(
                web3j,
                credentials,
                chainId
        );

        while (scanner.hasNextLine()) {
            List<Address> listAddresses = new ArrayList<>();
            List<Uint256> listAmounts = new ArrayList<>();

            for (int i = 0; i < maxTransferLimit && scanner.hasNextLine(); i++) {
                String[] parameters = scanner.nextLine().trim().split(",");
                if (parameters.length == 2) {
                    listAddresses.add(new Address(parameters[0]));
                    String[] amounts = parameters[1].trim().split("\\.");
                    BigInteger amount = new BigInteger(amounts[0] + ((amounts.length > 1) ? amounts[1] : ""));
                    listAmounts.add(new Uint256(amount));
                } else {
                    System.out.println("Error... => " + Arrays.toString(parameters));
                }
                if (limiter) {
                    break;
                }
            }
            loopPosition++;
            if (loopPosition < start) {
                continue;
            }
            if (loopPosition > end) {
                break;
            }

            List<Type> inputParameters = new ArrayList<>();
            inputParameters.add(new Address(tokenAddress));
            inputParameters.add(new DynamicArray<>(Address.class, listAddresses));
            inputParameters.add(new DynamicArray<>(Uint256.class, listAmounts));

            Function function = new Function(
                    "disperseTokenSimple",
                    inputParameters,
                    Collections.emptyList()
            );

            String hash = rawTransactionManager.sendTransaction(
                    new BigInteger(gasPrice),
                    new BigInteger(gasLimit),
                    disperseContractAddress,
                    FunctionEncoder.encode(function),
                    BigInteger.ZERO
            ).getTransactionHash();

            System.out.println("Hash " + loopPosition + " : " + hash);
            if (limiter) {
                break;
            }
            wait2p5Sec();
        }

        System.out.println("Exiting...");
        scanner.close();
        fileInputStream.close();
    }

    private static void wait2p5Sec() throws InterruptedException {
        Thread.sleep(2500);
    }
}