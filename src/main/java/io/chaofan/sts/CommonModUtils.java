package io.chaofan.sts;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CommonModUtils {
    public static final Logger logger = LogManager.getLogger(CommonModUtils.class.getName());

    private static String getLocalizationPath(String prefix, String file) {
        return prefix + "/localization/" + file;
    }

    public static String getLocalizationFilePath(String prefix, String file) {
        String language = Settings.language.toString().toLowerCase();
        logger.info("getLocalizationFilePath - file=" + file + ", language=" + language);

        String path = getLocalizationPath(prefix, language + "/" + file);
        URL url = CommonModUtils.class.getResource("/" + path);
        if (url != null) {
            return path;
        } else {
            return getLocalizationPath(prefix, "eng/" + file);
        }
    }

    public static void loadCustomStringsFile(String prefix, Class<?> stringType, String file) {
        BaseMod.loadCustomStringsFile(stringType, getLocalizationFilePath(prefix, file));
    }

    public static void loadKeywordsFile(Map<String, Keyword> backup) {
        Gson gson = new Gson();
        String json = Gdx.files.internal(getLocalizationFilePath(ChaofanMod.MOD_ID, "keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        Keyword[] keywords = gson.fromJson(json, Keyword[].class);

        if (keywords != null) {
            for (Keyword keyword : keywords) {
                BaseMod.addKeyword(ChaofanMod.MOD_ID, keyword.PROPER_NAME, keyword.NAMES.clone(), keyword.DESCRIPTION);
                if (backup != null) {
                    backup.put(keyword.KEY, keyword);
                }
            }
        }
    }

    public static class Keyword {
        public String KEY;
        public String PROPER_NAME;
        public String[] NAMES;
        public String DESCRIPTION;
    }
}
