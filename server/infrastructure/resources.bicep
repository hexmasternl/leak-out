// Everything inside the resource group: monitoring, PostgreSQL, the Container Apps environment,
// the API container app and the daily pipe generator job. Deployed by main.bicep.

@description('Azure region of the resources.')
param location string = resourceGroup().location

@description('Prefix of the resource names; the resource group name, so every resource is recognisable as part of it.')
param namePrefix string = resourceGroup().name

@description('Full image reference of the API including the tag.')
param imageName string

@description('Full image reference of the daily pipe generator including the tag.')
param generatorImageName string

@description('E-mail address that gets the daily pipe generator\'s failure alert; empty sends no e-mail.')
param alertEmailAddress string = ''

@description('Version of the deployed release (SERVICE_VERSION), reported as the OpenTelemetry service.version.')
param serviceVersion string

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

@description('Minimum number of API replicas; 1 keeps a warm replica, 0 lets the API scale to zero.')
@minValue(0)
param minReplicas int = 1

@description('Maximum number of API replicas.')
@minValue(1)
param maxReplicas int = 3

var postgresAdministratorLogin = 'leakoutadmin'
var dailyPipesDbConnectionString = 'Host=${postgres.outputs.fullyQualifiedDomainName};Database=dailypipesdb;Username=${postgresAdministratorLogin};Password=${postgresAdminPassword};Ssl Mode=Require'

module monitoring 'modules/monitoring.bicep' = {
  name: 'monitoring'
  params: {
    location: location
    workspaceName: '${namePrefix}-log'
    applicationInsightsName: '${namePrefix}-appi'
  }
}

module postgres 'modules/postgres.bicep' = {
  name: 'postgres'
  params: {
    location: location
    serverName: '${namePrefix}-psql'
    administratorLogin: postgresAdministratorLogin
    administratorPassword: postgresAdminPassword
  }
}

module containerAppsEnvironment 'modules/containerapps-env.bicep' = {
  name: 'containerapps-env'
  params: {
    location: location
    environmentName: '${namePrefix}-cae'
    workspaceCustomerId: monitoring.outputs.workspaceCustomerId
    workspaceSharedKey: monitoring.outputs.workspaceSharedKey
  }
}

module api 'modules/api.bicep' = {
  name: 'api'
  params: {
    location: location
    containerAppName: '${namePrefix}-api'
    environmentId: containerAppsEnvironment.outputs.environmentId
    imageName: imageName
    serviceVersion: serviceVersion
    registryServer: registryServer
    registryUsername: registryUsername
    registryPassword: registryPassword
    playersDbConnectionString: 'Host=${postgres.outputs.fullyQualifiedDomainName};Database=playersdb;Username=${postgresAdministratorLogin};Password=${postgresAdminPassword};Ssl Mode=Require'
    scoresDbConnectionString: 'Host=${postgres.outputs.fullyQualifiedDomainName};Database=scoresdb;Username=${postgresAdministratorLogin};Password=${postgresAdminPassword};Ssl Mode=Require'
    dailyPipesDbConnectionString: dailyPipesDbConnectionString
    googleClientId: googleClientId
    googleClientSecret: googleClientSecret
    authSigningKey: authSigningKey
    applicationInsightsConnectionString: monitoring.outputs.applicationInsightsConnectionString
    minReplicas: minReplicas
    maxReplicas: maxReplicas
  }
}

module dailyPipeJob 'modules/daily-pipe-job.bicep' = {
  name: 'daily-pipe-job'
  params: {
    location: location
    jobName: '${namePrefix}-dailypipe'
    environmentId: containerAppsEnvironment.outputs.environmentId
    imageName: generatorImageName
    serviceVersion: serviceVersion
    registryServer: registryServer
    registryUsername: registryUsername
    registryPassword: registryPassword
    dailyPipesDbConnectionString: dailyPipesDbConnectionString
    applicationInsightsConnectionString: monitoring.outputs.applicationInsightsConnectionString
    applicationInsightsId: monitoring.outputs.applicationInsightsId
    alertEmailAddress: alertEmailAddress
  }
}

@description('Fully qualified domain name of the API.')
output apiFullyQualifiedDomainName string = api.outputs.fullyQualifiedDomainName

@description('Name of the daily pipe generator job.')
output dailyPipeJobName string = dailyPipeJob.outputs.jobName
