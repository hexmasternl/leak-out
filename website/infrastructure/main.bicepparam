// Parameters of the website deployment. Nothing here is secret: the Static Web App's deployment
// token is read by the workflow at run time and never stored (see website/README.md).
using 'main.bicep'

param resourceGroupName = 'rg-leakout-web-prod'
param location = 'westeurope'
param locationAbbreviation = 'weu'
param sku = 'Free'

// Set to the custom domain (for example 'leakout.hexmaster.nl') once its CNAME record points at
// the Static Web App's default host name.
param customDomain = ''
