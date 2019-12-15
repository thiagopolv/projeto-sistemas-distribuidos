package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
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

    public String loadFile(String resourceName) {

        InputStream ios = null;
        String ioReturn = "";
        try {
            if (!StringUtils.isEmpty(folderPrefix)) {
                resourceName = folderPrefix + "/" + resourceName;
            }

            ios = new FileInputStream(resourceName);
            ioReturn = IOUtils.toString(ios, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading file " + resourceName + " - " + e.getMessage());
        }

        return ioReturn;
    }

    //TODO: Quebrado
    public void saveFile(String resourceName, Object object) {

        try {
            if (!StringUtils.isEmpty(folderPrefix)) {
                resourceName = folderPrefix + "/" + resourceName;
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(resourceName), object);
        } catch (IOException e) {
            throw new RuntimeException("Error saving file " + resourceName + " - " + e.getMessage());
        }
    }

    private ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    public static void main(String[] args) throws IOException {

//        JsonLoader jsonLoader = new JsonLoader("src/main/data");
//
//        List<Auction> list = new ArrayList<>(jsonLoader.loadList("auctions0.json", Auction.class));
//
//        Auction auction = Auction.build(100, null, 50.00);
//
//        list.add(auction);
//
//        ObjectMapper om = new ObjectMapper();
//
//        jsonLoader.saveFile("auctions1.json", list);
//
//        System.out.println(list);
    }
}
