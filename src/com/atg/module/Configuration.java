package com.atg.module;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Configuration {

    private static Configuration self = new Configuration();

    private String mSourceDirectory;
    private String mFilter;
    private String mDump;
    private Boolean mDebug;

    private Configuration() {
    }

    public static Configuration getInstance() {
        return self;
    }

    public void init(String[] pArgs) {
        Pattern p = Pattern.compile("^([^=]+)=(.*$)");
        for (String arg : pArgs) {
            Matcher m = p.matcher(arg);
            if (m.matches()) {
                String key = m.group(1);
                String value = m.group(2);
                setValue(key, value);
            }
        }
    }

    public void setValue(String key, String value) {
        try {
            Method[] methods = Configuration.class.getMethods();
            Predicate<String> p = Pattern.compile("^set" + key + "$", Pattern.CASE_INSENSITIVE).asPredicate();

            for (Method method : methods) {
                if (p.test(method.getName())) {
                    Parameter[] param = method.getParameters();
                    if (param.length == 1) {
                        Constructor<?> cont = param[0].getType().getConstructor(String.class);
                        Class c = param[0].getType();
                        Object obj = cont.newInstance(value);
                        method.invoke(self, obj);
                    }
                    break;
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getSourceDirectory() {
        return mSourceDirectory;
    }

    public void setSourceDirectory(String pSourceDirectory) {
        this.mSourceDirectory = pSourceDirectory;
    }

    public String getFilter() {
        return mFilter;
    }

    public void setFilter(String pFilter) {
        this.mFilter = pFilter;
    }

    public String getDump() {
        return mDump;
    }

    public void setDump(String pDump) {
        this.mDump = pDump;
    }

    public Boolean isDebug() {
        return mDebug;
    }

    public void setDebug(Boolean pDebug) {
        this.mDebug = pDebug;
    }

    public static void main(String[] args) {
        Configuration c = Configuration.getInstance();
        c.init(new String[]{"debug=true"});
    }
}
