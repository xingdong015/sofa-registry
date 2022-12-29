package com.alipay.sofa.registry.server.shared.remoting;

import com.alipay.sofa.registry.common.model.store.URL;
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

    public Exchange getExchangeByPrototype(URL.ProtocolType protocolType){
        return protocolType == URL.ProtocolType.BOLT ? boltExchange : grpcExchange;
    }
}
