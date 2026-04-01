package systemy.namingserver;
import org.springframework.web.bind.annotation.*;

@RestController
public class NamingController {

    // Listen for GET requests asking for a file's location
    @GetMapping("/files/{filename}")
    public NodeResponse getFileLocation(@PathVariable("filename") String filename) {

        // TODO: Later, call Robeen's MapManager and my Routing Algorithm here.

        // For now, just return a hardcoded fake response so Role C can start testing!
        return new NodeResponse(17, "192.168.0.4");
    }
}