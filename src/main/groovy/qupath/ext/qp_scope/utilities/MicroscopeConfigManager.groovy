package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Paths

class MicroscopeConfigManager {
    static final logger = LoggerFactory.getLogger(MicroscopeConfigManager.class)
    private static MicroscopeConfigManager instance
    private Map configData

    private MicroscopeConfigManager(String configPath) {
        this.configData = loadConfig(configPath)
    }

    static MicroscopeConfigManager getInstance(String configPath) {
        if (instance == null) {
            instance = new MicroscopeConfigManager(configPath)
        }
        return instance
    }

    private static Map loadConfig(String configPath) {
        Yaml yaml = new Yaml()
        try (InputStream inputStream = new FileInputStream(configPath)) {
            return yaml.load(inputStream)
        } catch (FileNotFoundException e) {
            logger.error("YAML file not found: " + configPath, e)
            return null
        } catch (Exception e) {
            logger.error("Error parsing YAML file: " + configPath, e)
            return null
        }
    }

    def getConfigItem(String... keys) {
        Object currentLevel = configData // Start with the whole config data
        for (String key : keys) {
            if (currentLevel instanceof Map && ((Map) currentLevel).containsKey(key)) {
                currentLevel = ((Map) currentLevel).get(key)
            } else {
                return null // Key not found or not a Map where expected
            }
        }
        return currentLevel
    }

}

