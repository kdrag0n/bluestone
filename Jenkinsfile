pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
                sh './gradlew shadowJar'
            }
        }
    }
    post {
        always {
            archive 'build/libs/**/*.jar'
        }
    }
}