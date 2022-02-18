package TaxBuilder;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainClass {

    // MongoDB
    static MongoClient mongoClient;
    static MongoCollection<Document> allAddressesDataCollection;
    static Document identifierDoc = new Document("identifier", "general");

    // Data from Mongo
    static final ArrayList<String> ERC20Addresses = new ArrayList<>();
    static final HashMap<String, ERC20Coin> knownERC20Coins = new HashMap<>();

    // Data Fetcher
    static OkHttpClient okHttpClient = new OkHttpClient();

    // Wallets...
    static final ChainData[] allChainData = new ChainData[3];
    static final HashMap<String, Wallet> allOwnedWallets = new HashMap<>(); // Wallet Address  => Wallet

    public static void main(String[] args) throws Exception {
        preInitialize();

        Set<String> allWalletAddresses = allOwnedWallets.keySet();
        for (String walletAddress : allWalletAddresses) {
            Wallet wallet = allOwnedWallets.get(walletAddress);

            for (int i = 0; i < allChainData.length; i++) {
                if (wallet.chains[i]) {
                    JSONArray allTransactions = makeCallToAPI(i, walletAddress, 2);


                } else {
                    continue;
                }

                // TODO : Remove this break statement....
                break;
            }
        }

        postPrinting();
    }



    @SuppressWarnings("SpellCheckingInspection")
    private static void initializeMongoSetup() {
        ConnectionString connectionString = new ConnectionString(
                "mongodb+srv://" + System.getenv("mongoID") + ":" +
                        System.getenv("mongoPass") + "@hellgatesbotcluster.zm0r5.mongodb.net/test" +
                        "?keepAlive=true&poolSize=30&autoReconnect=true&socketTimeoutMS=360000&connectTimeoutMS=360000"
        );
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString).retryWrites(true).writeConcern(WriteConcern.MAJORITY).build();
        mongoClient = MongoClients.create(mongoClientSettings);
        // MongoDB Related Stuff
        mongoClient.startSession();
        allAddressesDataCollection = mongoClient.getDatabase("Crypto-Transfer-Generator-Database")
                .getCollection("Addresses-Data-Collection");


        // General variables initialization
        Document foundDoc = allAddressesDataCollection.find(identifierDoc).first();
        assert foundDoc != null;

        List<?> list = (List<?>) foundDoc.get("ERC20");
        for (Object item : list) {
            if (item instanceof String) {
                String address = (String) item;

                ERC20Addresses.add(address);
                List<?> coinData = (List<?>) foundDoc.get(address);
                knownERC20Coins.put(address, new ERC20Coin(address, (String) coinData.get(0), Integer.parseInt((String) coinData.get(1))));
            }
        }
    }

//    private static void addNewCoinToKnownList(String address) throws Exception {
//        address = Keys.toChecksumAddress(address);
//        if (ERC20Addresses.contains(address)) {
//            return;
//        }
//
//        ERC20 erc20 = ERC20.load(address, web3j, Credentials.create(System.getenv("PrivateKey")), null);
//        String name = erc20.name().send();
//        String decimals = erc20.decimals().send().toString();
//
//        if (name != null && decimals != null && !name.equalsIgnoreCase("") && !decimals.equalsIgnoreCase("")) {
//            Document foundIdentifierDoc = allAddressesDataCollection.find(identifierDoc).first();
//            assert foundIdentifierDoc != null;
//
//            ArrayList<String> inputs = new ArrayList<>();
//            inputs.add(name);
//            inputs.add(decimals);
//
//            ERC20Addresses.add(address);
//            knownERC20Coins.put(address, new ERC20Coin(address, inputs.get(0), Integer.parseInt(inputs.get(1))));
//
//            try {
//                Document updatedDoc = new Document("ERC20", ERC20Addresses)
//                        .append(address, inputs);
//                Bson updateOperation = new Document("$set", updatedDoc);
//                allAddressesDataCollection.updateOne(foundIdentifierDoc, updateOperation);
//            } catch (Exception e) {
//                e.printStackTrace();
//                ERC20Addresses.remove(address);
//                knownERC20Coins.remove(address);
//            }
//
//        } else {
//            throw new Exception("Unable to generate data for the ERC20 coin : " + address);
//        }
//    }

    @SuppressWarnings("SpellCheckingInspection")
    private static void preInitialize() throws IOException {
        initializeMongoSetup();

        // TODO : Update all of these...
        allChainData[0] = new ChainData("ETH", "https://api.etherscan.io", "h5dvqex79hetycy4kxx4bmwyypmgf5tege");
        allChainData[1] = new ChainData("BSC", "", "");
        allChainData[2] = new ChainData("MATIC", "", "");

        buildWallet("Metamask Self-Use", "0xf7C1f4cA54D64542061E6f53A9D38E2f5A6A4Ecc", true, true, false);
        buildWallet("Metamask Hell-Gates", "0x57AbCF8F01D08489236a490661aDB85c3aBB47Bc",true, true, false);
        buildWallet("Metamask Garbage", "0xDE92A18631BdA9E627cc546f69231593Ea5A11F0",true, false, false);
        buildWallet("Trust Wallet 1", "0x32F65952DE6Af390227f9e6d445F3cBD5da0e9D0",true, false, false);
        buildWallet("Enjin Safety Legacy", "0x3bbF84273aa2CBe378356561AA1B8bFD3D77651F", true, false, false);
        buildWallet("Enjin Safety Non-Legacy", "0x1BfAA3c12c15aC523ef8A4EB092C36b037bb0224", true, false, false);
        buildWallet("Enjin Main", "0x8c7B31eF7f282330Fa705677c185d495356F8026", false, false, false); // TODO: Change
    }

    private static void buildWallet(String walletName, String walletAddress, boolean eth, boolean bsc, boolean matic) throws IOException {
        walletAddress = walletAddress.toLowerCase();
        allOwnedWallets.put(walletAddress, new Wallet(walletName, walletAddress, new boolean[] {eth, bsc, matic}));
    }

    private static JSONArray makeCallToAPI(int chainIndex, String walletAddress, int mode) throws IOException {
        if (mode != 1 && mode != 2) {
            throw new IOException("Invalid mode number");
        }
        Request request = new Request.Builder()
                .url(String.format((mode == 1) ? allChainData[chainIndex].normalTransactionUrl : allChainData[chainIndex].ERC20TransactionUrl,
                        allChainData[chainIndex].host, walletAddress.toLowerCase(), allChainData[chainIndex].apiKey))
                .build();
        Response response = okHttpClient.newCall(request).execute();

        if (response.code() == 200) {
            assert response.body() != null;
            return new JSONObject(response.body().string()).getJSONArray("result");
        } else {
            throw new IOException("API ERROR....");
        }
    }

    private static void postPrinting() throws IOException {
        Set<String> keys = allOwnedWallets.keySet();
        for (String key : keys) {
            allOwnedWallets.get(key).close();
        }
    }
}
