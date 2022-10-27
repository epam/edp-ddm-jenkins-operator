void call() {

        // Retrieve jenkins creds
        def JENKINS_ADMIN_USERNAME = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.username} " +
                "-n $NAMESPACE | base64 --decode", returnStdout: true)
        def JENKINS_ADMIN_PASSWORD = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.password} " +
                "-n $NAMESPACE | base64 --decode", returnStdout: true)

        def JENKINS_HOST = sh(script: "oc get route jenkins -o jsonpath={.spec.host} -n $NAMESPACE", returnStdout: true)
        def JENKINS_PATH = sh(script: "oc get route jenkins -o jsonpath={.spec.path} -n $NAMESPACE", returnStdout: true).replaceAll("/\\z", "")
        def JENKINS_URL_WITH_CREDS = "https://$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD@${JENKINS_HOST}${JENKINS_PATH}"

        // Define parameters for triggering of registry job-provisioner
        def codebasesList = sh(script: "oc get codebases --no-headers -o=custom-columns=NAME:.metadata.name " +
                "-n $NAMESPACE", returnStdout: true).tokenize()
        codebasesList.each { codebase ->
        def DEFAULT_BRANCH = sh(script: "oc get codebase $codebase -o jsonpath={.spec.defaultBranch} -n $NAMESPACE",
                returnStdout: true)
        def GIT_CREDENTIALS_ID = sh(script: "oc get gitserver gerrit -o jsonpath={.spec.nameSshKeySecret} " +
                "-n $NAMESPACE", returnStdout: true)
        def GERRIT_PORT = sh(script: "oc get gitserver gerrit -o jsonpath={.spec.sshPort} -n $NAMESPACE", returnStdout: true)
        def REPOSITORY_PATH = "ssh://jenkins@gerrit:$GERRIT_PORT/$codebase"
        def DEPLOYMENT_MODE = sh(script: "helm get values registry-configuration -n $NAMESPACE | grep 'deploymentMode: ' | awk '{print \$2}'", returnStdout: true).trim()

        // Trigger registry job-provisioner
        sh "curl -XPOST \"$JENKINS_URL_WITH_CREDS/job/job-provisions/job/ci/job/registry/buildWithParameters?" +
                "NAME=$codebase&DEFAULT_BRANCH=$DEFAULT_BRANCH&GIT_CREDENTIALS_ID=$GIT_CREDENTIALS_ID&" +
                "GERRIT_PORT=$GERRIT_PORT&REPOSITORY_PATH=$REPOSITORY_PATH&DEPLOYMENT_MODE=$DEPLOYMENT_MODE\""
        }
}

return this;
