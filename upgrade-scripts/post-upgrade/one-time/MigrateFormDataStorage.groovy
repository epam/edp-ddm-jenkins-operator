void call() {
    // Define variables
    String JENKINS_ADMIN_USERNAME = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.username} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_ADMIN_PASSWORD = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.password} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_PATH = sh(script: "oc get route jenkins -o jsonpath={.spec.path} -n $NAMESPACE", returnStdout: true).replaceAll("/\\z", "")
    String JENKINS_URL_WITH_CREDS = "http://$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD@jenkins.${NAMESPACE}.svc:8080$JENKINS_PATH"
    String DEPLOYMENT_MODE = sh(script: "helm get values registry-configuration -n $NAMESPACE | grep 'deploymentMode: ' | awk '{print \$2}'", returnStdout: true).trim()

    String stages = '%5B%7B%22stages%22%3A%20%5B%7B%22name%22%3A%20%22checkout%22%7D%2C%7B%22name%22%3A%20%22init-registry%22%7D%2C%7B%22name%22%3A%20%22form-data-storage-migration%22%7D%5D%7D%5D'
    sh "curl -XPOST \"${JENKINS_URL_WITH_CREDS}/job/registry-regulations/job/MASTER-Build-registry-regulations/buildWithParameters?" +
            "FULL_DEPLOY=true&STAGES=${stages}&DEPLOYMENT_MODE=$DEPLOYMENT_MODE\""
}

return this;
