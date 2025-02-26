package me.TechsCode.GradeBasePlugin;

import me.TechsCode.GradeBasePlugin.extensions.MetaExtension;
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
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ResourceManager {
    private MetaExtension meta;

    public static boolean loadBasePlugin(Project project, String githubToken, String version) throws IOException {

        if (!isTokenValid(githubToken)) {
            System.out.println("Invalid token. Please check your Bearer token.");
            return false;
        }else{
            System.out.println("Valid token.");
        }
        if (githubToken == null) return false;

        File libraryFolder = new File(project.getProjectDir().getAbsolutePath() + "/libs");
        libraryFolder.mkdirs();

        File libraryFile = new File(libraryFolder.getAbsolutePath() + "/BasePlugin.jar");
        libraryFile.delete();

        System.out.println("Version: " + version);
        
        String RETRIEVE_RELEASES = "https://api.github.com/repos/mineverseAdventures/BasePlugin/releases/tags/" + version;

        URL url = new URL(RETRIEVE_RELEASES);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + githubToken);
        connection.setRequestMethod("GET");

        // Read entire JSON response
        String json;
        try (InputStream stream = connection.getInputStream()) {
            json = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        System.out.println("JSON Response: " + json);

        JSONParser parser = new JSONParser();
        JSONObject root = null;
        try {
            root = (JSONObject) parser.parse(json);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JSONArray assets = (JSONArray) root.get("assets");
        if (assets == null || assets.isEmpty()) {
            System.out.println("No assets found in the release.");
            return false;
        }

        JSONObject asset = (JSONObject) assets.get(0);
        String assetUrl = (String) asset.get("url");
        System.out.println("Downloading from: " + assetUrl);

        URL url2 = new URL(assetUrl);
        HttpsURLConnection connection2 = (HttpsURLConnection) url2.openConnection();
        connection2.setRequestProperty("Accept", "application/octet-stream");
        connection2.setRequestProperty("Authorization", "Bearer " + githubToken);
        connection2.setRequestMethod("GET");

        // Use connection2's input stream for file download
        try (ReadableByteChannel uChannel = Channels.newChannel(connection2.getInputStream());
             FileOutputStream foStream = new FileOutputStream(libraryFile);
             FileChannel fChannel = foStream.getChannel()) {
            fChannel.transferFrom(uChannel, 0, Long.MAX_VALUE);
        }

        System.out.println("Download complete: " + libraryFile.getAbsolutePath());
        return true;
    }
    private static boolean isTokenValid(String token) {
        try {
            URL url = new URL("https://api.github.com/user"); // Endpoint to check the token
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code Works: " + responseCode);
            return responseCode == HttpsURLConnection.HTTP_OK; // Return true if the token is valid
        } catch (IOException e) {
            e.printStackTrace(); // Log the exception if needed
            return false; // Assume invalid if an exception occurs
        }
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
