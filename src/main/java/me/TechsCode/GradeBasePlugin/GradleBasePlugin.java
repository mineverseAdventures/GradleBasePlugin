package me.TechsCode.GradeBasePlugin;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import me.TechsCode.GradeBasePlugin.extensions.MetaExtension;
import me.TechsCode.GradeBasePlugin.tasks.GenerateMetaFilesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GradleBasePlugin implements Plugin<Project> {

    private static final String[] repositories = new String[]{
            "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
            "https://oss.sonatype.org/content/repositories/snapshots",
            "https://jitpack.io",
            "https://repo.codemc.io/repository/maven-public/"
    };

    private static final String[] dependencies = new String[]{
            "compileOnly#org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT",
            "compileOnly#org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT",
            "compileOnly#net.md-5:bungeecord-api:1.12-SNAPSHOT"
    };

    private static final String[] relocations = new String[]{
            "me.TechsCode.base#me.TechsCode.PROJECT_NAME.base",
            "me.TechsCode.tpl#me.TechsCode.PROJECT_NAME.tpl",
            "me.TechsCode.dependencies#me.TechsCode.PROJECT_NAME.dependencies"
    };

    private MetaExtension meta;
    private String githubToken;

    @Override
    public void apply(Project project) {
        DeploymentFile deploymentFile = new DeploymentFile(project);

        this.meta = project.getExtensions().create("meta", MetaExtension.class);
        this.githubToken = System.getenv("GITHUB_TOKEN");

        try {
            ResourceManager.createGitIgnore(project);
            ResourceManager.createWorkflow(project);
        } catch (IOException ignored){
            ignored.printStackTrace();
        }

        // Registering GradleBasePlugin tasks
        project.getTasks().create("generateMetaFiles", GenerateMetaFilesTask.class);

        // Setting up Shadow Plugin
        project.getPlugins().apply("com.github.johnrengelman.shadow");
        getShadowJar(project).getArchiveFileName().set(project.getName()+".jar");
        getShadowJar(project).setProperty("destinationDir", project.file(deploymentFile.getLocalOutputPath()));
        getShadowJar(project).dependsOn("generateMetaFiles");

        project.getTasks().getByName("build").dependsOn("shadowJar");
        project.getTasks().getByName("build").doLast((task) -> uploadToRemotes(task, deploymentFile));

        // Add onProjectEvaluation hook
        project.afterEvaluate(this::onProjectEvaluation);
    }

    private void onProjectEvaluation(Project project){
        if(meta.validate()) return;

        log(Color.BLUE_BOLD_BRIGHT + "Configuring Gradle Project - Build Settings...");
        log();
        log("Project Info:");
        log("Plugin: " + project.getName() + " on Version: " + meta.version);
        log();

        if(!meta.baseVersion.equalsIgnoreCase("none")) {
            if(ResourceManager.loadBasePlugin(project, githubToken, meta.baseVersion)) {
                log("Successfully retrieved BasePlugin.jar from Github...");
                project.getDependencies().add("implementation", project.files("libs/BasePlugin.jar"));
            } else {
                log(Color.RED+"Could not retrieve BasePlugin.jar from Github... Using older build if available");
                log(Color.RED+"Make sure that you have set the GITHUB_TOKEN environment variable that has access to the BasePlugin repository");
            }
        }

        // Setting properties
        project.setProperty("version", meta.version);
        project.setProperty("sourceCompatibility", "1.8");
        project.setProperty("targetCompatibility", "1.8");

        // Setting up repositories
        project.getRepositories().jcenter();
        project.getRepositories().mavenLocal();
        project.getRepositories().mavenCentral();
        Arrays.stream(repositories).forEach(url -> project.getRepositories().maven((maven) -> maven.setUrl(url)));

        // Setting up dependencies
        Arrays.stream(dependencies).map(entry -> entry.split("#")).forEach(confAndUrl -> project.getDependencies().add(confAndUrl[0], confAndUrl[1]));

        // Setting up relocations
        Arrays.stream(relocations).map(entry -> entry.split("#")).forEach(fromTo -> getShadowJar(project).relocate(fromTo[0], fromTo[1].replace("PROJECT_NAME", project.getName())));
    }

    /* After the build prcoess is completed, the file will be uploaded to all remotes */
    private void uploadToRemotes(Task buildTask, DeploymentFile deploymentFile){
        File file = new File(deploymentFile.getLocalOutputPath()+"/"+buildTask.getProject().getName()+".jar");

        for(DeploymentFile.Remote all : deploymentFile.getRemotes()){
            if(all.isEnabled()){
                all.uploadFile(file);
            }
        }
    }

    private ShadowJar getShadowJar(Project project){
        return (ShadowJar) project.getTasks().getByName("shadowJar");
    }

    public static void log(String message){
        System.out.println(Color.RESET + message + Color.RESET);
    }

    public static void log(){
        System.out.println();
    }
}
