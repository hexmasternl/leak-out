# Spec Delta

## Purpose

Defines where the public website's sources and infrastructure live, and how the website is built, checked and deployed to Azure Static Web Apps.

## ADDED Requirements

### Requirement: Website folder layout
The website's sources SHALL live in `website/src/` and its infrastructure templates in `website/infrastructure/`.

#### Scenario: Layout
- **WHEN** a contributor looks at the repository
- **THEN** the Hugo site is in `website/src/` and the Bicep templates are in `website/infrastructure/`

### Requirement: Infrastructure as Bicep
The Azure resources that host the website, including the Static Web App, SHALL be described in Bicep in `website/infrastructure/` and deployed from there.

#### Scenario: Bicep builds
- **WHEN** the Bicep templates in `website/infrastructure/` are built
- **THEN** the build reports no errors

### Requirement: Deploy on change to main
A GitHub Actions workflow SHALL build the website with Hugo, deploy the infrastructure and deploy the built website to the Azure Static Web App on a push to `main` that changes `website/` or the workflow. Pushes that change neither SHALL NOT deploy the website.

#### Scenario: Website change on main
- **WHEN** a push to `main` changes a file in `website/`
- **THEN** the workflow deploys the infrastructure and the website, and the live website shows the change

#### Scenario: App-only change on main
- **WHEN** a push to `main` changes only files outside `website/`
- **THEN** the website workflow does not deploy

### Requirement: Pull request build check
A pull request that changes `website/` SHALL get a check that builds the site with Hugo and builds the Bicep templates, and that fails when either fails.

#### Scenario: Broken site in a pull request
- **WHEN** a pull request makes the Hugo build fail
- **THEN** the website check fails

### Requirement: No secrets in the repository
The deployment SHALL take its Azure credentials from GitHub secrets or federated credentials; no deployment token or key SHALL be committed.

#### Scenario: Secret scan
- **WHEN** the repository is searched for the Static Web App deployment token
- **THEN** it is not found
