package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static java.lang.Integer.parseInt;

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
        return getPropertyByName("server.host");
    }

    public static Integer getServerPort() {
        return parseInt(getPropertyByName("server.port"));
    }

    public static Integer getNumberOfServers() {
        return parseInt(getPropertyByName("number.of.servers"));
    }

    public static Integer getSaveCopies() {
        return parseInt(getPropertyByName("save.copies"));
    }

    public static Integer getDaysToExpireAuction() {
        return parseInt(getPropertyByName("days.to.finish.auction"));

    }

    public static Integer getNumberOfLogs() {
        return parseInt(getPropertyByName("number.of.logs"));

    }

    public static Integer getLogSize() {
        return parseInt(getPropertyByName("log.size"));
    }

    private static String getPropertyByName(String s) {
        return getProperties().getPropertyByName(s);
    }


}
