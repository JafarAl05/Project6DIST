package systemy.namingserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage {
    static void storeMap(ConcurrentHashMap<Integer, String> map) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String mapString = objectMapper.writeValueAsString(map);
        FileWriter myWriter = new FileWriter("nameserver_map.json");
        myWriter.write(mapString);
        myWriter.close();
        System.out.println("Writing JSON:" + mapString);
    }

    static ConcurrentHashMap<Integer, String> loadMap(String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filename);
        ConcurrentHashMap<Integer, String> outputMap;
        if (file.exists()) {
            TypeReference<ConcurrentHashMap<Integer, String>> typeReference = new TypeReference<ConcurrentHashMap<Integer, String>> () {};
            outputMap = objectMapper.readValue(file, typeReference);
        } else {
            outputMap = new ConcurrentHashMap<Integer, String>();
        }
        return outputMap;
    }
}
