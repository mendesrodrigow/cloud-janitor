package cloudjanitor.aws.ec2;

import software.amazon.awssdk.services.sts.StsClient;
import cloudjanitor.aws.AWSAccount;
import cloudjanitor.aws.AWSFilter;
import cloudjanitor.spi.Task;

import javax.enterprise.context.Dependent;
import javax.inject.Named;
import java.util.List;
import java.util.stream.Stream;

@Named("cleanup-aws")
@Dependent
public class FilterAccount extends AWSFilter {
    /*

    @Override
    protected List<AWSAccount> filterResources() {
        var sts = getClient(StsClient.class);
        var accountId = sts.getCallerIdentity().account();
        setResourceDescription(accountId);
        return List.of(new AWSAccount(accountId));
    }

    @Override
    protected Stream<Task> mapSubtasks(AWSAccount accountId) {
        return Stream.of(
                new FilterRegions(),
                new FilterRecords());
    }

    @Override
    protected String getResourceType() {
        return "AWS Account";
    }

     */

}
