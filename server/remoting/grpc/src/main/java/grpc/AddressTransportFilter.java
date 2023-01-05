package grpc;

import com.alipay.remoting.Url;
import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.ServerTransportFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.config.Loggers;

import java.net.InetSocketAddress;

public class AddressTransportFilter extends ServerTransportFilter {

    private final ConnectionManager connectionManager;

    public AddressTransportFilter(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Attributes transportReady(Attributes transportAttrs) {
        InetSocketAddress remoteAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        InetSocketAddress localAddress  = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR);
        int remotePort = remoteAddress.getPort();
        int localPort = localAddress.getPort();
        String remoteIp = remoteAddress.getAddress().getHostAddress();
        Url    key          = new Url(remoteIp, remotePort);
        Attributes attrWrapper = transportAttrs.toBuilder().set(GrpcServerConstants.ATTR_TRANS_KEY_CONN_ID, key.getUniqueKey())
                .set(GrpcServerConstants.ATTR_TRANS_KEY_REMOTE_IP, remoteIp)
                .set(GrpcServerConstants.ATTR_TRANS_KEY_REMOTE_PORT, remotePort)
                .set(GrpcServerConstants.ATTR_TRANS_KEY_LOCAL_PORT, localPort).build();
        return attrWrapper;
    }

    @Override
    public void transportTerminated(Attributes transportAttrs) {
        String connectionId = null;
        try {
            connectionId = transportAttrs.get(GrpcServerConstants.ATTR_TRANS_KEY_CONN_ID);
        } catch (Exception e) {
            // Ignore
        }
        if (StringUtils.isNotBlank(connectionId)) {
            connectionManager.unregister(connectionId);
        }
    }
}