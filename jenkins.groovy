pipeline
{
   agent { label 'linux' }

   options
   {
      buildDiscarder logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '100', daysToKeepStr: '30', numToKeepStr: '100')
      timestamps()
   }

   tools
   {
     go 'Go'
   }

   stages
   {
      stage('Build')
      {
         steps {
            git 'https://github.com/Graylog2/graylog-project-cli.git'

            sh "make"
         }

         post
         {
            success
            {
               archiveArtifacts '*.linux'
               archiveArtifacts '*.darwin'
            }
         }
      }

      stage('Release')
      {

         when
         {
             buildingTag()
         }

         environment
         {
             GITHUB_CREDS = credentials('github-access-token')
         }

         steps
         {
           echo "Releasing ${TAG_NAME} to Github..."

           script
           {
             def RELEASE_DATA = sh returnStdout: true, script: "curl -s --user \"$GITHUB_CREDS\" -X POST --data \'{ \"tag_name\": \"${TAG_NAME}\", \"name\": \"${TAG_NAME}\", \"body\": \"Insert features here.\", \"draft\": true }\' https://api.github.com/repos/Graylog2/graylog-project-cli/releases"
             def props = readJSON text: RELEASE_DATA
             env.RELEASE_ID = props.id

             sh 'curl -H "Authorization: token $GITHUB_CREDS" -H "Content-Type: application/octet-stream" --data-binary @graylog-project.linux https://uploads.github.com/repos/Graylog2/graylog-project-cli/releases/$RELEASE_ID/assets?name=graylog-project.linux'
             sh 'curl -H "Authorization: token $GITHUB_CREDS" -H "Content-Type: application/octet-stream" --data-binary @graylog-project.darwin https://uploads.github.com/repos/Graylog2/graylog-project-cli/releases/$RELEASE_ID/assets?name=graylog-project.darwin'
           }
         }
      }
   }
}
