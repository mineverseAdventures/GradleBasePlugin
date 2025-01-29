package me.TechsCode.GradeBasePlugin;

import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.internal.impldep.com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class DeploymentFile {

    private static JSONObject root;

    public DeploymentFile(Project project){
        File global = new File(System.getProperty("user.home")+"/deployment.json");
        File local = new File(project.getProjectDir().getAbsolutePath()+"/deployment.json");

        File file = local;

        if(global.exists() && !local.exists()){
            file = global;
        }

        if(!file.exists()){
            try {
                InputStream src = ResourceManager.class.getResourceAsStream("/deployment.json");
                Files.copy(src, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            JSONParser jsonParser = new JSONParser();
            root = (JSONObject) jsonParser.parse(json);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }


    public String getLocalOutputPath(){
        JSONObject local = (JSONObject) root.get("local");
        return (String) local.get("path");
    }

    public List<Remote> getRemotes(){
        List<Remote> remotes = new ArrayList<>();

        for(Object object : (JSONArray) root.get("remotes")){
            JSONObject remote = (JSONObject) object;

            remotes.add(new Remote(remote));
        }

        return remotes;
    }

    public class Remote {

        private boolean enabled;
        private String hostname, username, password, path;
        private long port;

        public Remote(JSONObject jsonObject) {
            this.enabled = (boolean) jsonObject.get("enabled");
            this.hostname = (String) jsonObject.get("hostname");
            this.port = jsonObject.containsKey("port") ? (long) jsonObject.get("port") : 22;
            this.username = (String) jsonObject.get("username");
            this.password = (String) jsonObject.get("password");
            this.path = (String) jsonObject.get("path");
        }

        public void uploadFile(File file){
            try {
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");

                JSch jsch = new JSch();
                Session session = jsch.getSession(username, hostname, Math.round(port));
                session.setPassword(password);
                session.setConfig(config);
                session.connect();
                ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
                sftp.connect();
                sftp.cd(path);
                sftp.put(new FileInputStream(file), file.getName(), ChannelSftp.OVERWRITE);
                sftp.exit();
                session.disconnect();
            } catch (JSchException | SftpException | FileNotFoundException e) {
                GradleBasePlugin.log("§7Couldnt upload file to remote '"+hostname+"':");
                GradleBasePlugin.log(e.getMessage());
            }
        }

        public boolean isEnabled() {
            return enabled;
        }


    }
}
