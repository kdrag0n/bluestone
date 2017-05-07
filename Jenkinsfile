pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
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