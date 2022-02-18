package TaxBuilder;

public class ChainData {
    final String chainName;
    final String apiKey;
    final String host;

    final String normalTransactionUrl = "%s/api?" +
            "module=account" +
            "&action=txlist" +
            "&address=%s" +
            "&startblock=0" +
            "&endblock=99999999" +
            "&sort=asc" +
            "&apikey=%s";

    final String ERC20TransactionUrl = "%s/api?" +
            "module=account" +
            "&action=tokentx" +
            "&address=%s" +
            "&startblock=0" +
            "&endblock=999999999" +
            "&sort=asc" +
            "&apikey=%s";

    ChainData(String chainName, String host, String apiKey) {
        this.chainName = chainName;
        this.host = host;
        this.apiKey = apiKey;
    }
}
