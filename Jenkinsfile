// Requires a Linux Docker agent and the Docker Pipeline plugin.
pipeline {
    agent { label 'linux && docker' }
    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
    }
    parameters {
        booleanParam(name: 'BUILD_IMAGES', defaultValue: false,
            description: 'Build local Docker images after successful tests; no push or deployment.')
    }
    environment { CI = 'true' }
    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
                script {
                    env.IMAGE_TAG = "${sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()}-${env.BUILD_NUMBER}"
                }
            }
        }
        stage('Java verification') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    reuseNode true
                }
            }
            steps {
                sh 'mvn --batch-mode --no-transfer-progress -Dmaven.repo.local=.m2/repository clean verify'
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml', allowEmptyResults: false
                }
            }
        }
        stage('Python verification') {
            agent {
                docker {
                    image 'python:3.12-slim'
                    reuseNode true
                }
            }
            steps {
                sh 'python -m venv .venv-ci'
                sh '.venv-ci/bin/pip install --disable-pip-version-check -r ai-inference/requirements-dev.txt'
                sh 'mkdir -p reports'
                sh '(cd ai-inference && ../.venv-ci/bin/python -m pytest --junitxml=../reports/ai-inference.xml)'
                sh '.venv-ci/bin/python -m pytest tests --junitxml=reports/backend-scripts.xml'
            }
            post {
                always {
                    junit testResults: 'reports/*.xml', allowEmptyResults: false
                }
            }
        }
        stage('Build Docker images') {
            when { expression { params.BUILD_IMAGES } }
            steps {
                sh '''
                    set -eu
                    for service in discovery-server api-gateway auth-service catalog-service order-service payment-service ai-service notification-service admin-service; do
                        docker build --pull -f "$service/Dockerfile" -t "eshop/$service:$IMAGE_TAG" .
                    done
                    docker build --pull -t "eshop/ai-inference:$IMAGE_TAG" ai-inference
                '''
            }
        }
    }
    post { cleanup { deleteDir() } }
}
