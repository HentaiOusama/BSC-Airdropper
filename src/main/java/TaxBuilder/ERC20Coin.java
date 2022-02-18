package TaxBuilder;

public class ERC20Coin {

    final String contractAddress, name;
    final int decimals;

    ERC20Coin(String contactAddress, String name, int decimals) {
        this.contractAddress = contactAddress;
        this.name = name;
        this.decimals = decimals;
    }
}
