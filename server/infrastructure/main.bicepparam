// Non-secret defaults of the deployment. The image reference, the registry credentials and
// every secret are passed on the command line (see README.md); none are stored here.
using 'main.bicep'

param resourceGroupName = 'rg-leakout'
param location = 'westeurope'
param namePrefix = 'leakout'
param maxReplicas = 3
