package samcask.scheduledrestart;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class OldRestartConfig {
    String oldFileContent;

    public OldRestartConfig(Path oldFilePath) throws IOException {
        oldFileContent = Files.readString(oldFilePath);
    }

    public String extractValue(String key, char[] endPoints, String defaultValue) {
        String searchKey = "\"" + key + "\": ";
        int startIndex = oldFileContent.indexOf(searchKey);
        if (startIndex != -1) {
            startIndex += searchKey.length();
            int endIndex = -1;
            for (char endPoint : endPoints) {
                int index = oldFileContent.indexOf(endPoint, startIndex);
                if (endIndex == -1 || index < endIndex) endIndex = index;
            }
            if (endIndex != -1) {
                return oldFileContent.substring(startIndex, endIndex);
            }
        }
        return defaultValue;
    }
}
