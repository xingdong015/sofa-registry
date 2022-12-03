package com.alipay.sofa.registry.server.shared.remoting;

import com.alipay.sofa.registry.remoting.exchange.Exchange;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chengzhengzheng
 * @date 2022/12/2
 */
public class ExchangeManager {
    @Autowired
    private Exchange boltExchange;
    @Autowired
    private Exchange grpcExchange;

    private Map<String, Exchange> exchangeMap = new HashMap<>();

    private String currentExchangeName;

    @PostConstruct
    public void init(){
        currentExchangeName = "grpc";
        exchangeMap.put("grpc",grpcExchange);
        exchangeMap.put("bolt",boltExchange);
    }


    public Exchange getSessionExchange() {
        return exchangeMap.get(currentExchangeName);
    }

}
