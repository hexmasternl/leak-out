// Parameters of the deployment. Non-secret defaults are set here; the image reference, the
// registry credentials and every secret are read from environment variables named after the
// GitHub secrets, so no value is stored here or passed on the command line (see README.md).
// A missing environment variable fails the deployment before anything changes.
using 'main.bicep'

param resourceGroupName = 'rg-leakout-prod'
param location = 'northeurope'
param locationAbbreviation = 'neu'
param maxReplicas = 3

param imageName = readEnvironmentVariable('API_IMAGE')
param registryServer = readEnvironmentVariable('ACR_LOGIN_SERVER')
param registryUsername = readEnvironmentVariable('ACR_LOGIN_USERNAME')
param registryPassword = readEnvironmentVariable('ACR_LOGIN_PASSWORD')
param postgresAdminPassword = readEnvironmentVariable('POSTGRES_ADMIN_PASSWORD')
param googleClientId = readEnvironmentVariable('GOOGLE_CLIENT_ID')
param googleClientSecret = readEnvironmentVariable('GOOGLE_CLIENT_SECRET')
param authSigningKey = readEnvironmentVariable('GOOGLE_SIGNING_KEY')
