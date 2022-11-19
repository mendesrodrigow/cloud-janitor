package cj;

import io.quarkus.runtime.Startup;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Singleton
@Startup
public class Inputs {
    @Inject
    Configuration configuration;

    Map<Input, InputConfig> inputConfigs = new HashMap<>();

    public Inputs(){
        System.out.println("Inputs()");
    }
    public Inputs putConfig(Input input,
                            String configKey,
                            Function<Configuration, Optional<?>> configFn,
                            Supplier<?> defaultFn){
        var inputConfig = new InputConfig(input, configKey, configFn, defaultFn);
        inputConfigs.put(input, inputConfig);
        return this;
    }

    public Object getFromConfig(Input input) {
        var inputConfig = inputConfigs.get(input);
        if (inputConfig != null) {
            @SuppressWarnings("all")
            var inputValue = inputConfig.applyConfigFn(configuration).orElse(null);
            return inputValue;
        }
        return null;
    }

    public Object getFromDefault(Input input) {
        var inputConfig = inputConfigs.get(input);
        if (inputConfig != null) {
            @SuppressWarnings("redundant")
            var result = inputConfig.applyDefaultFn();
            return result;
        }
        return null;
    }

    public InputConfig getConfig(Input inputKey) {
        return inputConfigs.get(inputKey);
    }
}