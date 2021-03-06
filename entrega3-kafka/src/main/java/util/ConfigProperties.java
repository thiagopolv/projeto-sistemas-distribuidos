package util;

import static java.lang.Integer.parseInt;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SuppressWarnings("ConstantConditions")
public class ConfigProperties extends Properties {

    private static String ROOT_PATH = Thread.currentThread().getContextClassLoader().getResource("")
            .getPath();
    private static String CONFIG_PATH;

    private static ConfigProperties configProperties = null;

    private ConfigProperties() {
    }

    public static ConfigProperties getProperties() {

        if (configProperties == null) {
            configProperties = new ConfigProperties();
            ROOT_PATH = modifyPathIfOSNotcompatible();
            CONFIG_PATH = ROOT_PATH + "application.properties";
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
        return parseInt(getProperties().getProperty("server.port"));
    }

    public static Integer getNumberOfServers() {
        return parseInt(getProperties().getProperty("number.of.servers"));
    }

    public static Integer getSaveCopies() {
        return parseInt(getProperties().getProperty("save.copies"));
    }

    public static Integer getDaysToExpireAuction() {
        return parseInt(getProperties().getProperty("days.to.finish.auction"));
    }

    public static Integer getNumberOfLogs() {
        return parseInt(getProperties().getProperty("number.of.logs"));
    }

    public static Integer getLogSize() {
        return parseInt(getProperties().getProperty("log.size"));
    }

    public static String getKafkaHost() {
        return getProperties().getProperty("kafka.host");
    }

    public static Integer getNumberOfNodes() {
        return Integer.parseInt(getProperties().getProperty("number.of.nodes"));
    }

    public static Integer getNumberOfReplicas() {
        return Integer.parseInt(getProperties().getProperty("number.of.replicas"));
    }

    public static Integer getBasePort() {
        return Integer.parseInt(getProperties().getProperty("base.port"));
    }

    public static Integer getNodePortDifference() {
        return Integer.parseInt(getProperties().getProperty("node.port.difference"));
    }

    public static Integer getLastBaseHash() {
        String property = getProperties().getProperty("last.base.hash");
        return Integer.decode(property);
    }

    private static String modifyPathIfOSNotcompatible() {
        if (System.getProperty("os.name").contains("Windows")) {
            return System.getProperty("user.dir") + "\\target\\classes\\";
        }
        return ROOT_PATH;
    }
}

