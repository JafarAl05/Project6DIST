package systemy.namingserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class FileStorage {
    public void storeMap(HashMap<String, Integer> map) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String mapString = objectMapper.writeValueAsString(map);
        FileWriter myWriter = new FileWriter("nameserver_map.txt");
        myWriter.write(mapString);
        myWriter.close();
    }

    public HashMap<String, Integer> loadMap(String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(filename);
        TypeReference<HashMap<String, Integer>> typeReference = new TypeReference<HashMap<String, Integer>> () {};
        HashMap<String, Integer> outputMap = objectMapper.readValue(file, typeReference);
        return outputMap;
    }
}
