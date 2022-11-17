package cj;

import cj.aws.AWSConfiguration;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static cj.Utils.existing;

@ConfigMapping(prefix = "cj" )
@StaticInitSafe
public interface Configuration {
    boolean PRINT_STACK_TRACE = false;
    float MEDIUM_POLL_FACTOR = 4.0f;
    float LARGE_POLL_FACTOR = 8.0f;

    @WithName("pollInterval")
    @WithDefault("10.00")
    float pollInterval();

    @WithName("templateName")
    @WithDefault("cj-bootstrap")
    String templateName();

    @WithName("aws")
    AWSConfiguration aws();

    @WithName("ocp")
    OCPConfiguration ocp();

    @WithName("report")
    ReportConfiguration report();

    @WithName("tasks")
    List<TaskConfiguration>  tasks();

    @WithName("task")
    Optional<String> task();

    @WithName("capabilities")
    Optional<List<String>> capabilities();

    @WithName("parallel")
    @WithDefault("false")
    boolean parallel();

    default Path getApplicationPath(){
        var home = System.getProperty("user.home");
        var homePath = Path.of(home);
        var appPath = homePath.resolve(".cj");
        var appDir = appPath.toFile();
        if (! appDir.exists()){
            //noinspection ResultOfMethodCallIgnored
            appDir.mkdirs();
        }
        return appPath;
    }


    default Path getReportsPath(){
        var execPath = getExecutionPath();
        var reportsPath = execPath.resolve("reports");
        return existing(reportsPath);
    }

    default Path getExecutionPath(){
        var appPath = getApplicationPath();
        var execId = StaticConfig.executionId;
        var execPath = appPath.resolve(execId);
        return existing(execPath);
    }

    final class StaticConfig {
        static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddss_hhmmss");
        static final LocalDateTime loadTime = LocalDateTime.now();
        static final String executionId = dtf.format(loadTime);
    }


    default long mediumPollIntervalMs() {
        return pollIntervalMs(MEDIUM_POLL_FACTOR);
    }
    default long largePollIntervalMs() {
        return pollIntervalMs(LARGE_POLL_FACTOR);

    }

    @SuppressWarnings("redundant")
    default long pollIntervalMs(float sizeFactor){
        float pollInterval = pollInterval();
        float result = sizeFactor * pollInterval * noise() * 1000;
        @SuppressWarnings("redundant")
        long resultLong = Float.valueOf(result).longValue();
        return resultLong;
    }

    default float noise() {
        Random rand = new Random();
        @SuppressWarnings("redundant")
        float noise = rand.nextFloat(1.00f, 1.25f);
        return noise;
    }

    default long largeAtMostTimeoutMs() {
        var atMostMin = 30;
        var atMostMs = atMostMin * 60 * 1000;
        return Float.valueOf(atMostMs).longValue();
    }

}
