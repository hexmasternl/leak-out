// The API container app: pulls the API image from the existing registry, receives its
// configuration as secrets and scales on HTTP traffic from 0 replicas.

@description('Azure region of the container app.')
param location string

@description('Name of the container app.')
param containerAppName string

@description('Name of the container and the OpenTelemetry service; matches the Aspire resource name.')
param serviceName string = 'hexmaster-leakout-api'

@description('Resource id of the Container Apps environment.')
param environmentId string

@description('Full image reference including the tag, for example myregistry.azurecr.io/hexmaster-leakout-api:<sha>.')
param imageName string

@description('Login server of the existing container registry.')
param registryServer string

@description('User name for the container registry.')
param registryUsername string

@description('Password for the container registry.')
@secure()
param registryPassword string

@description('Npgsql connection string of the playersdb database.')
@secure()
param playersDbConnectionString string

@description('Npgsql connection string of the scoresdb database.')
@secure()
param scoresDbConnectionString string

@description('Client id of the web OAuth client of the Google Cloud project linked to the game.')
@secure()
param googleClientId string

@description('Client secret of that web OAuth client.')
@secure()
param googleClientSecret string

@description('Key the API signs its own access tokens with.')
@secure()
@minLength(32)
param authSigningKey string

@description('Connection string of the Application Insights component.')
@secure()
param applicationInsightsConnectionString string

@description('Maximum number of replicas.')
@minValue(1)
param maxReplicas int = 3

var targetPort = 8080

resource containerApp 'Microsoft.App/containerApps@2026-07-01' = {
  name: containerAppName
  location: location
  properties: {
    environmentId: environmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: targetPort
        transport: 'auto'
        allowInsecure: false
      }
      registries: [
        {
          server: registryServer
          username: registryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: registryPassword
        }
        {
          name: 'playersdb-connection'
          value: playersDbConnectionString
        }
        {
          name: 'scoresdb-connection'
          value: scoresDbConnectionString
        }
        {
          name: 'google-client-id'
          value: googleClientId
        }
        {
          name: 'google-client-secret'
          value: googleClientSecret
        }
        {
          name: 'auth-signing-key'
          value: authSigningKey
        }
        {
          name: 'appinsights-connection'
          value: applicationInsightsConnectionString
        }
      ]
    }
    template: {
      containers: [
        {
          name: serviceName
          image: imageName
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              name: 'ASPNETCORE_ENVIRONMENT'
              value: 'Production'
            }
            {
              name: 'OTEL_SERVICE_NAME'
              value: serviceName
            }
            {
              name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
              secretRef: 'appinsights-connection'
            }
            {
              name: 'ConnectionStrings__playersdb'
              secretRef: 'playersdb-connection'
            }
            {
              name: 'ConnectionStrings__scoresdb'
              secretRef: 'scoresdb-connection'
            }
            {
              name: 'GooglePlayGames__ClientId'
              secretRef: 'google-client-id'
            }
            {
              name: 'GooglePlayGames__ClientSecret'
              secretRef: 'google-client-secret'
            }
            {
              name: 'Authentication__SigningKey'
              secretRef: 'auth-signing-key'
            }
          ]
          probes: [
            {
              type: 'Startup'
              httpGet: {
                path: '/alive'
                port: targetPort
              }
              // Up to 50 seconds for a cold start after scaling from zero.
              initialDelaySeconds: 1
              periodSeconds: 5
              failureThreshold: 10
            }
            {
              type: 'Liveness'
              httpGet: {
                path: '/alive'
                port: targetPort
              }
              periodSeconds: 30
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/health'
                port: targetPort
              }
              periodSeconds: 10
              failureThreshold: 3
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'http-concurrency'
            http: {
              metadata: {
                concurrentRequests: '20'
              }
            }
          }
        ]
      }
    }
  }
}

@description('Fully qualified domain name of the API.')
output fullyQualifiedDomainName string = containerApp.properties.configuration.ingress.fqdn
