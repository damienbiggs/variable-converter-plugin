package org.jenkinsci.plugins.variableconverter;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.EnvironmentContributingAction;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Used to convert a source variable value to the destination value via reg ex
 */
public class VariableConverterBuilder extends Builder {

    private final String sourceVariable;

    private final String sourceVariablePattern;

    private final String destinationVariable;

    private final String destinationVariablePattern;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public VariableConverterBuilder(String sourceVariable, String sourceVariablePattern, String destinationVariable,
                                    String destinationVariablePattern) {
        this.sourceVariable = sourceVariable;
        this.sourceVariablePattern = sourceVariablePattern;
        this.destinationVariable = destinationVariable;
        this.destinationVariablePattern = destinationVariablePattern;
    }

    public String getSourceVariable() {
        return sourceVariable;
    }

    public String getSourceVariablePattern() {
        return sourceVariablePattern;
    }

    public String getDestinationVariable() {
        return destinationVariable;
    }

    public String getDestinationVariablePattern() {
        return destinationVariablePattern;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException,
            InterruptedException {
        EnvVars env = build.getEnvironment(listener);
        PrintStream logger = listener.getLogger();


        String expandedSourceVarName = env.expand(sourceVariable);
        String expandedSourceVarPatternName = env.expand(sourceVariablePattern);
        String expandedDestinationVarName = env.expand(destinationVariable);
        String expandedDestinationVarPatternName = env.expand(destinationVariablePattern);

        if (StringUtils.isBlank(expandedDestinationVarName)) {
            logger.println("Source variable name should not be blank");
            return false;
        }

        if (StringUtils.isBlank(expandedSourceVarPatternName)) {
            logger.println("Source variable pattern should not be blank");
            return false;
        }

        Matcher matcher = Pattern.compile(expandedSourceVarPatternName).matcher(expandedSourceVarName);
        if (!matcher.find()) {
            logger.println(format("No match of reg ex value %s for variable value %s",
                    expandedSourceVarPatternName, expandedSourceVarName));
            return false;
        }

        if (StringUtils.isBlank(expandedDestinationVarName)) {
            logger.println("Destination variable name should not be blank");
            return false;
        }

        if (StringUtils.isBlank(expandedDestinationVarPatternName)) {
            logger.println("Destination variable pattern should not be blank");
            return false;
        }

        String outputValue = expandedDestinationVarPatternName;

        for (int i = 1; i <= matcher.groupCount(); i ++) {
            outputValue = outputValue.replace("{" + i + "}", matcher.group(i));
        }

        logger.println(format("Adding environment variable named %s with value of %s",
                expandedDestinationVarName, outputValue));

        EnvVariableAction envVariableAction = new EnvVariableAction();
        envVariableAction.add(expandedDestinationVarName, outputValue);
        build.addAction(envVariableAction);

        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link VariableConverterBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckSourceVariable(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a sourceVariable");
            if (value.length() < 2)
                return FormValidation.warning("Isn't the sourceVariable too short?");
            return FormValidation.ok();
        }

        public FormValidation doCheckSourceVariablePattern(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a sourceVariablePattern");
            if (value.length() < 2)
                return FormValidation.warning("Isn't the sourceVariablePattern too short?");
            return FormValidation.ok();
        }

        public FormValidation doCheckDestinationVariable(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a destinationVariable");
            if (value.length() < 2)
                return FormValidation.warning("Isn't the destinationVariable too short?");
            return FormValidation.ok();
        }

        public FormValidation doCheckDestinationVariablePattern(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a destinationVariablePattern");
            if (value.length() < 2)
                return FormValidation.warning("Isn't the destinationVariablePattern too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable sourceVariable is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Variable Converter";
        }
    }

    private static class EnvVariableAction implements EnvironmentContributingAction {
        private Map<String,String> data = new HashMap<String,String>();

        private void add(String key, String val) {
            if (data==null) return;
            data.put(key, val);
        }

        public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
            if (data!=null) env.putAll(data);
        }

        public String getIconFileName() { return null; }
        public String getDisplayName() { return null; }
        public String getUrlName() { return null; }

        public String getValue(String name) {
            return data != null ? data.get(name) : null;
        }

        public Set<String> getNames() {
            return data != null ? data.keySet() : new HashSet<String>();
        }
    }
}

