/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.diirt.datasource.pods.web;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.epics.pvmanager.ChannelHandler;
import org.epics.pvmanager.DataSource;
import org.epics.pvmanager.PVManager;
import static org.epics.pvmanager.util.Executors.namedPool;
import org.epics.pvmanager.vtype.DataTypeSupport;

/**
 * Data source for web=pods.
 *
 * @author carcassi
 */
public final class WebPodsDataSource extends DataSource {

    static {
	// Install type support for the types it generates.
	DataTypeSupport.install();
    }
    
    private final WebPodsClient client;
    private final WebPodsDataSourceConfiguration configuration;
    private static ExecutorService exec = Executors.newSingleThreadExecutor(namedPool("diirt web-pods datasource connection "));
    private static Logger log = Logger.getLogger(WebPodsDataSource.class.getName());

    public WebPodsDataSource() {
        this(WebPodsDataSourceConfiguration.readConfiguration(new WebPodsDataSourceFactory().getDefaultConfPath()));
    }

    /**
     * Creates a new data source.
     * 
     * @param configuration datasource configuration
     */
    public WebPodsDataSource(WebPodsDataSourceConfiguration configuration) {
        super(true);
        client = new WebPodsClient();
        this.configuration = configuration;
    }

    @Override
    public void close() {
        super.close();
    }
    
    private void reconnect() {
        exec.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    if (client.isConnected()) {
                        return;
                    }
                    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                    container.connectToServer(client, WebPodsDataSource.this.configuration.getSocketLocation());
                    log.info("Web-pods datasource connected");
                } catch (DeploymentException | IOException ex) {
                    log.log(Level.WARNING, "Web-pods datasource connection problems", ex);
                }
            }
        });
    }

    WebPodsClient getClient() {
        if (!client.isConnected()) {
            reconnect();
        }
        return client;
    }
   
    @Override
    protected ChannelHandler createChannel(String channelName) {	
	return new WebPodsChannelHandler(this, channelName);
    }
    
}
