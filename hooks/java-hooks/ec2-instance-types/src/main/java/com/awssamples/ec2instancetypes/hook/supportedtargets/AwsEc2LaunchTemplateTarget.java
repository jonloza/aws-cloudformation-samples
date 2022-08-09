package com.awssamples.ec2instancetypes.hook.supportedtargets;

import java.util.Set;

import com.awssamples.ec2instancetypes.hook.CallbackContext;
import com.awssamples.ec2instancetypes.hook.model.aws.ec2.launchtemplate.AwsEc2Launchtemplate;
import com.awssamples.ec2instancetypes.hook.model.aws.ec2.launchtemplate.AwsEc2LaunchtemplateTargetModel;
import com.awssamples.ec2instancetypes.hook.model.aws.ec2.launchtemplate.InstanceRequirements;
import com.awssamples.ec2instancetypes.hook.model.aws.ec2.launchtemplate.LaunchTemplateData;
import com.awssamples.ec2instancetypes.hook.supportedproperties.AwsEc2LaunchTemplateInstanceRequirementsProperty;
import com.awssamples.ec2instancetypes.hook.supportedproperties.AwsEc2LaunchTemplateInstanceTypeProperty;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;
import software.amazon.cloudformation.proxy.hook.targetmodel.ResourceHookTargetModel;

/**
 * Class containing helper methods for the AWS::EC2::Instance resource type.
 */
public final class AwsEc2LaunchTemplateTarget
        implements SupportedTarget, AwsEc2LaunchTemplateInstanceTypeProperty,
        AwsEc2LaunchTemplateInstanceRequirementsProperty {

    private final String typeName = AwsEc2Launchtemplate.TYPE_NAME;

    /**
     * Return the resource type name.
     *
     * @return String
     */
    @Override
    public final String getTypeName() {
        return typeName;
    }

    /**
     * Validate the specified target's configuration.
     *
     * @param proxy                      AmazonWebServicesClientProxy
     * @param allowedEc2InstanceTypesSet Set<String>
     * @param hookContext                HookContext
     * @param logger                     Logger
     * @param targetName                 String
     * @return ProgressEvent<HookTargetModel, CallbackContext>
     */
    @Override
    public final ProgressEvent<HookTargetModel, CallbackContext> validateTarget(
            final AmazonWebServicesClientProxy proxy,
            final Set<String> allowedEc2InstanceTypesSet,
            final HookContext hookContext,
            final Logger logger,
            final String targetName) {
        final String targetInstanceType = getTargetEc2InstanceType(hookContext);
        final InstanceRequirements targetInstanceRequirements = getTargetInstanceRequirements(hookContext);

        final ProgressEvent<HookTargetModel, CallbackContext> validateTargetProperties = validateInstanceTypeAndInstanceRequirementsTargetProperties(
                targetInstanceType,
                targetInstanceRequirements,
                logger);
        if (!validateTargetProperties.getStatus().equals(OperationStatus.IN_PROGRESS)) {
            return validateTargetProperties;
        }

        if (targetInstanceType != null) {
            final ProgressEvent<HookTargetModel, CallbackContext> validateInstanceTypeTargetProperty = validateInstanceTypeTargetPropertyNoDefaultValue(
                    allowedEc2InstanceTypesSet,
                    targetInstanceType,
                    targetName,
                    logger);
            if (!validateInstanceTypeTargetProperty.getStatus().equals(OperationStatus.IN_PROGRESS)) {
                return validateInstanceTypeTargetProperty;
            }
        } else if (targetInstanceRequirements != null) {
            final LaunchTemplateData launchTemplateData = getResourcePropertiesFromTargetModel(hookContext)
                    .getLaunchTemplateData();

            final ProgressEvent<HookTargetModel, CallbackContext> validateInstanceRequirementsTargetProperty = validateInstanceRequirementsTargetProperty(
                    targetInstanceRequirements,
                    proxy,
                    allowedEc2InstanceTypesSet,
                    hookContext,
                    launchTemplateData,
                    logger);
            if (!validateInstanceRequirementsTargetProperty.getStatus().equals(OperationStatus.IN_PROGRESS)) {
                return validateInstanceRequirementsTargetProperty;
            }
        }

        final String successMessage = String.format("Successfully verified instance type(s) for target: [%s].",
                targetName);

        return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .message(successMessage)
                .build();
    }

    /**
     * Return resource-specific properties from this hook's context.
     *
     * @param hookContext HookContext
     * @return AwsEc2Launchtemplate
     */
    @Override
    public final AwsEc2Launchtemplate getResourcePropertiesFromTargetModel(final HookContext hookContext) {
        final ResourceHookTargetModel<AwsEc2Launchtemplate> targetModel = hookContext
                .getTargetModel(AwsEc2LaunchtemplateTargetModel.class);
        return targetModel.getResourceProperties();
    }

    /**
     * Return the instance type from the target's relevant property.
     *
     * @param hookContext HookContext
     * @return String
     */
    public final String getTargetEc2InstanceType(final HookContext hookContext) {
        final AwsEc2Launchtemplate resourceProperties = getResourcePropertiesFromTargetModel(hookContext);
        return resourceProperties.getLaunchTemplateData().getInstanceType();
    }

    /**
     * Return the InstanceRequirements property from this resource type.
     *
     * @param hookContext HookContext
     * @return InstanceRequirements
     */
    public final InstanceRequirements getTargetInstanceRequirements(final HookContext hookContext) {
        final AwsEc2Launchtemplate resourceProperties = getResourcePropertiesFromTargetModel(hookContext);
        return resourceProperties.getLaunchTemplateData().getInstanceRequirements();
    }

}
