pipeline {
	options {
		timeout(time: 60, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}
  agent {
    kubernetes {
      inheritFrom 'fedora-gtk3-mutter-java-node'
      defaultContainer 'jnlp'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: container
    image: docker.io/akurtakov/fedora-gtk3-mutter-java-node:f41-node22
    tty: true
    resources:
      limits:
        memory: "2Gi"
        cpu: "1"
      requests:
        memory: "2Gi"
        cpu: "1"
  - name: jnlp
    volumeMounts:
    - mountPath: /home/jenkins/.ssh
      name: volume-known-hosts
  volumes:
  - configMap:
      name: known-hosts
    name: volume-known-hosts
"""
    }
  }
	environment {
		NPM_CONFIG_USERCONFIG = "$WORKSPACE/.npmrc"
	}
	stages {
		stage('Prepare-environment') {
			steps {
				container('container') {
					sh 'node --version'
					sh 'npm --version'
					sh 'npm config set cache="$WORKSPACE/npm-cache"'
				}
			}
		}
		stage('Build') {
			steps {
				container('container') {
					withCredentials([file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING'),string(credentialsId: 'gpg-passphrase', variable: 'MAVEN_GPG_PASSPHRASE')]) {
					wrap([$class: 'Xvnc', useXauthority: true]) {
						sh 'mvn clean verify -B -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Psign -Dmaven.repo.local=$WORKSPACE/.m2/repository -Dtycho.pgp.signer.bc.secretKeys="${KEYRING}"' 
					}
					}
				}
			}
			post {
				always {
					archiveArtifacts artifacts: 'org.eclipse.shellwax.site/target/,*/target/work/configuration/*.log,*/target/work/data/.metadata/.log,*/target/work/data/languageServers-log/**'
				}
			}
		}
		stage('Deploy') {
			when {
				branch 'master'
			}
			steps {
				sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh 'ssh genie.shellwax@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/shellwax/snapshots'
					sh 'ssh genie.shellwax@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/shellwax/snapshots'
					sh 'scp -r org.eclipse.shellwax.site/target/repository/* genie.shellwax@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/shellwax/snapshots'
				}
			}
		}
	}
}
