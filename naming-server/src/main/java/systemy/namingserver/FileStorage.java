package systemy.namingserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class FileStorage {
    static void storeMap(HashMap<Integer, String> map) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String mapString = objectMapper.writeValueAsString(map);
        FileWriter myWriter = new FileWriter("nameserver_map.json");
        myWriter.write(mapString);
        myWriter.close();
    }

    static HashMap<Integer, String> loadMap(String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filename);
        HashMap<Integer, String> outputMap;
        if (file.exists()) {
            TypeReference<HashMap<Integer, String>> typeReference = new TypeReference<HashMap<Integer, String>> () {};
            outputMap = objectMapper.readValue(file, typeReference);
        } else {
            outputMap = new HashMap<Integer, String>();
        }
        return outputMap;
    }
}
