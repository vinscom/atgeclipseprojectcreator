package com.atg.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Module {

    private String mModuleName;
    private String mManifestFile;
    private Properties mProperties;
    private List<File> Libreries;
    private List<Module> Dependency;
    private boolean processed;
    private boolean gettingLoaded;

    public boolean isGettingLoaded() {
        return gettingLoaded;
    }

    public void setGettingLoaded(boolean gettingLoaded) {
        this.gettingLoaded = gettingLoaded;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Module(String mModuleName, String mManifestFile, Properties mProperties) {
        this.mModuleName = mModuleName;
        this.mManifestFile = mManifestFile;
        this.mProperties = mProperties;
    }

    public String getmModuleName() {
        return mModuleName;
    }

    public void setmModuleName(String mModuleName) {
        this.mModuleName = mModuleName;
    }

    public String getmManifestFile() {
        return mManifestFile;
    }

    public void setmManifestFile(String mManifestFile) {
        this.mManifestFile = mManifestFile;
    }

    public Properties getmProperties() {
        return mProperties;
    }

    public void setmProperties(Properties mProperties) {
        this.mProperties = mProperties;
    }

    public List<File> getLibreries() {
        if (Libreries == null) {
            Libreries = new ArrayList<>();
        }
        return Libreries;
    }

    public void setLibreries(List<File> Libreries) {
        this.Libreries = Libreries;
    }

    public List<Module> getDependency() {
        if (Dependency == null) {
            Dependency = new ArrayList<>();
        }
        return Dependency;
    }

    public void setDependency(List<Module> Dependency) {
        this.Dependency = Dependency;
    }

    @Override
    public boolean equals(Object obj) {
        Module m = (Module) obj;
        if (getmManifestFile() != null && getmManifestFile().equals(m.getmManifestFile())) {
            return true;
        }
        return getmModuleName() != null && getmModuleName().equals(m.getmModuleName());
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        try {
            sb.append("--------------------------------------\n");
            sb.append("Module Name : " + getmModuleName() + "\n");
            sb.append("Manifest File : " + getmManifestFile() + "\n");
            sb.append("Modules : \n");
            for (Module mod : getDependency()) {
                sb.append(mod.getmModuleName()).append("\n");
            }
            sb.append("Lib : \n");
            for (File file : getLibreries()) {
                sb.append(file.getCanonicalPath()).append("\n");
            }
            sb.append("--------------------------------------\n");
        } catch (IOException ex) {
            Logger.getLogger(Module.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

}
