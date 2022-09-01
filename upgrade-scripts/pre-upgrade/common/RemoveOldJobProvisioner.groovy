void call() {
    sh "echo Removing old job provisioner..."

    sh "oc delete jenkinsscript jenkins-job-provisions-registry -n $NAMESPACE || true"
    sh "oc delete configmap jenkins-job-provisions-registry -n $NAMESPACE || true"
}

return this;
