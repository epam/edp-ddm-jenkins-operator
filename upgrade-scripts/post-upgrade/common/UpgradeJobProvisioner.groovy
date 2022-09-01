void call() {
    sh "echo Restarting codebase operator..."

    // Restart codebase-operator to trigger Create-release pipelines
    sh "oc scale deployment/codebase-operator --replicas=0 -n $NAMESPACE || true"
    sh "oc scale deployment/codebase-operator --replicas=1 -n $NAMESPACE || true"
}

return this;
