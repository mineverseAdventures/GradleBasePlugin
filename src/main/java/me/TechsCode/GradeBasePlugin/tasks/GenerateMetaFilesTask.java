package me.TechsCode.GradeBasePlugin.tasks;

import me.TechsCode.GradeBasePlugin.Color;
import me.TechsCode.GradeBasePlugin.GradleBasePlugin;
import me.TechsCode.GradeBasePlugin.extensions.MetaExtension;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class GenerateMetaFilesTask extends DefaultTask {

    @TaskAction
    public void generateMetaFiles() {
        GradleBasePlugin.log(Color.BLUE_BRIGHT + "Generating Plugin.yml & Bungee.yml");

        File build = getProject().getBuildDir();
        File resourcesFolder = new File(build.getAbsolutePath() + "/resources/main");
        resourcesFolder.mkdirs();

        try {
            MetaExtension meta = getProject().getExtensions().getByType(MetaExtension.class);
            int buildNumber = getBuildNumber();

            createPluginYml(resourcesFolder, getProject().getName(), meta.version, buildNumber, meta.loadAfter, meta.loadBefore, meta.load);
            createBungeeYml(resourcesFolder, getProject().getName(), meta.version, buildNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createPluginYml(File resourcesFolder, String projectName, String projectVersion, int buildNumber, String loadAfter, String loadBefore, String load) throws IOException {
        File file = new File(resourcesFolder.getAbsolutePath() + "/plugin.yml");
        file.createNewFile();

        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println("name: " + projectName);
        writer.println("version: " + projectVersion);
        writer.println("author: Tech");
        writer.println("website: " + projectName + ".com");
        writer.println("build: " + buildNumber);
        writer.println("main: me.TechsCode." + getProject().getName() + ".base.loader.SpigotLoader");
        writer.println("api-version: 1.13");

        if (loadAfter != null) writer.println("softdepend: " + loadAfter);
        if (loadBefore != null) writer.println("loadbefore: " + loadBefore);
        if (load != null) writer.println("load: " + load);

        writer.close();
    }

    private void createBungeeYml(File resourcesFolder, String projectName, String projectVersion, int buildNumber) throws IOException {
        File file = new File(resourcesFolder.getAbsolutePath() + "/bungee.yml");
        file.createNewFile();

        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println("name: " + projectName);
        writer.println("version: " + projectVersion);
        writer.println("build: " + buildNumber);
        writer.println("main: me.TechsCode." + getProject().getName() + ".base.loader.BungeeLoader");
        writer.println("author: Tech");
        writer.close();
    }

    private int getBuildNumber() {
        String buildNumber = System.getenv("BUILD_NUMBER");

        return buildNumber != null ? Integer.parseInt(buildNumber) : 0;
    }
}
