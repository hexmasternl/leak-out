// Entry point of the Leak Out website infrastructure. Deployed at subscription scope: creates the
// website's own resource group and deploys the Static Web App into it, so the website can be
// changed or removed without touching the score server (see website/README.md).
targetScope = 'subscription'

@description('Name of the resource group that holds the website; created when it does not exist.')
param resourceGroupName string = 'rg-leakout-web-prod'

@description('Azure region of the resource group and the Static Web App. Static Web Apps run in a few regions only.')
@allowed([
  'westeurope'
  'centralus'
  'eastus2'
  'westus2'
  'eastasia'
])
param location string

@description('Abbreviation of the Azure region, used for naming resources.')
param locationAbbreviation string = 'weu'

@description('Pricing tier of the Static Web App.')
@allowed([
  'Free'
  'Standard'
])
param sku string = 'Free'

@description('Optional custom domain, for example leakout.hexmaster.nl. Leave empty to use only the default host name. Create the CNAME record to the default host name first.')
param customDomain string = ''

var targetResourceGroupName = '${resourceGroupName}-${locationAbbreviation}'

resource resourceGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
  name: targetResourceGroupName
  location: location
}

module website 'modules/static-web-app.bicep' = {
  name: 'leakout-website'
  scope: resourceGroup
  params: {
    name: 'stapp-leakout-prod-${locationAbbreviation}'
    location: location
    sku: sku
    customDomain: customDomain
  }
}

@description('Resource group of the Static Web App, used by the workflow to read its deployment token.')
output resourceGroupName string = resourceGroup.name

@description('Name of the Static Web App.')
output staticWebAppName string = website.outputs.name

@description('Default host name of the Static Web App: the origin to allow in the score server CORS settings.')
output defaultHostname string = website.outputs.defaultHostname

@description('Host name the site is published under: the custom domain when set, otherwise the default host name.')
output publicHostname string = empty(customDomain) ? website.outputs.defaultHostname : customDomain
