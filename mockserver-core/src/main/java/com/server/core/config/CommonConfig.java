package com.server.core.config;

import com.server.core.notification.Notification;
import com.server.core.util.ImportExportUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tools.jackson.databind.ObjectMapper;

import java.security.Security;

/**
 * @author Kazi Tanvir Azad
 */
public enum CommonConfig {
    INSTANCE;
    private Notification notification;

    private final ObjectMapper mapper;
    private final ImportExportUtil ioUtil;
    private final BouncyCastleProvider provider;

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ImportExportUtil getIoUtil() {
        return ioUtil;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Notification notification() {
        return notification;
    }

    public void initBountyCastleProvider() {
        if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
            Security.addProvider(provider);
        }
    }

    {
        this.mapper = new ObjectMapper();
        this.ioUtil = new ImportExportUtil();
        this.provider = new BouncyCastleProvider();
    }
}
