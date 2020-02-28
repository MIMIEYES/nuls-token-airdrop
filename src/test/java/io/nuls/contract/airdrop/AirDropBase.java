/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.airdrop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.basic.Result;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.v2.NulsSDKBootStrap;
import io.nuls.v2.SDKContext;
import io.nuls.v2.model.dto.ContractValidateCallForm;
import io.nuls.v2.model.dto.ImputedGasContractCallForm;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.model.dto.RpcResultError;
import io.nuls.v2.util.JsonRpcUtil;
import io.nuls.v2.util.ListUtil;
import io.nuls.v2.util.NulsSDKTool;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;

import static io.nuls.v2.constant.Constant.CONTRACT_MINIMUM_PRICE;
import static io.nuls.v2.constant.Constant.MAX_GASLIMIT;

/**
 * @author: PierreLuo
 * @date: 2020-02-27
 */
public abstract class AirDropBase extends AddressFactory{

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private String invokeAddress;
    private String privateKey;
    private String tokenAirdropContract;
    // 0.1个
    protected String airdropTokenAmount = "1000000";
    protected String publicServiceHost;
    protected int chainId;
    protected int assetId;
    protected String tokenContract;
    protected String txRemark;

    public abstract String getNulsApiHost();

    public void before() throws InterruptedException {
        SDKContext.wallet_url = getNulsApiHost();
        RpcResult info = JsonRpcUtil.request("info", ListUtil.of());
        Map result = (Map) info.getResult();
        chainId = (Integer) result.get("chainId");
        assetId = (Integer) result.get("assetId");
        // initial SDK
        NulsSDKBootStrap.init(chainId, getNulsApiHost());
        // 主网配置
        if (chainId == 1) {
            publicServiceHost = "https://public1.nuls.io/";
            invokeAddress = "NULSd6HgZ8xEbCKo9J5MwgJYVy9F3Cpzvh2GY";
            privateKey = "";
            tokenContract = "";
            tokenAirdropContract = "NULSd6HgrdJTSeuiHnXqD6CFiNuE9ULfEj4NP";
        }
        // 测试网配置
        else if (chainId == 2) {
            publicServiceHost = "http://beta.public1.nuls.io/";
            invokeAddress = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
            privateKey = "9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b";
            tokenContract = "tNULSeBaNAVKdxePZHVfpLoCR3QYov9yjFRgdh";
            tokenAirdropContract = "tNULSeBaMyNU4JYK3K4DRQFxEwNAzu62UphVWT";
        } else {
            throw new RuntimeException(String.format("Unkonw chainId: %s", chainId));
        }
    }

    protected void airdrop(JSONArray list) throws InterruptedException, JsonProcessingException {
        int size = list.size();
        String[] tos = new String[size];
        String[] values = new String[size];
        JSONObject account;
        String address;
        for (int k = 0; k < size; k++) {
            account = list.getJSONObject(k);
            address = account.getString("address");
            tos[k] = address;
            values[k] = airdropTokenAmount;
        }

        Object[] args = new Object[]{
                tokenContract,
                tos,
                values
        };
        this.broadcastContractTx(invokeAddress, privateKey, tokenAirdropContract, BigInteger.ZERO, "airdrop", "", args, new String[]{"Address", "String[]", "String[]"});
    }

    protected void airdrop(String[] addressArray) throws InterruptedException, JsonProcessingException {
        int size = addressArray.length;
        String[] tos = new String[size];
        String[] values = new String[size];
        JSONObject account;
        String address;
        for (int k = 0; k < size; k++) {
            address = addressArray[k];
            tos[k] = address;
            values[k] = airdropTokenAmount;
        }

        Object[] args = new Object[]{
                tokenContract,
                tos,
                values
        };
        this.broadcastContractTx(invokeAddress, privateKey, tokenAirdropContract, BigInteger.ZERO, "airdrop", "", args, new String[]{"Address", "String[]", "String[]"});
    }

    private void broadcastContractTx(String invokeAddress, String privateKey, String contractAddress, BigInteger value,
                                     String methodName,
                                     String methodDesc,
                                     Object[] args,
                                     String[] argsType) throws InterruptedException, JsonProcessingException {
        String sender = invokeAddress;
        if(StringUtils.isBlank(txRemark)) {
            txRemark = "system invoke";
        }
        String remark = txRemark;

        RpcResult<Map> validateResult = JsonRpcUtil.request("validateContractCall", ListUtil.of(chainId, sender, value,
                MAX_GASLIMIT, CONTRACT_MINIMUM_PRICE, contractAddress, methodName, methodDesc, args));
        RpcResultError validateResultError = validateResult.getError();
        if (validateResultError != null) {
            logger.error("validateContractCall error[0] - [{}]", validateResultError.toString());
            return;
        }
        Map map = (Map) validateResult.getResult();
        boolean success = (Boolean) map.get("success");
        if (!success) {
            logger.error("validateContractCall error[1] - [{}]", (String) map.get("msg"));
            return;
        }

        // 在线接口(可跳过) - 估算调用合约需要的GAS，可不估算，离线写一个合理的值
        RpcResult<Map> rpcResult = JsonRpcUtil.request("imputedContractCallGas", ListUtil.of(chainId, sender, value, contractAddress, methodName, methodDesc, args));
        RpcResultError rpcResultError = rpcResult.getError();
        if (rpcResultError != null) {
            logger.error("imputedContractCallGas error - [{}]", rpcResultError.toString());
            return;
        }
        Map result = rpcResult.getResult();
        Long gasLimit = Long.valueOf(result.get("gasLimit").toString());
        logger.info("gas init cost: {}", gasLimit);

        // 在线接口(不可跳过，一定要调用的接口) - 获取账户余额信息
        RpcResult<Map> balanceResult = JsonRpcUtil.request("getAccountBalance", ListUtil.of(chainId, chainId, assetId, sender));
        rpcResultError = balanceResult.getError();
        if (rpcResultError != null) {
            logger.error("getAccountBalance error - [{}]", rpcResultError.toString());
            return;
        }
        result = balanceResult.getResult();
        BigInteger senderBalance = new BigInteger(result.get("balance").toString());
        String nonce = result.get("nonce").toString();

        // 离线接口 - 组装调用合约的离线交易
        Result<Map> txOfflineR = NulsSDKTool.callContractTxOffline(sender, senderBalance, nonce, value, contractAddress, gasLimit, methodName, methodDesc, args, argsType, remark);
        if (txOfflineR.isFailed()) {
            logger.error("make contract tx error - [{}]", txOfflineR.toString());
            return;
        }
        Map txMap = txOfflineR.getData();
        String txHex = (String) txMap.get("txHex");
        String hash = (String) txMap.get("hash");

        // 离线接口 - 签名交易
        Result<Map> signTxR = NulsSDKTool.sign(txHex, invokeAddress, privateKey);
        if (signTxR.isFailed()) {
            logger.error("sign contract tx error - [{}]", signTxR.toString());
            return;
        }
        Map resultData = signTxR.getData();
        String _hash = (String) resultData.get("hash");
        if (!hash.equals(_hash)) {
            logger.error("hash is not consistent, tx hash - [{}], sign tx hash", hash, _hash);
            return;
        }
        String signedTxHex = (String) resultData.get("txHex");

        // 在线接口 - 广播交易
        RpcResult<Map> broadcastTxResult = JsonRpcUtil.request("broadcastTx", ListUtil.of(chainId, signedTxHex));
        rpcResultError = broadcastTxResult.getError();
        if (rpcResultError != null) {
            logger.error("broadcast contract tx error - [{}]", rpcResultError.toString());
            return;
        }
        logger.info("transaction hash is [{}]", hash);
    }

    /**
     * 弃用的方法
     */
    @Deprecated
    private String broadcastContractTx(String contractAddress, BigInteger value,
                                       String methodName,
                                       String methodDesc,
                                       Object[] args,
                                       String[] argsType) throws InterruptedException, JsonProcessingException {
        String sender = invokeAddress;
        String remark = "system invoke";

        ContractValidateCallForm validateCallForm = new ContractValidateCallForm();
        validateCallForm.setSender(sender);
        validateCallForm.setValue(value.longValue());
        validateCallForm.setGasLimit(MAX_GASLIMIT);
        validateCallForm.setPrice(CONTRACT_MINIMUM_PRICE);
        validateCallForm.setContractAddress(contractAddress);
        validateCallForm.setMethodName(methodName);
        validateCallForm.setMethodDesc(methodDesc);
        validateCallForm.setArgs(args);
        Result vResult = NulsSDKTool.validateContractCall(validateCallForm);
        if (!vResult.isSuccess()) {
            logger.error("validateContractCall error[0] - [{}]", JSONUtils.obj2PrettyJson(vResult));
            return null;
        }
        Map map = (Map) vResult.getData();
        boolean success = (Boolean) map.get("success");
        if (!success) {
            logger.error("validateContractCall error[1] - [{}]", (String) map.get("msg"));
            return null;
        }

        // 在线接口(可跳过) - 估算调用合约需要的GAS，可不估算，离线写一个合理的值
        ImputedGasContractCallForm iForm = new ImputedGasContractCallForm();
        iForm.setSender(sender);
        iForm.setValue(value);
        iForm.setContractAddress(contractAddress);
        iForm.setMethodName(methodName);
        iForm.setMethodDesc(methodDesc);
        iForm.setArgs(args);
        Result iResult = NulsSDKTool.imputedContractCallGas(iForm);
        Assert.assertTrue(JSONUtils.obj2PrettyJson(iResult), iResult.isSuccess());
        if (!iResult.isSuccess()) {
            logger.error("imputedContractCallGas error - [{}]", JSONUtils.obj2PrettyJson(iResult));
            return null;
        }
        Map result = (Map) iResult.getData();
        Long gasLimit = Long.valueOf(result.get("gasLimit").toString());
        logger.info("gas init cost: {}", gasLimit);

        // 在线接口(不可跳过，一定要调用的接口) - 获取账户余额信息
        Result accountBalanceR = NulsSDKTool.getAccountBalance(sender, 2, 1);
        if (!accountBalanceR.isSuccess()) {
            logger.error("getAccountBalance error - [{}]", JSONUtils.obj2PrettyJson(accountBalanceR));
            return null;
        }
        Map balance = (Map) accountBalanceR.getData();
        BigInteger senderBalance = new BigInteger(balance.get("available").toString());
        String nonce = balance.get("nonce").toString();

        // 离线接口 - 组装调用合约的离线交易
        Result<Map> txOfflineR = NulsSDKTool.callContractTxOffline(sender, senderBalance, nonce, value, contractAddress, gasLimit, methodName, methodDesc, args, argsType, remark);
        if (txOfflineR.isFailed()) {
            logger.error("make contract tx error - [{}]", txOfflineR.toString());
            return null;
        }
        Map txMap = txOfflineR.getData();
        String txHex = (String) txMap.get("txHex");
        String hash = (String) txMap.get("hash");

        // 离线接口 - 签名交易
        Result<Map> signTxR = NulsSDKTool.sign(txHex, sender, privateKey);
        if (signTxR.isFailed()) {
            logger.error("sign contract tx error - [{}]", JSONUtils.obj2PrettyJson(signTxR));
            return null;
        }
        Map resultData = signTxR.getData();
        String _hash = (String) resultData.get("hash");

        if (!hash.equals(_hash)) {
            logger.error("hash is not consistent, tx hash - [{}], sign tx hash", hash, _hash);
            return null;
        }
        String signedTxHex = (String) resultData.get("txHex");

        // 在线接口 - 广播交易
        Result<Map> broadcaseTxR = NulsSDKTool.broadcast(signedTxHex);
        if (broadcaseTxR.isFailed()) {
            logger.error("broadcast contract tx error - [{}]", JSONUtils.obj2PrettyJson(broadcaseTxR));
            return null;
        }
        Map data = broadcaseTxR.getData();
        String finalHash = (String) data.get("hash");
        return finalHash;
    }
}
