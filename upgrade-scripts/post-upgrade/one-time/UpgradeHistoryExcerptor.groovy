void call() {
    int historyExcerptorDeployStatus = sh(script: "oc get codebase history-excerptor --no-headers -o custom-columns=:metadata.name -n $NAMESPACE | wc -l", returnStdout: true).toInteger()
    int historyExcerptorBranchStatus = sh(script: "oc get codebasebranch -l app.edp.epam.com/codebaseName=history-excerptor --no-headers -o custom-columns=:metadata.name -n $NAMESPACE | wc -l", returnStdout: true).toInteger()

    if (historyExcerptorDeployStatus > 0 && historyExcerptorBranchStatus < 1) {

        // Define variables
        def JENKINS_ADMIN_USERNAME = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.username} " +
                "-n $NAMESPACE | base64 --decode", returnStdout: true)
        def JENKINS_ADMIN_PASSWORD = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.password} " +
                "-n $NAMESPACE | base64 --decode", returnStdout: true)

        def JENKINS_HOST = sh(script: "oc get route jenkins -o jsonpath={.spec.host} -n $NAMESPACE", returnStdout: true)
        def JENKINS_PATH = sh(script: "oc get route jenkins -o jsonpath={.spec.path} -n $NAMESPACE", returnStdout: true).replaceAll("/\\z", "")
        def JENKINS_URL = "https://${JENKINS_HOST}${JENKINS_PATH}"
        def JENKINS_URL_WITH_CREDS = "https://$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD@${JENKINS_HOST}${JENKINS_PATH}"

        // Define parameters for manually triggering of registry job-provisioner for history-excerptor
        def NAME = "history-excerptor"
        def DEFAULT_BRANCH = sh(script: "oc get codebase $NAME -o jsonpath={.spec.defaultBranch} -n $NAMESPACE",
                returnStdout: true)
        def GIT_CREDENTIALS_ID = sh(script: "oc get gitserver gerrit -o jsonpath={.spec.nameSshKeySecret} " +
                "-n $NAMESPACE", returnStdout: true)
        def GERRIT_PORT = sh(script: "oc get gitserver gerrit -o jsonpath={.spec.sshPort} -n $NAMESPACE", returnStdout: true)
        def REPOSITORY_PATH = "ssh://jenkins@gerrit:$GERRIT_PORT/$NAME"
        def DEPLOYMENT_MODE = sh(script: "helm get values registry-configuration -n $NAMESPACE | grep 'deploymentMode: ' | awk '{print \$2}'", returnStdout: true).trim()

        // Create codebasebranch CR for history-excerptor codebase
        sh "oc apply -f ./resources/historyExcerptorCodebasebranch.yaml -n $NAMESPACE"
        // Create folder with temp name history-excerptor-job for history-excerptor pipeline
        sh "curl -XPOST \"$JENKINS_URL_WITH_CREDS/createItem?name=history-excerptor-job&mode=com.cloudbees.hudson.plugins.folder.Folder&Submit=OK\" " +
                "-H \"Content-Type:application/x-www-form-urlencoded\""

        // Move pipeline into history-excerptor-job folder and rename it to history-excerptor
        sh "curl --user '$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD' " +
                "--data-urlencode \"script=\$(< ./resources/movejob.groovy)\" $JENKINS_URL/scriptText"

        // Manually trigger registry job-provisioner to create Create-release-history-excerptor pipeline
        sh "curl -XPOST \"$JENKINS_URL_WITH_CREDS/job/job-provisions/job/ci/job/registry/buildWithParameters?" +
                "NAME=$NAME&DEFAULT_BRANCH=$DEFAULT_BRANCH&GIT_CREDENTIALS_ID=$GIT_CREDENTIALS_ID&" +
                "GERRIT_PORT=$GERRIT_PORT&REPOSITORY_PATH=$REPOSITORY_PATH&DEPLOYMENT_MODE=$DEPLOYMENT_MODE\""
    }
}

return this;
