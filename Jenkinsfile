pipeline {
    agent {
        kubernetes {
            label 'microservice-build-agent'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: maven:3.8.6-openjdk-11
    command: ['cat']
    tty: true
    resources:
      requests:
        memory: "2Gi"
        cpu: "1"
      limits:
        memory: "4Gi"
        cpu: "2"
    volumeMounts:
    - name: maven-repo
      mountPath: /root/.m2
    - name: docker-sock
      mountPath: /var/run/docker.sock
    - name: kube-config
      mountPath: /root/.kube
      readOnly: true
  - name: docker
    image: docker:20.10.17
    command: ['cat']
    tty: true
    volumeMounts:
    - name: docker-sock
      mountPath: /var/run/docker.sock
  volumes:
  - name: maven-repo
    persistentVolumeClaim:
      claimName: maven-repo-pvc
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
  - name: kube-config
    secret:
      secretName: kube-config-secret
"""
        }
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }
    
    environment {
        REGISTRY = 'registry.example.com'
        APP_NAME = 'microservice-template'
        K8S_NAMESPACE = 'microservices'
        MAVEN_OPTS = '-Xmx2g'
        DOCKER_HOST = 'tcp://localhost:2375'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }
        
        stage('Initialize') {
            steps {
                script {
                    // Set version based on branch
                    if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                        env.BUILD_VERSION = "latest"
                    } else if (env.BRANCH_NAME == 'develop') {
                        env.BUILD_VERSION = "develop"
                    } else {
                        env.BUILD_VERSION = env.BRANCH_NAME.replace('/', '-')
                    }
                    
                    echo "Building version: ${BUILD_VERSION}"
                }
            }
        }
        
        stage('Dependency Check') {
            steps {
                container('maven') {
                    sh 'mvn dependency:go-offline -B'
                }
            }
        }
        
        stage('Build') {
            steps {
                container('maven') {
                    sh 'mvn clean compile -B'
                }
            }
        }
        
        stage('Test') {
            steps {
                container('maven') {
                    sh 'mvn test -B'
                    junit 'target/surefire-reports/*.xml'
                }
            }
            post {
                always {
                    container('maven') {
                        sh 'mvn jacoco:report'
                    }
                    publishHTML(target: [
                        reportName: 'Jacoco Code Coverage',
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        keepAll: true,
                        allowMissing: false
                    ])
                }
            }
        }
        
        stage('Static Analysis') {
            steps {
                container('maven') {
                    sh 'mvn checkstyle:check -B'
                    sh 'mvn pmd:check -B'
                    sh 'mvn spotbugs:check -B'
                }
            }
            post {
                always {
                    publishHTML(target: [
                        reportName: 'CheckStyle Report',
                        reportDir: 'target/checkstyle-result.html',
                        reportFiles: 'checkstyle-result.html',
                        keepAll: true,
                        allowMissing: true
                    ])
                    publishHTML(target: [
                        reportName: 'PMD Report',
                        reportDir: 'target/pmd.html',
                        reportFiles: 'pmd.html',
                        keepAll: true,
                        allowMissing: true
                    ])
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                container('maven') {
                    sh 'mvn org.owasp:dependency-check-maven:check -B'
                    sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar -Dsonar.host.url=${SONAR_URL} -Dsonar.login=${SONAR_TOKEN}'
                }
            }
            post {
                always {
                    dependencyCheckPublisher(pattern: '**/dependency-check-report.xml')
                }
            }
        }
        
        stage('Package') {
            steps {
                container('maven') {
                    sh 'mvn package -DskipTests -B'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                container('docker') {
                    script {
                        docker.withRegistry("https://${REGISTRY}", 'docker-registry-credentials') {
                            def image = docker.build("${APP_NAME}:${BUILD_VERSION}", "-f Dockerfile .")
                            image.push()
                            
                            if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                                image.push('latest')
                            }
                        }
                    }
                }
            }
        }
        
        stage('Integration Test') {
            steps {
                container('maven') {
                    sh '''
                        mvn verify -B -Pintegration-test \
                        -Deureka.client.enabled=false \
                        -Dspring.profiles.active=test
                    '''
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    sh """
                        kubectl set image deployment/${APP_NAME} \
                        ${APP_NAME}=${REGISTRY}/${APP_NAME}:${BUILD_VERSION} \
                        -n ${K8S_NAMESPACE} --record
                        
                        kubectl rollout status deployment/${APP_NAME} \
                        -n ${K8S_NAMESPACE} --timeout=300s
                    """
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                script {
                    sh """
                        kubectl set image deployment/${APP_NAME} \
                        ${APP_NAME}=${REGISTRY}/${APP_NAME}:${BUILD_VERSION} \
                        -n ${K8S_NAMESPACE} --record
                        
                        kubectl rollout status deployment/${APP_NAME} \
                        -n ${K8S_NAMESPACE} --timeout=300s
                    """
                }
            }
        }
        
        stage('Smoke Test') {
            steps {
                script {
                    sh """
                        # Wait for service to be ready
                        sleep 30
                        
                        # Run smoke tests
                        curl -f http://${APP_NAME}.${K8S_NAMESPACE}:8080/actuator/health || exit 1
                        curl -f http://${APP_NAME}.${K8S_NAMESPACE}:8080/api/example/health || exit 1
                    """
                }
            }
        }
        
        stage('Performance Test') {
            when {
                branch 'main'
            }
            steps {
                container('maven') {
                    sh 'mvn gatling:test -B -Pperformance-test'
                }
            }
            post {
                always {
                    publishHTML(target: [
                        reportName: 'Gatling Report',
                        reportDir: 'target/gatling',
                        reportFiles: 'index.html',
                        keepAll: true,
                        allowMissing: true
                    ])
                }
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline completed'
            cleanWs()
        }
        success {
            echo 'Pipeline succeeded'
            // Send success notification
            slackSend(color: 'good', message: "Build ${BUILD_NUMBER} succeeded for ${JOB_NAME}")
        }
        failure {
            echo 'Pipeline failed'
            // Send failure notification
            slackSend(color: 'danger', message: "Build ${BUILD_NUMBER} failed for ${JOB_NAME}")
        }
        unstable {
            echo 'Pipeline unstable'
            slackSend(color: 'warning', message: "Build ${BUILD_NUMBER} unstable for ${JOB_NAME}")
        }
    }
}