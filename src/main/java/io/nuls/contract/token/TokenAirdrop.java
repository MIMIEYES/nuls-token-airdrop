package io.nuls.contract.token;

import io.nuls.contract.ownership.Ownable;
import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Contract;
import io.nuls.contract.sdk.Msg;

import java.math.BigInteger;

import static io.nuls.contract.sdk.Utils.require;

/**
 * Token Airdrop
 */
public class TokenAirdrop extends Ownable implements Contract {

    public TokenAirdrop() {

    }

    public void airdrop(Address nrc20, String[] tos, String[] values) {
        onlyOwner();
        require(!Msg.address().equals(nrc20), "Do nothing by yourself");
        require(nrc20.isContract(), "[" + nrc20.toString() + "] is not a contract address");
        String to;
        String value;
        String[][] args;
        String methodName;
        for (int i = 0, len = tos.length; i < len; i++) {
            to = tos[i];
            value = values[i];
            methodName = "transfer";
            args = new String[][]{
                    new String[]{to},
                    new String[]{value}};
            nrc20.call(methodName, "(Address to, BigInteger value) return boolean", args, BigInteger.ZERO);
        }
    }

    public void airdropFrom(Address nrc20, String from, String[] tos, String[] values) {
        onlyOwner();
        require(!Msg.address().equals(nrc20), "Do nothing by yourself");
        require(nrc20.isContract(), "[" + nrc20.toString() + "] is not a contract address");
        String to;
        String value;
        String[][] args;
        String methodName;
        for (int i = 0, len = tos.length; i < len; i++) {
            to = tos[i];
            value = values[i];
            methodName = "transferFrom";
            args = new String[][]{
                    new String[]{from},
                    new String[]{to},
                    new String[]{value}};
            nrc20.call(methodName, "(Address from, Address to, BigInteger value) return boolean", args, BigInteger.ZERO);
        }
    }

}