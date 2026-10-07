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

resource environment 'Microsoft.App/managedEnvironments@2026-07-01' = {
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
  }
}

@description('Resource id of the Container Apps environment.')
output environmentId string = environment.id
