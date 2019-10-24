package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.Auction;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonLoader {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private String folderPrefix = "";

    public JsonLoader() {
    }

    public JsonLoader(String folderPrefix) {
        this.folderPrefix = folderPrefix;
    }

    public void setFolderPrefix(String folderPrefix) {
        this.folderPrefix = folderPrefix;
    }

    public <T> List<T> loadList(String resourceName, Class<T> clazz) {
        try {
            String json = loadFile(resourceName);

            ObjectMapper mapper = getObjectMapper();
            @SuppressWarnings("unchecked") final T[] a = (T[]) Array.newInstance(clazz, 1);
            @SuppressWarnings("unchecked")
            T[] arrayValue = (T[]) mapper.readValue(json, a.getClass());
            return Arrays.asList(arrayValue);
        } catch (IOException ioex) {
            throw new RuntimeException("Error loading file list " + resourceName, ioex);
        }
    }

    public <T> T loadObject(String resourceName, Class<T> clazz) {
        try {
            String json = loadFile(resourceName);

            ObjectMapper mapper = getObjectMapper();
            return mapper.readValue(json, clazz);
        } catch (IOException ioex) {
            throw new RuntimeException("Error loading file object " + resourceName + " - " + ioex.getMessage(),
                    ioex);
        }
    }

    public String loadFile(String resourceName) throws IOException {

        if (!StringUtils.isEmpty(folderPrefix)) {
            resourceName = folderPrefix + "/" + resourceName;
        }

        InputStream ios = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (ios == null) {
            throw new FileNotFoundException(resourceName);
        }
        return IOUtils.toString(ios, StandardCharsets.UTF_8);
    }

    //TODO: Quebrado
    public void saveFile(String resourceName, Object object) throws IOException {

        if (!StringUtils.isEmpty(folderPrefix)) {
            resourceName = folderPrefix + "/" + resourceName;
        }

        objectMapper.writeValue(new File(getClass().getClassLoader().getResource(resourceName).getFile()), object);

    }

    private ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    //TODO: Quebrado
    public static void main(String[] args) throws IOException {

        JsonLoader jsonLoader = new JsonLoader("data");

        List<Auction> list = jsonLoader.loadList("auctions.json", Auction.class);

        List<Auction> lists = new ArrayList(Arrays.asList(list));

        Auction auction = Auction.build(50, null, 50.00);

        lists.add(auction);

        jsonLoader.saveFile("auctionsss.json", lists);
    }
}
