node {

    stage 'Checkout'

    git url: 'https://github.com/qwazr/webapps.git'

    stage 'Build' 

    withMaven(maven: 'Maven') {
        sh "mvn -U clean deploy"
    }

}
