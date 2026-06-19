Appointment Scheduling Module
==========================
[![Build Status](https://travis-ci.org/openmrs/openmrs-module-appointmentscheduling.svg?branch=master)](https://travis-ci.org/openmrs/openmrs-module-appointmentscheduling)

---

## Developer Onboarding (R-13)

This fork is maintained by Liam, Martijn, and Christian as part of a security audit project. The module processes patient PHI and is audited against NEN-7510-2:2024. This section covers everything a new contributor needs to get started.

### Environment Architecture

| Environment | Branch | GitHub Environment | Infrastructure | Purpose |
|---|---|---|---|---|
| Local dev | feature branch | — | Developer machine | Development & unit testing |
| Acceptance | `develop` | `acceptance` | Dedicated acceptance VPS | Integration & acceptance testing |
| Production | `main` | `production` | Separate production VPS | Live system |

Each GitHub Environment has its own isolated set of secrets (`VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`) scoped exclusively to that environment. The acceptance environment can never access production secrets, and vice versa.

### What Prevents Test Data from Reaching Production

- **Separate infrastructure:** acceptance and production run on entirely separate VPS instances with separate databases. There is no shared storage, no shared database, and no network path between them.
- **Secret scoping:** production `VPS_HOST` / `VPS_SSH_KEY` secrets are scoped to the `production` GitHub Environment and are inaccessible during CI runs triggered by PRs or `develop` pushes. A test deployment physically cannot reach the production VPS.
- **Branch gate:** only `develop` merges into `main` (enforced by `workflow-monitor.yml`). No feature branch or hotfix can bypass acceptance.
- **Separate CD jobs:** the CD pipeline deploys to acceptance first; production deployment requires the acceptance job to succeed and uses a different environment block with separate secrets.

### New Developer Setup

1. **Fork and clone**
   ```
   git clone https://github.com/<your-org>/Appointment-Scheduling-Audit.git
   cd Appointment-Scheduling-Audit
   ```

2. **Install prerequisites**
   - Java 8 (the module targets OpenMRS 1.9.x which requires Java 8)
   - Maven 3.x
   - Docker (for running the local OpenMRS stack)

3. **Configure environment variables**
   ```
   cp .env.example .env
   # Fill in values — see .env.example for required keys
   ```

4. **Build the module**
   ```
   mvn package
   ```
   The deployable artifact is produced at `omod/target/*.omod`.

5. **Start the local OpenMRS stack**
   ```
   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
   ```

6. **Deploy the module**
   Because this project is a fork of the default **Bahmni Appointment Scheduling** module (`appointmentschedulingui 2.1.0-SNAPSHOT`), both cannot run simultaneously. First remove the default module:
   OpenMRS admin UI at `http://localhost/openmrs` → **Administration → Manage Modules** → find **Bahmni Appointment Scheduling** → click **Stop**, then **Uninstall**.

   Then upload the build artifact: **Add or Upgrade Module** → select the `.omod` file from `omod/target/` → **Upload**.

7. **Run tests**
   ```
   mvn test
   ```

8. **Open a pull request**
   Push your feature branch and open a PR targeting `develop`. Ensure all CI checks pass before requesting a review (see [Branch and PR Rules](#branch-and-pr-rules)).

### Branch and PR Rules

- `main` is the release branch. Only `develop` may merge into it — no direct pushes, no force pushes.
- `develop` is the integration branch. No direct pushes; all changes arrive via PR.
- Feature branches follow the pattern `feature/<short-description>`.
- Every PR to `develop` requires **1 approving reviewer** and must pass:
  - Maven build + unit tests (`mvn clean install`)
  - CodeQL SAST analysis
  - SonarQube Cloud quality gate
  - GitHub Dependency Review (on PRs)
- The `workflow-monitor.yml` workflow sends a Discord alert whenever `.github/workflows/` files change. Do not modify workflow files without team awareness.

### Security Requirements for Contributors

- **2FA:** two-factor authentication must be enabled on your GitHub account before you can join the organisation. This is enforced at the org level.
- **No secrets in commits:** GitHub Secret Scanning is active on this repository. Committing credentials will trigger an alert and you will be asked to rotate the secret immediately.
- **Do not push directly to `develop` or `main`.** Branch protection rules will reject the push.
- **Pin GitHub Actions to a full SHA digest.** Any new workflow step using a third-party action must use the pinned SHA form (e.g. `uses: actions/checkout@abc1234...`), not a mutable tag like `@v4`.
- **Do not bypass `workflow-monitor.yml`.** Changes to CI/CD workflows must be visible to the team via the Discord channel before merge.

---

## Overview

The Appointment Scheduling Module is for scheduling patient appointments and managing provider schedules. This module also allows for managing the patient queue in a clinic. This README contains information primarily pertinent to developers. If you are a user looking for user related instruction, navigate to the [Wiki page section](#wiki) of this documentation.

<br>

## File Tree

* api/			- This folder contains all Appointment Scheduling API java and test files.
* omod/			- This folder contains all of the module's java and test files.
* .gitattributes	- Lists git attributes that were changed from default.
* .gitignore		- Lists files to be ignored when pushing to git.
* .travis.yml		- Configures Travis CI for automated testing.
* LICENSE.txt		- OpenMRS license agreement.
* OpenMRSFormatter.xml	- OpenMRS formatting file.
* README.md		- Describes the Appointment Scheduling module.
* pom.xml		- Used for building the project with maven.

<br>

## Build Instructions

If your module's file tree is set up correctly [(see section above)](#file-tree), building and packaging the app is a simple process thanks to maven. Navigate to the module's root directory, and run the command `mvn package`. This will package the application according to maven's typical package instructions. The packaged module will be available in /omod/target

<br>

## Wiki

The wiki page for the Appointment Scheduling module contains information more pertinent to users. To view this information, navigate to the following [link](https://wiki.openmrs.org/display/docs/Appointment+Scheduling+Module).
