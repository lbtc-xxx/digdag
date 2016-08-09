package io.digdag.core.database;

import com.google.inject.Inject;
import io.digdag.client.config.Config;
import io.digdag.core.SecretCrypto;
import io.digdag.spi.SecretControlStore;
import io.digdag.spi.SecretControlStoreManager;
import org.skife.jdbi.v2.DBI;

public class DatabaseSecretControlStoreManager
        implements SecretControlStoreManager
{
    private final Config systemConfig;
    private final DatabaseConfig config;
    private final DBI dbi;
    private final SecretCrypto crypto;

    @Inject
    public DatabaseSecretControlStoreManager(Config systemConfig, DatabaseConfig config, DBI dbi, SecretCrypto crypto)
    {
        this.systemConfig = systemConfig;
        this.config = config;
        this.dbi = dbi;
        this.crypto = crypto;
    }

    @Override
    public SecretControlStore getSecretControlStore(int siteId)
    {
        return new DatabaseSecretControlStore(config, dbi, siteId, crypto);
    }
}
