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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.nuls.v2.constant.Constant.CONTRACT_MINIMUM_PRICE;
import static io.nuls.v2.constant.Constant.MAX_GASLIMIT;

/**
 * 1. NRC20代币转入空投合约中
 * 2. 设置主网或测试网NULS-API
 * 3. 空投
 *
 * @author: PierreLuo
 * @date: 2019-10-08
 */
public class TokenAirDropTest extends AirDropBase{


    private String mainNulsApiHost = "https://api.nuls.io/";
    private String testNulsApiHost = "http://beta.api.nuls.io/";

    @BeforeClass
    public static void beforeClass() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
    }

    /**
     * 设置主网或测试网NULS-API
     */
    @Override
    public String getNulsApiHost() {
        return mainNulsApiHost;
    }

    @Before
    public void before() throws InterruptedException {
        super.before();
    }

    @Test
    public void dropCoinRanking() throws InterruptedException, JsonProcessingException {
        this.tokenContract = "";
        this.airdropTokenAmount = "1000000";
        this.txRemark = "Coin Ranking Drop";
        // 参数
        int totalLimit = 1000;
        int pageSize = 20;
        /**
         查询账户持币排名
         params[chainId, pageIndex, pageSize, sortType]
         chainId : int //链id
         pageIndex : int //页码
         pageSize : int //每页条数
         sortType: int //排名顺序 0 总余额倒序，1 总余额正序
         */
        int total = pageSize + 1;
        boolean initTotal = false;
        for (int page = 1, i = 0; i < total && i < totalLimit; page++, i = i + pageSize) {
            RpcResult result = JsonRpcUtil.request(publicServiceHost, "getCoinRanking", ListUtil.of(chainId, page, pageSize, 0));
            Map map = (Map) result.getResult();
            JSONObject jsonObject = new JSONObject(map);
            if (!initTotal) {
                total = jsonObject.getInteger("totalCount");
                initTotal = true;
            }
            JSONArray list = jsonObject.getJSONArray("list");
            this.airdrop(list);
        }
    }

    @Test
    public void getContractTokens() throws InterruptedException, JsonProcessingException {
        this.tokenContract = "NULSd6HgwJmD4SC1NAJXu8tC6NKsWs99P2jpw";
        // 参数
        int pageSize = 100;
        for (int page = 1; page <= 12; page++) {
            RpcResult result = JsonRpcUtil.request(publicServiceHost, "getContractTokens", ListUtil.of(chainId, page, pageSize, this.tokenContract));
            Map map = (Map) result.getResult();
            List<Map> dataList = (List<Map>) map.get("list");
            for (Map data : dataList) {
                String address = data.get("address").toString();
                String balance = data.get("balance").toString();
                System.out.println(address + "," + balance);
            }
        }
    }

    @Test
    public void dropTNX() throws JsonProcessingException, InterruptedException {
        this.tokenContract = "NULSd6HgnvP3rBcFG9iobGGdj3uAfnYmLiRnv";
        this.airdropTokenAmount = "2000000000";
        this.txRemark = "chinese community";
        this.airdrop(this.tnxAddressArray);
    }


    @Test
    public void dropPL() throws JsonProcessingException, InterruptedException {
        this.tokenContract = "NULSd6HgjvThi4V8g5dQcE2gs49Vep1RRZG47";
        this.airdropTokenAmount = "200";
        this.txRemark = "air drop test pl";
        this.airdrop(this.plAddressArray);
    }


}
