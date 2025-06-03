# Secure Kotlin Mobile Application CI/CD

This repository contains a comprehensive CI/CD pipeline for a Kotlin mobile application with a focus on security, designed for the Secure Programming minor.

## Overview

This project implements a secure CI/CD pipeline that integrates various security practices and tools throughout the development lifecycle. The pipeline is designed to detect security issues early and ensure that only secure code is deployed to production.

## CI/CD Pipeline Structure

The pipeline consists of the following stages:

1. **Security Checks**
   - OWASP Dependency Check for finding vulnerable dependencies
   - Detekt static analysis with security rules
   - MobSF Security Analysis for mobile-specific vulnerabilities

2. **Static Code Analysis**
   - Android Lint checks
   - SonarQube analysis

3. **Testing**
   - Unit testing
   - Instrumentation testing
   - Code coverage tracking with JaCoCo

4. **Build**
   - Debug APK build
   - Secure release APK and Bundle build with signing

5. **Deployment**
   - Firebase App Distribution for beta testing

## Security Features

### Secure Code Analysis

- **Detekt** with custom security rules
- **OWASP Dependency Check** for identifying vulnerable dependencies
- **MobSF** mobile security framework integration
- **SonarQube** for continuous code quality and security analysis

### Secure Build Process

- Secured signing process using environment variables
- ProGuard configuration for code obfuscation and minimization
- Removal of logging in release builds
- Minification and shrinking of resources

### Secure Dependencies

- Regular dependency updates enforced
- Known-vulnerable dependencies blocked
- Dependency verification

### Secure Deployment

- Secured keys and credentials using GitHub Secrets
- Secure distribution to testers

## Getting Started

### Prerequisites

- JDK 17 or higher
- Android Studio latest version
- Git

### Setup

1. Clone this repository
2. Configure GitHub Secrets for:
   - `SIGNING_KEY_ALIAS`
   - `SIGNING_KEY_PASSWORD`
   - `SIGNING_STORE_PASSWORD`
   - `ENCODED_KEYSTORE` (base64 encoded keystore file)
   - `FIREBASE_APP_ID`
   - `FIREBASE_SERVICE_ACCOUNT` (JSON content of service account file)
   - `SONAR_TOKEN`

3. Push to the repository to trigger the CI/CD pipeline

## Directory Structure

```
├── .github/workflows/   # GitHub Actions workflow files
├── app/                 # Android application module
├── config/              # Configuration files
│   ├── detekt/          # Detekt static analysis configuration
│   └── owasp/           # OWASP dependency check configuration
└── scripts/             # Security and CI/CD scripts
```

## Security Policy

See the [SECURITY.md](SECURITY.md) file for details on our security policy, including how to report vulnerabilities.

## Contribution Guidelines

1. All code must pass security checks before being merged
2. PRs require security review
3. Security issues must be fixed immediately

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

- OWASP Mobile Security Project
- Android Security Best Practices
