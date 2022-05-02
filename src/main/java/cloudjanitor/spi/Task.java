package cloudjanitor.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.swing.text.html.Option;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A task is a function to be executed, as a basic unit of work.
 */
public interface Task {

    /**
     * The code to be executed by the Task, if considered *safe* by the TaskManager.
     */
    default void runSafe(){
        throw new RuntimeException("Task not implemented");
    }

    /**
     * A *write* task is only *safe* to execute if the *dry run* flag is false.
     */
    default boolean isWrite(){
        return true;
    };

    /**
     * Task start time, if started
     */
    default Optional<LocalDateTime> getStartTime(){
        return Optional.empty();
    }

    default void setStartTime(LocalDateTime localDateTime){
        throw new RuntimeException("Method not implemented");
    }

    /**
    * Task end time, if finished
     */
    default Optional<LocalDateTime> getEndTime(){
        return Optional.empty();
    }

    default void setEndTime(LocalDateTime localDateTime){
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Elapsed Time between Start and End, if finished
     */
    default Optional<Duration> getElapsedTime(){
        var start = getStartTime();
        var end = getEndTime();
        if (start.isPresent() && end.isPresent()){
            return Optional.of(Duration.between(start.get(),end.get()));
        }else return Optional.empty();
    }

    /* Task Chaining */
    default List<Task> getDependencies(){
        return List.of();
    }

    default Map<String, Object> getOutputs(){
        return Map.of();
    }

    default Optional<Object> findOutput(String key){
        return Optional.empty();
    }
    default Optional<String> findString(String key) { return Optional.empty(); }

    default Map<String, Object> getErrors(){
        return Map.of();
    }


    /* Task Naming */
    default String getSimpleName(){
        return getClass().getSimpleName().split("_")[0];
    }

    default String getPackage(){
        return getClass().getPackage().getName();
    }

    default String getName() {
        var name = getSimpleName();
        var named = getClass().getAnnotation(Named.class);
        if (named != null) {
            name = named.value();
        }
        var superclass = getClass().getSuperclass();
        named = superclass.getAnnotation(Named.class);
        if (named != null) {
            name = named.value();
        }
        return name;
    }

    /* Task Throttling */
    default Optional<Long> getWaitAfterRun() {
        return Optional.of(1_000L);
    }



}
