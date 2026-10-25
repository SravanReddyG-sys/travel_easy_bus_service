pipeline {
    agent any

    // Automatically trigger this pipeline when GitHub notifies Jenkins of a new code push
    triggers {
        githubPush()
    }

    stages {
        // Stage 1: Checkout - Pull latest source code from the GitHub repository
        stage('Checkout') {
            steps {
                echo '=== Stage 1: Checking out code from GitHub ==='
                checkout scm
            }
        }

        // Stage 2: Build - Compile the Java Spring Boot application in backend/
        stage('Build') {
            steps {
                echo '=== Stage 2: Compiling Java backend application ==='
                dir('backend') {
                    script {
                        if (isUnix()) {
                            sh 'mvn clean compile'
                        } else {
                            bat 'mvn clean compile'
                        }
                    }
                }
            }
        }

        // Stage 3: Test - Execute unit and integration tests and generate JUnit test reports
        stage('Test') {
            steps {
                echo '=== Stage 3: Running tests ==='
                dir('backend') {
                    script {
                        if (isUnix()) {
                            sh 'mvn test'
                        } else {
                            bat 'mvn test'
                        }
                    }
                }
            }
            post {
                always {
                    echo '=== Reporting: Publishing JUnit test results ==='
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        // Stage 4: Package - Package the tested application into an executable JAR file
        stage('Package') {
            steps {
                echo '=== Stage 4: Packaging application into JAR ==='
                dir('backend') {
                    script {
                        if (isUnix()) {
                            sh 'mvn package -DskipTests'
                        } else {
                            bat 'mvn package -DskipTests'
                        }
                    }
                }
            }
            post {
                success {
                    echo '=== Archiving packaged JAR artifact ==='
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }
    }

    post {
        success {
            echo '==================================================='
            echo ' CI Pipeline Succeeded! All tests passed and packaged.'
            echo '==================================================='
        }
        failure {
            echo '==================================================='
            echo ' CI Pipeline Failed! Please check the build/test logs.'
            echo '==================================================='
        }
    }
}
