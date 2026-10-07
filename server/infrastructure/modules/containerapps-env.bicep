// Consumption-only Container Apps environment that sends console and system logs to Log Analytics.

@description('Azure region of the environment.')
param location string

@description('Name of the Container Apps environment.')
param environmentName string

@description('Workspace (customer) id of the Log Analytics workspace.')
param workspaceCustomerId string

@description('Shared key of the Log Analytics workspace.')
@secure()
param workspaceSharedKey string

resource environment 'Microsoft.App/managedEnvironments@2025-01-01' = {
  name: environmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: workspaceCustomerId
        sharedKey: workspaceSharedKey
      }
    }
    // No workload profiles: a Consumption-only environment, billed per use.
    zoneRedundant: false
  }
}

@description('Resource id of the Container Apps environment.')
output environmentId string = environment.id
