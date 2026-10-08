// Parameters of the website deployment. Nothing here is secret: the Static Web App's deployment
// token is read by the workflow at run time and never stored (see website/README.md).
using 'main.bicep'

param resourceGroupName = 'rg-leakout-web-prod'
param location = 'westeurope'
param locationAbbreviation = 'weu'
param sku = 'Free'

// The site's custom domain. Its CNAME record must point at the Static Web App's default host
// name (red-sea-080a37703.6.azurestaticapps.net) before a deployment; empty for none.
param customDomain = 'leakout.hexmaster.nl'
