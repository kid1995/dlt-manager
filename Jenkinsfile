#!groovy
@Library('si-dp-shared-libs')
import de.signaliduna.TargetSegment

SERVICE_GROUP = "elpa"
PROJECT_ROOT_FOLDER = "./"
SERVICE_BACKEND_NAME = "dltmanager"
APP_BACKEND_NAME = "dltmanager"
APP_BACKEND_FOLDER = "backend/"
JAVA_VERSION = "21"

APP_FRONTEND_NAME = "dltmanager-ui"
APP_FRONTEND_FOLDER = "frontend"

DB_VERSION = "6.0"
DB_BACKUP_COUNT = 3
DB_BACKUP_SCHEDULE = "0 2 * * *"
DB_SIZE = "S"

BACKEND_DEPLOYMENT_PARAMS = [
        (TargetSegment.tst): [
                'NUM_REPLICAS': 1, // affects quotas
                'REQUESTS_CPU': '10m', // affects quotas
                'REQUESTS_MEMORY': '200Mi', // affects quotas
                'LIMITS_CPU': '4000m',
                'LIMITS_MEMORY': '500Mi',
        ],
        (TargetSegment.abn): [
                'NUM_REPLICAS': 1, // affects quotas
                'REQUESTS_CPU': '10m', // affects quotas
                'REQUESTS_MEMORY': '200Mi', // affects quotas
                'LIMITS_CPU': '4000m',
                'LIMITS_MEMORY': '500Mi',
        ],
        (TargetSegment.prd): [
                'NUM_REPLICAS': 1, // affects quotas
                'REQUESTS_CPU': '10m', // affects quotas
                'REQUESTS_MEMORY': '200Mi', // affects quotas
                'LIMITS_CPU': '4000m',
                'LIMITS_MEMORY': '500Mi',
        ]
]

FRONTEND_DEPLOYMENT_PARAMS = [
        (TargetSegment.tst): [
                'NUM_REPLICAS'   : 1, // affects quotas
                'REQUESTS_CPU'   : '10m', // affects quotas
                'REQUESTS_MEMORY': '5Mi', // affects quotas
                'LIMITS_CPU'     : '100m',
                'LIMITS_MEMORY'  : '100Mi',
        ],
        (TargetSegment.abn): [
                'NUM_REPLICAS'   : 1, // affects quotas
                'REQUESTS_CPU'   : '10m', // affects quotas
                'REQUESTS_MEMORY': '5Mi', // affects quotas
                'LIMITS_CPU'     : '100m',
                'LIMITS_MEMORY'  : '100Mi',
        ],
        (TargetSegment.prd): [
                'NUM_REPLICAS'   : 1, // affects quotas
                'REQUESTS_CPU'   : '10m', // affects quotas
                'REQUESTS_MEMORY': '5Mi', // affects quotas
                'LIMITS_CPU'     : '100m',
                'LIMITS_MEMORY'  : '100Mi',
        ]
]

//@formatter:on

node {
    properties([
        pipelineTriggers(createNightlyBuildTriggers()),
				parameters([
					booleanParam(name: 'Release', defaultValue: false, description: 'Release the libs 🚀')
				])
    ])

    si_jenkins.notify createNotifyConfig(), {
        //do not deploy any branches created by the renovate bot
        //Build and verify
        si_git.checkoutBranch(env.BRANCH_NAME, createNotifyConfig())
        si_java.version("21")
         // NOTE: Keep in sync with .nvmrc
				si_npm.node_version('22.14.0')

        def projectVersion = readProjectVersion()
        echo "Deploying Version: $projectVersion"

        if (si_git.isMaster()) {
            if (projectVersion.contains("SNAPSHOT")) {
                error('Found SNAPSHOT version on master. On master please deploy stable version only.')
            }
        }

        stage("Build") {
            buildAndRefreshDependenciesForLibrary(PROJECT_ROOT_FOLDER, JAVA_VERSION)

						si_npm.ciInstall(APP_FRONTEND_FOLDER)

						//the `prd` argument is mapped to file `environment.prod.ts` which may contain placeholder values
						//that can be replaced by environment specific env.-vars when the Docker container is started
						//(e.g. value `NOT_SET_BACKEND_URL` will be replaced by env.-var `BACKEND_URL`).
						//Due to the this mechanism `environment.prod.ts` can be used for all environments (not only for `prd` but also for `tst` and `abn`)
						si_npm.ciBuildProject("prd", APP_FRONTEND_FOLDER)
        }
        stage("Verify") {
            si_java.check(PROJECT_ROOT_FOLDER, 10, 10, 10)

						si_npm.ciTest(APP_FRONTEND_FOLDER)

						try {
								si_npm.ciE2E(APP_FRONTEND_FOLDER)
						} catch (err) {
								// defaultValue of screenshotsFolder is defined at this link
								// https://docs.cypress.io/app/references/configuration#Screenshots
								archiveArtifacts artifacts: "${APP_FRONTEND_FOLDER}/cypress/screenshots/**/*.png", allowEmptyArchive: 'true'
								error(err)
						}
        }
        stage("Analyze") {
            si_java.staticAnalysis(PROJECT_ROOT_FOLDER)

						si_npm.ciAudit(APP_FRONTEND_FOLDER)
						si_npm.ciLint(APP_FRONTEND_FOLDER)
        }

				if (!params.Release) {
					stage("Publish Libraries") {
						publish(PROJECT_ROOT_FOLDER, JAVA_VERSION)
					}
				} else if (si_git.isDevelop()) {
					// Removes the "-SNAPSHOT" suffix from the `version` property in `gradle.properties` (if so), commits & pushes the change.
					// The publishing is done via step 'Publish Libraries' in a subsequent run of this job (which is triggered by `git push`).
					stage("Set release version") {
						setReleaseVersion(PROJECT_ROOT_FOLDER, JAVA_VERSION)
					}

					// Increments the patch version of the `version` property in `gradle.properties`, adds "-SNAPSHOT" suffix commits & pushes the change.
					// The publishing is done via step 'Publish Libraries' in a subsequent run of this job (which is triggered by `git push`).
					stage("Set new snapshot version") {
						setSnapshotVersion(PROJECT_ROOT_FOLDER, JAVA_VERSION)
					}
				}

        if (si_git.isMaster()) {
						deployPROD()
        } else {
            buildDeploy(TargetSegment.tst)
            if (si_git.isDevelop()) {
                buildDeploy(TargetSegment.abn)
            }
        }
    }
}

String readProjectVersion() {
    String version = 'Unknown-SNAPSHOT'
    dir(APP_BACKEND_FOLDER) {
        script {
            version = sh(returnStdout: true, script: """
                            . use-jdk-21
                            ./gradlew properties -q | grep "version:" | awk '{print \$2}'
                            """)
        }
    }
    return version.trim()
}

def createNightlyBuildTriggers() {
    if (si_git.isDevelop()) {
        return [cron('H H(21-22) * * 1-5')]
    } else {
        return []
    }
}

def createNotifyConfig() {
    if (si_git.isDevelop() || si_git.isMaster()) {
        return [
                Bitbucket : true,
                RocketChat: [
                        Channel: '#elpa-ae'
                ]
        ];
    } else {
        return [Bitbucket: true];
    }
}

void buildAndRefreshDependenciesForLibrary(String appBackendFolder, String javaVersion) {
	String versionScript = "use-jdk-" + javaVersion
	dir(appBackendFolder) {
		sh """
			. ${versionScript}
			./gradlew --refresh-dependencies build -x check
			./gradlew installDist
		"""
	}
}

void publish(String appBackendFolder, String javaVersion) {
	withCredentials([usernamePassword(credentialsId: 'jenkins_m2repo', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USERNAME')]) {
		dir(appBackendFolder) {
			sh '''
				. use-jdk-''' + javaVersion + '''
				./gradlew publishAllPublicationsToSILocalRepository -PNEXUS_USER=${NEXUS_USERNAME} -PNEXUS_PASSWORD=${NEXUS_PASSWORD}
			'''
		}
	}
}

void setReleaseVersion(String appBackendFolder, String javaVersion) {
	dir(appBackendFolder) {
		sh '''
			. use-jdk-''' + javaVersion + '''
			./gradlew removeSnapshot
			LIB_VERSION=$(./gradlew -q printVersion)
			git add gradle.properties
			git commit -m "Set release version ${LIB_VERSION}"
			git tag "v${LIB_VERSION}"
			git push
			git push origin "v${LIB_VERSION}"
		'''
	}
}

void setSnapshotVersion(String appBackendFolder, String javaVersion) {
	dir(appBackendFolder) {
		sh '''
			. use-jdk-''' + javaVersion + '''
			./gradlew increasePatchVersion
			./gradlew addSnapshot
			LIB_VERSION=$(./gradlew -q printVersion)
			git add gradle.properties
			git commit -m "Set snapshot version ${LIB_VERSION}"
			git push
		'''
	}
}

private void deployPROD() {
    stage("Build container image for PROD") {
    		buildContainers(TargetSegment.abn)
    }
    stage("Workaround deploy ABN so PRD can get deployed") {
      deployApplications(TargetSegment.abn);
    }
    stage("Deploy to PRD") {
        si_docker.publishImageAbnToPrd(SERVICE_GROUP, SERVICE_BACKEND_NAME, APP_BACKEND_NAME)
        si_docker.publishImageAbnToPrd(SERVICE_GROUP, SERVICE_BACKEND_NAME, APP_FRONTEND_NAME)
        si_its360.notifyReleaseDeployment(SERVICE_GROUP, SERVICE_BACKEND_NAME, {
            deployApplications(TargetSegment.prd)
        })
    }
}

private void buildDeploy(TargetSegment targetSegment) {
    stage("Build container for $targetSegment") {
    		buildContainers(targetSegment)
    }
    stage("Deploy to $targetSegment") {
        deployApplications(targetSegment)
    }
}

private void buildContainers(TargetSegment targetSegment) {
		si_docker.buildImage(APP_BACKEND_FOLDER, SERVICE_GROUP, SERVICE_BACKEND_NAME, APP_BACKEND_NAME, targetSegment)
		si_docker.buildImage(APP_FRONTEND_FOLDER, SERVICE_GROUP, APP_BACKEND_NAME, APP_FRONTEND_NAME, targetSegment)
}

void deployApplications(TargetSegment targetSegment) {
    echo "deploying application $APP_BACKEND_NAME to $targetSegment"
		si_mongodb.deployDB(SERVICE_GROUP, SERVICE_BACKEND_NAME, targetSegment, DB_VERSION, DB_BACKUP_COUNT, DB_BACKUP_SCHEDULE, DB_SIZE)
    withCredentials([
            usernamePassword(credentialsId: "elpa-technical-user-password-$targetSegment", passwordVariable: 'AUTH_PASSWORD', usernameVariable: 'USERNAME')
    ]) {
    		def shortenedBackendURL = shortenUrl(targetSegment, SERVICE_BACKEND_NAME)

    		def shortenedFrontendURL = shortenUrl(targetSegment, APP_FRONTEND_NAME);
			  def frontendServiceUrls = ['BACKEND_URL': "https://$shortenedBackendURL", 'BASE_ROUTE_URL': shortenedFrontendURL]
				def frontendDeploymentParams = FRONTEND_DEPLOYMENT_PARAMS[targetSegment] + frontendServiceUrls

        si_openshift.deployApplication(SERVICE_GROUP, SERVICE_BACKEND_NAME, APP_BACKEND_NAME, targetSegment,
        	BACKEND_DEPLOYMENT_PARAMS[targetSegment]+[SECRET_FILE_NAME: SERVICE_GROUP + "-secrets", 'AUTH_PASSWORD' : AUTH_PASSWORD, 'BASE_ROUTE_URL': shortenedBackendURL])

				si_openshift.deployApplication(SERVICE_GROUP, SERVICE_BACKEND_NAME, APP_FRONTEND_NAME, targetSegment, frontendDeploymentParams)
    }

    echo "finished deploying application $APP_BACKEND_NAME to $targetSegment"
}

private String shortenUrl(TargetSegment targetSegment, String appName) {
    String projectUrl = si_openshift.getProjectUrl(SERVICE_GROUP, appName, targetSegment)
    String branchName;
    if (si_git.isDevelop() || si_git.isMaster()) {
        branchName = targetSegment;
    } else {
        branchName = si_git.branchName().toLowerCase()
                .replaceAll('^.*/', '') // cut prefixes, e.g. "feature/"
                .replaceAll('[^a-z0-9-]', '-') // replace special characters
        branchName = branchName.substring(0, Math.min(branchName.length(), 15))
    }
    return "${branchName}-${appName}-${projectUrl}"
}
