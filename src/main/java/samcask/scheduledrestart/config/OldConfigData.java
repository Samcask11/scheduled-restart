package samcask.scheduledrestart.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.function.Function;

@Deprecated
public class OldConfigData {
    String oldFileContent;

    public OldConfigData(Path oldFilePath) throws IOException {
        oldFileContent = Files.readString(oldFilePath);
    }

    public String extractValue(String key, String defaultValue, char[] endPoints, Function<String, ?> parse) {
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
                try {
                    parse.apply(oldFileContent.substring(startIndex, endIndex));
                    return oldFileContent.substring(startIndex, endIndex);
                } catch (Exception ignored) {}
            }
        }
        return defaultValue;
    }

    public String extractString(String key, String defaulValue) {
        return extractValue(key, defaulValue, new char[] { ',', '\n' }, (String value) -> value);
    }

    public String extractInteger(String key, String defaulValue) {
        return extractValue(key, defaulValue, new char[] { ',', '\n' }, Integer::parseInt);
    }

    public String extractIntegerArray(String key, String defaulValue) {
        return extractValue(key, defaulValue, new char[] { ']' }, (String value) -> ConfigData.parseArray(value, Integer::parseInt));
    }

    public String extractTimeArray(String key, String defaulValue) {
        return extractValue(key, defaulValue, new char[] { ']' }, (String value) -> ConfigData.parseArray(value, LocalTime::parse));
    }
}
