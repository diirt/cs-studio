/**
 * Copyright (C) 2012-14 epics-util developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.epics.util.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for all configuration in diirt.
 * <p>
 * The configuration directory used is given by:
 * <ul>
 *     <li>The Java property <b>diirt.home</b> if set. It can either be
 *     set when creating the JVM using -D or programmatically using
 *     <code>System.setProperty</code>. When set programmatically, one
 *     must make sure that it is set before any call to {@link #configurationDirectory()},
 *     since the property is only read once and then cached.</li>
 *     <li>The environment variable <b>DIIRT_HOME</b> if set.</li>
 *     <li>The default <b>$USER_HOME/.diirt</b></li>
 * </ul>
 *
 * @author carcassi
 */
public class Configuration {
   
    private static Logger log = Logger.getLogger(Configuration.class.getName());
    private static final File configurationDirectory = configurationDirectory();
    
    private static File configurationDirectory() {
        // First look for java property
        String diirtHome = System.getProperty("diirt.home");
        
        // Second look for environment variable
        if (diirtHome == null) {
            diirtHome = System.getenv("DIIRT_HOME");
        }

        File dir;
        if (diirtHome != null) {
            dir = new File(diirtHome);
        } else {
            // Third use default in home directory
            dir = new File(System.getProperty("user.home"), ".diirt");
        }
        dir.mkdirs();
        return dir;
    }

    public static File getDirectory() {
        return configurationDirectory;
    }
    
    public static InputStream getFileAsStream(String relativeFilePath, Object obj, String defaultResource) throws IOException {
        File mappings = new File(Configuration.getDirectory(), relativeFilePath);
        if (!mappings.exists()) {
            mappings.getParentFile().mkdirs();
            try (InputStream input = obj.getClass().getResourceAsStream(defaultResource);
                    OutputStream output = new FileOutputStream(mappings)) {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            log.log(Level.INFO, "Initializing configuration file " + mappings);
        }
        
        log.log(Level.INFO, "Loading " + mappings);
        return new FileInputStream(mappings);
    }

}
