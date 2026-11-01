// The daily pipe generator: a Container Apps job in the API's environment that runs the generator
// image every day at 00:00 UTC. A run publishes today's daily pipe and exits; a failed run (exit
// code 1) is retried up to replicaRetryLimit times, and its error logs reach Application Insights,
// where the alert below fires on them.

@description('Azure region of the job.')
param location string

@description('Name of the Container Apps job.')
param jobName string

@description('Name of the container and the OpenTelemetry service; matches the Aspire resource name.')
param serviceName string = 'daily-pipe-generator'

@description('Resource id of the Container Apps environment.')
param environmentId string

@description('Full image reference of the generator including the tag.')
param imageName string

@description('Version of the deployed release, reported as the OpenTelemetry service.version.')
param serviceVersion string

@description('Login server of the existing container registry.')
param registryServer string

@description('User name for the container registry.')
param registryUsername string

@description('Password for the container registry.')
@secure()
param registryPassword string

@description('Npgsql connection string of the dailypipesdb database.')
@secure()
param dailyPipesDbConnectionString string

@description('Connection string of the Application Insights component.')
@secure()
param applicationInsightsConnectionString string

@description('Resource id of the Application Insights component the failure alert queries.')
param applicationInsightsId string

@description('E-mail address that gets the failure alert; empty sends no e-mail (the alert still shows in Azure Monitor).')
param alertEmailAddress string = ''

@description('Cron expression of the schedule, in UTC.')
param cronExpression string = '0 0 * * *'

@description('How often a failed run is retried.')
@minValue(0)
param replicaRetryLimit int = 3

@description('Seconds a run may take before it is stopped and counted as failed.')
@minValue(60)
param replicaTimeoutSeconds int = 600

resource job 'Microsoft.App/jobs@2026-07-01' = {
  name: jobName
  location: location
  properties: {
    environmentId: environmentId
    configuration: {
      triggerType: 'Schedule'
      scheduleTriggerConfig: {
        cronExpression: cronExpression
        parallelism: 1
        replicaCompletionCount: 1
      }
      replicaRetryLimit: replicaRetryLimit
      replicaTimeout: replicaTimeoutSeconds
      registries: [
        {
          server: registryServer
          username: registryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: registryPassword
        }
        {
          name: 'dailypipesdb-connection'
          value: dailyPipesDbConnectionString
        }
        {
          name: 'appinsights-connection'
          value: applicationInsightsConnectionString
        }
      ]
    }
    template: {
      containers: [
        {
          name: serviceName
          image: imageName
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              name: 'DOTNET_ENVIRONMENT'
              value: 'Production'
            }
            {
              name: 'OTEL_SERVICE_NAME'
              value: serviceName
            }
            {
              name: 'OTEL_RESOURCE_ATTRIBUTES'
              value: 'service.version=${serviceVersion}'
            }
            {
              name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
              secretRef: 'appinsights-connection'
            }
            {
              name: 'ConnectionStrings__dailypipesdb'
              secretRef: 'dailypipesdb-connection'
            }
          ]
        }
      ]
    }
  }
}

resource actionGroup 'Microsoft.Insights/actionGroups@2024-10-01-preview' = if (!empty(alertEmailAddress)) {
  name: '${jobName}-ag'
  location: 'global'
  properties: {
    groupShortName: 'dailypipe'
    enabled: true
    emailReceivers: [
      {
        name: 'maintainer'
        emailAddress: alertEmailAddress
        useCommonAlertSchema: true
      }
    ]
  }
}

// A failed run logs an error ("Generating the daily pipe of ... failed") and a critical
// "Daily pipe run failed" before it exits with 1; every retry logs them again.
resource failureAlert 'Microsoft.Insights/scheduledQueryRules@2023-12-01' = {
  name: '${jobName}-failed'
  location: location
  properties: {
    displayName: 'Daily pipe generator run failed'
    description: 'The daily pipe generator logged an error: today\'s daily pipe may not be published. Run the job by hand once the cause is fixed (server/README.md).'
    severity: 1
    enabled: true
    scopes: [
      applicationInsightsId
    ]
    evaluationFrequency: 'PT15M'
    windowSize: 'PT15M'
    autoMitigate: true
    criteria: {
      allOf: [
        {
          query: 'union traces, exceptions | where cloud_RoleName == "${serviceName}" and severityLevel >= 3'
          timeAggregation: 'Count'
          operator: 'GreaterThan'
          threshold: 0
          failingPeriods: {
            numberOfEvaluationPeriods: 1
            minFailingPeriodsToAlert: 1
          }
        }
      ]
    }
    actions: {
      actionGroups: empty(alertEmailAddress) ? [] : [
        actionGroup.id
      ]
    }
  }
}

@description('Name of the Container Apps job, for starting a run by hand.')
output jobName string = job.name
