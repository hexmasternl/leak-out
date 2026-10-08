// Entry point of the Leak Out server infrastructure. Deployed at subscription scope: creates
// the resource group and deploys resources.bicep into it.
targetScope = 'subscription'

@description('Name of the resource group that holds every resource; created when it does not exist.')
param resourceGroupName string = 'rg-leakout-prod'

@description('Azure region of the resource group and its resources.')
param location string

@description('Abbreviation of the Azure region, used for naming resources.')
param locationAbbreviation string = 'neu'

@description('Full image reference of the API including the tag.')
@minLength(1)
param imageName string

@description('Version of the deployed release (SERVICE_VERSION), reported as the OpenTelemetry service.version.')
param serviceVersion string

@description('Login server of the existing container registry (ACR_LOGIN_SERVER).')
@minLength(1)
param registryServer string

@description('User name for the container registry (ACR_LOGIN_USERNAME).')
param registryUsername string

@description('Password for the container registry (ACR_LOGIN_PASSWORD).')
@secure()
param registryPassword string

@description('Administrator password of the PostgreSQL server.')
@secure()
param postgresAdminPassword string

@description('Client id of the web OAuth client of the Google Cloud project linked to the game (GOOGLE_CLIENT_ID).')
@secure()
param googleClientId string

@description('Client secret of that web OAuth client (GOOGLE_CLIENT_SECRET).')
@secure()
param googleClientSecret string

@description('Key the API signs its own access tokens with (GOOGLE_SIGNING_KEY), at least 32 characters.')
@secure()
@minLength(32)
param authSigningKey string

@description('Minimum number of API replicas; 1 keeps a warm replica, 0 lets the API scale to zero.')
@minValue(0)
param minReplicas int = 1

@description('Maximum number of API replicas.')
@minValue(1)
param maxReplicas int = 3

var targetResourcegroupName = '${resourceGroupName}-${locationAbbreviation}'

resource resourceGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
  name: targetResourcegroupName
  location: location
}

module resources 'resources.bicep' = {
  name: 'leakout-resources'
  scope: resourceGroup
  params: {
    location: location
    imageName: imageName
    serviceVersion: serviceVersion
    registryServer: registryServer
    registryUsername: registryUsername
    registryPassword: registryPassword
    postgresAdminPassword: postgresAdminPassword
    googleClientId: googleClientId
    googleClientSecret: googleClientSecret
    authSigningKey: authSigningKey
    minReplicas: minReplicas
    maxReplicas: maxReplicas
  }
}

@description('Name of the resource group.')
output resourceGroupName string = resourceGroup.name

@description('Fully qualified domain name of the API.')
output apiFullyQualifiedDomainName string = resources.outputs.apiFullyQualifiedDomainName
