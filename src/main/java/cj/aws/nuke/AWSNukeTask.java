package cj.aws.nuke;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

import cj.*;
import cj.TaskMaturity.Level;
import cj.aws.AWSTask;

import java.nio.file.Path;

import static cj.Capabilities.*;

@Dependent
@Named("aws-nuke")
@TaskDescription("Runs aws-nuke")
@TaskMaturity(Level.experimental)
@ExpectedInputs({})
@SuppressWarnings("unused")
public class AWSNukeTask extends AWSTask {
    @Override
    public void apply() {
        debug("aws-nuke started.");
        runNuke(renderConfig());
        debug("aws-nuke completed.");
    }

    private void runNuke(Path cfgFile) {
        var dryRun = hasCapabilities(CLOUD_DELETE_RESOURCES) ?
                "--no-dry-run" :
                "";
        var cmd = new String[]{
                "aws-nuke"
                , dryRun
                ,"--force"
                ,"--config"
                , cfgFile.toString()
        };
        checkpoint("Executing aws-nuke: {}",
                String.join(" ", cmd));
        tasks().shell(cmd);
    }

    private Path renderConfig() {
        return render("aws-nuke.qute.yaml", "aws-nuke.yaml");
    }
}
