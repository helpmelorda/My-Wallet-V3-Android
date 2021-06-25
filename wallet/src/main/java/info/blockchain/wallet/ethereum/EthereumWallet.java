package info.blockchain.wallet.ethereum;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import info.blockchain.balance.AssetCatalogue;
import info.blockchain.balance.AssetInfo;
import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.keys.MasterKey;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class EthereumWallet {

    public static final int METADATA_TYPE_EXTERNAL = 5;
    private static final int ACCOUNT_INDEX = 0;

    @JsonProperty("ethereum")
    private EthereumWalletData walletData;

    public EthereumWallet() {
        //default constructor for Jackson
    }

    /**
     * Creates new Ethereum wallet and derives account from provided wallet seed.
     *
     * @param walletMasterKey DeterministicKey of root node
     * @param label the default label for non custodial assets
     */
    public EthereumWallet(
        MasterKey walletMasterKey,
        String label
    ) {
        ArrayList<EthereumAccount> accounts = new ArrayList<>();
        accounts.add(
            EthereumAccount.Companion.deriveAccount(
                walletMasterKey.toDeterministicKey(),
                ACCOUNT_INDEX,
                label
            )
        );

        this.walletData = new EthereumWalletData();
        this.walletData.setHasSeen(false);
        this.walletData.setDefaultAccountIdx(0);
        this.walletData.setTxNotes(new HashMap<>());
        this.walletData.setAccounts(accounts);
    }

    /**
     * Loads existing Ethereum wallet from derived Ethereum metadata node.
     *
     * @return Existing Ethereum wallet or Null if no existing Ethereum wallet found.
     */
    public static EthereumWallet load(String walletJson) throws IOException {

        if (walletJson != null) {
            EthereumWallet ethereumWallet = fromJson(walletJson);

            // Web can store an empty EthereumWalletData object
            if (ethereumWallet.walletData == null || ethereumWallet.walletData.getAccounts().isEmpty()) {
                return null;
            } else {
                return ethereumWallet;
            }
        } else {
            return null;
        }
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public static EthereumWallet fromJson(String json) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        return mapper.readValue(json, EthereumWallet.class);
    }

    public boolean hasSeen() {
        return walletData.getHasSeen();
    }

    /**
     * Set flag to indicate that user has acknowledged their ether wallet.
     */
    public void setHasSeen(boolean hasSeen) {
        walletData.setHasSeen(hasSeen);
    }

    /**
     * @return Single Ethereum account
     */
    public EthereumAccount getAccount() {

        if (walletData.getAccounts().isEmpty()) {
            return null;
        }

        return walletData.getAccounts().get(ACCOUNT_INDEX);
    }

    public void renameAccount(String newLabel){
        EthereumAccount account = getAccount();
        account.setLabel(newLabel);
        ArrayList<EthereumAccount> accounts = new ArrayList<>();
        accounts.add(account);
        walletData.setAccounts(accounts);
    }

    public HashMap<String, String> getTxNotes() {
        return walletData.getTxNotes();
    }

    public void putTxNotes(String txHash, String txNote) {
        HashMap<String, String> notes = walletData.getTxNotes();
        notes.put(txHash, txNote);
    }

    public void removeTxNotes(String txHash) {
        HashMap<String, String> notes = walletData.getTxNotes();
        notes.remove(txHash);
    }

    @Deprecated // Eth payload last tx features are no longer used
    public void setLastTransactionHash(String txHash) {
        walletData.setLastTx(txHash);
    }

    @Deprecated // Eth payload last tx features are no longer used
    public void setLastTransactionTimestamp(long timestamp) {
        walletData.setLastTxTimestamp(timestamp);
    }

    public Erc20TokenData getErc20TokenData(String tokenName) {
        return walletData.getErc20Tokens().get(tokenName.toLowerCase());
    }

    public boolean updateErc20Tokens(
        AssetCatalogue assetCatalogue,
        String label
    ) {
        boolean wasUpdated = false;
        if (walletData.getErc20Tokens() == null) {
            walletData.setErc20Tokens(new HashMap<>());
            wasUpdated = true;
        }

        HashMap<String, Erc20TokenData> map = walletData.getErc20Tokens();
        List<AssetInfo> erc20Tokens = assetCatalogue.supportedL2Assets(CryptoCurrency.ETHER.INSTANCE);
        for(AssetInfo token: erc20Tokens) {
            String name = token.getTicker().toLowerCase();

            if (!map.containsKey(name) || !map.get(name).hasLabelAndAddressStored()) {
                map.put(
                    name,
                    Erc20TokenData.Companion.createTokenData(token, label)
                );
                wasUpdated = true;
            }
        }
        return wasUpdated;
    }
}
