/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.cli.parsing;

import grails.util.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of the {@link CommandLine} interface.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultCommandLine implements CommandLine {

    Properties systemProperties = new Properties();
    Map<String, Object> undeclaredOptions = new HashMap<String, Object>();
    Map<String, SpecifiedOption> declaredOptions = new HashMap<String, SpecifiedOption>();
    List<String> javacNonStandardOptions = new ArrayList<String>();
    List<String> remainingArgs = new ArrayList<String>();
    private String environment;
    private String commandName;
    
    public void addDeclaredOption(String name, Option option) {
        addDeclaredOption(name, option, Boolean.TRUE);
    }

    public void addUndeclaredOption(String option) {
        undeclaredOptions.put(option, Boolean.TRUE);
    }

    public void addUndeclaredOption(String option, Object value) {
        undeclaredOptions.put(option, value);
    }

    public void addDeclaredOption(String name, Option option, Object value) {
        SpecifiedOption so = new SpecifiedOption();
        so.option = option;
        so.value = value;

        declaredOptions.put(name, so);
    }
    
    public void addJavacNonStandardOption(String option) {
        javacNonStandardOptions.add(option);
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
        System.setProperty(Environment.KEY, environment);
    }

    public void setCommand(String name) {
        commandName = name;
    }

    public String getEnvironment() {
        boolean useDefaultEnv = environment == null;
        String env;
        if (useDefaultEnv && commandName != null) {
            env = lookupEnvironmentForCommand();
        }
        else {
            String fallbackEnv = System.getProperty(Environment.KEY) != null ? System.getProperty(Environment.KEY) : Environment.DEVELOPMENT.getName();
            env = environment != null ? environment : fallbackEnv;
        }

        System.setProperty(Environment.KEY, env);
        System.setProperty(Environment.DEFAULT, String.valueOf(useDefaultEnv));

        return env;
    }

    public String lookupEnvironmentForCommand() {
        String fallbackEnv = System.getProperty(Environment.KEY) != null ? System.getProperty(Environment.KEY) : Environment.DEVELOPMENT.getName();
        String env = CommandLineParser.DEFAULT_ENVS.get(commandName);
        return env == null ? fallbackEnv : env;
    }

    public boolean isEnvironmentSet() {
        return environment != null;
    }

    public void setCommandName(String cmd) {
        if (REFRESH_DEPENDENCIES_ARGUMENT.equals(cmd)) {
            addUndeclaredOption(REFRESH_DEPENDENCIES_ARGUMENT);
        }
        commandName = cmd;
    }

    public String getCommandName() {
        return commandName;
    }

    public void addRemainingArg(String arg) {
        remainingArgs.add(arg);
    }

    public List<String> getRemainingArgs() {
        return remainingArgs;
    }

    public String[] getRemainingArgsArray() {
        return remainingArgs.toArray(new String[remainingArgs.size()]);
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }

    public boolean hasOption(String name) {
        return declaredOptions.containsKey(name) || undeclaredOptions.containsKey(name);
    }
    
    public boolean hasJavacNonStandardOptions() {
        return javacNonStandardOptions.size() > 0;
    }
    
    public List<String> getJavacNonStandardOptions() {
        return javacNonStandardOptions;
    }

    public Object optionValue(String name) {
        if (declaredOptions.containsKey(name)) {
            SpecifiedOption specifiedOption = declaredOptions.get(name);
            return specifiedOption.value;
        }
        if (undeclaredOptions.containsKey(name)) {
            return undeclaredOptions.get(name);
        }
        return null;
    }

    public String getRemainingArgsString() {
        return remainingArgsToString(" ");
    }

    public String getRemainingArgsLineSeparated() {
        return remainingArgsToString("\n");
    }

    private String remainingArgsToString(String separator) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        List<String> args = new ArrayList<String>(remainingArgs);
        for (Map.Entry<String, Object> entry : undeclaredOptions.entrySet()) {
            if (entry.getValue() instanceof Boolean && ((Boolean)entry.getValue())) {
                args.add('-' + entry.getKey());
            }
            else {
                args.add('-' + entry.getKey() + '=' + entry.getValue());
            }
        }
        for (String arg : args) {
            sb.append(sep).append(arg);
            sep = separator;
        }
        return sb.toString();
    }

    public Map<String, Object> getUndeclaredOptions() {
        return undeclaredOptions;
    }

    public void addSystemProperty(String name, String value) {
        if (Environment.KEY.equals(name)) {
            setEnvironment(value);
        }
        systemProperties.put(name, value);
    }

    public static class SpecifiedOption {
        private Option option;
        private Object value;

        public Option getOption() {
            return option;
        }

        public Object getValue() {
            return value;
        }
    }
}
