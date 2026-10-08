// The Static Web App that hosts the website. Content is uploaded by the website-deploy workflow
// (prebuilt Hugo output), so there is no repository link and no build configuration here.

@description('Name of the Static Web App.')
param name string

@description('Azure region of the Static Web App.')
param location string

@description('Pricing tier: Free or Standard.')
param sku string

@description('Optional custom domain; empty for none.')
param customDomain string

resource staticWebApp 'Microsoft.Web/staticSites@2024-04-01' = {
  name: name
  location: location
  sku: {
    name: sku
    tier: sku
  }
  properties: {
    // Deployed from GitHub Actions with a deployment token, not linked to the repository.
    provider: 'None'
    allowConfigFileUpdates: true
    stagingEnvironmentPolicy: 'Disabled'
    publicNetworkAccess: 'Enabled'
  }
  tags: {
    application: 'leak-out'
    component: 'website'
  }
}

resource domain 'Microsoft.Web/staticSites/customDomains@2024-04-01' = if (!empty(customDomain)) {
  parent: staticWebApp
  name: empty(customDomain) ? 'unused' : customDomain
  properties: {
    validationMethod: 'cname-delegation'
  }
}

output name string = staticWebApp.name
output defaultHostname string = staticWebApp.properties.defaultHostname
