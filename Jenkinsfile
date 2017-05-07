pipeline {
    agent any
    stages {
        stage('Stuff') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh './gradlew shadowJar'
            }
        }
        stage('Ensure Sanity') {
            steps {
                sh 'echo "I\'m fine."'
            }
        }
    }
    post {
        always {
            archive 'build/libs/**/*.jar'
        }
    }
}