./mvnw package -Dmaven.test.skip=true

scp -i ~/.ssh/aws-eld-ec2.pem \
    ./target/*.jar \
    ubuntu@54.178.81.72:~/megalab-news/megalab-news-api.jar

ssh -i ~/.ssh/aws-eld-ec2.pem ubuntu@54.178.81.72
