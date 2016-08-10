package io.digdag.standards;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.treasuredata.client.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.jboss.resteasy.util.Encode.decode;

public class Proxies
{
    private static final Logger logger = LoggerFactory.getLogger(Proxies.class);

    @VisibleForTesting
    public static Optional<ProxyConfig> proxyConfigFromEnv(Map<String, String> env)
    {
        String var = env.getOrDefault("http_proxy", env.get("HTTP_PROXY"));
        if (var == null) {
            return Optional.absent();
        }

        URI uri;
        try {
            uri = new URI(var);
        }
        catch (URISyntaxException e) {
            logger.warn("Failed to parse 'http_proxy' environment variable, ignoring: {}", var, e);
            return Optional.absent();
        }

        ProxyConfig.ProxyConfigBuilder builder = new ProxyConfig.ProxyConfigBuilder();

        builder.setHost(uri.getHost());

        if (uri.getPort() != -1) {
            builder.setPort(uri.getPort());
        }

        if (uri.getRawUserInfo() != null) {
            String userInfo = uri.getRawUserInfo();
            int colonIndex = userInfo.indexOf(':');
            if (colonIndex == -1) {
                builder.setUser(decode(userInfo));
            }
            else {
                String user = userInfo.substring(0, colonIndex);
                String pass = userInfo.substring(colonIndex + 1, userInfo.length());
                builder.setUser(decode(user));
                builder.setPassword(decode(pass));
            }
        }

        if ("https".equals(uri.getScheme())) {
            builder.useSSL(true);
        }

        return Optional.of(builder.createProxyConfig());
    }
}
