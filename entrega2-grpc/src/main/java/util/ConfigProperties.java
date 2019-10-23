package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties extends Properties {

    private static final String ROOT_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    private static final String CONFIG_PATH = ROOT_PATH + "application.properties";

    private static ConfigProperties configProperties = null;

    private ConfigProperties() {
    }

    public static ConfigProperties getProperties() {

        if(configProperties == null) {
            configProperties = new ConfigProperties();
            try {
                FileInputStream in = new FileInputStream(CONFIG_PATH);
                configProperties.load(in);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return configProperties;
    }

    public static String getServerHost() {
        return getProperties().getProperty("server.host");
    }

    public static Integer getServerPort() {
        return Integer.parseInt(getProperties().getProperty("server.port"));
    }
}
