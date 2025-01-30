package me.TechsCode.GradeBasePlugin;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ResourceManager {

    public static boolean loadBasePlugin(Project project, String githubToken, String version) {
        if (githubToken == null) return false;

        File libraryFolder = new File(project.getProjectDir().getAbsolutePath() + "/libs");
        libraryFolder.mkdirs();

        File libraryFile = new File(libraryFolder.getAbsolutePath() + "/BasePlugin.jar");
        libraryFile.delete();

        String RETRIEVE_RELEASES = "https://api.github.com/repos/techscode/baseplugin/releases/tags/" + version + "?access_token=" + githubToken;

        try {
            JSONParser parser = new JSONParser();
            String json = IOUtils.toString(new URI(RETRIEVE_RELEASES), "UTF-8");
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray assets = (JSONArray) root.get("assets");
            JSONObject asset = (JSONObject) assets.get(0);
            URL url = new URL((String) asset.get("url"));

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.setRequestProperty("Authorization", "token " + githubToken);

            ReadableByteChannel uChannel = Channels.newChannel(connection.getInputStream());
            FileOutputStream foStream = new FileOutputStream(libraryFile.getAbsolutePath());
            FileChannel fChannel = foStream.getChannel();
            fChannel.transferFrom(uChannel, 0, Long.MAX_VALUE);
            uChannel.close();
            foStream.close();
            fChannel.close();
        } catch (IOException | URISyntaxException | ParseException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void createGitIgnore(Project project) throws IOException {
        InputStream src = ResourceManager.class.getResourceAsStream("/gitignore.file");
        Files.copy(src, Paths.get(new File(project.getProjectDir().getAbsolutePath() + "/.gitignore").toURI()), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void createWorkflow(Project project) throws IOException {
        File destination = new File(project.getProjectDir().getAbsolutePath() + "/.github/workflows/build.yml");
        destination.mkdirs();

        InputStream src = ResourceManager.class.getResourceAsStream("/workflows/build.yml");
        Files.copy(src, Paths.get(destination.toURI()), StandardCopyOption.REPLACE_EXISTING);
    }
}
