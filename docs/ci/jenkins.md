# Jenkins: full frontend and backend usage guide

This guide explains how to configure Jenkins and run the two existing pipelines. Commands in this guide are instructions for your Jenkins administrator; creating the repository files does not install Jenkins or create jobs.

## 1. What you will get

| Repository | Suggested Jenkins job | Build environment | Results |
| --- | --- | --- | --- |
| `Order_Shop_Frontend` | `eshop-frontend` | Node 24 / Alpine | Angular tests, JUnit report, production website archive |
| `E-Shop` | `eshop-backend` | Maven 3.9 / Java 17; Python 3.12 | Java and Python tests, JUnit reports, service JARs |

Each repository has a root `Jenkinsfile`. Both pipelines can optionally build local Docker images. Registry publishing and application deployment require additional environment-specific stages.

The repositories are independent: create one Jenkins job per repository. A frontend run does not trigger the backend job, and a backend run does not trigger the frontend job.

## 2. Values to prepare

Replace examples with your own values. Never paste real passwords or private keys into repository files.

| Value | Example | Where it is used |
| --- | --- | --- |
| Jenkins URL | `https://jenkins.example.com` | Browser, agents, webhooks |
| Frontend Git URL | `https://git.example.com/team/Order_Shop_Frontend.git` | Frontend branch source |
| Backend Git URL | `https://git.example.com/team/E-Shop.git` | Backend branch source |
| Checkout credential ID | `eshop-git-read` | Jenkins source control access |
| Agent name | `eshop-linux-agent` | Jenkins node configuration |
| Agent labels | `linux docker` | Matches `linux && docker` in both Jenkinsfiles |
| Agent directory | `/home/jenkins/agent` | Workspace and agent files |

Use the actual Git branch names discovered from your repositories. This guide does not assume `main` or `master`.

## 3. Install and initialize Jenkins

1. Install Jenkins LTS using the [official Linux installation guide](https://www.jenkins.io/doc/book/installing/linux/). Follow its current Java requirements and package signing instructions.
2. Open your Jenkins URL and complete the initial unlock/setup flow. For a package installation, the unlock password is normally available to the administrator with:

   ```bash
   sudo cat /var/lib/jenkins/secrets/initialAdminPassword
   ```

3. Create an administrator account and set the correct Jenkins URL under **Manage Jenkins → System**. Menu names can vary slightly by release.
4. Use **Manage Jenkins → Plugins** to install the plugins below and restart when requested.
5. Configure a dedicated build agent as described in the next section. Set the controller's built-in node executor count to `0` so these builds run on agents.

The Java runtime that launches Jenkins and its agents must meet your Jenkins release requirements. The backend application compiles inside a Java 17 container; that does not mean Jenkins itself should run on Java 17.

### Required plugins

| Plugin | Purpose |
| --- | --- |
| Pipeline, including Declarative | Reads and executes Jenkinsfiles |
| Git | Checks out repository code |
| Docker Pipeline | Runs build stages inside Docker containers |
| JUnit | Displays test results |
| Timestamper | Adds console timestamps |
| Your Git provider's branch source plugin | Discovers branches and pull/merge requests |

For GitHub, use GitHub Branch Source. For another provider, use the branch source plugin compatible with that provider and your Jenkins version. The generic Docker plugin is separate from Docker Pipeline; install Docker Pipeline for these Jenkinsfiles.

## 4. Prepare the Linux Docker agent

Use a dedicated Linux machine or VM with a writable workspace, Git, Docker CLI, a running Docker daemon, and a Java runtime compatible with your Jenkins controller. Install Docker Engine using the official instructions for your Linux distribution.

The following Ubuntu/Debian example assumes the agent runs as a local account named `jenkins` and Docker is already installed:

```bash
sudo apt-get update
sudo apt-get install -y git curl ca-certificates
sudo install -d -o jenkins -g jenkins /home/jenkins/agent
sudo usermod -aG docker jenkins
```

Restart the agent process or start a new login session after changing group membership. Membership in the Docker group grants powerful host access; use an agent dedicated to trusted build workloads.

Verify access as the agent account:

```bash
sudo -u jenkins -H git --version
sudo -u jenkins -H docker version
sudo -u jenkins -H docker info
sudo -u jenkins -H docker run --rm node:24-alpine node --version
sudo -u jenkins -H docker run --rm maven:3.9-eclipse-temurin-17 mvn --version
sudo -u jenkins -H docker run --rm python:3.12-slim python --version
```

The agent needs outbound access to Git, Docker Hub, npm, Maven repositories and PyPI. Host Node, Maven and Python installations are unnecessary for the containerized Jenkins stages.

Docker Pipeline mounts the agent workspace into build containers. Use a Docker daemon on the same machine, or ensure a remote daemon can access the exact same workspace paths. A controller container with an inaccessible host workspace commonly causes mount failures.

### Register the agent in Jenkins

1. Open **Manage Jenkins → Nodes → New Node**.
2. Name it `eshop-linux-agent` and choose **Permanent Agent**.
3. Set remote root directory to `/home/jenkins/agent`.
4. Start with **1 executor**; increase it only when the agent has sufficient resources.
5. Set **Labels** to `linux docker` and select the option to use this node only for jobs whose label expressions match.
6. Choose an inbound connection method or SSH launch method supported by your installation.
7. For an inbound agent, save and follow the exact launch command shown on that node's page. Use its WebSocket option when appropriate for your network. Keep the generated agent secret private and arrange for the process to start as a service after reboot.
8. Confirm the node is online before running jobs.

See [Using Jenkins agents](https://www.jenkins.io/doc/book/using/using-agents/) for connection setup.

## 5. Add Git credentials

For a public repository, anonymous checkout may be sufficient. For a private repository:

1. Open **Manage Jenkins → Credentials** and choose the credential store/folder used by these jobs.
2. Add **Username with password** for HTTPS; put a provider access token in the password field. Alternatively, add an **SSH Username with private key** credential for an SSH Git URL.
3. Assign an ID such as `eshop-git-read` and grant only the repository access required for checkout and branch discovery.
4. Select this credential when configuring each job's branch source.
5. For SSH, configure host-key verification using your Git server's verified host key.

Provider plugins may require a separate API credential for branch/PR discovery. Follow the plugin's credential requirements when the connection check requests one.

## 6. Publish the pipeline files to Git

Jenkins checks out the remote branch. It cannot see files that exist only on your development computer.

In each repository, review and commit the intended CI changes, then push the branch you want Jenkins to build. Include:

- Root `Jenkinsfile`.
- `docs/ci/jenkins.md`.
- Relevant `.gitignore`, `.dockerignore` and README changes.

Both repositories currently contain unrelated working changes. Review file diffs and stage only the changes you intend to publish. The frontend also requires a committed `package-lock.json` consistent with `package.json` because CI uses `npm ci`.

## 7. Create the frontend job

1. From the Jenkins dashboard select **New Item**.
2. Enter `eshop-frontend` and choose **Multibranch Pipeline**.
3. Under **Branch Sources**, add the source appropriate to your Git provider.
4. Enter the frontend repository URL and select the checkout/discovery credentials.
5. Enable branch discovery. Enable pull/merge request discovery if needed, with trust rules suitable for your repository.
6. Under **Build Configuration**, select the Jenkinsfile-based mode and set **Script Path** to `Jenkinsfile`.
7. Save, then select **Scan Multibranch Pipeline Now**.
8. Open a discovered branch and watch its first build.

A private branch source must be able to read the repository and its root Jenkinsfile. If Jenkins discovers no branches, check the scan log, repository URL, credentials and branch filters.

## 8. Create the backend job

Repeat section 7 using job name `eshop-backend` and the backend repository URL. Keep script path `Jenkinsfile`.

The backend checkout must include the complete multi-module repository, including `pom.xml`, every service module, `common-lib`, `ai-inference` and root `tests`. Checking out an individual service directory is insufficient for this pipeline.

### Alternative: a single-branch Pipeline job

If branch discovery is unnecessary, create a **Pipeline** job instead. Under **Pipeline**, choose **Pipeline script from SCM**, select Git, supply the repository URL and credentials, choose the actual branch, and set script path to `Jenkinsfile`.

Use **Pipeline script from SCM** so `checkout scm` has source-control configuration. The root scripts are not intended to be pasted into an unrelated inline job with no SCM configured.

## 9. Run a build manually

1. Open the frontend or backend job, then the branch you want to build.
2. On the first run, use **Build Now** if parameters are not yet displayed. Jenkins reads the parameter definition from the Jenkinsfile.
3. For subsequent runs choose **Build with Parameters**.
4. Leave `BUILD_IMAGES` unchecked for normal test/build runs.
5. Select **Build** and open its build number.
6. Follow **Console Output** and the stage view. A queued build waiting for `linux && docker` means no matching agent is currently available.

### Frontend stages

| Stage | Actions |
| --- | --- |
| Checkout | Removes the job workspace, checks out Git and calculates the image tag |
| Install, test and build | Runs `npm ci`, Angular tests, production build, then archives the website |
| Build Docker image | Runs only when `BUILD_IMAGES=true` and earlier checks succeed |

### Backend stages

| Stage | Actions |
| --- | --- |
| Checkout | Cleans and checks out the full repository |
| Java verification | Runs Maven `clean verify` across the reactor and archives JARs |
| Python verification | Creates a virtual environment, installs pinned requirements and tests AI inference plus root scripts |
| Build Docker images | Builds nine Java service images and the Python inference image when enabled |

Java verification precedes Python verification. A failed Java stage stops the backend run before the Python tests.

## 10. Find artifacts and test reports

Open a completed build's page:

- **Test Result** displays published JUnit results when the tests generated report files.
- **Console Output** contains dependency, compilation, test and Docker messages.
- **Artifacts** contains `frontend-dist.tar.gz` for the frontend or `**/target/*.jar` for the backend after successful packaging and archiving.

The frontend archive contains the production website files at its root. It can be extracted into a staging directory with:

```bash
mkdir -p frontend-preview
# Run where you downloaded frontend-dist.tar.gz:
tar -xzf frontend-dist.tar.gz -C frontend-preview
```

Serve the result with a web server configured for Angular client-side routes and the application's API endpoints. Extracting an archive alone does not configure an API proxy or deploy the application.

Workspaces are removed when a run finishes, including failed runs. Archived artifacts, console logs and published reports remain according to retention: 20 build records and five builds' artifacts. Download important release artifacts before they expire or copy them to a release artifact store.

## 11. Build Docker images

Run a trusted branch with `BUILD_IMAGES` checked. The pipeline builds only after successful validation and uses:

```text
<12-character-Git-commit>-<Jenkins-build-number>
```

Example image references:

```text
eshop/frontend:0123456789ab-42
eshop/auth-service:0123456789ab-18
eshop/ai-inference:0123456789ab-18
```

Backend images: `discovery-server`, `api-gateway`, `auth-service`, `catalog-service`, `order-service`, `payment-service`, `ai-service`, `notification-service`, `admin-service` and `ai-inference` under the `eshop/` namespace.

Inspect images **on the agent that performed the build**:

```bash
docker image ls --filter 'reference=eshop/*'
```

Images stay in that agent's Docker daemon. They are not automatically transferred to your laptop, a registry, another agent or your running Compose stack. Application Dockerfiles may compile the code again while building images.

The pipelines do not prune Docker images or build caches. Arrange disk retention on dedicated agents. The runtime image tags follow version lines; maintain approved image digests if release reproducibility requires immutable base images.

## 12. Enable automatic builds

For a provider-specific multibranch job:

1. Confirm a manual scan and branch build succeed.
2. Configure the webhook URL and event types documented by the installed Git provider plugin. Use the correct endpoint for that plugin rather than guessing a universal endpoint.
3. Configure the webhook secret where supported, and ensure the Git server can reach the Jenkins endpoint.
4. Push a small change on a branch, then verify webhook delivery and the Jenkins scan/build log.
5. If webhooks cannot reach Jenkins, configure a periodic multibranch scan using the job's available scan trigger settings.

Treat external pull requests as untrusted code. Restrict agent access, credentials and release jobs using your provider plugin's trust configuration. A Jenkinsfile-level parameter is not a security boundary against someone allowed to modify that Jenkinsfile.

## 13. Validate Jenkinsfile syntax

Run the Declarative Pipeline linter against your Jenkins controller so validation uses your installed plugins. From the root of each repository:

```bash
export JENKINS_URL='https://jenkins.example.com'
read -r -p 'Jenkins username: ' JENKINS_USER
# curl prompts for the password: supply a Jenkins API token.
curl --fail-with-body --user "$JENKINS_USER" \
  -F 'jenkinsfile=<Jenkinsfile' \
  "$JENKINS_URL/pipeline-model-converter/validate"
```

Generate an API token from your Jenkins user account settings. Successful validation should report that the Jenkinsfile is valid. Read the response body as well as the HTTP status; syntax errors may be reported in the body. If authentication or CSRF policy rejects the request, follow the controller's policy and the [official linter instructions](https://www.jenkins.io/doc/book/pipeline/development/). Do not disable CSRF protection to bypass the error.

A valid Jenkinsfile still needs a real branch build to verify credentials, mounts, package downloads and application tests.

## 14. Reproduce checks locally

Run these commands from the relevant repository root with the matching runtime versions.

### Frontend — Node 24

```bash
npm ci
mkdir -p reports
npm test -- --reporters=junit --output-file=reports/frontend.xml
npm run build -- --configuration=production
tar -czf frontend-dist.tar.gz -C dist/e-shop/browser .
```

### Backend — Java 17 / Maven 3.9

```bash
java -version
mvn --version
mvn --batch-mode --no-transfer-progress -Dmaven.repo.local=.m2/repository clean verify
```

Check that Maven reports Java 17. Set `JAVA_HOME` to your installed JDK 17 when necessary; the Jenkins container already selects the intended JDK.

### Python — Python 3.12

```bash
python3.12 -m venv .venv-ci
.venv-ci/bin/pip install -r ai-inference/requirements-dev.txt
mkdir -p reports
(cd ai-inference && ../.venv-ci/bin/python -m pytest --junitxml=../reports/ai-inference.xml)
.venv-ci/bin/python -m pytest tests --junitxml=reports/backend-scripts.xml
```

On Ubuntu/Debian, local virtual environments may require the venv package matching your installed Python. The Jenkins Python container includes virtual-environment support.

## 15. Troubleshooting

| Symptom | What to check |
| --- | --- |
| Waiting for an executor / label | Agent is online and has both `linux` and `docker` labels; an executor is free |
| `docker: not found` | Docker CLI is installed on the agent and available to its process |
| Docker socket permission denied | Agent user has daemon access; restart its process after group changes |
| Workspace missing inside container | Docker daemon can mount the same agent workspace paths |
| Permission denied writing workspace/cache | Workspace ownership matches the agent user; check files left by previous root-run containers |
| `Invalid agent type docker` | Install Docker Pipeline |
| Missing `junit` or `timestamps` step | Install JUnit or Timestamper respectively |
| `npm ci` fails | Lockfile is committed and agrees with package.json; check Node version and registry access |
| No JUnit XML files found | Inspect the earlier install/compile failure; tests may not have reached report generation |
| Frontend budget warning | Compare bundle size with angular.json; warnings alone do not fail the current build, exceeding the error budget does |
| Backend compiler uses wrong release | Confirm Java 17 in the build container or local Maven environment |
| Package/image download failure | Agent DNS, network, proxy, registry limits and certificate trust |
| Parameter is missing | Run the branch once so Jenkins loads its Jenkinsfile parameters |
| Docker image absent on your laptop | Inspect the Docker daemon on the build agent or add a registry publishing workflow |
| Build timeout | Inspect the slow stage; the overall limit is 60 minutes |

### Findings from the initial local validation

These findings describe the working tree checked when CI was added; rerun the checks after application changes:

- The frontend production build passed with an initial bundle-size warning.
- Frontend tests were blocked by TypeScript errors in `src/app/features/admin/settings/access-groups.spec.ts`, referring to missing `CustomerFormPage.chosenGroups` and `chosenPermissions`.
- With Java 17 selected, the Maven reactor stopped in auth-service at `RefreshCookieStorageTest.rotationLooksUpHashAndStoresOnlyNewHashLinkedToUser` because `functionPermissionRepository` was null. Later reactor modules were not fully verified by that run.
- The Python 3.12 checks passed: 103 AI inference tests and 7 root script tests, with 3 root tests skipped.
- A Jenkins controller linter and end-to-end Jenkins job run have not been performed from this workspace.

Fix application/test failures before expecting a green pipeline. The Jenkinsfiles retain the test checks rather than skipping failures.

## 16. Publishing and deployment

Before extending these jobs for a release, decide:

1. Registry hostname, image namespace and registry credential ID.
2. Which protected branch or release tag may publish.
3. Staging/production deployment host or cluster and environment configuration.
4. Required runtime secrets, API routing, health checks and rollback target.
5. Who may approve production deployment, if your release process requires approval.

Store credentials in Jenkins Credentials or the deployment secret store. Bind release credentials only in trusted release stages. Keep secrets out of Git, build arguments and frontend assets; browser-delivered assets are public to their users.

A release workflow can then publish the verified image tag, deploy that exact tag, check application health, and restore the previous tag if deployment fails. The current files implement CI and optional local image builds; they do not connect to a registry or deployment environment.

## 17. Official references

- [Jenkins Linux installation](https://www.jenkins.io/doc/book/installing/linux/)
- [Using agents](https://www.jenkins.io/doc/book/using/using-agents/)
- [Declarative Pipeline syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Docker Pipeline](https://www.jenkins.io/doc/book/pipeline/docker/)
- [JUnit reports](https://www.jenkins.io/doc/pipeline/steps/junit/)
- [Pipeline linter](https://www.jenkins.io/doc/book/pipeline/development/)
