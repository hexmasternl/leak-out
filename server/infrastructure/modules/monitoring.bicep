// Log Analytics workspace and workspace-based Application Insights for the API and the daily pipe generator.

@description('Azure region of the resources.')
param location string

@description('Name of the Log Analytics workspace.')
param workspaceName string

@description('Name of the Application Insights component.')
param applicationInsightsName string

resource workspace 'Microsoft.OperationalInsights/workspaces@2026-03-01' = {
  name: workspaceName
  location: location
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    workspaceCapping: {
      // Caps ingestion, and therefore cost, at 1 GB per day.
      dailyQuotaGb: 1
    }
  }
}

resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: applicationInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: workspace.id
  }
}

@description('Resource id of the Log Analytics workspace.')
output workspaceResourceId string = workspace.id

@description('Workspace (customer) id the Container Apps environment sends logs to.')
output workspaceCustomerId string = workspace.properties.customerId

@description('Primary shared key of the Log Analytics workspace.')
@secure()
output workspaceSharedKey string = workspace.listKeys().primarySharedKey

@description('Connection string of the Application Insights component.')
@secure()
output applicationInsightsConnectionString string = applicationInsights.properties.ConnectionString

@description('Resource id of the Application Insights component.')
output applicationInsightsId string = applicationInsights.id
