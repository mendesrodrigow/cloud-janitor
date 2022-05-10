package cloudjanitor;

import cloudjanitor.aws.ec2.DeleteEmptyVPCTest;
import cloudjanitor.aws.sts.GetCallerIdentityTask;
import cloudjanitor.simple.HelloTask;
import cloudjanitor.simple.HelloTaskTest;
import cloudjanitor.simple.ToUppperTask;
import cloudjanitor.simple.ToUppperTaskTest;

/**
 * This class suggests an order to learn how Cloud Janitor works by reading the tests.
 * Really, it does not do anything else.
 */
public class LearnableOrder {
    Class[] simpleTasks = new Class[]{
            HelloTaskTest.class,
            ToUppperTaskTest.class
    };

    Class[] awsBasics = {
            GetCallerIdentityTask.class
    };

    Class[] awsEmptyDeletes = {
            DeleteEmptyVPCTest.class
    };

    Class[] populatedTests = {
            DeleteEmptyVPCTest.class
    };
}
