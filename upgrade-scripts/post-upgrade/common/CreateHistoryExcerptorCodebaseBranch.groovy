void call() {
    int historyExcerptorBranchStatus = sh(script: "oc get codebasebranch -l app.edp.epam.com/codebaseName=history-excerptor --no-headers -o custom-columns=:metadata.name -n $NAMESPACE | wc -l", returnStdout: true).toInteger()
    if (historyExcerptorBranchStatus < 1 && !"${env.dnsWildcard}".contains('cicd')) {

        // Create codebasebranch CR for history-excerptor codebase (on target only)
        sh "oc apply -f ../one-time/resources/historyExcerptorCodebasebranch.yaml -n $NAMESPACE"
    }
}

return this;
