// Azure Database for PostgreSQL Flexible Server on the cheapest burstable tier, with the
// databases of the Players and Scores modules.

@description('Azure region of the server.')
param location string

@description('Name of the PostgreSQL Flexible Server.')
param serverName string

@description('Administrator login of the server.')
param administratorLogin string

@description('Administrator password of the server.')
@secure()
param administratorPassword string

@description('Databases created on the server; the names match the Aspire connection names.')
param databaseNames string[] = [
  'playersdb'
  'scoresdb'
]

resource server 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: serverName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '17'
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorPassword
    authConfig: {
      activeDirectoryAuth: 'Disabled'
      passwordAuth: 'Enabled'
    }
    storage: {
      storageSizeGB: 32
      autoGrow: 'Disabled'
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    network: {
      publicNetworkAccess: 'Enabled'
    }
  }
}

// Consumption Container Apps have no fixed outbound IP, so all Azure services are allowed;
// access stays protected by the password and the required TLS (require_secure_transport is on by default).
resource allowAllAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2024-08-01' = {
  parent: server
  name: 'AllowAllAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

@batchSize(1)
resource databases 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = [
  for databaseName in databaseNames: {
    parent: server
    name: databaseName
    properties: {
      charset: 'UTF8'
      collation: 'en_US.utf8'
    }
  }
]

@description('Fully qualified domain name of the server.')
output fullyQualifiedDomainName string = server.properties.fullyQualifiedDomainName
