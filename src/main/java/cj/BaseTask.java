package cj;

import cj.fs.FSUtils;
import cj.ocp.CapabilityNotFoundException;
import cj.shell.CheckShellCommandExistsTask;
import cj.shell.ShellInput;
import cj.shell.ShellTask;
import cj.spi.Task;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cj.CJInput.dryRun;
import static cj.CJInput.fixTask;
import static cj.Errors.Type;
import static cj.Errors.Type.Message;
import static cj.shell.ShellInput.*;
@Dependent
public class BaseTask implements Task {
    @Inject
    transient Tasks tasks;

    @Inject
    Configuration config;

    @Inject
    Instance<RetryTask> retry;

    @Inject
    Instance<ShellTask> shellInstance;


    //TODO: Use null instead of Optional in fields
    LocalDateTime startTime = null;
    LocalDateTime endTime = null;

    Map<Input, Object> inputs = new HashMap<>();
    Map<Output, Object> outputs = new HashMap<>();
    Map<Errors, Object> errors = new HashMap<>();

    /* Submits a delegate task for execution */
    public Task submit(Task delegate){
        delegate.getInputs().putAll(getInputs());
        return tasks.submit(delegate);
    }

    protected Task submitInstance(Instance<? extends Task> delegate, Input input, Object value){
        return tasks.submit(delegate.get().withInput(input, value));
    }
    protected Task submit(Task delegate, Input input, Object value){
        return tasks.submit(delegate.withInput(input, value));
    }


    /* Task Interface Methods */
    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(LocalDateTime localDateTime) {
        startTime = localDateTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(LocalDateTime localDateTime) {
        this.endTime = localDateTime;
    }

    /* Input, Output and Errors */

    public Optional<Object> output(Output key) {
        var result = Optional.ofNullable(getOutputs().get(key));
        if (result.isEmpty()){
            for (Task dep: getDependencies()){
                result = dep.output(key);
                if (result.isPresent()){
                    return result;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> outputList(Output key, Class<T> valueClass) {
        var output = output(key);
        if (output.isEmpty()){
            return List.of();
        }else{
            var outputValue = output.get();
            if (outputValue instanceof List<?> outputList){
                return (List<T>) outputList;
            }else {
                throw new RuntimeException( "Output " + key + " is not a List<"+valueClass.getName()+">");
            }
        }
    }
    @SuppressWarnings("all")
    public <T> List<T> inputList(Input key, Class<T> valueClass) {
        var in = input(key);
        if (in.isPresent()){
            var val = in.get();
            if (val instanceof List<?> vals){
                @SuppressWarnings("unchecked")
                var result = (List<T>) vals;
                return result;
            } else {
                throw new IllegalArgumentException("Input " + key + " is not a list");
            }
        }else
            return List.of();
    }


    @Override
    public Optional<String> outputString(Output key) {
        return output(key)
                .map(Object::toString);
    }

    @Override
    public Map<Output, Object> getOutputs() {
        return outputs;
    }

    @Override
    public Map<Errors, Object> getErrors() {
        return errors;
    }

    protected void success(Output key, Object value){
        output(key, value);
    }

    protected void success(){
        trace("Task success(): {}", this);
    }

    /* Logging Shortcuts */
    protected void info(String message, Object... args){
        logger().info(fmt(message), args);
    }
    protected void trace(String message, Object... args){
        logger().trace(fmt(message), args);
    }
    protected void debug(String message, Object... args){
        logger().debug(fmt(message), args);
    }

    protected void error(String message, Object... args){
        logger().error(fmt(message), args);
    }

    protected RuntimeException fail(String message, Object... args) {
        var msg = fmt(message).formatted(args);
        error(msg);
        getErrors().put(Message ,msg);
        return new TaskFailedException(msg);
    }

    private String fmt(String message) {
        var context = getContextString();
        var separator = context.isEmpty() ? "" : getContextSeparator();
        return context + separator + message;
    }

    protected String getContextString() {
        return "";
    }

    private String getContextSeparator() {
        return " || ";
    }

    protected RuntimeException fail(Exception ex) {
        logger().error(ex.getMessage(), ex);
        if( Configuration.PRINT_STACK_TRACE){
            ex.printStackTrace();
        }
        getErrors().put(Type.Exception , ex);
        return new RuntimeException(ex);
    }

    protected void warn(String message, Object... args) {
        logger().warn(fmt(message), args);
    }
    protected void warn(Exception ex, String message, Object... args) {
        warn(ex.getMessage());
        warn(fmt(message), args);
    }


    protected Logger logger() {
        return LoggerFactory.getLogger(getLoggerName());
    }

    @Override
    public String toString() {
        return  "%s ".formatted(
                getSimpleName());

    }

    public Configuration getConfig() {
        return config;
    }


    @Override
    public Map<Input, Object> getInputs(){
        return inputs;
    }

    public String expectInputString(Input key){
        return inputString(key).orElseThrow();
    }

    public String getInputString(Input key){
        return getInputString(key, null);
    }

    public String getInputString(Input key, String defaultValue){
        return inputAs(key, String.class).orElse(defaultValue);
    }

    public Task withInput(Input key, Object value) {
        inputs.put(key, value);
        return this;
    }

    public Task withInputs(Map<Input, Object> inputs) {
        this.inputs = inputs;
        return this;
    }

    public Object withInput(Input key) {
        return inputs.get(key);
    }

    public Optional<Object> input(Input key){
        if (key == null) return Optional.empty();
        var value = inputs.get(key);
        if (value == null) {
            value = cfgInputString(key);
        }
        if (value == null) {
            value = inputss.getFromDefault(key);
        }
        return Optional.ofNullable(value);
    }

    //TODO move to task inputs
    @Inject
    Inputs inputss;
    public Object cfgInputString(Input key){
        return inputss.getFromConfig(key);
    }

    @SuppressWarnings("all")
    public <T> Optional<T> inputAs(Input key, Class<T> clazz){
        @SuppressWarnings("unchecked")
        var result = (Optional<T>) input(key);
        return result;
    }


    public <T> Optional<T> outputAs(Output key, Class<T> clazz){
        @SuppressWarnings("unchecked")
        var result = (Optional<T>) Optional.ofNullable(outputs.get(key));
        return result;
    }

    @SuppressWarnings("all")
    public <T> T getInput(Input key, Class<T> inputClass){
        @SuppressWarnings("unchecked")
        var result = input(key);
        if (result.isEmpty()){
            throw new IllegalArgumentException("Failed to resolve expected input "+ key);
        }
        return (T) result.get();
    }

    public String matchMark(boolean match){
        return match ? "X" : "O";
    }

    public List<Task> delegateAll(Task... tasks) {
        return Stream.of(tasks)
                .map(t -> t.withInputs(getInputs()))
                .toList();
    }

    public Task delegate(Task task){
        return task.withInputs(getInputs());
    }

    public void submitAll(Task... delegates){
        Stream.of(delegates).forEach(this::submit);
    }
    public void submitAll(List<Task> delegates){
        delegates.forEach(this::submit);
    }

    public Optional<String> inputString(Input key){
        return input(key).map(Object::toString);
    }

    public Object output(Output key, Object value){
        if (value instanceof Optional<?> opt){
            value = opt.orElse(null);
        }
        if (value != null) {
            trace("{} / {} := {}", toString(), key.toString(), value.toString());
            return outputs.put(key, value);
        } else return null;
    }


    protected String getExecutionId(){
        return tasks.getExecutionId();
    }

    protected Task retry(Task theMainTask, Task theFixTask) {
        var retryTask = retry.get()
                .withInput(CJInput.task, theMainTask)
                .withInput(fixTask, theFixTask);
        return submit(retryTask);
    }

    protected Task withInput(Instance<? extends Task> instance, Input input, Object value) {
        try{
            var task = instance.get();
            task = task.withInput(input, value);
            return task;
        } catch (Exception ex){
            ex.printStackTrace();
            debug("Failed to create task instance for {}", instance);
            throw new RuntimeException(ex);
        }
    }

    protected Path getTaskDir(String dirName) {
        return FSUtils.getTaskDir(getContextName(), dirName);
    }

    private String getContextName() {
        return getClass().getPackageName().replaceAll("cj.","");
    }

    protected Optional<String> exec(String... cmdArgs){
        return exec(DEFAULT_TIMEOUT_MINS, false, cmdArgs);
    }

    @SuppressWarnings("unused")
    protected Optional<String> exec(Boolean dryRun, String... cmdArgs){
        return exec(DEFAULT_TIMEOUT_MINS, dryRun, cmdArgs);
    }

    @SuppressWarnings("all")
    protected Optional<String> exec(Long timeout, String... cmdArgs){
        return exec(timeout, false, cmdArgs);
    }

    protected Optional<String> exec(Long timeoutMins, Boolean isDryRun, String... cmdArgs){
        if (cmdArgs.length == 1){
            var cmd = cmdArgs[0];
            if (cmd.contains(" ")){
                cmdArgs = cmd.split(" ");
            }
        }
        var shellTask = shellTask(isDryRun, cmdArgs)
                .withInput(timeout, timeoutMins);
        submit(shellTask);
        @SuppressWarnings("all")
        var output = shellTask.outputString(Output.shell.stdout);
        return output;
    }

    protected ShellTask shellTask(List<String> cmdsList) {
        String[] cmdArgs = cmdsList.toArray(String[]::new);
        return shellTask(false , cmdArgs);
    }
    protected ShellTask shellTask(String... cmdArgs) {
        return shellTask(false , cmdArgs);
    }

    protected ShellTask shellTask(Boolean isDryRun, String... cmdArgs) {
        var cmdList = Arrays.asList(cmdArgs);
        var shellTask = shellInstance.get();
        if (isDryRun != null){
            shellTask.withInput(dryRun, isDryRun);
        }
        shellTask.withInput(cmds, cmdList);
        return shellTask;
    }

    protected boolean hasCapabilities(Capabilities... cs){
        return tasks.hasCapabilities(cs);
    }

    protected <T> void forEach(List<T> list, Consumer<T> consumer) {
        var stream = list.stream();
        if (getConfig().parallel()){
            stream = stream.parallel();
        }
        stream.forEach(consumer);
    }

    @Inject
    Instance<CheckShellCommandExistsTask> checkCmd;

    @SuppressWarnings("UnusedReturnValue")
    protected Task checkCmd(String executable, Map<OS, String[]> fixMap) {
        var checkTask = withInput(checkCmd, ShellInput.cmd, executable);
        var installTask = shellTask(OS.get(fixMap));
        return retry(checkTask, installTask);
    }

    @Inject
    Engine engine;

    protected Template getTemplate(String location) {
        return engine.getTemplate(location);
    }

    protected Map<String, String> getInputsMap() {
        @SuppressWarnings("redundant")
        var inputsMap = getExpectedInputs()
                .stream()
                .collect(Collectors.toMap(
                        Input::toString,
                        this::inputString
                )).entrySet()
                .stream()
                .filter( e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().get()));
        return inputsMap;
    }

    protected void expectCapability(@SuppressWarnings("SameParameterValue") Capabilities capability) {
        if(! hasCapabilities(capability)){
            debug("Missing capability {} ", capability);
            throw new CapabilityNotFoundException(capability);
        }
    }

}