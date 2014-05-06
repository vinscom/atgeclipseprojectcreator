package com.atg.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ManifestToEclipse {

    static final Logger Log = Logger.getLogger(ManifestToEclipse.class.getName());
    static final String MANIFEST_FILE_NAME = "MANIFEST.MF";
    static final String ATG_HOME_DIR = System.getenv("DYNAMO_ROOT").toLowerCase() + "\\";
    static final String ECLIPSE_CLASS_PATH = "<classpathentry kind=\"lib\" path=\"{0}\"/>\n";
    static final String ECLIPSE_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<classpath>\n"
            + "<classpathentry kind=\"src\" path=\"src\"/>\n"
            + "<classpathentry kind=\"output\" path=\"classes\"/>\n"
            + "<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n"
            + "{0}"
            + "</classpath>";
    static final String ECLIPSE_PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<projectDescription>\n"
            + "	<name>{0}</name>\n"
            + "	<comment></comment>\n"
            + "	<projects>\n"
            + "	</projects>\n"
            + "	<buildSpec>\n"
            + "		<buildCommand>\n"
            + "			<name>org.eclipse.jdt.core.javabuilder</name>\n"
            + "			<arguments>\n"
            + "			</arguments>\n"
            + "		</buildCommand>\n"
            + "	</buildSpec>\n"
            + "	<natures>\n"
            + "		<nature>org.eclipse.jdt.core.javanature</nature>\n"
            + "	</natures>\n"
            + "</projectDescription>";
    private final Map<String, Module> globalModuleRepository = new HashMap<>();
    private boolean isDebug = false;

    public List<Module> findModuleManifestFiles(File pDir, List<Module> pManifestFiles) throws IOException {

        for (File file : pDir.listFiles()) {
            if (file.isDirectory()) {
                findModuleManifestFiles(file, pManifestFiles);
            } else if (MANIFEST_FILE_NAME.equals(file.getName())) {
                pManifestFiles.add(new Module(null, file.getCanonicalPath(), null));
            }
        }

        return pManifestFiles;
    }

    public ManifestToEclipse() {
        isDebug = Configuration.getInstance().isDebug();
    }

    private boolean isStringEmpty(String pSource) {
        return pSource == null || pSource.trim().isEmpty();
    }

    public Module loadModuleNameFromManifestPath(Module pModule) {

        //Module Name
        String moduleDir = System.getenv("DYNAMO_ROOT").toLowerCase() + "\\";
        Pattern moduleDirP = Pattern.compile(moduleDir.replace("\\", "\\\\"), Pattern.CASE_INSENSITIVE);
        Pattern manifPathP = Pattern.compile("\\\\[^\\\\]+\\\\[^\\\\]+$");

        String moduleName = moduleDirP.matcher(pModule.getmManifestFile()).replaceFirst("");
        moduleName = manifPathP.matcher(moduleName).replaceFirst("");
        moduleName = moduleName.replace("\\", ".");

        pModule.setmModuleName(moduleName);

        return pModule;
    }

    public Module loadManifestPathFromModuleName(Module pModule) {
        pModule.setmManifestFile(createManifestPathFromModuleName(pModule.getmModuleName()));
        return pModule;
    }

    public String createManifestPathFromModuleName(String pModuleName) {
        return MessageFormat.format("{0}{1}{2}", ATG_HOME_DIR, pModuleName.replace(".", "\\"), "\\META-INF\\MANIFEST.MF");
    }

    public Module checkModuleInfo(Module pModule) {

        if (!isStringEmpty(pModule.getmManifestFile()) && !isStringEmpty(pModule.getmModuleName())) {
            return pModule;
        }

        if (!isStringEmpty(pModule.getmManifestFile())) {
            loadModuleNameFromManifestPath(pModule);
            return pModule;
        }

        if (!isStringEmpty(pModule.getmModuleName())) {
            loadManifestPathFromModuleName(pModule);
            return pModule;
        }

        return null;
    }

    public Module loadModuleProperties(Module pModule) throws IOException {
        Pattern key = Pattern.compile("^[A-Z][^:]+:.+");
        StringBuilder sb = new StringBuilder();
        Properties prop = new Properties();

        try (BufferedReader br = new BufferedReader(new FileReader(pModule.getmManifestFile()))) {
            while (br.ready()) {
                String line = br.readLine();
                if (key.matcher(line).matches()) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            prop.load(new StringReader(sb.toString()));
        }
        pModule.setmProperties(prop);
        return pModule;
    }

    public Module loadLibraryFiles(Module pModule) throws IOException {

        List<File> result = new ArrayList<>();
        File manifest = new File(pModule.getmManifestFile());

        String manifestPath = manifest.getCanonicalPath();
        String workingDir = manifestPath.replaceFirst("\\\\[^\\\\]+\\\\[^\\\\]+$", "");

        Properties manifestProperties = pModule.getmProperties();
        String libs = manifestProperties.getProperty("ATG-Class-Path");

        if (!isStringEmpty(libs)) {
            for (String file : Arrays.asList(libs.split("\\s+"))) {
                File f = new File(workingDir + "\\" + file);
                if (f.exists()) {
                    result.add(f);
                } else {
                    Log.info("**********File or folder does not exist : " + f.getCanonicalPath());
                }
            }
        }

        pModule.setLibreries(result);

        return pModule;
    }

    private List<String> loadModuleListFromManifestProperty(String pRequiredProperty) {
        List<String> modules = new ArrayList<>();

        List<String> moduleNames = Arrays.asList(pRequiredProperty.split("\\s+"));

        ListIterator<String> itr = moduleNames.listIterator();

        while (itr.hasNext()) {
            String moduleName = itr.next();
            File f = new File(createManifestPathFromModuleName(moduleName));
            if (f.exists()) {
                modules.add(moduleName);
            } else {
                Log.info("++++++++Module could not be resolved to file, could be new line problem, trying to resolve : " + moduleName);
                if (itr.hasNext()) {
                    moduleName = moduleName + itr.next();
                    Log.info("++++++++New module name after resolving : " + moduleName);
                    f = new File(createManifestPathFromModuleName(moduleName));
                    if (f.exists()) {
                        modules.add(moduleName);
                    } else {
                        Log.info("++++++++Could not resolve module : " + moduleName);
                        itr.previous();
                    }
                }
            }
        }

        return modules;
    }

    private Module loadRequiredModules(Module pModule, Map<String, Module> globalModuleRepository) throws IOException {

        String requiredModuleProp = pModule.getmProperties().getProperty("ATG-Required");

        List<Module> requiredModules = new ArrayList<>();

        if (!isStringEmpty(requiredModuleProp)) {
            List<String> moduleNames = loadModuleListFromManifestProperty(requiredModuleProp);
            for (String moduleName : moduleNames) {
                Module m = globalModuleRepository.get(moduleName);
                if (m == null) {
                    loadATGModuleInfo(new Module(moduleName, null, null));
                }
                m = globalModuleRepository.get(moduleName);
                if (m != null) {
                    requiredModules.add(m);
                }
            }
        }
        pModule.setDependency(requiredModules);
        return pModule;
    }

    public boolean isModuleGettingLoaded(Module pModule) {
        Module m = globalModuleRepository.get(pModule.getmModuleName());
        if (m != null && m.isGettingLoaded()) {
            return true;
        }
        return false;
    }

    public boolean isModuleProcessed(Module pModule) {
        Module m = globalModuleRepository.get(pModule.getmModuleName());
        if (m != null && m.isProcessed()) {
            return true;
        }
        return false;
    }

    public Module loadATGModuleInfo(Module pModule) throws IOException {

        if (checkModuleInfo(pModule) == null) {
            return null;
        }

        if (isModuleGettingLoaded(pModule)) {
            return pModule;
        }

        if (isModuleProcessed(pModule)) {
            return pModule;
        }

        if (blackListed(pModule)) {
            return pModule;
        }

        Log.info("======== Loading:" + pModule.getmModuleName());

        pModule.setGettingLoaded(true);

        globalModuleRepository.put(pModule.getmModuleName(), pModule);

        loadModuleProperties(pModule);
        loadLibraryFiles(pModule);
        loadRequiredModules(pModule, globalModuleRepository);

        pModule.setGettingLoaded(false);
        pModule.setProcessed(true);

        Log.info("======== Loaded:" + pModule.getmModuleName());
        return pModule;
    }

    public void process(String pFolder) throws IOException {

        List<Module> maniFiles = findModuleManifestFiles(new File(pFolder), new ArrayList<>());

        for (Module module : maniFiles) {
            loadATGModuleInfo(module);
        }

        for (Module m : maniFiles) {
            Module module = globalModuleRepository.get(m.getmModuleName());
            if (module == null) {
                continue;
            }
            String eclipseClassFile = generatePathForEclipseClasspath(m);
            Log.info("-------Writing .classpath File : " + eclipseClassFile);
            writeFile(generateTextFileForEclipseClasspath(module), eclipseClassFile);
            Log.info("-------Writing .classpath Finished : " + eclipseClassFile);

            String eclipseProject = generatePathForEclipseProject(m);
            Log.info("-------Writing .project File : " + eclipseProject);
            writeFile(generateTextFileForEclipseProject(module), eclipseProject);
            Log.info("-------Writing .project Finished : " + eclipseProject);
        }

        dumpGlobalModuleRepository();

        Log.info("Finsih");
    }

    private String generatePathForEclipseProject(Module m) {
        String workingDir = m.getmManifestFile().replaceFirst("\\\\[^\\\\]+\\\\[^\\\\]+$", "");
        return workingDir + "\\.project";
    }

    private String generateTextFileForEclipseProject(Module module) {
        return MessageFormat.format(ECLIPSE_PROJECT_FILE, module.getmModuleName());
    }

    public String generatePathForEclipseClasspath(Module pModule) {
        String workingDir = pModule.getmManifestFile().replaceFirst("\\\\[^\\\\]+\\\\[^\\\\]+$", "");
        return workingDir + "\\.classpath";
    }

    public String generateTextFileForEclipseClasspath(Module pModule) throws IOException {
        Set<String> s = generateAllClassPathList(pModule);
        return MessageFormat.format(ECLIPSE_FILE, generateEclipseClassPath(s));

    }

    public String generateEclipseClassPath(Set<String> pClasses) {
        StringBuilder sb = new StringBuilder();
        for (String file : pClasses) {
            sb.append(MessageFormat.format(ECLIPSE_CLASS_PATH, file));
        }
        return sb.toString();
    }

    public Set<String> generateAllClassPathList(Module module) throws IOException {
        List<File> classfiles = generateAllClassFileList(module, 0);
        Set<String> uniquefiles = new HashSet<>();

        for (File f : classfiles) {
            uniquefiles.add(f.getCanonicalPath());
        }

        return uniquefiles;
    }

    public void writeFile(String pData, String pPath) throws FileNotFoundException {
        File f = new File(pPath);
        if (f.exists()) {
            f.delete();
        }
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.write(pData);
        }
    }

    public List<File> generateAllClassFileList(Module module, int pLevel) {

        if (pLevel > 10) {
            return Collections.emptyList();
        }

        List<File> classfiles = new ArrayList<>(module.getLibreries());

        for (Module mod : module.getDependency()) {
            classfiles.addAll(generateAllClassFileList(mod, ++pLevel));
        }

        return classfiles;
    }

    private boolean blackListed(Module pModule) {

        String filter = Configuration.getInstance().getFilter();

        if (!isStringEmpty(filter)) {
            String[] blackListModule = filter.split(",");

            for (String black : blackListModule) {
                if (pModule.getmModuleName() != null && pModule.getmModuleName().contains(black)) {
                    return true;
                }
            }

        }

        return false;
    }

    public void dumpGlobalModuleRepository() {

        String dumpFilePath = Configuration.getInstance().getDump();

        if (isStringEmpty(dumpFilePath)) {
            return;
        }

        try (PrintWriter pw = new PrintWriter(new File(dumpFilePath))) {
            for (Map.Entry<String, Module> entry : globalModuleRepository.entrySet()) {
                pw.write(entry.getValue().toString());
            }
        } catch (FileNotFoundException ex) {
            Log.log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            Configuration.getInstance().init(args);
        }
        (new ManifestToEclipse()).process(Configuration.getInstance().getSourceDirectory());
    }

}
