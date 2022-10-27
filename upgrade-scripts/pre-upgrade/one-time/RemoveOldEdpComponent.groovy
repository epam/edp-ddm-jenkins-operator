void call() {
    def jenkinsEDPComponentOwner = sh(script: "oc get EDPComponents jenkins -o jsonpath={.metadata.ownerReferences[0].kind} -n $NAMESPACE || true", returnStdout: true)
    if (jenkinsEDPComponentOwner.equals( "Jenkins" )) {
        sh "echo Scale down jenkins-operator..."
        sh "oc scale deploy jenkins-operator --replicas=0 -n $NAMESPACE"
        sh "echo Removing EDP component with deprecated Jenkins route..."
        sh "oc delete EDPComponents jenkins -n $NAMESPACE"
    }
}

return this;
