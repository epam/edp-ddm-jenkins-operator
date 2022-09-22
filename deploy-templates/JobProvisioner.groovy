import groovy.json.*
import jenkins.model.Jenkins

String codebaseName = "${NAME}"
String codebaseBranch = "${BRANCH}"
String defaultBranch = "${DEFAULT_BRANCH}"
String repositoryPath = "${REPOSITORY_PATH}"
String codebaseHistoryName = "history-excerptor"

String deployRegistryRegulationsStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"},' +
        '{"name": "get-changes"},' +
        '{"name": "registry-regulations-validation"},' +
        '{"name": "shutdown-services"},' +
        '{"name": "create-backup"},' +
        '{"name": "create-redash-roles"}]},' +
        '{"parallelStages": [{"name": "deploy-data-model"},' +
        '[{"name": "create-redash-snippets"},' +
        '{"name": "upload-global-vars-changes"},' +
        '{"name": "create-trembita-business-process"},' +
        '{"name": "update-registry-settings"},' +
        '{"name": "update-theme-login-page"},' +
        '{"name": "create-keycloak-roles"},' +
        '{"name": "bpms-rollout"},' +
        '{"name": "upload-business-process-changes"},' +
        '{"name": "create-permissions-business-process"},' +
        '{"name": "upload-form-changes"},' +
        '{"name": "create-reports"},' +
        '{"name": "import-excerpts"},' +
        '{"name": "publish-notification-templates"}]]},' +
        '{"stages": [{"name": "publish-geoserver-configuration"},' +
        '{"name": "run-autotests"}]}' +
        ']'

String deployDataModelStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"}]},' +
        '{"parallelStages": [' +
        '{"name": "create-schema"},' +
        '[{"name": "create-projects"},' +
        '{"name": "create-pipelines"},' +
        '{"name": "clone-projects"}]]},' +
        '{"stages": [{"name": "generate-projects"},' +
        '{"name": "commit-projects"},' +
        '{"name": "build-projects"},' +
        '{"name": "deploy-projects"}]}' +
        ']'

String buildDataComponentStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "build-image-from-dockerfile"}]}' +
        ']'

String createReleaseStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "create-branch"},' +
        '{"name": "trigger-job"}]}' +
        ']'

String deleteRegistryStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"},' +
        '{"name": "delete-registry"}]}' +
        ']'

String deleteServiceStages = '[' +
        '{"stages": [{"name": "delete-services"}]}' +
        ']'

String cleanupStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"},' +
        '{"name": "cleanup-trigger"}]}' +
        ']'

String historyExcerptorStages = '[' +
        '{"stages": [{"name": "data-validation"},' +
        '{"name": "checkout"},' +
        '{"name": "create-excerptor-job"},' +
        '{"name": "get-history-report"}]}' +
        ']'

switch (codebaseName) {

    case "registry-regulations":
        createFolder(codebaseName)
        createReleaseDeletePipeline(new String("Create-release-${codebaseName}"), codebaseName, defaultBranch,
                createReleaseStages, repositoryPath)
        createCleanUpPipeline("cleanup-job", codebaseName, cleanupStages,
                repositoryPath, codebaseHistoryName)
        createReleaseDeletePipeline(new String("Delete-release-${codebaseName}"), codebaseName, defaultBranch,
                deleteRegistryStages, repositoryPath)
        if (codebaseBranch) {
            createCiPipeline(new String("Build-${codebaseName}"), codebaseName, codebaseBranch,
                    deployRegistryRegulationsStages, repositoryPath)
            createCiPipeline(new String("Build-${codebaseName}-data-model"), codebaseName, codebaseBranch,
                    deployDataModelStages, repositoryPath, false)
        }
        break
    case "history-excerptor":
        createFolder(codebaseName)
        createReleaseDeletePipeline(new String("Create-release-${codebaseName}"), codebaseName, defaultBranch,
                createReleaseStages, repositoryPath)
        createReleaseDeletePipeline(new String("Delete-release-${codebaseName}"), codebaseName, defaultBranch,
                deleteServiceStages, repositoryPath)
        createHistoryExcerptorPipeline("history-excerptor", codebaseName,
                historyExcerptorStages, repositoryPath)
        break
    default:
        createFolder(codebaseName)
        createReleaseDeletePipeline(new String("Create-release-${codebaseName}"), codebaseName, defaultBranch,
                createReleaseStages, repositoryPath)
        createReleaseDeletePipeline(new String("Delete-release-${codebaseName}"), codebaseName, defaultBranch,
                deleteServiceStages, repositoryPath)
        if (codebaseBranch)
            createCiPipeline(new String("Build-${codebaseName}"), codebaseName, codebaseBranch,
                    buildDataComponentStages, repositoryPath)
}


void createCiPipeline(String pipelineName, String codebaseName, String codebaseBranch, String stages,
                      String repositoryPath, boolean trigger = true) {
    pipelineJob("${codebaseName}/${codebaseBranch.toUpperCase().replaceAll(/\//, "-")}-${pipelineName}") {
        concurrentBuild(false)
        logRotator {
            numToKeep(10)
        }
        if (trigger) {
            triggers {
                gerrit {
                    events {
                        changeMerged()
                    }
                    project("plain:${codebaseName}", ["plain:${codebaseBranch}"])
                }
            }
        }
        definition {
            cps {
                script("@Library(['edp-library-pipelines']) _ \n\nBuild()")
                sandbox(true)
            }
            parameters {
                if (codebaseName == "registry-regulations" && !pipelineName.contains("data-model"))
                    booleanParam("FULL_DEPLOY", false)
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("CODEBASE_BRANCH", codebaseBranch)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
            }

        }
    }
}

void createReleaseDeletePipeline(String pipelineName, String codebaseName, String defaultBranch,
                                 String stages, String repositoryPath) {
    pipelineJob("${codebaseName}/${pipelineName}") {
        concurrentBuild(false)
        logRotator {
            numToKeep(20)
        }
        definition {
            cps {
                script("@Library(['edp-library-pipelines']) _ \n\nBuild()")
                sandbox(true)
            }
            parameters {
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("BRANCH", defaultBranch)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
            }

        }
    }
}

void createCleanUpPipeline(String pipelineName, String codebaseName, String stages,
                           String repositoryPath, String codebaseHistoryName) {
    pipelineJob(pipelineName) {
        concurrentBuild(false)
        logRotator {
            numToKeep(10)
        }
        definition {
            cps {
                script("@Library(['edp-library-pipelines']) _ \n\nBuild()")
                sandbox(true)
            }
            parameters {
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("CODEBASE_HISTORY_NAME", codebaseHistoryName)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
            }

        }
    }
}

void createHistoryExcerptorPipeline(String pipelineName, String codebaseName, String stages,
                                    String repositoryPath) {
    pipelineJob("${codebaseName}/${pipelineName}") {
        concurrentBuild(false)
        logRotator {
            numToKeep(10)
        }
        definition {
            cps {
                script("@Library(['edp-library-pipelines']) _ \n\nBuild()")
                sandbox(true)
            }
            parameters {
                stringParam("NAME_OF_TABLE", "", "Enter table name")
                stringParam("ID", "", "Enter UUID")
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
            }

        }
    }
}

void createFolder(String folderName) {
    Jenkins jenkins = Jenkins.instance
    if (!jenkins.getItem(folderName))
        folder(folderName)
}
