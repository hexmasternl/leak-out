// Everything inside the resource group: monitoring, PostgreSQL, the Container Apps environment
// and the API container app. Deployed by main.bicep.

@description('Azure region of the resources.')
param location string = resourceGroup().location

@description('Prefix of the resource names.')
param namePrefix string = 'leakout'

@description('Full image reference of the API including the tag.')
param imageName string

@description('Login server of the existing container registry (ACR_LOGIN_SERVER).')
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

@description('Maximum number of API replicas.')
@minValue(1)
param maxReplicas int = 3

var suffix = uniqueString(resourceGroup().id)
var postgresAdministratorLogin = 'leakoutadmin'

module monitoring 'modules/monitoring.bicep' = {
  name: 'monitoring'
  params: {
    location: location
    workspaceName: '${namePrefix}-log-${suffix}'
    applicationInsightsName: '${namePrefix}-appi-${suffix}'
  }
}

module postgres 'modules/postgres.bicep' = {
  name: 'postgres'
  params: {
    location: location
    serverName: '${namePrefix}-psql-${suffix}'
    administratorLogin: postgresAdministratorLogin
    administratorPassword: postgresAdminPassword
  }
}

module containerAppsEnvironment 'modules/containerapps-env.bicep' = {
  name: 'containerapps-env'
  params: {
    location: location
    environmentName: '${namePrefix}-cae-${suffix}'
    workspaceCustomerId: monitoring.outputs.workspaceCustomerId
    workspaceSharedKey: monitoring.outputs.workspaceSharedKey
  }
}

module api 'modules/api.bicep' = {
  name: 'api'
  params: {
    location: location
    environmentId: containerAppsEnvironment.outputs.environmentId
    imageName: imageName
    registryServer: registryServer
    registryUsername: registryUsername
    registryPassword: registryPassword
    playersDbConnectionString: 'Host=${postgres.outputs.fullyQualifiedDomainName};Database=playersdb;Username=${postgresAdministratorLogin};Password=${postgresAdminPassword};Ssl Mode=Require'
    scoresDbConnectionString: 'Host=${postgres.outputs.fullyQualifiedDomainName};Database=scoresdb;Username=${postgresAdministratorLogin};Password=${postgresAdminPassword};Ssl Mode=Require'
    googleClientId: googleClientId
    googleClientSecret: googleClientSecret
    authSigningKey: authSigningKey
    applicationInsightsConnectionString: monitoring.outputs.applicationInsightsConnectionString
    maxReplicas: maxReplicas
  }
}

@description('Fully qualified domain name of the API.')
output apiFullyQualifiedDomainName string = api.outputs.fullyQualifiedDomainName
