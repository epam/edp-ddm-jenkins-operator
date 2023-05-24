import groovy.json.*
import jenkins.model.Jenkins

String codebaseName = "${NAME}"
String codebaseBranch = "${BRANCH}"
String defaultBranch = "${DEFAULT_BRANCH}"
String repositoryPath = "${REPOSITORY_PATH}"
String deploymentMode = "${DEPLOYMENT_MODE}"
String cronScheduleForCleanupVersionCandidateDBs = "${CRON_SCHEDULE_FOR_CLEANUP_VC_DB}"
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
        ((deploymentMode.equals("development") ? '[{"name": "create-redash-snippets"},' : '[')) +
        '{"name": "upload-global-vars-changes"},' +
        '{"name": "create-trembita-business-process"},' +
        '{"name": "update-registry-settings"},' +
        '{"name": "update-theme-login-page"},' +
        '{"name": "create-keycloak-roles"},' +
        '{"name": "update-bp-grouping"},' +
        '{"name": "bpms-rollout"},' +
        '{"name": "upload-business-process-changes"},' +
        '{"name": "create-permissions-business-process"},' +
        '{"name": "upload-form-changes"},' +
        '{"name": "create-reports"},' +
        '{"name": "import-excerpts"},' +
        ((deploymentMode.equals("development") ? '{"name": "import-mock-integrations"},' : '')) +
        '{"name": "publish-notification-templates"}]]},' +
        '{"stages": [{"name": "publish-geoserver-configuration"},' +
        '{"name": "run-autotests"}]}' +
        ']'

String deployDataModelStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"}]},' +
        '{"parallelStages": [' +
        '{"name": "create-schema"},' +
        '[{"name": "create-projects"}]]},' +
        '{"stages": [{"name": "delete-data-services"},' +
        '{"name": "clone-projects"},' +
        '{"name": "generate-projects"},' +
        '{"name": "commit-projects"},' +
        '{"name": "build-projects"},' +
        '{"name": "deploy-projects"}]}' +
        ']'

String buildDataComponentStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "build-image-from-dockerfile"}]}' +
        ']'

String codeReviewStagesRegistryRegulations = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"},' +
        '{"name": "create-schema-version-candidate"}]}' +
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
        '{"name": "cleanup-of-version-candidate-dbs"},' +
        '{"name": "delete-data-services"},' +
        '{"name": "cleanup-trigger"}]}' +
        ']'

String cleanupVersionCandidateDBsStages = '[' +
        '{"stages": [{"name": "cleanup-of-version-candidate-dbs"}]}' +
        ']'

String formMigrationStages = '[' +
        '{"stages": [{"name": "checkout"},' +
        '{"name": "init-registry"},' +
        '{"name": "form-data-storage-migration"}]}' +
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
                createReleaseStages, repositoryPath, deploymentMode)
        createCleanUpPipeline("cleanup-job", codebaseName, cleanupStages,
                repositoryPath, codebaseHistoryName, deploymentMode)
        createCleanUpVersionCandidateDBsPipeline("cleanup-of-version-candidate-db", codebaseName, cleanupVersionCandidateDBsStages,
                repositoryPath, codebaseHistoryName, deploymentMode, cronScheduleForCleanupVersionCandidateDBs)
        createFormMigrationPipeline("form-storage-migration", codebaseName, formMigrationStages, repositoryPath, deploymentMode)
        createReleaseDeletePipeline(new String("Delete-release-${codebaseName}"), codebaseName, defaultBranch,
                deleteRegistryStages, repositoryPath, deploymentMode)
        if (codebaseBranch) {
            createCiPipeline(new String("Build-${codebaseName}"), codebaseName, codebaseBranch,
                    deployRegistryRegulationsStages, repositoryPath, deploymentMode)
            createCiPipeline(new String("Build-${codebaseName}-data-model"), codebaseName, codebaseBranch,
                    deployDataModelStages, repositoryPath, deploymentMode, false)
            createCiPipeline(new String("Code-review-${codebaseName}"), codebaseName, codebaseBranch,
                    codeReviewStagesRegistryRegulations, repositoryPath, deploymentMode)
        }
        break
    case "history-excerptor":
        createFolder(codebaseName)
        createReleaseDeletePipeline(new String("Create-release-${codebaseName}"), codebaseName, defaultBranch,
                createReleaseStages, repositoryPath, deploymentMode)
        createReleaseDeletePipeline(new String("Delete-release-${codebaseName}"), codebaseName, defaultBranch,
                deleteServiceStages, repositoryPath, deploymentMode)
        createHistoryExcerptorPipeline("history-excerptor", codebaseName,
                historyExcerptorStages, repositoryPath, deploymentMode)
        break
    default:
        createFolder(codebaseName)
        createReleaseDeletePipeline(new String("Create-release-${codebaseName}"), codebaseName, defaultBranch,
                createReleaseStages, repositoryPath, deploymentMode)
        if (codebaseBranch)
            createCiPipeline(new String("Build-${codebaseName}"), codebaseName, codebaseBranch,
                    buildDataComponentStages, repositoryPath, deploymentMode)
}


void createCiPipeline(String pipelineName, String codebaseName, String codebaseBranch, String stages,
                      String repositoryPath, String deploymentMode, boolean trigger = true) {
    pipelineJob("${codebaseName}/${codebaseBranch.toUpperCase().replaceAll(/\//, "-")}-${pipelineName}") {
        concurrentBuild(false)
        logRotator {
            numToKeep(10)
        }
        if (trigger) {
            triggers {
                gerrit {
                    events {
                        if (pipelineName.contains("Code-review"))
                            patchsetCreated()
                        else
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
                if (codebaseName == "registry-regulations" && !pipelineName.contains("data-model") && !pipelineName.contains("Code-review") )
                    booleanParam("FULL_DEPLOY", false)
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("CODEBASE_BRANCH", codebaseBranch)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
                stringParam("DEPLOYMENT_MODE", deploymentMode)
            }

        }
    }
}

void createReleaseDeletePipeline(String pipelineName, String codebaseName, String defaultBranch,
                                 String stages, String repositoryPath, String deploymentMode) {
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
                stringParam("DEPLOYMENT_MODE", deploymentMode)
            }

        }
    }
}

void createFormMigrationPipeline(String pipelineName, String codebaseName, String stages,
                                 String repositoryPath, String deploymentMode) {
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
                booleanParam("DELETE_INVALID_DATA", false)
                booleanParam("DELETE_AFTER_MIGRATION", false)
                stringParam("ADDITIONAL_KEY_PATTERNS", "")
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
                stringParam("DEPLOYMENT_MODE", deploymentMode)
            }
        }
    }
}

void createCleanUpPipeline(String pipelineName, String codebaseName, String stages,
                           String repositoryPath, String codebaseHistoryName, String deploymentMode) {
    pipelineJob(pipelineName) {
        concurrentBuild(false)
        logRotator {
            numToKeep(10)
        }
        definition {
            cps {
                script("@Library(['edp-library-pipelines']) _ \n\nCleanup()")
                sandbox(true)
            }
            parameters {
                booleanParam("DELETE_REGISTRY_REGULATIONS_GERRIT_REPOSITORY", false, "If checked, registry-regulations repository will be recreated from empty template")
                stringParam("STAGES", stages)
                stringParam("CODEBASE_NAME", codebaseName)
                stringParam("CODEBASE_HISTORY_NAME", codebaseHistoryName)
                stringParam("REPOSITORY_PATH", repositoryPath)
                stringParam("LOG_LEVEL", "INFO", "ERROR, WARN, INFO or DEBUG")
                stringParam("DEPLOYMENT_MODE", deploymentMode)
            }

        }
    }
}

void createCleanUpVersionCandidateDBsPipeline(String pipelineName, String codebaseName, String stages,
                                              String repositoryPath, String codebaseHistoryName, String deploymentMode, String cronSchedule) {
    pipelineJob(pipelineName) {
        concurrentBuild(false)
        logRotator {
            numToKeep(10)
        }
        triggers {
            cron(cronSchedule)
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
                stringParam("DEPLOYMENT_MODE", deploymentMode)
            }

        }
    }
}

void createHistoryExcerptorPipeline(String pipelineName, String codebaseName, String stages,
                                    String repositoryPath, String deploymentMode) {
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
                stringParam("DEPLOYMENT_MODE", deploymentMode)
            }

        }
    }
}

void createFolder(String folderName) {
    Jenkins jenkins = Jenkins.instance
    if (!jenkins.getItem(folderName))
        folder(folderName)
}
