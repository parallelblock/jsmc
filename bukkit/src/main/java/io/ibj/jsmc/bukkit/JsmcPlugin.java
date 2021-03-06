package io.ibj.jsmc.bukkit;

import io.ibj.jsmc.api.DependencyManager;
import io.ibj.jsmc.core.BasicDependencyManager;
import io.ibj.jsmc.core.resolvers.FileSystemResolver;
import io.ibj.jsmc.core.resolvers.ModuleResolver;
import io.ibj.jsmc.core.resolvers.SystemDependencyResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// todo - configurable default folder. May mean forcing instantiation of resolvers at 'onEnable'

/**
 * @author Joseph Hirschfeld (Ichbinjoe) [joe@ibj.io]
 * @since 9/11/16
 */
public class JsmcPlugin extends JavaPlugin {

    private static final String ERROR_HEADER = "=============================================\n";

    private FileSystemResolver fileSystemResolver;
    private SystemDependencyResolver<Path> systemDependencyResolver;
    private SystemDependencyResolver<Path> addOnDependencyResolver;
    public ModuleResolver moduleResolver;
    public BasicDependencyManager<Path> dependencyManager;

    public JsmcPlugin() {
        systemDependencyResolver = new SystemDependencyResolver<>(null);
    }

    @Override
    public void onDisable() {
        if (dependencyManager == null)
            return;
        try {
            for (DependencyManager.Entry e : dependencyManager.getLoadedModules())
                dependencyManager.unload(e);

        } catch (Exception e) {
            String msg = "" +
                    ERROR_HEADER +
                    "A severe exception has occurred! jsmc may not have shut down correctly.\n" +
                    "jsmc was unable to unload all of it's dependencies safely. This is usually\n" +
                    "not jsmc's fault, but instead a module on the system unable to shut down\n" +
                    "cleanly.\n" +
                    ERROR_HEADER;
            getLogger().log(Level.SEVERE, msg, e);
        }
    }

    @Override
    public void onEnable() {
        YamlConfiguration c;
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists())
                saveResource("config.yml", false);
            c = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            String msg = "" +
                    ERROR_HEADER +
                    "A severe exception has occurred! jsmc will not be able to load!\n" +
                    "jsmc was unable to read off/save a new config.yml in the jsmc\n" +
                    "plugin folder. This is usually due to file permissions or a\n" +
                    "malformed yaml.\n" +
                    "jsmc will now disable!\n" +
                    ERROR_HEADER;
            getLogger().log(Level.SEVERE, msg, e);
            setEnabled(false);
            return;
        }

        String loaderModuleName = c.getString("loader");
        if (loaderModuleName == null) {
            getLogger().info("loader null in config.yml, using mc-bukkit-default-loader...");
            loaderModuleName = "mc-bukkit-default-loader";
        }

        Path rootPath = new File(c.getString("root", "./")).toPath();
        Path node_modules = rootPath.resolve("node_modules");

        if (!Files.exists(node_modules)) {
            try {
                seedInstall(node_modules);
            } catch (IOException e) {
                String msg = "" +
                        ERROR_HEADER +
                        "A severe exception has occurred! jsmc will not be able to load!\n" +
                        "jsmc could not find the node_modules directory, and failed to create it!\n" +
                        "jsmc will now disable!\n" +
                        ERROR_HEADER;
                getLogger().log(Level.SEVERE, msg, e);
                setEnabled(false);
                return;
            }
        } else {
            try {
                if (!Files.isDirectory(node_modules) && !(Files.isSymbolicLink(node_modules) && Files.isDirectory(node_modules.toRealPath()))) {
                    String msg = "" +
                            ERROR_HEADER +
                            "A severe exception has occurred! jsmc will not be able to load!\n" +
                            "node_modules is not a directory or a symlink to a directory!\n" +
                            "jsmc will now disable!\n" +
                            ERROR_HEADER;
                    getLogger().log(Level.SEVERE, msg);
                    setEnabled(false);
                    return;
                }
            } catch (IOException e) {
                String msg = "" +
                        ERROR_HEADER +
                        "A severe exception has occurred! jsmc will not be able to load!\n" +
                        "node_modules' symlink could not be resolved into a working path!\n" +
                        "jsmc will now disable!\n" +
                        ERROR_HEADER;
                getLogger().log(Level.SEVERE, msg, e);
                setEnabled(false);
                e.printStackTrace();
                return;
            }
        }


        fileSystemResolver = new FileSystemResolver(() -> moduleResolver, rootPath);
        addOnDependencyResolver = new SystemDependencyResolver<>(systemDependencyResolver);
        moduleResolver = new ModuleResolver(rootPath, fileSystemResolver, addOnDependencyResolver);
        dependencyManager = new BasicDependencyManager<>(moduleResolver, rootPath);

        try {
            dependencyManager.load(loaderModuleName); // this should then sequentially load everything else
        } catch (Exception e) {
            String msg = "" +
                    ERROR_HEADER +
                    "A severe exception has occurred! jsmc will not be able to load!\n" +
                    "jsmc was unable to start the dependency management module:\n" + loaderModuleName + "\n" +
                    "jsmc will now disable!\n" +
                    ERROR_HEADER;
            getLogger().log(Level.SEVERE, msg, e);
            setEnabled(false);
        }
    }

    public void seedInstall(Path node_modules) throws IOException {
        getLogger().info("Bootstrapping node_modules folder onto server root");

        Files.createDirectory(node_modules);

        try(ZipInputStream zis = new ZipInputStream(getResource("package.zip"))) {
            ZipEntry e;
            while((e = zis.getNextEntry()) != null) {
                Path child = node_modules.resolve(e.getName());
                if (e.isDirectory())
                    Files.createDirectory(child);
                else
                    Files.copy(zis, child);
            }
        }
    }
}
