package cj.aws.ec2;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import cj.aws.AWSFilter;

import javax.enterprise.context.Dependent;

import static cj.Output.AWS.TargetGroupsMatch;

@Dependent
public class FilterTargetGroups extends AWSFilter {
    private boolean match(TargetGroup resource) {
        var prefix = aws().config().filterPrefix();
        var match = true;
        if (prefix.isPresent()) {
            match = resource.targetGroupName().startsWith(prefix.get());
        }
        return match;
    }

    @Override
    public void apply() {
        var elb = aws().elbv2();
        var resources = elb.describeTargetGroups().targetGroups();
        var matches = resources.stream().filter(this::match).toList();
        success(TargetGroupsMatch, matches);
    }

}